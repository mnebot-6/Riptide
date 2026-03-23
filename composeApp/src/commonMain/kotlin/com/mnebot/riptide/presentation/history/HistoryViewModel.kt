package com.mnebot.riptide.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskSchedule
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.WorkBlockRepository
import com.mnebot.riptide.presentation.main.currentDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

class HistoryViewModel(
    private val dayTaskRepository: DayTaskRepository,
    private val daySummaryRepository: DaySummaryRepository,
    private val workBlockRepository: WorkBlockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val today = currentDate()
            val from = today.minus(29, DateTimeUnit.DAY)

            val tasks = dayTaskRepository.getCompletedRange(from, today)
            val summaries = daySummaryRepository.getRange(from, today)
            val blocks = workBlockRepository.getAll()

            val tasksByDate: Map<LocalDate, List<DayTask>> = tasks.groupBy { task ->
                when (val s = task.schedule) {
                    is TaskSchedule.OneTime -> s.date
                    is TaskSchedule.Recurring -> task.completedAt?.date ?: today
                }
            }

            _uiState.update {
                it.copy(
                    tasksByDate = tasksByDate,
                    summaryByDate = summaries.associateBy { it.date },
                    blocks = blocks,
                    isLoading = false
                )
            }
        }
    }
}
