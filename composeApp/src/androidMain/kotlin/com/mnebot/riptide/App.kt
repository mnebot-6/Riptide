package com.mnebot.riptide

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mnebot.riptide.data.remote.AuthManager
import com.mnebot.riptide.data.remote.RiptideApi
import com.mnebot.riptide.data.sync.InitialSyncPreparer
import com.mnebot.riptide.data.sync.SyncManager
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import com.mnebot.riptide.presentation.ROUTE_MAIN
import com.mnebot.riptide.presentation.ROUTE_ONBOARDING
import com.mnebot.riptide.presentation.mainGraph
import com.mnebot.riptide.presentation.onboardingGraph
import com.mnebot.riptide.presentation.main.MainViewModel

@Composable
fun App(
    viewModel: MainViewModel,
    nightSummaryScheduler: NightSummaryScheduler,
    userPreferencesRepository: UserPreferencesRepository,
    authManager: AuthManager? = null,
    syncManager: SyncManager? = null,
    initialSyncPreparer: InitialSyncPreparer? = null,
    api: RiptideApi? = null
) {
    val onboardingCompleted by remember { userPreferencesRepository.hasCompletedOnboarding() }
        .collectAsState(initial = null)

    val navController = rememberNavController()

    // Wait for DataStore to emit the first value before building the NavHost.
    // This prevents the NavHost from being created with the wrong startDestination.
    val completed = onboardingCompleted ?: return

    NavHost(
        navController = navController,
        startDestination = if (completed) ROUTE_MAIN else ROUTE_ONBOARDING,
        enterTransition  = { slideInHorizontally(tween(300)) { it } },
        exitTransition   = { slideOutHorizontally(tween(300)) { -it / 3 } },
        popEnterTransition  = { slideInHorizontally(tween(300)) { -it / 3 } },
        popExitTransition   = { slideOutHorizontally(tween(300)) { it } }
    ) {
        onboardingGraph(
            userPreferencesRepository = userPreferencesRepository,
            navController = navController
        )
        mainGraph(
            mainViewModel = viewModel,
            nightSummaryScheduler = nightSummaryScheduler,
            navController = navController,
            authManager = authManager,
            syncManager = syncManager,
            initialSyncPreparer = initialSyncPreparer,
            api = api,
            userPreferences = userPreferencesRepository
        )
    }
}
