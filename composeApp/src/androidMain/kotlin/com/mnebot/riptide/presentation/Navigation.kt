package com.mnebot.riptide.presentation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.runtime.rememberCoroutineScope
import com.mnebot.riptide.NightSummaryScheduler
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import com.mnebot.riptide.presentation.aquarium.EcosystemScreen
import com.mnebot.riptide.presentation.block.BlockFormResult
import com.mnebot.riptide.presentation.block.BlockFormScreen
import com.mnebot.riptide.presentation.block.BlockFormViewModel
import com.mnebot.riptide.presentation.block.BlockFormViewModelFactory
import com.mnebot.riptide.presentation.history.HistoryViewModel
import com.mnebot.riptide.presentation.history.HistoryViewModelFactory
import com.mnebot.riptide.presentation.main.MainShellScreen
import com.mnebot.riptide.presentation.main.MainViewModel
import com.mnebot.riptide.presentation.main.SyncConflictDialog
import com.mnebot.riptide.presentation.onboarding.OnboardingScreen
import com.mnebot.riptide.presentation.settings.SettingsScreen
import com.mnebot.riptide.presentation.stats.StatsViewModel
import com.mnebot.riptide.presentation.stats.StatsViewModelFactory
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mnebot.riptide.data.remote.AuthManager
import com.mnebot.riptide.data.remote.RiptideApi
import com.mnebot.riptide.data.repository.BlockCategoryRepositoryImpl
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.data.repository.EcosystemStateRepositoryImpl
import com.mnebot.riptide.data.repository.PersonalDateRepositoryImpl
import com.mnebot.riptide.data.repository.RecurringTaskDefRepositoryImpl
import com.mnebot.riptide.data.repository.UserPreferencesRepositoryImpl
import com.mnebot.riptide.data.repository.WorkBlockRepositoryImpl
import com.mnebot.riptide.data.sync.InitialSyncPreparer
import com.mnebot.riptide.data.sync.SyncManager
import com.mnebot.riptide.domain.HolidayCalendar
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.PackageInstaller
import com.mnebot.riptide.domain.RecurringTaskGenerator
import com.mnebot.riptide.domain.SmartSeeder
import com.mnebot.riptide.domain.TaskPackages
import com.mnebot.riptide.domain.repository.UserPreferencesRepository as UserPrefs
import com.mnebot.riptide.presentation.calendar.CalendarUiState
import com.mnebot.riptide.presentation.calendar.PersonalDateDialog
import com.mnebot.riptide.presentation.packages.PackageBrowserScreen
import com.mnebot.riptide.wallpaper.RiptideWallpaperService
import kotlinx.coroutines.launch

const val ROUTE_ONBOARDING = "onboarding"
const val ROUTE_MAIN = "main"
const val ROUTE_BLOCK_CREATE = "block/create"
const val ROUTE_BLOCK_EDIT = "block/edit/{blockId}"
const val ROUTE_ECOSYSTEM = "ecosystem"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_PACKAGES = "packages"

fun NavGraphBuilder.onboardingGraph(
    userPreferencesRepository: UserPreferencesRepository,
    navController: NavController
) {
    composable(ROUTE_ONBOARDING) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        OnboardingScreen(
            onComplete = { quizState ->
                scope.launch {
                    if (quizState != null) {
                        val database = DatabaseProvider.getDatabase(context)
                        val prefs = UserPreferencesRepositoryImpl(context)
                        val workBlockRepo = WorkBlockRepositoryImpl(database.workBlockDao())
                        val recurringTaskDefRepo = RecurringTaskDefRepositoryImpl(database.recurringTaskDefDao())
                        val dayTaskRepo = DayTaskRepositoryImpl(database.dayTaskDao())
                        val blockCategoryRepo = BlockCategoryRepositoryImpl(database.blockCategoryDao())
                        val ecosystemStateRepo = EcosystemStateRepositoryImpl(database.ecosystemStateDao())
                        val installer = PackageInstaller(
                            workBlockRepository = workBlockRepo,
                            recurringTaskDefRepository = recurringTaskDefRepo,
                            userPreferencesRepository = prefs,
                            marineCategoryAssigner = MarineCategoryAssigner(workBlockRepo, blockCategoryRepo, ecosystemStateRepo),
                            recurringTaskGenerator = RecurringTaskGenerator(recurringTaskDefRepo, dayTaskRepo)
                        )
                        val seeder = SmartSeeder(installer, dayTaskRepo)
                        seeder.seedFromQuiz(quizState)
                    }
                    userPreferencesRepository.setOnboardingCompleted()
                }
                navController.navigate(ROUTE_MAIN) {
                    popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                }
            }
        )
    }
}

