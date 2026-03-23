package com.mnebot.riptide.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.TaskReminderScheduler
import com.mnebot.riptide.domain.EcosystemProcessor
import com.mnebot.riptide.domain.LootboxResolver
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
    private val lootboxResolver: LootboxResolver,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ecosystemStateRepository: EcosystemStateRepository,
    private val marineCreatureRepository: MarineCreatureRepository,
    private val taskReminderScheduler: TaskReminderScheduler? = null,
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
            val newStatus: TaskStatus
            val completedAt: LocalDateTime?

            if (task.status == TaskStatus.COMPLETED) {
                // Si el resumen nocturno ya procesó esta fecha → EXPIRED, si no → PENDING
                val taskDate = (task.schedule as? TaskSchedule.OneTime)?.date
                val summaryExists = taskDate != null && daySummaryRepository.getByDate(taskDate) != null
                newStatus = if (summaryExists) TaskStatus.EXPIRED else TaskStatus.PENDING
                completedAt = null
            } else {
                newStatus = TaskStatus.COMPLETED
                completedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                taskReminderScheduler?.cancelReminder(task.id)
            }

            dayTaskRepository.update(
                task.copy(
                    status = newStatus,
                    completedAt = completedAt,
                    hasBeenRewarded = if (newStatus == TaskStatus.COMPLETED) true else task.hasBeenRewarded
                )
            )

            if (newStatus == TaskStatus.COMPLETED && !task.hasBeenRewarded) {
                val block = uiState.value.blocks.find { it.id == task.blockId }
                val categories = block?.marineCategories ?: emptyList()
                val newLootboxes = ecosystemProcessor.addXpForTask(categories)
                if (newLootboxes.isNotEmpty()) {
                    _uiState.update { it.copy(pendingLootboxes = it.pendingLootboxes + newLootboxes) }
                }
            }

            loadDay(_uiState.value.selectedDate)
        }
    }

    fun addOneTimeTask(
        title: String,
        blockId: String?,
        date: LocalDate,
        time: LocalTime?,
        notificationsEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            val taskId = generateUUID()
            dayTaskRepository.insert(
                DayTask(
                    id = taskId,
                    blockId = blockId,
                    title = title,
                    schedule = TaskSchedule.OneTime(date = date, time = time),
                    status = TaskStatus.PENDING,
                    completedAt = null,
                    postponedTo = null,
                    sourceTaskId = null,
                    notificationsEnabled = notificationsEnabled
                )
            )
            if (notificationsEnabled && time != null) {
                val scheduledAt = LocalDateTime(date, time)
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                if (scheduledAt > now) {
                    taskReminderScheduler?.scheduleReminder(taskId, title, scheduledAt)
                }
            }
            loadDay(_uiState.value.selectedDate)
        }
    }

    suspend fun getRecurringTaskDef(sourceId: String): RecurringTaskDef? =
        recurringTaskDefRepository.getById(sourceId)

    fun addRecurringTask(
        title: String,
        blockId: String,
        time: LocalTime?,
        recurrence: Recurrence,
        notificationsEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            val def = RecurringTaskDef(
                id = generateUUID(),
                blockId = blockId,
                title = title,
                time = time,
                recurrence = recurrence,
                isActive = true,
                notificationsEnabled = notificationsEnabled
            )
            recurringTaskDefRepository.insert(def)
            recurringTaskGenerator.generateUpTo(currentDate(), daysAhead = 7)
            // Schedule reminders for newly generated instances if notifications enabled
            if (notificationsEnabled && time != null) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                dayTaskRepository.getBySourceTask(def.id)
                    .filter { it.notificationsEnabled && it.status == TaskStatus.PENDING }
                    .forEach { task ->
                        val taskDate = (task.schedule as? TaskSchedule.OneTime)?.date ?: return@forEach
                        val scheduledAt = LocalDateTime(taskDate, time)
                        if (scheduledAt > now) {
                            taskReminderScheduler?.scheduleReminder(task.id, task.title, scheduledAt)
                        }
                    }
            }
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun updateRecurringTask(
        sourceId: String,
        title: String,
        blockId: String,
        time: LocalTime?,
        recurrence: Recurrence,
        notificationsEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            val def = recurringTaskDefRepository.getById(sourceId) ?: return@launch
            // Cancel reminders for all pending future instances before deleting them
            val today = currentDate()
            val pendingFuture = dayTaskRepository.getBySourceTask(sourceId)
                .filter { it.status == TaskStatus.PENDING }
                .filter { (it.schedule as? TaskSchedule.OneTime)?.date?.let { d -> d >= today } == true }
            pendingFuture.forEach { task ->
                taskReminderScheduler?.cancelReminder(task.id)
                dayTaskRepository.delete(task.id)
            }
            recurringTaskDefRepository.update(
                def.copy(
                    title = title,
                    blockId = blockId,
                    time = time,
                    recurrence = recurrence,
                    notificationsEnabled = notificationsEnabled
                )
            )
            recurringTaskGenerator.generateUpTo(today, daysAhead = 7)
            // Schedule reminders for newly generated instances
            if (notificationsEnabled && time != null) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                dayTaskRepository.getBySourceTask(sourceId)
                    .filter { it.notificationsEnabled && it.status == TaskStatus.PENDING }
                    .forEach { task ->
                        val taskDate = (task.schedule as? TaskSchedule.OneTime)?.date ?: return@forEach
                        val scheduledAt = LocalDateTime(taskDate, time)
                        if (scheduledAt > now) {
                            taskReminderScheduler?.scheduleReminder(task.id, task.title, scheduledAt)
                        }
                    }
            }
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
                    .filter { it.status != TaskStatus.CANCELLED }
                val tasksByBlock: Map<String?, List<DayTask>> = tasks.groupBy { it.blockId }

                val streaksByBlock = blocks.associate { block ->
                    block.id to (blockStreakRepository.getByBlockId(block.id)?.currentStreak ?: 0)
                }

                val ecosystemByCategory = MarineCategory.entries.mapNotNull { category ->
                    ecosystemStateRepository.getByCategory(category)?.let { category to it }
                }.toMap()

                val allCreaturesFromDb = MarineCategory.entries.flatMap { category ->
                    ecosystemStateRepository.getByCategory(category) ?: return@flatMap emptyList<MarineCreature>()
                    marineCreatureRepository.getByCategory(category)
                }
                val creatureLevelBySpecies = allCreaturesFromDb.associate { it.species to it.creatureLevel }

                _uiState.update {
                    it.copy(
                        blocks = blocks,
                        tasksByBlock = tasksByBlock,
                        streaksByBlock = streaksByBlock,
                        isLoading = false,
                        error = null,
                        ecosystemByCategory = ecosystemByCategory,
                        creatureLevelBySpecies = creatureLevelBySpecies,
                        creaturesData = allCreaturesFromDb
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
            taskReminderScheduler?.cancelReminder(task.id)
            dayTaskRepository.delete(task.id)
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun updateOneTimeTask(
        original: DayTask,
        title: String,
        blockId: String?,
        date: LocalDate,
        time: LocalTime?,
        notificationsEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            taskReminderScheduler?.cancelReminder(original.id)
            dayTaskRepository.update(
                original.copy(
                    title = title,
                    blockId = blockId,
                    schedule = TaskSchedule.OneTime(date = date, time = time),
                    notificationsEnabled = notificationsEnabled
                )
            )
            if (notificationsEnabled && time != null) {
                val scheduledAt = LocalDateTime(date, time)
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                if (scheduledAt > now) {
                    taskReminderScheduler?.scheduleReminder(original.id, title, scheduledAt)
                }
            }
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun postponeTask(task: DayTask, date: LocalDate, time: LocalTime?) {
        viewModelScope.launch {
            taskReminderScheduler?.cancelReminder(task.id)
            val postponedTo = LocalDateTime(date, time ?: LocalTime(0, 0))
            dayTaskRepository.update(task.copy(status = TaskStatus.POSTPONED, postponedTo = postponedTo))
            val newId = generateUUID()
            dayTaskRepository.insert(
                task.copy(
                    id = newId,
                    schedule = TaskSchedule.OneTime(date = date, time = time),
                    status = TaskStatus.PENDING,
                    postponedTo = null
                )
            )
            if (task.notificationsEnabled && time != null) {
                val scheduledAt = LocalDateTime(date, time)
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                if (scheduledAt > now) {
                    taskReminderScheduler?.scheduleReminder(newId, task.title, scheduledAt)
                }
            }
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun deleteRecurringTaskInstance(task: DayTask) {
        viewModelScope.launch {
            taskReminderScheduler?.cancelReminder(task.id)
            dayTaskRepository.update(task.copy(status = TaskStatus.CANCELLED))
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun deleteRecurringTaskFromDate(task: DayTask) {
        viewModelScope.launch {
            val sourceId = task.sourceTaskId ?: return@launch
            val date = (task.schedule as? TaskSchedule.OneTime)?.date ?: return@launch
            val def = recurringTaskDefRepository.getById(sourceId) ?: return@launch
            // Cancel reminders for pending instances from this date onwards
            dayTaskRepository.getBySourceTask(sourceId)
                .filter { it.status == TaskStatus.PENDING }
                .filter { (it.schedule as? TaskSchedule.OneTime)?.date?.let { d -> d >= date } == true }
                .forEach { taskReminderScheduler?.cancelReminder(it.id) }
            recurringTaskDefRepository.update(def.copy(isActive = false))
            dayTaskRepository.deleteBySourceIdFromDate(sourceId, date)
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun deleteRecurringTaskAll(task: DayTask) {
        viewModelScope.launch {
            val sourceId = task.sourceTaskId ?: return@launch
            val def = recurringTaskDefRepository.getById(sourceId) ?: return@launch
            // Cancel reminders for all pending instances
            dayTaskRepository.getBySourceTask(sourceId)
                .filter { it.status == TaskStatus.PENDING }
                .forEach { taskReminderScheduler?.cancelReminder(it.id) }
            recurringTaskDefRepository.update(def.copy(isActive = false))
            dayTaskRepository.deleteBySourceId(sourceId)
            loadDay(_uiState.value.selectedDate)
        }
    }

    private suspend fun checkPendingLootboxes() {
        // Migrar legacy (emojis) → limpiar
        val oldEmojis = userPreferencesRepository.getPendingUnlocks()
        if (oldEmojis.isNotEmpty()) {
            userPreferencesRepository.setPendingUnlocks(emptyList())
        }

        // Cargar lootboxes nuevas
        val lootboxes = userPreferencesRepository.getPendingLootboxes()
        if (lootboxes.isEmpty()) return
        _uiState.update { it.copy(pendingLootboxes = it.pendingLootboxes + lootboxes) }
        userPreferencesRepository.setPendingLootboxes(emptyList())
    }

    /** Usuario pulsa "Abrir" en la lootbox → resuelve especie aleatoria por rareza */
    fun openLootbox() {
        val lootbox = _uiState.value.pendingLootboxes.firstOrNull() ?: return
        viewModelScope.launch {
            val spec = lootboxResolver.resolve(lootbox)
            _uiState.update { it.copy(revealedSpecies = spec) }
        }
    }

    /** Usuario confirma nombre tras revelar especie */
    fun confirmUnlock(spec: CreatureSpec, nickname: String) {
        viewModelScope.launch {
            val lootbox = _uiState.value.pendingLootboxes.firstOrNull()
            val ecosystemState = _uiState.value.ecosystemByCategory[spec.category]
            if (ecosystemState != null) {
                marineCreatureRepository.insert(
                    MarineCreature(
                        id = generateUUID(),
                        ecosystemId = ecosystemState.id,
                        species = spec.species,
                        nickname = nickname.trim(),
                        unlockedAtLevel = lootbox?.categoryLevel ?: ecosystemState.currentLevel,
                        experience = 0,
                        creatureLevel = 1,
                        unlockedAt = Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                )
            }
            _uiState.update {
                it.copy(
                    pendingLootboxes = it.pendingLootboxes.drop(1),
                    revealedSpecies = null
                )
            }
            loadDay(_uiState.value.selectedDate)
        }
    }

    fun dismissLootbox() {
        _uiState.update {
            it.copy(
                pendingLootboxes = it.pendingLootboxes.drop(1),
                revealedSpecies = null
            )
        }
    }

    fun reload() {
        viewModelScope.launch {
            loadDay(_uiState.value.selectedDate)
            checkPendingSummary()
            checkPendingLootboxes()
        }
    }

    fun updateNightSummaryTime(time: LocalTime) {
        viewModelScope.launch {
            userPreferencesRepository.setNightSummaryTime(time)
        }
    }

    // Persiste el nickname editado desde el dialog de detalle de criatura
    fun updateCreatureNickname(creatureId: String, nickname: String) {
        viewModelScope.launch {
            val creature = _uiState.value.creaturesData.find { it.id == creatureId } ?: return@launch
            marineCreatureRepository.update(
                creature.copy(nickname = nickname.trim().ifEmpty { null })
            )
            loadDay(_uiState.value.selectedDate)
        }
    }
}