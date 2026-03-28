package com.mnebot.riptide.presentation

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
import com.mnebot.riptide.presentation.history.HistoryScreen
import com.mnebot.riptide.presentation.history.HistoryViewModel
import com.mnebot.riptide.presentation.history.HistoryViewModelFactory
import com.mnebot.riptide.presentation.main.MainScreen
import com.mnebot.riptide.presentation.main.MainViewModel
import com.mnebot.riptide.presentation.onboarding.OnboardingScreen
import com.mnebot.riptide.presentation.stats.StatsScreen
import com.mnebot.riptide.presentation.stats.StatsViewModel
import com.mnebot.riptide.presentation.stats.StatsViewModelFactory
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.runtime.collectAsState
import com.mnebot.riptide.wallpaper.RiptideWallpaperService
import kotlinx.coroutines.launch

const val ROUTE_ONBOARDING = "onboarding"
const val ROUTE_MAIN = "main"
const val ROUTE_BLOCK_CREATE = "block/create"
const val ROUTE_BLOCK_EDIT = "block/edit/{blockId}"
const val ROUTE_ECOSYSTEM = "ecosystem"
const val ROUTE_STATS = "stats"
const val ROUTE_HISTORY = "history"

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
    navController: NavController
) {
    composable(ROUTE_MAIN) {
        val context = LocalContext.current
        MainScreen(
            viewModel = mainViewModel,
            nightSummaryScheduler = nightSummaryScheduler,
            onNavigateToCreateBlock = { navController.navigate(ROUTE_BLOCK_CREATE) },
            onNavigateToEditBlock = { blockId ->
                navController.navigate("block/edit/$blockId")
            },
            onNavigateToEcosystem = { navController.navigate(ROUTE_ECOSYSTEM) },
            onSetLiveWallpaper = {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(context, RiptideWallpaperService::class.java)
                    )
                }
                context.startActivity(intent)
            },
            onNavigateToStats = { navController.navigate(ROUTE_STATS) },
            onNavigateToHistory = { navController.navigate(ROUTE_HISTORY) }
        )
    }

    composable(ROUTE_BLOCK_CREATE) {
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

    composable(ROUTE_BLOCK_EDIT) { backStackEntry ->
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

    composable(ROUTE_STATS) {
        val context = LocalContext.current
        val statsViewModel: StatsViewModel = viewModel(factory = StatsViewModelFactory(context))
        val uiState by statsViewModel.uiState.collectAsState()
        StatsScreen(
            uiState = uiState,
            onRangeSelected = { range -> statsViewModel.selectRange(range) },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(ROUTE_HISTORY) {
        val context = LocalContext.current
        val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(context))
        val uiState by historyViewModel.uiState.collectAsState()
        HistoryScreen(
            uiState = uiState,
            onNavigateBack = { navController.popBackStack() },
            onRangeSelected = historyViewModel::selectRange,
            onSearchQueryChanged = historyViewModel::setSearchQuery,
            onBlockFilterChanged = historyViewModel::setBlockFilter
        )
    }
}
