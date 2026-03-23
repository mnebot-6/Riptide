package com.mnebot.riptide.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.domain.repository.BlockStreakRepository
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import com.mnebot.riptide.domain.repository.WorkBlockRepository
import com.mnebot.riptide.presentation.main.currentDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

class StatsViewModel(
    private val daySummaryRepository: DaySummaryRepository,
    private val blockStreakRepository: BlockStreakRepository,
    private val workBlockRepository: WorkBlockRepository
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
            val days = if (_uiState.value.range == StatsRange.WEEK) 6 else 29
            val from = today.minus(days, DateTimeUnit.DAY)

            val summaries = daySummaryRepository.getRange(from, today)

            val allStreaks = blockStreakRepository.getAll()
            val blocks = workBlockRepository.getAll()
            val streaksByBlock = blocks
                .mapNotNull { block ->
                    val streak = allStreaks.firstOrNull { it.blockId == block.id }
                        ?: return@mapNotNull null
                    if (streak.currentStreak > 0) block to streak else null
                }
                .sortedByDescending { (_, streak) -> streak.currentStreak }

            _uiState.update {
                it.copy(
                    summaries = summaries,
                    streaksByBlock = streaksByBlock,
                    isLoading = false
                )
            }
        }
    }
}
