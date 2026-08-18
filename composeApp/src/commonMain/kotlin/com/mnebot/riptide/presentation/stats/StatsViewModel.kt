package com.mnebot.riptide.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.domain.DayStreak
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import com.mnebot.riptide.presentation.main.currentDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.number

class StatsViewModel(
    private val daySummaryRepository: DaySummaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun selectRange(range: StatsRange) {
        _uiState.update { it.copy(range = range) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val today = currentDate()
            val range = _uiState.value.range

            val longestStreakEver = DayStreak.longest(daySummaryRepository.getAll())

            when (range) {
                StatsRange.WEEK, StatsRange.MONTH -> {
                    val days = if (range == StatsRange.WEEK) 6 else 29
                    val from = today.minus(days, DateTimeUnit.DAY)
                    val summaries = daySummaryRepository.getRange(from, today)

                    _uiState.update {
                        it.copy(
                            summaries = summaries,
                            longestStreakEver = longestStreakEver,
                            isLoading = false
                        )
                    }
                }

                StatsRange.ALL_TIME -> {
                    val allSummaries = daySummaryRepository.getAll()
                    val sorted = allSummaries.sortedBy { it.date }

                    val daysWithTasks = sorted.filter { it.tasksTotal > 0 }
                    val avgDailyCompletion = if (daysWithTasks.isNotEmpty()) {
                        daysWithTasks
                            .map { it.tasksCompleted.toFloat() / it.tasksTotal }
                            .average()
                            .toFloat()
                    } else 0f

                    val totalDaysActive = sorted.count { it.tasksCompleted > 0 }
                    val totalCompleted = sorted.sumOf { it.tasksCompleted }

                    // Best week: sliding window of 7 days
                    val (bestWeekLabel, bestWeekAvg) = findBestWeek(daysWithTasks)

                    // Monthly trend
                    val monthlyTrend = buildMonthlyTrend(sorted)

                    // Current and previous month rates
                    val currentYm = yearMonthKey(today)
                    val prevMonth = today.minus(1, DateTimeUnit.MONTH)
                    val previousYm = yearMonthKey(prevMonth)

                    val currentMonthRate = monthlyTrend
                        .firstOrNull { it.yearMonth == currentYm }?.completionRate ?: 0f
                    val previousMonthRate = monthlyTrend
                        .firstOrNull { it.yearMonth == previousYm }?.completionRate ?: 0f

                    _uiState.update {
                        it.copy(
                            summaries = allSummaries,
                            longestStreakEver = longestStreakEver,
                            avgDailyCompletion = avgDailyCompletion,
                            totalDaysActive = totalDaysActive,
                            totalCompleted = totalCompleted,
                            bestWeekLabel = bestWeekLabel,
                            bestWeekAvg = bestWeekAvg,
                            monthlyTrend = monthlyTrend,
                            currentMonthRate = currentMonthRate,
                            previousMonthRate = previousMonthRate,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun findBestWeek(sorted: List<DaySummary>): Pair<String, Float> {
        if (sorted.size < 7) {
            // Not enough data for a full week; use all data as the "best week"
            if (sorted.isEmpty()) return "" to 0f
            val avg = sorted
                .map { it.tasksCompleted.toFloat() / it.tasksTotal.coerceAtLeast(1) }
                .average().toFloat()
            val startLabel = formatDateShort(sorted.first().date)
            val endLabel = formatDateShort(sorted.last().date)
            return "$startLabel - $endLabel" to avg
        }

        var bestAvg = 0f
        var bestStart = sorted.first().date

        // Use all summaries sorted by date; sliding window by index
        for (i in 0..sorted.size - 7) {
            val window = sorted.subList(i, i + 7)
            val avg = window
                .map { it.tasksCompleted.toFloat() / it.tasksTotal.coerceAtLeast(1) }
                .average().toFloat()
            if (avg > bestAvg) {
                bestAvg = avg
                bestStart = window.first().date
            }
        }

        val endDate = bestStart.plus(6, DateTimeUnit.DAY)
        val label = "${formatDateShort(bestStart)} - ${formatDateShort(endDate)}"
        return label to bestAvg
    }

    private fun buildMonthlyTrend(sorted: List<DaySummary>): List<MonthStat> {
        return sorted
            .groupBy { yearMonthKey(it.date) }
            .map { (ym, summaries) ->
                val withTasks = summaries.filter { it.tasksTotal > 0 }
                val rate = if (withTasks.isNotEmpty()) {
                    withTasks.map { it.tasksCompleted.toFloat() / it.tasksTotal }
                        .average().toFloat()
                } else 0f

                val month = summaries.first().date.month
                val label = MONTH_LABELS[month.number - 1]
                MonthStat(yearMonth = ym, label = label, completionRate = rate)
            }
            .sortedBy { it.yearMonth }
    }

    private fun yearMonthKey(date: LocalDate): String {
        val m = (date.month.ordinal + 1).toString().padStart(2, '0')
        return "${date.year}-$m"
    }

    private fun formatDateShort(date: LocalDate): String {
        val monthLabel = MONTH_LABELS[date.month.ordinal]
        return "$monthLabel ${date.day}"
    }

    companion object {
        // Short month labels used for trend chart and best-week formatting.
        // These are code-level fallbacks; the UI uses stringResource for user-facing text.
        private val MONTH_LABELS = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
    }
}
