package com.mnebot.riptide.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.domain.repository.BlockCategoryRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.WorkBlockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class MainViewModel(
    private val workBlockRepository: WorkBlockRepository,
    private val blockCategoryRepository: BlockCategoryRepository,
    private val dayTaskRepository: DayTaskRepository,
    private val marineCategoryAssigner: MarineCategoryAssigner
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(selectedDate = currentDate())
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadDay(_uiState.value.selectedDate)
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadDay(date)
    }

    fun toggleTaskCompleted(task: DayTask) {
        viewModelScope.launch {
            dayTaskRepository.update(task.copy(isCompleted = !task.isCompleted))
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun addTask(task: DayTask) {
        viewModelScope.launch {
            dayTaskRepository.insert(task)
            loadDay(_uiState.value.selectedDate)
        }
    }

    suspend fun insertBlockAndReassign(block: WorkBlock) {
        workBlockRepository.insert(block)
        marineCategoryAssigner.reassign()
        loadDay(_uiState.value.selectedDate)
    }

    suspend fun updateBlockAndReassign(block: WorkBlock) {
        workBlockRepository.update(block)
        marineCategoryAssigner.reassign()
        loadDay(_uiState.value.selectedDate)
    }

    suspend fun deleteBlockAndReassign(blockId: String) {
        workBlockRepository.delete(blockId)
        marineCategoryAssigner.reassign()
        loadDay(_uiState.value.selectedDate)
    }

    private fun loadDay(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val blocks = loadBlocksWithCategories()
                val tasks = dayTaskRepository.getByDate(date)
                val tasksByBlock = tasks.groupBy { it.blockId }
                _uiState.update {
                    it.copy(
                        blocks = blocks,
                        tasksByBlock = tasksByBlock,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    private suspend fun loadBlocksWithCategories(): List<WorkBlock> {
        val blocks = workBlockRepository.getAll()
        val blockIds = blocks.map { it.id }
        val allCategories = blockCategoryRepository.getCategoriesForBlocks(blockIds)
        val categoriesByBlock = allCategories.groupBy { it.blockId }
        return blocks.map { block ->
            block.copy(
                marineCategories = categoriesByBlock[block.id]?.map { it.category } ?: emptyList()
            )
        }
    }

    fun reload() {
        loadDay(_uiState.value.selectedDate)
    }
}