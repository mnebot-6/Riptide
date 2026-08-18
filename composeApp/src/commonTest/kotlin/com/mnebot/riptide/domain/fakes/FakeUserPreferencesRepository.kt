package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.LoggedInUser
import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val pendingLootboxes = mutableListOf<PendingLootbox>()
    private var wallpaperActivated = false

    fun activateWallpaper() { wallpaperActivated = true }

    override suspend fun getPendingUnlocks(): List<String> = emptyList()
    override suspend fun setPendingUnlocks(emojis: List<String>) {}
    override suspend fun getPendingLootboxes(): List<PendingLootbox> = pendingLootboxes.toList()
    override suspend fun setPendingLootboxes(lootboxes: List<PendingLootbox>) {
        pendingLootboxes.clear()
        pendingLootboxes.addAll(lootboxes)
    }
    override suspend fun getLastDismissedSummaryDate(): LocalDate? = null
    override suspend fun setLastDismissedSummaryDate(date: LocalDate) {}
    override fun getMorningReminderTime(): Flow<LocalTime?> = flowOf(null)
    override suspend fun setMorningReminderTime(time: LocalTime?) {}
    override fun hasCompletedOnboarding(): Flow<Boolean> = flowOf(true)
    override suspend fun setOnboardingCompleted() {}
    override suspend fun isWallpaperActivated(): Boolean = wallpaperActivated
    override suspend fun setWallpaperActivated() { wallpaperActivated = true }

    // Auth
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var loggedInUser: LoggedInUser? = null
    private var lastSyncTime: String? = null

    override suspend fun getAccessToken(): String? = accessToken
    override suspend fun getRefreshToken(): String? = refreshToken
    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }
    override suspend fun clearAuth() {
        accessToken = null
        refreshToken = null
        loggedInUser = null
        lastSyncTime = null
    }
    override suspend fun getLoggedInUser(): LoggedInUser? = loggedInUser
    override suspend fun saveUser(user: LoggedInUser) { loggedInUser = user }
    override fun isLoggedIn(): Flow<Boolean> = flowOf(accessToken != null)

    // Wallpaper FPS
    override fun getWallpaperFps(): Flow<Int> = flowOf(30)
    override suspend fun setWallpaperFps(fps: Int) {}

    // Task creation onboarding
    private var taskOnboardingShown = false
    override suspend fun hasShownTaskCreationOnboarding(): Boolean = taskOnboardingShown
    override suspend fun setTaskCreationOnboardingShown() { taskOnboardingShown = true }

    override suspend fun resetOnboarding() { taskOnboardingShown = false }

    // Sync
    override suspend fun getLastSyncTime(): String? = lastSyncTime
    override suspend fun setLastSyncTime(time: String) { lastSyncTime = time }

    // Initial sync conflict guard
    private var pendingInitialSync = false
    override suspend fun hasPendingInitialSync(): Boolean = pendingInitialSync
    override suspend fun setPendingInitialSync(pending: Boolean) { pendingInitialSync = pending }
}
