package com.mnebot.riptide

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalTime

class NightSummarySchedulerImpl : NightSummaryScheduler {
    override fun scheduleWorker() {}

    override fun getMorningReminderTime(): Flow<LocalTime?> = flowOf(null)
    override suspend fun setMorningReminderTime(time: LocalTime?) {}
    override fun scheduleMorningReminder(time: LocalTime?) {}
}
