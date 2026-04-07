package com.mnebot.riptide.presentation.main

import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.LoggedInUser
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.model.SyncStatus
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import kotlinx.datetime.LocalDate

data class MainUiState(
    val selectedDate: LocalDate,
    val blocks: List<WorkBlock> = emptyList(),
    val tasksByBlock: Map<String?, List<DayTask>> = emptyMap(),
    val streaksByBlock: Map<String, Int> = emptyMap(),
    val ecosystemByCategory: Map<MarineCategory, EcosystemState> = emptyMap(),
    val creatureLevelBySpecies: Map<CreatureSpecies, Int> = emptyMap(),
    val creaturesData: List<MarineCreature> = emptyList(),
    val globalStreak: Int = 0,
    val pendingSummary: DaySummary? = null,
    // Nuevo sistema de lootbox
    val pendingLootboxes: List<PendingLootbox> = emptyList(),
    val revealedSpecies: CreatureSpec? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Sync
    val loggedInUser: LoggedInUser? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val wallpaperFps: Int = 30,
    // Initial sync conflict
    val pendingSyncConflict: Boolean = false
)
