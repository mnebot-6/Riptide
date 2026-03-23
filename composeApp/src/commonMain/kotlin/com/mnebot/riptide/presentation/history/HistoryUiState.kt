package com.mnebot.riptide.presentation.history

import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.WorkBlock
import kotlinx.datetime.LocalDate

data class HistoryUiState(
    val tasksByDate: Map<LocalDate, List<DayTask>> = emptyMap(),
    val summaryByDate: Map<LocalDate, DaySummary> = emptyMap(),
    val blocks: List<WorkBlock> = emptyList(),
    val isLoading: Boolean = true
)
