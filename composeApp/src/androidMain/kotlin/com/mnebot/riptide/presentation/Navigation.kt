package com.mnebot.riptide.presentation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.mnebot.riptide.NightSummaryScheduler
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.presentation.block.BlockFormResult
import com.mnebot.riptide.presentation.block.BlockFormScreen
import com.mnebot.riptide.presentation.block.BlockFormViewModel
import com.mnebot.riptide.presentation.block.BlockFormViewModelFactory
import com.mnebot.riptide.presentation.main.MainScreen
import com.mnebot.riptide.presentation.main.MainViewModel
import androidx.compose.runtime.collectAsState

const val ROUTE_MAIN = "main"
const val ROUTE_BLOCK_CREATE = "block/create"
const val ROUTE_BLOCK_EDIT = "block/edit/{blockId}"

fun NavGraphBuilder.mainGraph(
    mainViewModel: MainViewModel,
    nightSummaryScheduler: NightSummaryScheduler,
    navController: NavController
) {
    composable(ROUTE_MAIN) {
        MainScreen(
            viewModel = mainViewModel,
            nightSummaryScheduler = nightSummaryScheduler,
            onNavigateToCreateBlock = { navController.navigate(ROUTE_BLOCK_CREATE) },
            onNavigateToEditBlock = { blockId ->
                navController.navigate("block/edit/$blockId")
            }
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
}