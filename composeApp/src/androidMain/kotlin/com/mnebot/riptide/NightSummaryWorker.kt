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
import com.mnebot.riptide.data.repository.DaySummaryRepositoryImpl
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.data.repository.EcosystemStateRepositoryImpl
import com.mnebot.riptide.data.repository.MarineCreatureRepositoryImpl
import com.mnebot.riptide.data.repository.WorkBlockRepositoryImpl
import com.mnebot.riptide.data.repository.UserPreferencesRepositoryImpl
import com.mnebot.riptide.domain.DecorationUnlockChecker
import com.mnebot.riptide.domain.EcosystemProcessor
import com.mnebot.riptide.domain.NightSummaryProcessor
import com.mnebot.riptide.widget.WidgetUpdater
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * Cierra el día a las 23:59:59. No envía notificación: a esa hora nadie la lee.
 * El resumen llega en el aviso matutino y en el diálogo al abrir la app.
 */
class NightSummaryWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = DatabaseProvider.getDatabase(context)
        val blockCategoryRepo = BlockCategoryRepositoryImpl(db.blockCategoryDao())
        val blocks = WorkBlockRepositoryImpl(db.workBlockDao()).getAll()
        val blockCategories = blocks.associate { block ->
            block.id to blockCategoryRepo.getCategoriesForBlocks(listOf(block.id)).map { it.category }
        }

        val dayTaskRepository = DayTaskRepositoryImpl(db.dayTaskDao())
        val daySummaryRepository = DaySummaryRepositoryImpl(db.daySummaryDao())
        val ecosystemStateRepository = EcosystemStateRepositoryImpl(db.ecosystemStateDao())
        val marineCreatureRepository = MarineCreatureRepositoryImpl(db.marineCreatureDao())
        val userPreferencesRepository = UserPreferencesRepositoryImpl(context)

        val ecosystemProcessor = EcosystemProcessor(
            ecosystemStateRepository,
            marineCreatureRepository
        )

        val processor = NightSummaryProcessor(
            dayTaskRepository = dayTaskRepository,
            daySummaryRepository = daySummaryRepository,
            ecosystemProcessor = ecosystemProcessor,
            userPreferencesRepository = userPreferencesRepository,
            decorationUnlockChecker = DecorationUnlockChecker(
                daySummaryRepository = daySummaryRepository,
                dayTaskRepository = dayTaskRepository,
                marineCreatureRepository = marineCreatureRepository,
                ecosystemProcessor = ecosystemProcessor,
                userPreferencesRepository = userPreferencesRepository
            ),
        )

        val target = inputData.getString("targetDate")
            ?.let { LocalDate.parse(it) }
            ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        processor.processDay(target, blockCategories)

        // El cierre expira tareas y estrena día: sin esto el widget sigue enseñando
        // la lista de ayer hasta que alguien abra la app.
        WidgetUpdater.refreshAll(context)

        schedule(context)

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "night_summary"

        /** Programa el cierre del próximo 23:59:59. */
        fun schedule(context: Context) {
            val now = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val closeAt = NightSummaryProcessor.DAY_CLOSE_TIME

            val nowSeconds = now.hour * 3600 + now.minute * 60 + now.second
            val targetSeconds = closeAt.hour * 3600 + closeAt.minute * 60 + closeAt.second
            val isLaterToday = targetSeconds > nowSeconds

            val delaySeconds = if (isLaterToday) {
                (targetSeconds - nowSeconds).toLong()
            } else {
                (24 * 3600 - nowSeconds + targetSeconds).toLong()
            }
            val targetDate = if (isLaterToday) now.date else now.date.plus(1, DateTimeUnit.DAY)

            val request = OneTimeWorkRequestBuilder<NightSummaryWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(workDataOf("targetDate" to targetDate.toString()))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
