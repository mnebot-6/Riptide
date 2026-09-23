package com.mnebot.riptide.data.sync

import com.mnebot.riptide.data.local.db.RiptideDatabase
import com.mnebot.riptide.data.local.nowIso
import com.mnebot.riptide.data.remote.RiptideApi
import com.mnebot.riptide.data.remote.dto.*
import com.mnebot.riptide.domain.model.SyncStatus
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "RiptideSync"

/**
 * Orchestrates bidirectional sync between the local Room database and the remote API.
 * Strategy: push dirty local entities, pull server changes, apply with conflict resolution.
 */
class SyncManager(
    private val db: RiptideDatabase,
    private val api: RiptideApi,
    private val userPrefs: UserPreferencesRepository,
    /** Se invoca tras aplicar cambios del servidor (refrescar widget, etc). */
    private val onServerChangesApplied: (suspend () -> Unit)? = null
) {

    sealed class SyncResult {
        data object Success : SyncResult()
        data object NotLoggedIn : SyncResult()
        data class Error(val message: String) : SyncResult()
    }

    private val _status = MutableStateFlow(SyncStatus.IDLE)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _lastSyncMillis = MutableStateFlow<Long?>(null)
    val lastSyncMillis: StateFlow<Long?> = _lastSyncMillis.asStateFlow()

    private var lastSyncAttempt: Long = 0
    private companion object {
        const val MIN_SYNC_INTERVAL_MS = 30_000L
    }

    suspend fun sync(force: Boolean = false): SyncResult {
        if (userPrefs.getAccessToken() == null) return SyncResult.NotLoggedIn
        if (userPrefs.hasPendingInitialSync()) return SyncResult.NotLoggedIn

        val now = System.currentTimeMillis()
        if (!force && now - lastSyncAttempt < MIN_SYNC_INTERVAL_MS) return SyncResult.Success
        lastSyncAttempt = now

        _status.value = SyncStatus.SYNCING
        val startMs = System.currentTimeMillis()
        Log.i(TAG, "Sync start (force=$force)")
        return try {
            // "" significa "nunca sincronizado": el servidor no puede parsearlo como fecha.
            val lastSync = userPrefs.getLastSyncTime()?.takeIf { it.isNotBlank() }
            val since = lastSync ?: ""
            // Corte tomado ANTES de leer nada. La marca de agua es local, no la del
            // servidor: si se guardase serverTime, todo lo que el usuario tocase durante
            // el viaje de ida y vuelta caería fuera de la ventana y no se subiría jamás.
            val cutoff = nowIso()
            val pushBlocks = db.workBlockDao().getModifiedSince(since).map { it.toDto() }
            val pushCats = db.blockCategoryDao().getModifiedSince(since).map { it.toDto() }
            val pushTasks = db.dayTaskDao().getModifiedSince(since).map { it.toDto() }
            val pushDefs = db.recurringTaskDefDao().getModifiedSince(since).map { it.toDto() }
            val pushSummaries = db.daySummaryDao().getModifiedSince(since).map { it.toDto() }
            val pushEco = db.ecosystemStateDao().getModifiedSince(since).map { it.toDto() }
            val pushCreatures = db.marineCreatureDao().getModifiedSince(since).map { it.toDto() }
            val pushTotal = pushBlocks.size + pushCats.size + pushTasks.size + pushDefs.size +
                pushSummaries.size + pushEco.size + pushCreatures.size
            Log.i(
                TAG,
                "Push: total=$pushTotal blocks=${pushBlocks.size} cats=${pushCats.size} " +
                    "tasks=${pushTasks.size} defs=${pushDefs.size} summaries=${pushSummaries.size} " +
                    "eco=${pushEco.size} creatures=${pushCreatures.size}"
            )
            val request = SyncRequest(
                lastSyncTime = lastSync,
                workBlocks = pushBlocks,
                blockCategories = pushCats,
                dayTasks = pushTasks,
                recurringTaskDefs = pushDefs,
                daySummaries = pushSummaries,
                ecosystemStates = pushEco,
                marineCreatures = pushCreatures
            )

            val response = api.sync(request)
            val pullTotal = response.workBlocks.size + response.blockCategories.size +
                response.dayTasks.size + response.recurringTaskDefs.size +
                response.daySummaries.size +
                response.ecosystemStates.size + response.marineCreatures.size
            Log.i(
                TAG,
                "Pull: total=$pullTotal blocks=${response.workBlocks.size} cats=${response.blockCategories.size} " +
                    "tasks=${response.dayTasks.size} defs=${response.recurringTaskDefs.size} summaries=${response.daySummaries.size} " +
                    "eco=${response.ecosystemStates.size} creatures=${response.marineCreatures.size}"
            )
            applyServerChanges(response)
            userPrefs.setLastSyncTime(cutoff)
            onServerChangesApplied?.invoke()

            _status.value = SyncStatus.SUCCESS
            _lastError.value = null
            _lastSyncMillis.value = System.currentTimeMillis()
            val elapsed = System.currentTimeMillis() - startMs
            Log.i(TAG, "Sync OK in ${elapsed}ms (push=$pushTotal, pull=$pullTotal)")
            SyncResult.Success
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startMs
            val msg = e.message ?: e.javaClass.simpleName
            Log.w(TAG, "Sync failed after ${elapsed}ms: ${e.javaClass.simpleName}: ${e.message}")
            _status.value = SyncStatus.ERROR
            _lastError.value = msg
            SyncResult.Error(msg)
        }
    }

    private suspend fun applyServerChanges(response: SyncResponse) {
        // Process in FK order: blocks first, then dependents

        if (response.workBlocks.isNotEmpty()) {
            db.workBlockDao().upsertAll(response.workBlocks.map { it.toEntity() })
        }
        if (response.blockCategories.isNotEmpty()) {
            db.blockCategoryDao().upsertAll(response.blockCategories.map { it.toEntity() })
        }
        if (response.recurringTaskDefs.isNotEmpty()) {
            db.recurringTaskDefDao().upsertAll(response.recurringTaskDefs.map { it.toEntity() })
        }
        if (response.dayTasks.isNotEmpty()) {
            // Special merge for hasBeenRewarded: never revert true -> false
            val serverTasks = response.dayTasks.mapNotNull { dto ->
                val localTask = db.dayTaskDao().getById(dto.id)
                // El pull devuelve también lo que acabamos de subir. Si la copia local
                // es igual o más nueva, aplicarla borraría lo que el usuario tocó
                // mientras la petición viajaba.
                if (localTask != null && dto.updatedAt != null &&
                    localTask.updatedAt.isNotBlank() && localTask.updatedAt >= dto.updatedAt
                ) return@mapNotNull null
                if (localTask != null && localTask.hasBeenRewarded && !dto.hasBeenRewarded) {
                    dto.copy(hasBeenRewarded = true).toEntity()
                } else {
                    dto.toEntity()
                }
            }
            if (serverTasks.isNotEmpty()) db.dayTaskDao().upsertAll(serverTasks)
        }
        if (response.daySummaries.isNotEmpty()) {
            db.daySummaryDao().upsertAll(response.daySummaries.map { it.toEntity() })
        }
        if (response.ecosystemStates.isNotEmpty()) {
            db.ecosystemStateDao().upsertAll(response.ecosystemStates.map { it.toEntity() })
        }
        if (response.marineCreatures.isNotEmpty()) {
            db.marineCreatureDao().upsertAll(response.marineCreatures.map { it.toEntity() })
        }
    }
}
