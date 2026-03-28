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

    fun selectRange(range: HistoryRange) {
        _uiState.update { it.copy(selectedRange = range) }
        load()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setBlockFilter(blockId: String?) {
        _uiState.update { it.copy(selectedBlockId = blockId) }
        applyFilters()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val today = currentDate()
            val from = fromDateForRange(_uiState.value.selectedRange, today)

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
                    summaryByDate = summaries.associateBy { s -> s.date },
                    blocks = blocks,
                    isLoading = false
                )
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        _uiState.update { state ->
            val query = state.searchQuery.trim().lowercase()
            val blockId = state.selectedBlockId

            val filtered = state.tasksByDate.mapValues { (_, tasks) ->
                tasks.filter { task ->
                    val matchesSearch = query.isEmpty() ||
                            task.title.lowercase().contains(query)
                    val matchesBlock = blockId == null ||
                            task.blockId == blockId
                    matchesSearch && matchesBlock
                }
            }.filterValues { it.isNotEmpty() }

            state.copy(filteredTasksByDate = filtered)
        }
    }

    private fun fromDateForRange(range: HistoryRange, today: LocalDate): LocalDate =
        when (range) {
            HistoryRange.DAYS_30 -> today.minus(29, DateTimeUnit.DAY)
            HistoryRange.DAYS_60 -> today.minus(59, DateTimeUnit.DAY)
            HistoryRange.DAYS_90 -> today.minus(89, DateTimeUnit.DAY)
            HistoryRange.ALL_TIME -> today.minus(3650, DateTimeUnit.DAY)
        }
}
