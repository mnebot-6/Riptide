package com.mnebot.riptide

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

interface NightSummaryScheduler {
    fun getNightSummaryTime(): Flow<LocalTime>
    suspend fun setNightSummaryTime(time: LocalTime)
    fun scheduleWorker(time: LocalTime)

    // Morning reminder (null = disabled)
    fun getMorningReminderTime(): Flow<LocalTime?>
    suspend fun setMorningReminderTime(time: LocalTime?)
    fun scheduleMorningReminder(time: LocalTime?)
}