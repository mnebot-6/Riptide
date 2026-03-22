package com.mnebot.riptide.domain.repository

import com.mnebot.riptide.domain.model.PendingLootbox
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

interface UserPreferencesRepository {
    fun getNightSummaryTime(): Flow<LocalTime>
    suspend fun setNightSummaryTime(time: LocalTime)

    // Legacy — migrar a lootboxes y borrar
    suspend fun getPendingUnlocks(): List<String>
    suspend fun setPendingUnlocks(emojis: List<String>)

    // Nuevo sistema de lootbox
    suspend fun getPendingLootboxes(): List<PendingLootbox>
    suspend fun setPendingLootboxes(lootboxes: List<PendingLootbox>)

    suspend fun getLastDismissedSummaryDate(): LocalDate?
    suspend fun setLastDismissedSummaryDate(date: LocalDate)
}
