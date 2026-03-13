package com.mnebot.riptide.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

interface UserPreferencesRepository {
    fun getNightSummaryTime(): Flow<LocalTime>
    suspend fun setNightSummaryTime(time: LocalTime)
    suspend fun getPendingUnlocks(): List<String>   // lista de emojis pendientes
    suspend fun setPendingUnlocks(emojis: List<String>)
}