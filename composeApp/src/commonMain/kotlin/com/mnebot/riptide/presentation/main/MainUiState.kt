package com.mnebot.riptide.presentation.main

import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.WorkBlock
import kotlinx.datetime.LocalDate

// MainUiState.kt
data class MainUiState(
    val selectedDate: LocalDate,
    val blocks: List<WorkBlock> = emptyList(),
    val tasksByBlock: Map<String?, List<DayTask>> = emptyMap(),
    val streaksByBlock: Map<String, Int> = emptyMap(),
    val pendingSummary: DaySummary? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)