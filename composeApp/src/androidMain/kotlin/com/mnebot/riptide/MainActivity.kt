package com.mnebot.riptide

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.repository.BlockCategoryRepositoryImpl
import com.mnebot.riptide.data.repository.DaySummaryRepositoryImpl
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.data.repository.RecurringTaskDefRepositoryImpl
import com.mnebot.riptide.data.repository.EcosystemStateRepositoryImpl
import com.mnebot.riptide.data.repository.MarineCreatureRepositoryImpl
import com.mnebot.riptide.data.repository.WorkBlockRepositoryImpl
import com.mnebot.riptide.data.repository.UserPreferencesRepositoryImpl
import com.mnebot.riptide.data.remote.ApiClient
import com.mnebot.riptide.data.remote.AuthManager
import com.mnebot.riptide.data.remote.DataStoreTokenProvider
import com.mnebot.riptide.data.remote.RiptideApi
import com.mnebot.riptide.data.sync.InitialSyncPreparer
import com.mnebot.riptide.data.sync.SyncManager
import com.mnebot.riptide.data.sync.SyncWorker
import com.mnebot.riptide.domain.EcosystemProcessor
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.NightSummaryProcessor
import com.mnebot.riptide.domain.RecurringTaskGenerator
import com.mnebot.riptide.presentation.main.MainViewModelFactory
import com.mnebot.riptide.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val LOOKAHEAD_DAYS = 7

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission granted or denied — channels already created */ }

    private val viewModel by viewModels<com.mnebot.riptide.presentation.main.MainViewModel> {
        MainViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Create notification channels and request POST_NOTIFICATIONS permission (API 33+)
        NotificationHelper.createChannels(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val scheduler = NightSummarySchedulerImpl(applicationContext)
        val userPreferencesRepository = UserPreferencesRepositoryImpl(applicationContext)

        // Sync infrastructure
        val tokenProvider = DataStoreTokenProvider(userPreferencesRepository)
        val httpClient = ApiClient.create(tokenProvider)
        val api = RiptideApi(httpClient)
        val dbForSync = DatabaseProvider.getDatabase(applicationContext)
        val authManager = AuthManager(api, userPreferencesRepository)
        val syncManager = SyncManager(dbForSync, api, userPreferencesRepository) {
            WidgetUpdater.refreshAll(applicationContext)
        }
        val initialSyncPreparer = InitialSyncPreparer(dbForSync)

        // Launch sync on startup if logged in and no pending conflict
        lifecycleScope.launch {
            if (userPreferencesRepository.getAccessToken() != null &&
                !userPreferencesRepository.hasPendingInitialSync()) {
                SyncWorker.schedulePeriodic(applicationContext)
                syncManager.sync()
            }
        }

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
            val blockCategories = blocks.associate { block ->
                block.id to blockCategoryRepo.getCategoriesForBlocks(listOf(block.id)).map { it.category }
            }

            val ecosystemProcessor = EcosystemProcessor(ecosystemStateRepo, marineCreatureRepo)

            val processor = NightSummaryProcessor(
                dayTaskRepository = DayTaskRepositoryImpl(db.dayTaskDao()),
                daySummaryRepository = DaySummaryRepositoryImpl(db.daySummaryDao()),
                ecosystemProcessor = ecosystemProcessor,
                userPreferencesRepository = userPreferencesRepository,
            )

            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date

            // Si la app estuvo días sin abrirse, esos días existieron: primero se
            // materializan sus tareas recurrentes y solo después se cierran. Al revés
            // quedarían días cerrados en falso y tareas huérfanas en el pasado.
            val daySummaryRepo = DaySummaryRepositoryImpl(db.daySummaryDao())
            val lastClosed = daySummaryRepo.getLatestN(1).firstOrNull()?.date
            val generateFrom = lastClosed?.plus(1, DateTimeUnit.DAY)?.coerceAtMost(today) ?: today
            val daysToCover = (today.toEpochDays() - generateFrom.toEpochDays()).toInt() + LOOKAHEAD_DAYS
            RecurringTaskGenerator(
                RecurringTaskDefRepositoryImpl(db.recurringTaskDefDao()),
                DayTaskRepositoryImpl(db.dayTaskDao())
            ).generateUpTo(generateFrom, daysAhead = daysToCover)

            // Red de seguridad: si el worker de las 23:59 no llegó a ejecutarse
            // (móvil apagado, Doze), cierra aquí los días que quedaron abiertos.
            processor.processPendingDays(today, blockCategories)
            scheduler.scheduleWorker()

            viewModel.reload()
        }

        // Refresh widget and sync whenever the app comes to the foreground
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                WidgetUpdater.refreshAll(applicationContext)
                if (userPreferencesRepository.getAccessToken() != null &&
                    !userPreferencesRepository.hasPendingInitialSync()) {
                    syncManager.sync()  // throttled — won't spam
                }
            }
        }

        setContent {
            App(
                viewModel = viewModel,
                nightSummaryScheduler = scheduler,
                userPreferencesRepository = userPreferencesRepository,
                authManager = authManager,
                syncManager = syncManager,
                initialSyncPreparer = initialSyncPreparer,
                api = api
            )
        }
    }
}