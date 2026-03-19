package com.mnebot.riptide

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.repository.BlockCategoryRepositoryImpl
import com.mnebot.riptide.data.repository.BlockStreakRepositoryImpl
import com.mnebot.riptide.data.repository.DaySummaryRepositoryImpl
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.data.repository.EcosystemStateRepositoryImpl
import com.mnebot.riptide.data.repository.MarineCreatureRepositoryImpl
import com.mnebot.riptide.data.repository.WorkBlockRepositoryImpl
import com.mnebot.riptide.data.repository.UserPreferencesRepositoryImpl
import com.mnebot.riptide.domain.BlockStreakProcessor
import com.mnebot.riptide.domain.EcosystemProcessor
import com.mnebot.riptide.domain.NightSummaryProcessor
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

class NightSummaryWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = DatabaseProvider.getDatabase(context)
        val blockCategoryRepo = BlockCategoryRepositoryImpl(db.blockCategoryDao())
        val blocks = WorkBlockRepositoryImpl(db.workBlockDao()).getAll()
        val blockNames = blocks.associate { it.id to it.name }
        val blockCategories = blocks.associate { block ->
            block.id to blockCategoryRepo.getCategoriesForBlocks(listOf(block.id)).map { it.category }
        }

        val blockStreakProcessor = BlockStreakProcessor(
            dayTaskRepository = DayTaskRepositoryImpl(db.dayTaskDao()),
            blockStreakRepository = BlockStreakRepositoryImpl(db.blockStreakDao())
        )
        val ecosystemProcessor = EcosystemProcessor(
            EcosystemStateRepositoryImpl(db.ecosystemStateDao()),
            MarineCreatureRepositoryImpl(db.marineCreatureDao())
        )
        val userPreferencesRepository = UserPreferencesRepositoryImpl(context)

        val processor = NightSummaryProcessor(
            dayTaskRepository = DayTaskRepositoryImpl(db.dayTaskDao()),
            daySummaryRepository = DaySummaryRepositoryImpl(db.daySummaryDao()),
            blockStreakProcessor = blockStreakProcessor,
            ecosystemProcessor = ecosystemProcessor,
            userPreferencesRepository = userPreferencesRepository,
        )

        val today = inputData.getString("targetDate")
            ?.let { LocalDate.parse(it) }
            ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val summaryTime = userPreferencesRepository.getNightSummaryTime().first()

        processor.processDay(today, blockNames, blockCategories, summaryTime)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "night_summary"

        fun schedule(context: Context, at: LocalTime) {
            val now = kotlin.time.Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())

            val nowMinutes = now.hour * 60 + now.minute
            val targetMinutes = at.hour * 60 + at.minute
            val targetIsTodaySide = targetMinutes > nowMinutes
            val delayMinutes = if (targetIsTodaySide) {
                (targetMinutes - nowMinutes).toLong()
            } else {
                (24 * 60 - nowMinutes + targetMinutes).toLong()
            }
            val targetDate = if (targetIsTodaySide) {
                now.date
            } else {
                now.date.plus(1, DateTimeUnit.DAY)
            }

            val data = workDataOf("targetDate" to targetDate.toString())
            val request = OneTimeWorkRequestBuilder<NightSummaryWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}