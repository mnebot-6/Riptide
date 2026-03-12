package com.mnebot.riptide.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.RecurringTaskGenerator
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.domain.repository.BlockCategoryRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.RecurringTaskDefRepository
import com.mnebot.riptide.domain.repository.WorkBlockRepository
import com.mnebot.riptide.generateUUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class MainViewModel(
    private val workBlockRepository: WorkBlockRepository,
    private val blockCategoryRepository: BlockCategoryRepository,
    private val dayTaskRepository: DayTaskRepository,
    private val recurringTaskDefRepository: RecurringTaskDefRepository,
    private val recurringTaskGenerator: RecurringTaskGenerator,
    private val marineCategoryAssigner: MarineCategoryAssigner
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(selectedDate = currentDate()))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recurringTaskGenerator.generateUpTo(currentDate(), daysAhead = 7)
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadDay(date)
    }

    fun toggleTaskCompleted(task: DayTask) {
        viewModelScope.launch {
            val newStatus = if (task.status == TaskStatus.COMPLETED)
                TaskStatus.PENDING else TaskStatus.COMPLETED
            val completedAt = if (newStatus == TaskStatus.COMPLETED)
                Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            else null
            dayTaskRepository.update(task.copy(status = newStatus, completedAt = completedAt))
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun addOneTimeTask(
        title: String,
        blockId: String?,
        date: LocalDate,
        time: LocalTime?
    ) {
        viewModelScope.launch {
            dayTaskRepository.insert(
                DayTask(
                    id = generateUUID(),
                    blockId = blockId,
                    title = title,
                    schedule = TaskSchedule.OneTime(date = date, time = time),
                    status = TaskStatus.PENDING,
                    completedAt = null,
                    postponedTo = null,
                    sourceTaskId = null
                )
            )
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun addRecurringTask(
        title: String,
        blockId: String,
        time: LocalTime,
        recurrence: Recurrence
    ) {
        viewModelScope.launch {
            val def = RecurringTaskDef(
                id = generateUUID(),
                blockId = blockId,
                title = title,
                time = time,
                recurrence = recurrence,
                isActive = true
            )
            recurringTaskDefRepository.insert(def)
            recurringTaskGenerator.generateUpTo(currentDate(), daysAhead = 7)
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
                val tasksByBlock: Map<String?, List<DayTask>> = tasks.groupBy { it.blockId }
                _uiState.update {
                    it.copy(
                        blocks = blocks,
                        tasksByBlock = tasksByBlock,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
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

    fun deleteTask(task: DayTask) {
        viewModelScope.launch {
            dayTaskRepository.delete(task.id)
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun updateOneTimeTask(
        original: DayTask,
        title: String,
        blockId: String?,
        date: LocalDate,
        time: LocalTime?
    ) {
        viewModelScope.launch {
            dayTaskRepository.update(
                original.copy(
                    title = title,
                    blockId = blockId,
                    schedule = TaskSchedule.OneTime(date = date, time = time)
                )
            )
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun postponeTask(task: DayTask, postponedTo: LocalDateTime) {
        viewModelScope.launch {
            dayTaskRepository.update(task.copy(status = TaskStatus.POSTPONED, postponedTo = postponedTo))
            dayTaskRepository.insert(
                task.copy(
                    id = generateUUID(),
                    schedule = TaskSchedule.OneTime(
                        date = postponedTo.date,
                        time = postponedTo.time
                    ),
                    status = TaskStatus.PENDING,
                    postponedTo = null
                )
            )
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun deleteRecurringTaskInstance(task: DayTask) {
        viewModelScope.launch {
            dayTaskRepository.delete(task.id)
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun deleteRecurringTaskFromDate(task: DayTask) {
        viewModelScope.launch {
            val sourceId = task.sourceTaskId ?: return@launch
            val date = (task.schedule as? TaskSchedule.OneTime)?.date ?: return@launch
            // Desactivar la definición para que no genere más instancias
            val def = recurringTaskDefRepository.getById(sourceId) ?: return@launch
            recurringTaskDefRepository.update(def.copy(isActive = false))
            // Borrar todas las instancias pendientes desde esta fecha inclusive
            dayTaskRepository.deleteBySourceIdFromDate(sourceId, date)
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun deleteRecurringTaskAll(task: DayTask) {
        viewModelScope.launch {
            val sourceId = task.sourceTaskId ?: return@launch
            val def = recurringTaskDefRepository.getById(sourceId) ?: return@launch
            recurringTaskDefRepository.update(def.copy(isActive = false))
            dayTaskRepository.deleteBySourceId(sourceId)
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun reload() {
        loadDay(_uiState.value.selectedDate)
    }
}