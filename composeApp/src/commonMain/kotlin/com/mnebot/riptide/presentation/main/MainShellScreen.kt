package com.mnebot.riptide.presentation.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mnebot.riptide.NightSummaryScheduler
import com.mnebot.riptide.domain.model.LoggedInUser
import com.mnebot.riptide.domain.model.SyncStatus
import com.mnebot.riptide.presentation.aquarium.AquariumBackground
import com.mnebot.riptide.presentation.aquarium.AquariumCreatures
import com.mnebot.riptide.presentation.aquarium.CreatureFreezeState
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.presentation.aquarium.rememberCreatureFreezeState
import com.mnebot.riptide.presentation.calendar.CalendarTabContent
import com.mnebot.riptide.presentation.calendar.CalendarUiState
import com.mnebot.riptide.domain.model.PersonalDate
import com.mnebot.riptide.presentation.history.HistoryUiState
import com.mnebot.riptide.presentation.navigation.BottomNavTab
import com.mnebot.riptide.presentation.navigation.RiptideBottomBar
import com.mnebot.riptide.presentation.pond.PondTabContent
import com.mnebot.riptide.presentation.progress.ProgressTabContent
import com.mnebot.riptide.presentation.stats.StatsRange
import com.mnebot.riptide.presentation.stats.StatsUiState

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
    onSignIn: () -> Unit = {},
    calendarUiState: CalendarUiState = CalendarUiState(),
    onCalendarMonthChanged: (Int, Int) -> Unit = { _, _ -> },
    onAddPersonalDate: () -> Unit = {},
    onDeletePersonalDate: (String) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(BottomNavTab.TODAY) }
    val creatureFreezeState = rememberCreatureFreezeState()
    val uiState by viewModel.uiState.collectAsState()
    var pondSelectedCreature by remember { mutableStateOf<Pair<MarineCreature, CreatureSpec>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Aquarium background — always rendered for TODAY and POND tabs
        if (selectedTab == BottomNavTab.TODAY || selectedTab == BottomNavTab.POND) {
            AquariumBackground(biomeTheme = uiState.selectedBiome)
            AquariumCreatures(
                ecosystemByCategory = uiState.ecosystemByCategory,
                creatureLevelBySpecies = uiState.creatureLevelBySpecies,
                creaturesData = uiState.creaturesData,
                freezeState = creatureFreezeState,
                onCreatureTap = { creature, spec ->
                    if (selectedTab == BottomNavTab.POND) {
                        pondSelectedCreature = creature to spec
                    }
                },
                categoryFilter = if (selectedTab == BottomNavTab.POND) uiState.selectedPond else null
            )
        }

        // Tab content with crossfade
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            },
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                BottomNavTab.TODAY -> MainScreen(
                    viewModel = viewModel,
                    nightSummaryScheduler = nightSummaryScheduler,
                    onNavigateToCreateBlock = onNavigateToCreateBlock,
                    onNavigateToEditBlock = onNavigateToEditBlock,
                    onNavigateToSettings = onNavigateToSettings,
                    onSignIn = onSignIn
                )
                BottomNavTab.POND -> PondTabContent(
                    ecosystemByCategory = uiState.ecosystemByCategory,
                    creaturesData = uiState.creaturesData,
                    creatureFreezeState = creatureFreezeState,
                    selectedCreature = pondSelectedCreature,
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
                BottomNavTab.CALENDAR -> CalendarTabContent(
                    uiState = calendarUiState,
                    onMonthChanged = onCalendarMonthChanged,
                    onAddPersonalDate = onAddPersonalDate,
                    onDeletePersonalDate = onDeletePersonalDate
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

        // Bottom navigation bar
        RiptideBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
