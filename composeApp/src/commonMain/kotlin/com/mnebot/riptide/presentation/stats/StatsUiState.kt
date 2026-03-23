package com.mnebot.riptide.presentation.stats

import com.mnebot.riptide.domain.model.BlockStreak
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.WorkBlock

enum class StatsRange { WEEK, MONTH }

data class StatsUiState(
    val range: StatsRange = StatsRange.WEEK,
    val summaries: List<DaySummary> = emptyList(),
    val streaksByBlock: List<Pair<WorkBlock, BlockStreak>> = emptyList(),
    val isLoading: Boolean = true
)
