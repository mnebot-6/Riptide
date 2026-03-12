package com.mnebot.riptide

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.repository.DaySummaryRepositoryImpl
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.domain.NightSummaryProcessor
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit

class NightSummaryWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = DatabaseProvider.getDatabase(context)
        val processor = NightSummaryProcessor(
            dayTaskRepository = DayTaskRepositoryImpl(db.dayTaskDao()),
            daySummaryRepository = DaySummaryRepositoryImpl(db.daySummaryDao())
        )
        val today = kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        processor.processDay(today)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "night_summary"

        fun schedule(context: Context, at: LocalTime) {
            val now = kotlin.time.Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())

            var targetHour = at.hour
            var targetMinute = at.minute

            // Calcular delay hasta la próxima ocurrencia
            val nowMinutes = now.hour * 60 + now.minute
            val targetMinutes = targetHour * 60 + targetMinute
            val delayMinutes = if (targetMinutes > nowMinutes) {
                (targetMinutes - nowMinutes).toLong()
            } else {
                // Ya pasó hoy, programar para mañana
                (24 * 60 - nowMinutes + targetMinutes).toLong()
            }

            val request = OneTimeWorkRequestBuilder<NightSummaryWorker>()
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