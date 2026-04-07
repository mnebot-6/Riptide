package com.mnebot.riptide.data.sync

import com.mnebot.riptide.data.local.db.RiptideDatabase
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
    private val userPrefs: UserPreferencesRepository
) {

    sealed class SyncResult {
        data object Success : SyncResult()
        data object NotLoggedIn : SyncResult()
        data class Error(val message: String) : SyncResult()
    }

    private val _status = MutableStateFlow(SyncStatus.IDLE)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

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
        return try {
            val lastSync = userPrefs.getLastSyncTime()
            val since = lastSync ?: ""
            val request = SyncRequest(
                lastSyncTime = lastSync,
                workBlocks = db.workBlockDao().getModifiedSince(since).map { it.toDto() },
                blockCategories = db.blockCategoryDao().getModifiedSince(since).map { it.toDto() },
                dayTasks = db.dayTaskDao().getModifiedSince(since).map { it.toDto() },
                recurringTaskDefs = db.recurringTaskDefDao().getModifiedSince(since).map { it.toDto() },
                daySummaries = db.daySummaryDao().getModifiedSince(since).map { it.toDto() },
                blockStreaks = db.blockStreakDao().getModifiedSince(since).map { it.toDto() },
                ecosystemStates = db.ecosystemStateDao().getModifiedSince(since).map { it.toDto() },
                marineCreatures = db.marineCreatureDao().getModifiedSince(since).map { it.toDto() }
            )

            val response = api.sync(request)
            applyServerChanges(response)
            userPrefs.setLastSyncTime(response.serverTime)

            _status.value = SyncStatus.SUCCESS
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _status.value = SyncStatus.ERROR
            SyncResult.Error(e.message ?: "Sync failed")
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
            val serverTasks = response.dayTasks.map { dto ->
                val localTask = db.dayTaskDao().getById(dto.id)
                if (localTask != null && localTask.hasBeenRewarded && !dto.hasBeenRewarded) {
                    dto.copy(hasBeenRewarded = true).toEntity()
                } else {
                    dto.toEntity()
                }
            }
            db.dayTaskDao().upsertAll(serverTasks)
        }
        if (response.daySummaries.isNotEmpty()) {
            db.daySummaryDao().upsertAll(response.daySummaries.map { it.toEntity() })
        }
        if (response.blockStreaks.isNotEmpty()) {
            db.blockStreakDao().upsertAll(response.blockStreaks.map { it.toEntity() })
        }
        if (response.ecosystemStates.isNotEmpty()) {
            db.ecosystemStateDao().upsertAll(response.ecosystemStates.map { it.toEntity() })
        }
        if (response.marineCreatures.isNotEmpty()) {
            db.marineCreatureDao().upsertAll(response.marineCreatures.map { it.toEntity() })
        }
    }
}
