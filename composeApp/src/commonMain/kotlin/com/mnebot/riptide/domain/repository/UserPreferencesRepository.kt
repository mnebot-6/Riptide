package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.LoggedInUser
import com.mnebot.riptide.domain.model.PendingLootbox
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

interface UserPreferencesRepository {
    // Legacy — migrar a lootboxes y borrar
    suspend fun getPendingUnlocks(): List<String>
    suspend fun setPendingUnlocks(emojis: List<String>)

    // Nuevo sistema de lootbox
    suspend fun getPendingLootboxes(): List<PendingLootbox>
    suspend fun setPendingLootboxes(lootboxes: List<PendingLootbox>)

    suspend fun getLastDismissedSummaryDate(): LocalDate?
    suspend fun setLastDismissedSummaryDate(date: LocalDate)

    // Morning reminder (null = disabled)
    fun getMorningReminderTime(): Flow<LocalTime?>
    suspend fun setMorningReminderTime(time: LocalTime?)

    // Onboarding
    fun hasCompletedOnboarding(): Flow<Boolean>
    suspend fun setOnboardingCompleted()

    // Live wallpaper
    suspend fun isWallpaperActivated(): Boolean
    suspend fun setWallpaperActivated()

    // Auth
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clearAuth()
    suspend fun getLoggedInUser(): LoggedInUser?
    suspend fun saveUser(user: LoggedInUser)
    fun isLoggedIn(): Flow<Boolean>

    // Wallpaper FPS
    fun getWallpaperFps(): Flow<Int>
    suspend fun setWallpaperFps(fps: Int)

    // Task creation onboarding
    suspend fun hasShownTaskCreationOnboarding(): Boolean
    suspend fun setTaskCreationOnboardingShown()

    // Reset onboarding
    suspend fun resetOnboarding()

    // Sync
    suspend fun getLastSyncTime(): String?
    suspend fun setLastSyncTime(time: String)

    // Initial sync conflict guard
    suspend fun hasPendingInitialSync(): Boolean
    suspend fun setPendingInitialSync(pending: Boolean)
}
