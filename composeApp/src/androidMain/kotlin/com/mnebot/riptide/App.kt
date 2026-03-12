package com.mnebot.riptide

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mnebot.riptide.presentation.ROUTE_MAIN
import com.mnebot.riptide.presentation.mainGraph
import com.mnebot.riptide.presentation.main.MainViewModel

@Composable
fun App(
    viewModel: MainViewModel,
    nightSummaryScheduler: NightSummaryScheduler
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ROUTE_MAIN
    ) {
        mainGraph(
            mainViewModel = viewModel,
            nightSummaryScheduler = nightSummaryScheduler,
            navController = navController
        )
    }
}