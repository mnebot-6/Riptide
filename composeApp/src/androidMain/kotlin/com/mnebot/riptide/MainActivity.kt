package com.mnebot.riptide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.repository.BlockCategoryRepositoryImpl
import com.mnebot.riptide.data.repository.DaySummaryRepositoryImpl
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.data.repository.WorkBlockRepositoryImpl
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.NightSummaryProcessor
import com.mnebot.riptide.presentation.main.MainViewModel
import com.mnebot.riptide.presentation.main.MainViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val scheduler = NightSummarySchedulerImpl(applicationContext)

        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(applicationContext)
            val assigner = MarineCategoryAssigner(
                WorkBlockRepositoryImpl(db.workBlockDao()),
                BlockCategoryRepositoryImpl(db.blockCategoryDao())
            )
            DataSeeder.seedIfEmpty(db, assigner)

            // Procesar ayer si no se procesó
            val processor = NightSummaryProcessor(
                dayTaskRepository = DayTaskRepositoryImpl(db.dayTaskDao()),
                daySummaryRepository = DaySummaryRepositoryImpl(db.daySummaryDao())
            )
            val yesterday = kotlin.time.Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                .minus(1, DateTimeUnit.DAY)
            processor.processDay(yesterday)

            // Planificar WorkManager con la hora guardada
            val nightTime = scheduler.getNightSummaryTime().first()
            scheduler.scheduleWorker(nightTime)

            viewModel.reload()
        }

        setContent {
            App(
                viewModel = viewModel,
                nightSummaryScheduler = scheduler
            )
        }
    }
}