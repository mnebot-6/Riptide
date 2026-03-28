package com.mnebot.riptide.domain.fakes

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

    override fun getNightSummaryTime(): Flow<LocalTime> = flowOf(LocalTime(23, 30))
    override suspend fun setNightSummaryTime(time: LocalTime) {}
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
}
