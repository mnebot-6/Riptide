package com.mnebot.riptide.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

interface UserPreferencesRepository {
    fun getNightSummaryTime(): Flow<LocalTime>
    suspend fun setNightSummaryTime(time: LocalTime)
    suspend fun getPendingUnlocks(): List<String>
    suspend fun setPendingUnlocks(emojis: List<String>)
    suspend fun getLastDismissedSummaryDate(): LocalDate?
    suspend fun setLastDismissedSummaryDate(date: LocalDate)
}