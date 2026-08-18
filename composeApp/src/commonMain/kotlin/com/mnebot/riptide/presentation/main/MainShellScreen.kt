package com.mnebot.riptide.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mnebot.riptide.NightSummaryScheduler
import com.mnebot.riptide.presentation.aquarium.AquariumBackground
import com.mnebot.riptide.presentation.aquarium.AquariumCreatures
import com.mnebot.riptide.presentation.aquarium.CreaturePosition
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.presentation.aquarium.rememberCreatureFreezeState
import com.mnebot.riptide.presentation.history.HistoryUiState
import com.mnebot.riptide.presentation.navigation.BottomNavTab
import com.mnebot.riptide.presentation.navigation.RiptidePagerIndicator
import com.mnebot.riptide.presentation.pond.PondTabContent
import com.mnebot.riptide.presentation.progress.ProgressTabContent
import com.mnebot.riptide.presentation.stats.StatsRange
import com.mnebot.riptide.presentation.stats.StatsUiState
import kotlinx.coroutines.launch

@Composable
fun MainShellScreen(
    viewModel: MainViewModel,
    nightSummaryScheduler: NightSummaryScheduler,
    statsUiState: StatsUiState,
    historyUiState: HistoryUiState,
    onNavigateToCreateBlock: () -> Unit,
    onNavigateToEditBlock: (String) -> Unit,
    onNavigateToEcosystem: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRangeSelected: (StatsRange) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onBlockFilterChanged: (String?) -> Unit,
    onSignIn: () -> Unit = {}
) {
    val tabs = remember { BottomNavTab.entries.toList() }
    val todayIndex = tabs.indexOf(BottomNavTab.TODAY).coerceAtLeast(0)
    var initialIndex by rememberSaveable { mutableStateOf(todayIndex) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val creatureFreezeState = rememberCreatureFreezeState()
    val uiState by viewModel.uiState.collectAsState()
    var pondSelectedCreature by remember { mutableStateOf<Pair<MarineCreature, CreatureSpec>?>(null) }

    // Creature positions are computed every frame inside `AquariumCreatures` and mirrored
    // to this hoisted state. PondTabContent reads it to do its own hit-testing inside the
    // pager content (so the FAB stays clickable and horizontal swipes still navigate).
    val creaturePositions = remember { mutableStateOf<List<CreaturePosition>>(emptyList()) }

    LaunchedEffect(pagerState.currentPage) {
        initialIndex = pagerState.currentPage
    }

    val currentTab = tabs[pagerState.currentPage]
    val showAquarium = currentTab == BottomNavTab.TODAY || currentTab == BottomNavTab.POND

    Box(modifier = Modifier.fillMaxSize()) {
        if (showAquarium) {
            AquariumBackground(biomeTheme = uiState.selectedBiome)
            // Decorative + position publisher. Tap detection is delegated to PondTabContent
            // so the pager (and its FAB) keep handling pointer events normally.
            AquariumCreatures(
                ecosystemByCategory = uiState.ecosystemByCategory,
                creatureLevelBySpecies = uiState.creatureLevelBySpecies,
                creaturesData = uiState.creaturesData,
                freezeState = creatureFreezeState,
                positionsState = creaturePositions,
                tapEnabled = false
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            when (tabs[page]) {
                BottomNavTab.TODAY -> MainScreen(
                    viewModel = viewModel,
                    nightSummaryScheduler = nightSummaryScheduler,
                    onNavigateToCreateBlock = onNavigateToCreateBlock,
                    onNavigateToEditBlock = onNavigateToEditBlock,
                    onNavigateToSettings = onNavigateToSettings,
                    onSignIn = onSignIn
                )
                BottomNavTab.POND -> PondTabContent(
                    creaturesData = uiState.creaturesData,
                    creaturePositions = creaturePositions.value,
                    creatureFreezeState = creatureFreezeState,
                    selectedCreature = pondSelectedCreature,
                    ecosystemByCategory = uiState.ecosystemByCategory,
                    onCreatureTap = { creature, spec ->
                        pondSelectedCreature = creature to spec
                    },
                    onCreatureDismiss = {
                        pondSelectedCreature?.let { (creature, _) ->
                            creatureFreezeState.unfreeze(creature.species)
                        }
                        pondSelectedCreature = null
                    },
                    onCreatureNicknameChanged = { creatureId, nickname ->
                        viewModel.updateCreatureNickname(creatureId, nickname)
                    },
                    onNavigateToEcosystem = onNavigateToEcosystem
                )
                BottomNavTab.PROGRESS -> ProgressTabContent(
                    statsUiState = statsUiState,
                    historyUiState = historyUiState,
                    onRangeSelected = onRangeSelected,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onBlockFilterChanged = onBlockFilterChanged
                )
            }
        }

        RiptidePagerIndicator(
            pageCount = tabs.size,
            selectedIndex = pagerState.currentPage,
            onDotClick = { index ->
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
