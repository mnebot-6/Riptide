package com.mnebot.riptide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
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
    userPreferencesRepository: UserPreferencesRepository
) {
    val onboardingCompleted by remember { userPreferencesRepository.hasCompletedOnboarding() }
        .collectAsState(initial = null)

    val navController = rememberNavController()

    // Wait for DataStore to emit the first value before building the NavHost.
    // This prevents the NavHost from being created with the wrong startDestination.
    val completed = onboardingCompleted ?: return

    NavHost(
        navController = navController,
        startDestination = if (completed) ROUTE_MAIN else ROUTE_ONBOARDING
    ) {
        onboardingGraph(
            userPreferencesRepository = userPreferencesRepository,
            navController = navController
        )
        mainGraph(
            mainViewModel = viewModel,
            nightSummaryScheduler = nightSummaryScheduler,
            navController = navController
        )
    }
}
