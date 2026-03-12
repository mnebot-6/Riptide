package com.mnebot.riptide

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalTime

class NightSummarySchedulerImpl : NightSummaryScheduler {
    override fun getNightSummaryTime(): Flow<LocalTime> = flowOf(LocalTime(23, 30))
    override suspend fun setNightSummaryTime(time: LocalTime) {}
    override fun scheduleWorker(time: LocalTime) {}
}