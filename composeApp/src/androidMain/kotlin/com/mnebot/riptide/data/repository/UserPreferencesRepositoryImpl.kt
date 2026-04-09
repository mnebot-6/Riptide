package com.mnebot.riptide.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.LoggedInUser
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "riptide_prefs")

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    companion object {
        private val KEY_NIGHT_HOUR = intPreferencesKey("night_summary_hour")
        private val KEY_NIGHT_MINUTE = intPreferencesKey("night_summary_minute")
        private val KEY_PENDING_UNLOCKS = stringPreferencesKey("pending_unlocks")
        private val KEY_PENDING_LOOTBOXES = stringPreferencesKey("pending_lootboxes")
        private val KEY_DISMISSED_SUMMARY_DATE = stringPreferencesKey("dismissed_summary_date")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_WALLPAPER_ACTIVATED  = booleanPreferencesKey("wallpaper_activated")
        private val KEY_MORNING_HOUR   = intPreferencesKey("morning_reminder_hour")
        private val KEY_MORNING_MINUTE = intPreferencesKey("morning_reminder_minute")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_display_name")
        private val KEY_USER_AVATAR = stringPreferencesKey("user_avatar_url")
        private val KEY_LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
        private val KEY_WALLPAPER_FPS = intPreferencesKey("wallpaper_fps")
        private val KEY_TASK_ONBOARDING_SHOWN = booleanPreferencesKey("task_creation_onboarding_shown")
        private val KEY_PENDING_INITIAL_SYNC = booleanPreferencesKey("pending_initial_sync")
        private val KEY_INSTALLED_PACKAGES = stringPreferencesKey("installed_packages")
        private const val DEFAULT_HOUR = 23
        private const val DEFAULT_MINUTE = 30
        private const val DISABLED = -1          // sentinel for "no morning reminder"
        private const val SEPARATOR = "|"
    }

    override fun getNightSummaryTime(): Flow<LocalTime> =
        context.dataStore.data.map { prefs ->
            val hour = prefs[KEY_NIGHT_HOUR] ?: DEFAULT_HOUR
            val minute = prefs[KEY_NIGHT_MINUTE] ?: DEFAULT_MINUTE
            LocalTime(hour, minute)
        }

    override suspend fun setNightSummaryTime(time: LocalTime) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NIGHT_HOUR] = time.hour
            prefs[KEY_NIGHT_MINUTE] = time.minute
        }
    }

    override suspend fun getPendingUnlocks(): List<String> {
        val raw = context.dataStore.data.first()[KEY_PENDING_UNLOCKS] ?: return emptyList()
        return if (raw.isBlank()) emptyList() else raw.split(SEPARATOR)
    }

    override suspend fun setPendingUnlocks(emojis: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PENDING_UNLOCKS] = emojis.joinToString(SEPARATOR)
        }
    }

    // Formato: "FISH:4" (legacy) | "DECORATION:0:TREASURE_CHEST" (con directSpecies)
    override suspend fun getPendingLootboxes(): List<PendingLootbox> {
        val raw = context.dataStore.data.first()[KEY_PENDING_LOOTBOXES] ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEPARATOR).mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size < 2) return@mapNotNull null
            val category = runCatching { MarineCategory.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val level    = parts[1].toIntOrNull() ?: return@mapNotNull null
            val direct   = if (parts.size >= 3)
                runCatching { CreatureSpecies.valueOf(parts[2]) }.getOrNull()
            else null
            PendingLootbox(category, level, direct)
        }
    }

    override suspend fun setPendingLootboxes(lootboxes: List<PendingLootbox>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PENDING_LOOTBOXES] = lootboxes.joinToString(SEPARATOR) {
                if (it.directSpecies != null)
                    "${it.category.name}:${it.categoryLevel}:${it.directSpecies.name}"
                else
                    "${it.category.name}:${it.categoryLevel}"
            }
        }
    }

    override suspend fun getLastDismissedSummaryDate(): LocalDate? {
        val raw = context.dataStore.data.first()[KEY_DISMISSED_SUMMARY_DATE] ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    override suspend fun setLastDismissedSummaryDate(date: LocalDate) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DISMISSED_SUMMARY_DATE] = date.toString()
        }
    }

    override fun getMorningReminderTime(): Flow<LocalTime?> =
        context.dataStore.data.map { prefs ->
            val hour = prefs[KEY_MORNING_HOUR] ?: DISABLED
            if (hour == DISABLED) null
            else LocalTime(hour, prefs[KEY_MORNING_MINUTE] ?: 0)
        }

    override suspend fun setMorningReminderTime(time: LocalTime?) {
        context.dataStore.edit { prefs ->
            if (time == null) {
                prefs[KEY_MORNING_HOUR] = DISABLED
                prefs[KEY_MORNING_MINUTE] = 0
            } else {
                prefs[KEY_MORNING_HOUR] = time.hour
                prefs[KEY_MORNING_MINUTE] = time.minute
            }
        }
    }

    override fun hasCompletedOnboarding(): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[KEY_ONBOARDING_COMPLETED] ?: false }

    override suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    override suspend fun isWallpaperActivated(): Boolean =
        context.dataStore.data.first()[KEY_WALLPAPER_ACTIVATED] ?: false

    override suspend fun setWallpaperActivated() {
        context.dataStore.edit { prefs -> prefs[KEY_WALLPAPER_ACTIVATED] = true }
    }

    // ── Auth ────────────────────────────────────────────────────────────────────

    override suspend fun getAccessToken(): String? =
        context.dataStore.data.first()[KEY_ACCESS_TOKEN]

    override suspend fun getRefreshToken(): String? =
        context.dataStore.data.first()[KEY_REFRESH_TOKEN]

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    override suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_AVATAR)
            prefs.remove(KEY_LAST_SYNC_TIME)
            prefs.remove(KEY_PENDING_INITIAL_SYNC)
        }
    }

    override suspend fun getLoggedInUser(): LoggedInUser? {
        val prefs = context.dataStore.data.first()
        val id = prefs[KEY_USER_ID] ?: return null
        return LoggedInUser(
            id = id,
            email = prefs[KEY_USER_EMAIL] ?: "",
            displayName = prefs[KEY_USER_NAME],
            avatarUrl = prefs[KEY_USER_AVATAR]
        )
    }

    override suspend fun saveUser(user: LoggedInUser) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = user.id
            prefs[KEY_USER_EMAIL] = user.email
            user.displayName?.let { prefs[KEY_USER_NAME] = it }
            user.avatarUrl?.let { prefs[KEY_USER_AVATAR] = it }
        }
    }

    override fun isLoggedIn(): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[KEY_ACCESS_TOKEN] != null }

    // ── Wallpaper FPS ──────────────────────────────────────────────────────────

    override fun getWallpaperFps(): Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[KEY_WALLPAPER_FPS] ?: 30 }

    override suspend fun setWallpaperFps(fps: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_WALLPAPER_FPS] = fps }
    }

    // ── Task creation onboarding ────────────────────────────────────────────────

    override suspend fun hasShownTaskCreationOnboarding(): Boolean =
        context.dataStore.data.first()[KEY_TASK_ONBOARDING_SHOWN] ?: false

    override suspend fun setTaskCreationOnboardingShown() {
        context.dataStore.edit { prefs -> prefs[KEY_TASK_ONBOARDING_SHOWN] = true }
    }

    override suspend fun resetOnboarding() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ONBOARDING_COMPLETED)
            prefs.remove(KEY_TASK_ONBOARDING_SHOWN)
        }
    }

    // ── Sync ────────────────────────────────────────────────────────────────────

    override suspend fun getLastSyncTime(): String? =
        context.dataStore.data.first()[KEY_LAST_SYNC_TIME]

    override suspend fun setLastSyncTime(time: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIME] = time
        }
    }

    // ── Initial sync conflict guard ────────────────────────────────────────────

    override suspend fun hasPendingInitialSync(): Boolean =
        context.dataStore.data.first()[KEY_PENDING_INITIAL_SYNC] ?: false

    override suspend fun setPendingInitialSync(pending: Boolean) {
        context.dataStore.edit { prefs ->
            if (pending) prefs[KEY_PENDING_INITIAL_SYNC] = true
            else prefs.remove(KEY_PENDING_INITIAL_SYNC)
        }
    }

    // ── Installed task packages ───────────────────────────────────────────────

    override suspend fun getInstalledPackageIds(): Set<String> {
        val raw = context.dataStore.data.first()[KEY_INSTALLED_PACKAGES] ?: return emptySet()
        return if (raw.isBlank()) emptySet() else raw.split(SEPARATOR).toSet()
    }

    override suspend fun addInstalledPackageId(packageId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_INSTALLED_PACKAGES]?.split(SEPARATOR)?.toMutableSet() ?: mutableSetOf()
            current.add(packageId)
            prefs[KEY_INSTALLED_PACKAGES] = current.joinToString(SEPARATOR)
        }
    }

    override suspend fun removeInstalledPackageId(packageId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_INSTALLED_PACKAGES]?.split(SEPARATOR)?.toMutableSet() ?: return@edit
            current.remove(packageId)
            prefs[KEY_INSTALLED_PACKAGES] = current.joinToString(SEPARATOR)
        }
    }
}