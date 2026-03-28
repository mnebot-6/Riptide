package com.mnebot.riptide.presentation.stats

import com.mnebot.riptide.domain.model.BlockStreak
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.WorkBlock

enum class StatsRange { WEEK, MONTH, ALL_TIME }

data class MonthStat(
    val yearMonth: String,   // "2026-03"
    val label: String,       // "Mar"
    val completionRate: Float // 0.0-1.0
)

data class StatsUiState(
    val range: StatsRange = StatsRange.WEEK,
    val summaries: List<DaySummary> = emptyList(),
    val streaksByBlock: List<Pair<WorkBlock, BlockStreak>> = emptyList(),
    val isLoading: Boolean = true,
    // All-time KPIs
    val longestStreakEver: Int = 0,
    val avgDailyCompletion: Float = 0f,
    val totalDaysActive: Int = 0,
    val totalCompleted: Int = 0,
    val bestWeekLabel: String = "",
    val bestWeekAvg: Float = 0f,
    val monthlyTrend: List<MonthStat> = emptyList(),
    val currentMonthRate: Float = 0f,
    val previousMonthRate: Float = 0f
)
