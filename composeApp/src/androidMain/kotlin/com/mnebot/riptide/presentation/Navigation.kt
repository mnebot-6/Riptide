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
import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mnebot.riptide.BuildConfig
import com.mnebot.riptide.data.remote.AuthManager
import com.mnebot.riptide.data.remote.RiptideApi
import com.mnebot.riptide.data.sync.InitialSyncPreparer
import com.mnebot.riptide.data.sync.SyncManager
import com.mnebot.riptide.domain.repository.UserPreferencesRepository as UserPrefs
import com.mnebot.riptide.wallpaper.RiptideWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        OnboardingScreen(
            onComplete = { _ ->
                scope.launch {
                    userPreferencesRepository.setOnboardingCompleted()
                }
                navController.navigate(ROUTE_MAIN) {
                    popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                }
            },
            onOpenUrl = { url ->
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
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

        RequestNotificationsPermissionOnce()

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

        // Sync conflict dialog
        if (uiState.pendingSyncConflict) {
            SyncConflictDialog(
                // El flag debe bajarse ANTES de sync(): sync() aborta mientras
                // hasPendingInitialSync() sea true.
                onKeepLocal = {
                    mainViewModel.dismissSyncConflict()
                    scope.launch {
                        api?.deleteUserData()
                        initialSyncPreparer?.stampAllEntities()
                        userPreferences?.setPendingInitialSync(false)
                        syncManager?.sync(force = true)
                    }
                },
                onRestoreServer = {
                    mainViewModel.dismissSyncConflict()
                    scope.launch {
                        initialSyncPreparer?.clearAllLocalData()
                        userPreferences?.setLastSyncTime("")
                        userPreferences?.setPendingInitialSync(false)
                        syncManager?.sync(force = true)
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
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(ROUTE_SETTINGS) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
        val morningReminderTime by nightSummaryScheduler.getMorningReminderTime()
            .collectAsStateWithLifecycle(initialValue = null)

        val signInLauncher = rememberSignInLauncher(
            authManager, syncManager, initialSyncPreparer, mainViewModel, userPreferences
        )

        SettingsScreen(
            morningReminderTime = morningReminderTime,
            wallpaperFps = uiState.wallpaperFps,
            onWallpaperFpsChanged = { mainViewModel.setWallpaperFps(it) },
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
            lastSyncMillis = uiState.lastSyncMillis,
            lastSyncError = uiState.lastSyncError,
            onSignIn = {
                authManager?.let { am ->
                    signInLauncher.launch(am.getSignInIntent(context))
                }
            },
            onSignOut = { mainViewModel.signOut() },
            onSyncNow = { mainViewModel.syncNow() },
            onNavigateBack = { navController.popBackStack() },
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            onOpenUrl = { url ->
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            },
            onDeleteAccount = {
                try {
                    api?.deleteAccount()
                    withContext(Dispatchers.IO) {
                        DatabaseProvider.getDatabase(context).clearAllTables()
                    }
                    userPreferences?.clearAuth()
                    mainViewModel.signOut()
                    mainViewModel.reload()
                    navController.popBackStack()
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        )
    }
}

/**
 * Ask for POST_NOTIFICATIONS on Android 13+ the first time the user lands on Main.
 * If the system has already remembered a denial it silently no-ops — the OS won't
 * show the dialog a third time. Result is ignored: notification workers check
 * permission themselves and degrade gracefully when denied.
 */
@Composable
private fun RequestNotificationsPermissionOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignored */ }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
