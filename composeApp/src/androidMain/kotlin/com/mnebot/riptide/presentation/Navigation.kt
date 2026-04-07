package com.mnebot.riptide.presentation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.mnebot.riptide.presentation.onboarding.OnboardingScreen
import com.mnebot.riptide.presentation.settings.SettingsScreen
import com.mnebot.riptide.presentation.stats.StatsViewModel
import com.mnebot.riptide.presentation.stats.StatsViewModelFactory
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import com.mnebot.riptide.data.remote.AuthManager
import com.mnebot.riptide.data.sync.InitialSyncPreparer
import com.mnebot.riptide.data.sync.SyncManager
import com.mnebot.riptide.wallpaper.RiptideWallpaperService
import kotlinx.coroutines.launch

const val ROUTE_ONBOARDING = "onboarding"
const val ROUTE_MAIN = "main"
const val ROUTE_BLOCK_CREATE = "block/create"
const val ROUTE_BLOCK_EDIT = "block/edit/{blockId}"
const val ROUTE_ECOSYSTEM = "ecosystem"
const val ROUTE_SETTINGS = "settings"

fun NavGraphBuilder.onboardingGraph(
    userPreferencesRepository: UserPreferencesRepository,
    navController: NavController
) {
    composable(ROUTE_ONBOARDING) {
        val scope = rememberCoroutineScope()
        OnboardingScreen(
            onComplete = {
                scope.launch { userPreferencesRepository.setOnboardingCompleted() }
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
    initialSyncPreparer: InitialSyncPreparer? = null
) {
    composable(ROUTE_MAIN) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val signInLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            scope.launch {
                authManager?.let { am ->
                    val loginResult = am.handleSignInResult(result.data)
                    loginResult.onSuccess { user ->
                        mainViewModel.onSignInCompleted(user)
                        initialSyncPreparer?.stampAllEntities()
                        syncManager?.sync()
                    }
                }
            }
        }

        // Stats ViewModel
        val statsViewModel: StatsViewModel = viewModel(factory = StatsViewModelFactory(context))
        val statsUiState by statsViewModel.uiState.collectAsState()

        // History ViewModel
        val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(context))
        val historyUiState by historyViewModel.uiState.collectAsState()

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
        val block = mainViewModel.uiState.collectAsState().value.blocks.firstOrNull { it.id == blockId }

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
        val uiState by mainViewModel.uiState.collectAsState()
        EcosystemScreen(
            ecosystemByCategory = uiState.ecosystemByCategory,
            creaturesData = uiState.creaturesData,
            onCreatureNicknameChanged = { creatureId, nickname ->
                mainViewModel.updateCreatureNickname(creatureId, nickname)
            },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(ROUTE_SETTINGS) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val uiState by mainViewModel.uiState.collectAsState()
        val nightSummaryTime by nightSummaryScheduler.getNightSummaryTime()
            .collectAsState(initial = kotlinx.datetime.LocalTime(23, 30))
        val morningReminderTime by nightSummaryScheduler.getMorningReminderTime()
            .collectAsState(initial = null)

        val signInLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            scope.launch {
                authManager?.let { am ->
                    val loginResult = am.handleSignInResult(result.data)
                    loginResult.onSuccess { user ->
                        mainViewModel.onSignInCompleted(user)
                        initialSyncPreparer?.stampAllEntities()
                        syncManager?.sync()
                    }
                }
            }
        }

        SettingsScreen(
            nightSummaryTime = nightSummaryTime,
            morningReminderTime = morningReminderTime,
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
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
