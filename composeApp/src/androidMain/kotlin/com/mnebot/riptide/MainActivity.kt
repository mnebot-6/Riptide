package com.mnebot.riptide

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
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
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.NightSummaryProcessor
import com.mnebot.riptide.presentation.main.MainViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission granted or denied — channels already created */ }

    private val viewModel by viewModels<com.mnebot.riptide.presentation.main.MainViewModel> {
        MainViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Create notification channels and request POST_NOTIFICATIONS permission (API 33+)
        NotificationHelper.createChannels(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val scheduler = NightSummarySchedulerImpl(applicationContext)
        val userPreferencesRepository = UserPreferencesRepositoryImpl(applicationContext)

        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(applicationContext)
            val ecosystemStateRepo = EcosystemStateRepositoryImpl(db.ecosystemStateDao())
            val marineCreatureRepo = MarineCreatureRepositoryImpl(db.marineCreatureDao())
            val assigner = MarineCategoryAssigner(
                WorkBlockRepositoryImpl(db.workBlockDao()),
                BlockCategoryRepositoryImpl(db.blockCategoryDao()),
                ecosystemStateRepo
            )
            DataSeeder.seedIfEmpty(db, assigner)

            // Reschedule task reminders that may have been lost (e.g., after device restart)
            TaskReminderSchedulerImpl(applicationContext).rescheduleAll()

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
            val ecosystemProcessor = EcosystemProcessor(ecosystemStateRepo, marineCreatureRepo)

            val processor = NightSummaryProcessor(
                dayTaskRepository = DayTaskRepositoryImpl(db.dayTaskDao()),
                daySummaryRepository = DaySummaryRepositoryImpl(db.daySummaryDao()),
                blockStreakProcessor = blockStreakProcessor,
                ecosystemProcessor = ecosystemProcessor,
                userPreferencesRepository = userPreferencesRepository,
            )

            val yesterday = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                .minus(1, DateTimeUnit.DAY)

            val nightTime = scheduler.getNightSummaryTime().first()
            processor.processDay(yesterday, blockNames, blockCategories, nightTime)
            scheduler.scheduleWorker(nightTime)

            viewModel.reload()
        }

        setContent {
            App(
                viewModel = viewModel,
                nightSummaryScheduler = scheduler,
                userPreferencesRepository = userPreferencesRepository
            )
        }
    }
}