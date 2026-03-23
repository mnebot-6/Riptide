package com.mnebot.riptide

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

class MorningReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        NotificationHelper.sendMorningReminderNotification(context)

        // Reschedule for the same time tomorrow
        val prefs = com.mnebot.riptide.data.repository.UserPreferencesRepositoryImpl(context)
        val time = prefs.getMorningReminderTime().first() ?: return Result.success()
        schedule(context, time)

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "morning_reminder"

        fun schedule(context: Context, at: LocalTime?) {
            if (at == null) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val now = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())

            val nowMinutes = now.hour * 60 + now.minute
            val targetMinutes = at.hour * 60 + at.minute
            val targetIsTodaySide = targetMinutes > nowMinutes

            val delayMinutes = if (targetIsTodaySide) {
                (targetMinutes - nowMinutes).toLong()
            } else {
                (24 * 60 - nowMinutes + targetMinutes).toLong()
            }

            val request = OneTimeWorkRequestBuilder<MorningReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