fun NavGraphBuilder.mainGraph(
    mainViewModel: MainViewModel,
    nightSummaryScheduler: NightSummaryScheduler,
    navController: NavController,
    authManager: AuthManager? = null,
    syncManager: SyncManager? = null,
    initialSyncPreparer: InitialSyncPreparer? = null,
    api: RiptideApi? = null,
    userPreferences: UserPrefs? = null
) {
    composable(ROUTE_MAIN) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val signInLauncher = rememberSignInLauncher(
            authManager, syncManager, initialSyncPreparer, mainViewModel, userPreferences
        )

        // Stats ViewModel
        val statsViewModel: StatsViewModel = viewModel(factory = StatsViewModelFactory(context))
        val statsUiState by statsViewModel.uiState.collectAsStateWithLifecycle()

        // History ViewModel
        val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(context))
        val historyUiState by historyViewModel.uiState.collectAsStateWithLifecycle()

        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

        // Calendar state
        val database = DatabaseProvider.getDatabase(context)
        val personalDateRepo = remember { PersonalDateRepositoryImpl(database.personalDateDao()) }
        val calendarDayTaskRepo = remember { DayTaskRepositoryImpl(database.dayTaskDao()) }
        var calendarUiState by remember { mutableStateOf(CalendarUiState()) }
        var showPersonalDateDialog by remember { mutableStateOf(false) }

        if (showPersonalDateDialog) {
            PersonalDateDialog(
                onDismiss = { showPersonalDateDialog = false },
                onSave = { personalDate ->
                    showPersonalDateDialog = false
                    scope.launch {
                        personalDateRepo.insert(personalDate)
                        // Reload calendar data
                        val all = personalDateRepo.getAll()
                        calendarUiState = calendarUiState.copy(personalDates = all)
                    }
                }
            )
        }

        // Sync conflict dialog
        if (uiState.pendingSyncConflict) {
            SyncConflictDialog(
                onKeepLocal = {
                    mainViewModel.dismissSyncConflict()
                    scope.launch {
                        api?.deleteUserData()
                        initialSyncPreparer?.stampAllEntities()
                        syncManager?.sync(force = true)
                        userPreferences?.setPendingInitialSync(false)
                    }
                },
                onRestoreServer = {
                    mainViewModel.dismissSyncConflict()
                    scope.launch {
                        initialSyncPreparer?.clearAllLocalData()
                        userPreferences?.setLastSyncTime("")
                        syncManager?.sync(force = true)
                        userPreferences?.setPendingInitialSync(false)
                        mainViewModel.reload()
                    }
                }
            )
        }

        MainShellScreen(
            viewModel = mainViewModel,
            nightSummaryScheduler = nightSummaryScheduler,
            statsUiState = statsUiState,
            historyUiState = historyUiState,
            onNavigateToCreateBlock = { navController.navigate(ROUTE_BLOCK_CREATE) },
            onNavigateToEditBlock = { blockId ->
                navController.navigate("block/edit/$blockId")
            },
            onNavigateToEcosystem = { navController.navigate(ROUTE_ECOSYSTEM) },
            onNavigateToSettings = { navController.navigate(ROUTE_SETTINGS) },
            onRangeSelected = { range -> statsViewModel.selectRange(range) },
            onSearchQueryChanged = historyViewModel::setSearchQuery,
            onBlockFilterChanged = historyViewModel::setBlockFilter,
            onSignIn = {
                authManager?.let { am ->
                    signInLauncher.launch(am.getSignInIntent(context))
                }
            },
            calendarUiState = calendarUiState,
            onCalendarMonthChanged = { year, month ->
                scope.launch {
                    val firstDay = kotlinx.datetime.LocalDate(year, month, 1)
                    val lastDay = kotlinx.datetime.LocalDate(
                        year, month,
                        when (month) {
                            2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
                            4, 6, 9, 11 -> 30
                            else -> 31
                        }
                    )
                    val tasks = calendarDayTaskRepo.getCompletedRange(firstDay, lastDay) +
                        calendarDayTaskRepo.getByDate(firstDay) // ensure we get all statuses for the range
                    // Group all tasks for each date in the range
                    val tasksByDate = mutableMapOf<kotlinx.datetime.LocalDate, MutableList<com.mnebot.riptide.domain.model.DayTask>>()
                    for (d in 0..lastDay.day - 1) {
                        val date = kotlinx.datetime.LocalDate(year, month, d + 1)
                        val dateTasks = calendarDayTaskRepo.getByDate(date)
                        if (dateTasks.isNotEmpty()) {
                            tasksByDate[date] = dateTasks.toMutableList()
                        }
                    }
                    val personalDates = personalDateRepo.getAll()
                    val holidays = HolidayCalendar.getHolidays("ES", year)
                        .associate { it.date to it.name }
                    calendarUiState = CalendarUiState(
                        tasksByDate = tasksByDate,
                        personalDates = personalDates,
                        holidays = holidays
                    )
                }
            },
            onAddPersonalDate = { showPersonalDateDialog = true },
            onDeletePersonalDate = { id ->
                scope.launch {
                    personalDateRepo.delete(id)
                    val all = personalDateRepo.getAll()
                    calendarUiState = calendarUiState.copy(personalDates = all)
                }
            }
        )
    }

    composable(
        ROUTE_BLOCK_CREATE,
        enterTransition  = { slideInVertically(tween(300)) { it } },
        exitTransition   = { slideOutVertically(tween(300)) { it } },
        popEnterTransition  = { slideInVertically(tween(300)) { it } },
        popExitTransition   = { slideOutVertically(tween(300)) { it } }
    ) {
        val context = LocalContext.current
        val database = DatabaseProvider.getDatabase(context)
        val factory = BlockFormViewModelFactory(database)
        val blockFormViewModel: BlockFormViewModel = viewModel(factory = factory)
        val result by blockFormViewModel.result.collectAsStateWithLifecycle()

        LaunchedEffect(result) {
            if (result == BlockFormResult.Saved) {
                mainViewModel.reload()
                navController.popBackStack()
            }
        }

        BlockFormScreen(
            existingBlock = null,
            onSave = { block -> blockFormViewModel.saveBlock(block) },
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        ROUTE_BLOCK_EDIT,
        enterTransition  = { slideInVertically(tween(300)) { it } },
        exitTransition   = { slideOutVertically(tween(300)) { it } },
        popEnterTransition  = { slideInVertically(tween(300)) { it } },
        popExitTransition   = { slideOutVertically(tween(300)) { it } }
    ) { backStackEntry ->
        val blockId = backStackEntry.arguments?.getString("blockId") ?: return@composable
        val block = mainViewModel.uiState.collectAsStateWithLifecycle().value.blocks.firstOrNull { it.id == blockId }

        if (block == null) {
            LaunchedEffect(Unit) { navController.popBackStack() }
            return@composable
        }

        val context = LocalContext.current
        val database = DatabaseProvider.getDatabase(context)
        val factory = BlockFormViewModelFactory(database)
        val blockFormViewModel: BlockFormViewModel = viewModel(factory = factory)
        val result by blockFormViewModel.result.collectAsStateWithLifecycle()

        LaunchedEffect(result) {
            if (result == BlockFormResult.Saved || result == BlockFormResult.Deleted) {
                mainViewModel.reload()
                navController.popBackStack()
            }
        }

        BlockFormScreen(
            existingBlock = block,
            onSave = { updatedBlock -> blockFormViewModel.saveBlock(updatedBlock) },
            onDelete = { id -> blockFormViewModel.deleteBlock(id) },
            onBack = { navController.popBackStack() }
        )
    }

    composable(ROUTE_ECOSYSTEM) {
        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
        EcosystemScreen(
            ecosystemByCategory = uiState.ecosystemByCategory,
            creaturesData = uiState.creaturesData,
            onCreatureNicknameChanged = { creatureId, nickname ->
                mainViewModel.updateCreatureNickname(creatureId, nickname)
            },
            selectedPond = uiState.selectedPond,
            onPondSelected = { category ->
                mainViewModel.selectPond(category)
                navController.popBackStack()
            },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(ROUTE_SETTINGS) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
        val nightSummaryTime by nightSummaryScheduler.getNightSummaryTime()
            .collectAsStateWithLifecycle(initialValue = kotlinx.datetime.LocalTime(23, 30))
        val morningReminderTime by nightSummaryScheduler.getMorningReminderTime()
            .collectAsStateWithLifecycle(initialValue = null)

        val signInLauncher = rememberSignInLauncher(
            authManager, syncManager, initialSyncPreparer, mainViewModel, userPreferences
        )

        SettingsScreen(
            nightSummaryTime = nightSummaryTime,
            morningReminderTime = morningReminderTime,
            wallpaperFps = uiState.wallpaperFps,
            onWallpaperFpsChanged = { mainViewModel.setWallpaperFps(it) },
            onNightSummaryTimeChanged = { time ->
                nightSummaryScheduler.scheduleWorker(time)
                mainViewModel.updateNightSummaryTime(time)
            },
            onMorningReminderTimeChanged = { time ->
                scope.launch { nightSummaryScheduler.setMorningReminderTime(time) }
                nightSummaryScheduler.scheduleMorningReminder(time)
            },
            onSetLiveWallpaper = {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(context, RiptideWallpaperService::class.java)
                    )
                }
                context.startActivity(intent)
            },
            loggedInUser = uiState.loggedInUser,
            syncStatus = uiState.syncStatus,
            onSignIn = {
                authManager?.let { am ->
                    signInLauncher.launch(am.getSignInIntent(context))
                }
            },
            onSignOut = { mainViewModel.signOut() },
            onSyncNow = { mainViewModel.syncNow() },
            onNavigateToPackages = { navController.navigate(ROUTE_PACKAGES) },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(
        ROUTE_PACKAGES,
        enterTransition  = { slideInVertically(tween(300)) { it } },
        exitTransition   = { slideOutVertically(tween(300)) { it } },
        popEnterTransition  = { slideInVertically(tween(300)) { it } },
        popExitTransition   = { slideOutVertically(tween(300)) { it } }
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val database = DatabaseProvider.getDatabase(context)
        val prefs = remember { UserPreferencesRepositoryImpl(context) }
        val workBlockRepo = remember { WorkBlockRepositoryImpl(database.workBlockDao()) }
        val recurringTaskDefRepo = remember { RecurringTaskDefRepositoryImpl(database.recurringTaskDefDao()) }
        val dayTaskRepo = remember { DayTaskRepositoryImpl(database.dayTaskDao()) }
        val blockCategoryRepo = remember { BlockCategoryRepositoryImpl(database.blockCategoryDao()) }
        val ecosystemStateRepo = remember { EcosystemStateRepositoryImpl(database.ecosystemStateDao()) }

        val installer = remember {
            PackageInstaller(
                workBlockRepository = workBlockRepo,
                recurringTaskDefRepository = recurringTaskDefRepo,
                userPreferencesRepository = prefs,
                marineCategoryAssigner = MarineCategoryAssigner(workBlockRepo, blockCategoryRepo, ecosystemStateRepo),
                recurringTaskGenerator = RecurringTaskGenerator(recurringTaskDefRepo, dayTaskRepo)
            )
        }

        var installedIds by remember { mutableStateOf(emptySet<String>()) }
        var installing by remember { mutableStateOf(emptySet<String>()) }

        LaunchedEffect(Unit) {
            installedIds = prefs.getInstalledPackageIds()
        }

        PackageBrowserScreen(
            packages = TaskPackages.ALL,
            installedIds = installedIds,
            installing = installing,
            onInstall = { pkg ->
                installing = installing + pkg.id
                scope.launch {
                    installer.install(pkg)
                    installedIds = prefs.getInstalledPackageIds()
                    installing = installing - pkg.id
                    mainViewModel.reload()
                }
            },
            onUninstall = { pkg ->
                installing = installing + pkg.id
                scope.launch {
                    installer.uninstall(pkg)
                    installedIds = prefs.getInstalledPackageIds()
                    installing = installing - pkg.id
                    mainViewModel.reload()
                }
            },
            onNavigateBack = { navController.popBackStack() }
        )
    }
}

@Composable
private fun rememberSignInLauncher(
    authManager: AuthManager?,
    syncManager: SyncManager?,
    initialSyncPreparer: InitialSyncPreparer?,
    mainViewModel: MainViewModel,
    userPreferences: UserPrefs?
): androidx.activity.result.ActivityResultLauncher<android.content.Intent> {
    val scope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            authManager?.let { am ->
                val loginResult = am.handleSignInResult(result.data)
                loginResult.onSuccess { signInResult ->
                    mainViewModel.onSignInCompleted(signInResult.user)

                    val hasServer = signInResult.hasExistingServerData
                    val hasLocal = initialSyncPreparer?.hasLocalData() ?: false

                    when {
                        // Both have data → conflict dialog
                        hasServer && hasLocal -> {
                            userPreferences?.setPendingInitialSync(true)
                            mainViewModel.showSyncConflict()
                        }
                        // Only local → stamp + push
                        !hasServer && hasLocal -> {
                            initialSyncPreparer?.stampAllEntities()
                            syncManager?.sync(force = true)
                        }
                        // Only server → pull-only
                        hasServer && !hasLocal -> {
                            syncManager?.sync(force = true)
                            mainViewModel.reload()
                        }
                        // Neither → just schedule periodic sync
                        else -> { /* SyncWorker handles periodic sync */ }
                    }
                }
            }
        }
    }
}
