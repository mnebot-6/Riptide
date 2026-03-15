package com.mnebot.riptide.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.domain.EcosystemProcessor
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.RecurringTaskGenerator
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.domain.repository.BlockCategoryRepository
import com.mnebot.riptide.domain.repository.BlockStreakRepository
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.EcosystemStateRepository
import com.mnebot.riptide.domain.repository.MarineCreatureRepository
import com.mnebot.riptide.domain.repository.RecurringTaskDefRepository
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import com.mnebot.riptide.domain.repository.WorkBlockRepository
import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import com.mnebot.riptide.presentation.aquarium.allCreatures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class MainViewModel(
    private val workBlockRepository: WorkBlockRepository,
    private val blockCategoryRepository: BlockCategoryRepository,
    private val dayTaskRepository: DayTaskRepository,
    private val recurringTaskDefRepository: RecurringTaskDefRepository,
    private val recurringTaskGenerator: RecurringTaskGenerator,
    private val marineCategoryAssigner: MarineCategoryAssigner,
    private val blockStreakRepository: BlockStreakRepository,
    private val daySummaryRepository: DaySummaryRepository,
    private val ecosystemProcessor: EcosystemProcessor,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ecosystemStateRepository: EcosystemStateRepository,
    private val marineCreatureRepository: MarineCreatureRepository,
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

            if (newStatus == TaskStatus.COMPLETED) {
                val block = uiState.value.blocks.find { it.id == task.blockId }
                val categories = block?.marineCategories ?: emptyList()
                val newUnlocks = ecosystemProcessor.addXpForTask(categories)
                if (newUnlocks.isNotEmpty()) {
                    _uiState.update { it.copy(pendingUnlocks = it.pendingUnlocks + newUnlocks) }
                }
            }

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

    suspend fun getRecurringTaskDef(sourceId: String): RecurringTaskDef? =
        recurringTaskDefRepository.getById(sourceId)

    fun addRecurringTask(
        title: String,
        blockId: String,
        time: LocalTime?,
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

    fun updateRecurringTask(
        sourceId: String,
        title: String,
        blockId: String,
        time: LocalTime?,
        recurrence: Recurrence
    ) {
        viewModelScope.launch {
            val def = recurringTaskDefRepository.getById(sourceId) ?: return@launch
            recurringTaskDefRepository.update(
                def.copy(title = title, blockId = blockId, time = time, recurrence = recurrence)
            )
            val today = currentDate()
            dayTaskRepository.getBySourceTask(sourceId)
                .filter { it.status == TaskStatus.PENDING }
                .filter { (it.schedule as? TaskSchedule.OneTime)?.date?.let { d -> d >= today } == true }
                .forEach { dayTaskRepository.delete(it.id) }
            recurringTaskGenerator.generateUpTo(today, daysAhead = 7)
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

                val streaksByBlock = blocks.associate { block ->
                    block.id to (blockStreakRepository.getByBlockId(block.id)?.currentStreak ?: 0)
                }

                val ecosystemByCategory = MarineCategory.entries.mapNotNull { category ->
                    ecosystemStateRepository.getByCategory(category)?.let { category to it }
                }.toMap()

                val unlockedCreatures = MarineCategory.entries.flatMap { category ->
                    ecosystemStateRepository.getByCategory(category) ?: return@flatMap emptyList<MarineCreature>()
                    marineCreatureRepository.getByCategory(category)
                }
                val creatureLevelBySpecies = unlockedCreatures.associate { it.species to it.creatureLevel }

                _uiState.update {
                    it.copy(
                        blocks = blocks,
                        tasksByBlock = tasksByBlock,
                        streaksByBlock = streaksByBlock,
                        isLoading = false,
                        error = null,
                        ecosystemByCategory = ecosystemByCategory,
                        creatureLevelBySpecies = creatureLevelBySpecies
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

    private suspend fun checkPendingSummary() {
        val yesterday = currentDate().minus(1, DateTimeUnit.DAY)
        val summary = daySummaryRepository.getByDate(yesterday) ?: return
        val dismissed = userPreferencesRepository.getLastDismissedSummaryDate()
        if (dismissed == summary.date) return
        _uiState.update { it.copy(pendingSummary = summary) }
    }

    fun dismissSummary() {
        viewModelScope.launch {
            _uiState.value.pendingSummary?.date?.let { date ->
                userPreferencesRepository.setLastDismissedSummaryDate(date)
            }
            _uiState.update { it.copy(pendingSummary = null) }
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

    fun postponeTask(task: DayTask, date: LocalDate, time: LocalTime?) {
        viewModelScope.launch {
            val postponedTo = LocalDateTime(date, time ?: LocalTime(0, 0))
            dayTaskRepository.update(task.copy(status = TaskStatus.POSTPONED, postponedTo = postponedTo))
            dayTaskRepository.insert(
                task.copy(
                    id = generateUUID(),
                    schedule = TaskSchedule.OneTime(
                        date = date,
                        time = time
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

    private suspend fun checkPendingUnlocks() {
        val emojis = userPreferencesRepository.getPendingUnlocks()
        if (emojis.isEmpty()) return
        val specs = emojis.mapNotNull { emoji -> allCreatures.find { it.emoji == emoji } }
        if (specs.isNotEmpty()) {
            _uiState.update { it.copy(pendingUnlocks = it.pendingUnlocks + specs) }
            userPreferencesRepository.setPendingUnlocks(emptyList())
        }
    }

    fun confirmUnlock(spec: CreatureSpec, nickname: String) {
        viewModelScope.launch {
            val ecosystemState = _uiState.value.ecosystemByCategory[spec.category]
            if (ecosystemState != null) {
                marineCreatureRepository.insert(
                    MarineCreature(
                        id = generateUUID(),
                        ecosystemId = ecosystemState.id,
                        species = spec.species,  // directo, sin búsqueda
                        nickname = nickname.trim(),
                        unlockedAtLevel = spec.unlockLevel,
                        experience = 0,
                        creatureLevel = 1,
                        unlockedAt = Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                )
            }
            _uiState.update { it.copy(pendingUnlocks = it.pendingUnlocks.drop(1)) }
        }
    }

    fun dismissUnlock() {
        _uiState.update { it.copy(pendingUnlocks = it.pendingUnlocks.drop(1)) }
    }

    fun reload() {
        viewModelScope.launch {
            loadDay(_uiState.value.selectedDate)
            checkPendingSummary()
            checkPendingUnlocks()
        }
    }

    fun updateNightSummaryTime(time: LocalTime) {
        viewModelScope.launch {
            userPreferencesRepository.setNightSummaryTime(time)
        }
    }
}