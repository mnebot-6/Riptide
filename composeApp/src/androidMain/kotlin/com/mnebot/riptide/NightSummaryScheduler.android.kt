package com.mnebot.riptide

import android.content.Context
import com.mnebot.riptide.data.repository.UserPreferencesRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

class NightSummarySchedulerImpl(context: Context) : NightSummaryScheduler {
    private val prefs = UserPreferencesRepositoryImpl(context.applicationContext)
    private val appContext = context.applicationContext

    override fun getNightSummaryTime(): Flow<LocalTime> = prefs.getNightSummaryTime()
    override suspend fun setNightSummaryTime(time: LocalTime) = prefs.setNightSummaryTime(time)
    override fun scheduleWorker(time: LocalTime) = NightSummaryWorker.schedule(appContext, time)
}