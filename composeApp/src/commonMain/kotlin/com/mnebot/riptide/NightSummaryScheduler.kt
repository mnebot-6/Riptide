package com.mnebot.riptide

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

interface NightSummaryScheduler {
    /** El cierre del día es fijo (23:59:59): no hay hora que configurar. */
    fun scheduleWorker()

    // Morning reminder (null = disabled)
    fun getMorningReminderTime(): Flow<LocalTime?>
    suspend fun setMorningReminderTime(time: LocalTime?)
    fun scheduleMorningReminder(time: LocalTime?)
}
