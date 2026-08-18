package com.mnebot.riptide

import android.content.Context
import com.mnebot.riptide.data.repository.UserPreferencesRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

class NightSummarySchedulerImpl(context: Context) : NightSummaryScheduler {
    private val prefs = UserPreferencesRepositoryImpl(context.applicationContext)
    private val appContext = context.applicationContext

    override fun scheduleWorker() = NightSummaryWorker.schedule(appContext)

    override fun getMorningReminderTime(): Flow<LocalTime?> = prefs.getMorningReminderTime()
    override suspend fun setMorningReminderTime(time: LocalTime?) = prefs.setMorningReminderTime(time)
    override fun scheduleMorningReminder(time: LocalTime?) = MorningReminderWorker.schedule(appContext, time)
}
