package com.mnebot.riptide.presentation.history

import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.WorkBlock
import kotlinx.datetime.LocalDate

enum class HistoryRange(val days: Int) {
    DAYS_30(30),
    DAYS_60(60),
    DAYS_90(90)
}

data class HistoryUiState(
    val range: HistoryRange = HistoryRange.DAYS_30,
    val tasksByDate: Map<LocalDate, List<DayTask>> = emptyMap(),
    val summaryByDate: Map<LocalDate, DaySummary> = emptyMap(),
    val blocks: List<WorkBlock> = emptyList(),
    val isLoading: Boolean = true
)
