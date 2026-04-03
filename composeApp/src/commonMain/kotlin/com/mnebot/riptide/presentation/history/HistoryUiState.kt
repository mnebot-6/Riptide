package com.mnebot.riptide.presentation.history

import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.WorkBlock
import kotlinx.datetime.LocalDate

enum class HistoryRange { DAYS_30, DAYS_60, DAYS_90, ALL_TIME }

data class HistoryUiState(
    val tasksByDate: Map<LocalDate, List<DayTask>> = emptyMap(),
    val filteredTasksByDate: Map<LocalDate, List<DayTask>> = emptyMap(),
    val summaryByDate: Map<LocalDate, DaySummary> = emptyMap(),
    val blocks: List<WorkBlock> = emptyList(),
    val isLoading: Boolean = true,
    val selectedRange: HistoryRange = HistoryRange.ALL_TIME,
    val searchQuery: String = "",
    val selectedBlockId: String? = null
)
