package com.mnebot.riptide.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.TaskReminderScheduler
import com.mnebot.riptide.domain.DecorationUnlockChecker
import com.mnebot.riptide.domain.EcosystemProcessor
import com.mnebot.riptide.domain.LootboxResolver
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.RecurringTaskGenerator
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.domain.repository.BlockCategoryRepository
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val decorationUnlockChecker: DecorationUnlockChecker? = null,
    private val onTaskMutated: (suspend () -> Unit)? = null,
    /** Flow emitting SyncStatus from SyncManager (null when sync is not available). */
    private val syncStatusFlow: Flow<SyncStatus>? = null,
    /** Notify debounced sync trigger after local mutations. */
    private val onSyncMutation: (() -> Unit)? = null,
    /** Force immediate sync (e.g., user taps "Sync Now"). */
    private val onSyncNow: (() -> Unit)? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(selectedDate = currentDate()))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Timer runtime state (not persisted)
    data class TimerState(val remainingSeconds: Int, val isRunning: Boolean, val totalSeconds: Int)
    private val _timerStates = MutableStateFlow<Map<String, TimerState>>(emptyMap())
    val timerStates: StateFlow<Map<String, TimerState>> = _timerStates.asStateFlow()
    private val timerJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            recurringTaskGenerator.generateUpTo(currentDate(), daysAhead = 7)
            loadDay(_uiState.value.selectedDate)
            decorationUnlockChecker?.checkAll()
            // Pick up any lootboxes queued by checkAll (e.g. decoration unlocks)
            checkPendingLootboxes()
        }
        // Load logged-in user
        viewModelScope.launch {
            val user = userPreferencesRepository.getLoggedInUser()
            _uiState.update { it.copy(loggedInUser = user) }
        }
        // Observe sync status
        viewModelScope.launch {
            syncStatusFlow?.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
        // Observe wallpaper FPS
        viewModelScope.launch {
            userPreferencesRepository.getWallpaperFps().collect { fps ->
                _uiState.update { it.copy(wallpaperFps = fps) }
            }
        }
    }

    /** Called from the Activity layer after Google Sign-In completes successfully. */
    fun onSignInCompleted(user: LoggedInUser?) {
        _uiState.update { it.copy(loggedInUser = user) }
    }

    fun setWallpaperFps(fps: Int) {
        viewModelScope.launch { userPreferencesRepository.setWallpaperFps(fps) }
    }

    /** Sign out and clear auth state. */
    fun signOut() {
        viewModelScope.launch {
            userPreferencesRepository.clearAuth()
            _uiState.update { it.copy(loggedInUser = null, syncStatus = SyncStatus.IDLE) }
        }
    }

    /** Manual sync trigger. */
    fun syncNow() {
        onSyncNow?.invoke()
    }

    /** Show the initial sync conflict dialog. */
    fun showSyncConflict() {
        _uiState.update { it.copy(pendingSyncConflict = true) }
    }

    /** Dismiss the initial sync conflict dialog. */
    fun dismissSyncConflict() {
        _uiState.update { it.copy(pendingSyncConflict = false) }
    }

    /** Select a pond (biome) to display. */
    fun selectPond(category: MarineCategory) {
        _uiState.update {
            it.copy(
                selectedPond = category,
                selectedBiome = com.mnebot.riptide.presentation.aquarium.BiomeThemes.forCategory(category)
            )
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
                // Easter egg: Bimba se desbloquea al completar una tarea con "premio" o "treat"
                val bimbaUnlocked = decorationUnlockChecker?.checkBimba(task.title)
                if (bimbaUnlocked != null) checkPendingLootboxes()
            }

            loadDay(_uiState.value.selectedDate)
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
        }
    }

    // ── Countable tasks ───────────────────────────────────────────────────

    fun incrementTaskCount(task: DayTask) {
        if (!task.isCountable) return
        viewModelScope.launch {
            val newCount = (task.currentCount + 1).coerceAtMost(task.targetCount!!)
            val autoComplete = newCount >= task.targetCount
            val newStatus = if (autoComplete) TaskStatus.COMPLETED else task.status
            val completedAt = if (autoComplete)
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) else task.completedAt

            dayTaskRepository.update(
                task.copy(
                    currentCount = newCount,
                    status = newStatus,
                    completedAt = completedAt,
                    hasBeenRewarded = if (autoComplete) true else task.hasBeenRewarded
                )
            )

            if (autoComplete && !task.hasBeenRewarded) {
                val block = uiState.value.blocks.find { it.id == task.blockId }
                val categories = block?.marineCategories ?: emptyList()
                val newLootboxes = ecosystemProcessor.addXpForTask(categories)
                if (newLootboxes.isNotEmpty()) {
                    _uiState.update { it.copy(pendingLootboxes = it.pendingLootboxes + newLootboxes) }
                }
                taskReminderScheduler?.cancelReminder(task.id)
            }

            loadDay(_uiState.value.selectedDate)
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
        }
    }

    fun decrementTaskCount(task: DayTask) {
        if (!task.isCountable) return
        viewModelScope.launch {
            val newCount = (task.currentCount - 1).coerceAtLeast(0)
            val wasCompleted = task.status == TaskStatus.COMPLETED
            val needsRevert = wasCompleted && newCount < task.targetCount!!

            val newStatus = if (needsRevert) {
                val taskDate = (task.schedule as? TaskSchedule.OneTime)?.date
                val summaryExists = taskDate != null && daySummaryRepository.getByDate(taskDate) != null
                if (summaryExists) TaskStatus.EXPIRED else TaskStatus.PENDING
            } else task.status

            dayTaskRepository.update(
                task.copy(
                    currentCount = newCount,
                    status = newStatus,
                    completedAt = if (needsRevert) null else task.completedAt
                )
            )

            loadDay(_uiState.value.selectedDate)
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
        }
    }

    // ── Priority ─────────────────────────────────────────────────────────

    fun toggleTaskPriority(task: DayTask) {
        viewModelScope.launch {
            dayTaskRepository.update(task.copy(isPriority = !task.isPriority))
            loadDay(_uiState.value.selectedDate)
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
        }
    }

    // ── Notes ────────────────────────────────────────────────────────────

    fun updateTaskNotes(task: DayTask, notes: String?) {
        viewModelScope.launch {
            dayTaskRepository.update(task.copy(notes = notes))
            loadDay(_uiState.value.selectedDate)
            onSyncMutation?.invoke()
        }
    }

    // ── Timer (runtime only) ─────────────────────────────────────────────

    fun startTimer(taskId: String, durationMinutes: Int) {
        timerJobs[taskId]?.cancel()
        val totalSeconds = durationMinutes * 60
        _timerStates.update { it + (taskId to TimerState(totalSeconds, true, totalSeconds)) }
        timerJobs[taskId] = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _timerStates.update { map ->
                    val current = map[taskId] ?: return@update map
                    if (!current.isRunning) return@update map
                    map + (taskId to current.copy(remainingSeconds = remaining))
                }
                // Check if paused
                val state = _timerStates.value[taskId]
                if (state != null && !state.isRunning) {
                    // Wait until resumed or cancelled
                    while (_timerStates.value[taskId]?.isRunning == false) {
                        delay(200)
                    }
                }
            }
            // Timer completed — auto-complete the task
            _timerStates.update { it - taskId }
            timerJobs.remove(taskId)
            val tasks = _uiState.value.tasksByBlock.values.flatten()
            val task = tasks.find { it.id == taskId }
            if (task != null && task.status != TaskStatus.COMPLETED) {
                if (task.isCountable) incrementTaskCount(task)
                else toggleTaskCompleted(task)
            }
        }
    }

    fun pauseTimer(taskId: String) {
        _timerStates.update { map ->
            val current = map[taskId] ?: return@update map
            map + (taskId to current.copy(isRunning = false))
        }
    }

    fun resumeTimer(taskId: String) {
        _timerStates.update { map ->
            val current = map[taskId] ?: return@update map
            map + (taskId to current.copy(isRunning = true))
        }
    }

    fun cancelTimer(taskId: String) {
        timerJobs[taskId]?.cancel()
        timerJobs.remove(taskId)
        _timerStates.update { it - taskId }
    }

    // ── Task creation onboarding ────────────────────────────────────────────

    suspend fun hasShownTaskCreationOnboarding(): Boolean =
        userPreferencesRepository.hasShownTaskCreationOnboarding()

    fun setTaskCreationOnboardingShown() {
        viewModelScope.launch {
            userPreferencesRepository.setTaskCreationOnboardingShown()
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.resetOnboarding()
        }
    }

    // ── Task CRUD (original, updated signatures) ─────────────────────────

    fun addOneTimeTask(
        title: String,
        blockId: String?,
        date: LocalDate,
        time: LocalTime?,
        notificationsEnabled: Boolean = false,
        targetCount: Int? = null,
        notes: String? = null,
        timerDurationMinutes: Int? = null,
        isPriority: Boolean = false
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
                    notificationsEnabled = notificationsEnabled,
                    targetCount = targetCount,
                    notes = notes,
                    timerDurationMinutes = timerDurationMinutes,
                    isPriority = isPriority
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
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
        }
    }

    suspend fun getRecurringTaskDef(sourceId: String): RecurringTaskDef? =
        recurringTaskDefRepository.getById(sourceId)

    fun addRecurringTask(
        title: String,
        blockId: String,
        time: LocalTime?,
        recurrence: Recurrence,
        notificationsEnabled: Boolean = false,
        targetCount: Int? = null,
        noteTemplate: String? = null,
        timerDurationMinutes: Int? = null,
        isPriority: Boolean = false
    ) {
        viewModelScope.launch {
            val def = RecurringTaskDef(
                id = generateUUID(),
                blockId = blockId,
                title = title,
                time = time,
                recurrence = recurrence,
                isActive = true,
                notificationsEnabled = notificationsEnabled,
                targetCount = targetCount,
                noteTemplate = noteTemplate,
                timerDurationMinutes = timerDurationMinutes,
                isPriority = isPriority
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
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
        }
    }

    fun updateRecurringTask(
        sourceId: String,
        title: String,
        blockId: String,
        time: LocalTime?,
        recurrence: Recurrence,
        notificationsEnabled: Boolean = false,
        targetCount: Int? = null,
        noteTemplate: String? = null,
        timerDurationMinutes: Int? = null,
        isPriority: Boolean = false
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
                    notificationsEnabled = notificationsEnabled,
                    targetCount = targetCount,
                    noteTemplate = noteTemplate,
                    timerDurationMinutes = timerDurationMinutes,
                    isPriority = isPriority
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
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
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

                // XP sweep: award XP for tasks completed via widget (hasBeenRewarded=false)
                val rawTasks = dayTaskRepository.getByDate(date)
                val unrewarded = rawTasks.filter {
                    it.status == TaskStatus.COMPLETED && !it.hasBeenRewarded
                }
                val allNewLootboxes = mutableListOf<PendingLootbox>()
                for (task in unrewarded) {
                    val block = blocks.find { it.id == task.blockId }
                    val categories = block?.marineCategories ?: emptyList()
                    val lootboxes = ecosystemProcessor.addXpForTask(categories)
                    allNewLootboxes.addAll(lootboxes)
                    dayTaskRepository.update(task.copy(hasBeenRewarded = true))
                }
                if (allNewLootboxes.isNotEmpty()) {
                    _uiState.update { it.copy(pendingLootboxes = it.pendingLootboxes + allNewLootboxes) }
                }

                val tasks = rawTasks
                    .filter { it.status != TaskStatus.CANCELLED }
                    .map { task ->
                        // Reflect the reward flag we just set
                        if (unrewarded.any { it.id == task.id }) task.copy(hasBeenRewarded = true) else task
                    }
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

                val globalStreak = calculateGlobalStreak(date)

                _uiState.update {
                    it.copy(
                        blocks = blocks,
                        tasksByBlock = tasksByBlock,
                        streaksByBlock = streaksByBlock,
                        globalStreak = globalStreak,
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

    private suspend fun calculateGlobalStreak(referenceDate: LocalDate): Int {
        val from = referenceDate.minus(89, DateTimeUnit.DAY)
        val summaries = daySummaryRepository.getRange(from, referenceDate)
        val summaryByDate = summaries.associateBy { it.date }
        // Start from yesterday — today's summary hasn't been generated yet
        var date = referenceDate.minus(1, DateTimeUnit.DAY)
        var streak = 0
        while (date >= from) {
            val summary = summaryByDate[date]
            if (summary != null && summary.tasksCompleted > 0) {
                streak++
                date = date.minus(1, DateTimeUnit.DAY)
            } else {
                break
            }
        }
        return streak
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
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
        }
    }

    fun updateOneTimeTask(
        original: DayTask,
        title: String,
        blockId: String?,
        date: LocalDate,
        time: LocalTime?,
        notificationsEnabled: Boolean = false,
        targetCount: Int? = null,
        notes: String? = null,
        timerDurationMinutes: Int? = null,
        isPriority: Boolean = false
    ) {
        viewModelScope.launch {
            taskReminderScheduler?.cancelReminder(original.id)
            dayTaskRepository.update(
                original.copy(
                    title = title,
                    blockId = blockId,
                    schedule = TaskSchedule.OneTime(date = date, time = time),
                    notificationsEnabled = notificationsEnabled,
                    targetCount = targetCount,
                    notes = notes,
                    timerDurationMinutes = timerDurationMinutes,
                    isPriority = isPriority
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
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
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
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
        }
    }

    fun deleteRecurringTaskInstance(task: DayTask) {
        viewModelScope.launch {
            taskReminderScheduler?.cancelReminder(task.id)
            dayTaskRepository.update(task.copy(status = TaskStatus.CANCELLED))
            loadDay(_uiState.value.selectedDate)
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
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
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
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
            onTaskMutated?.invoke()
            onSyncMutation?.invoke()
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
            // Decoration lootboxes (directSpecies != null) already have their creature inserted
            // by DecorationUnlockChecker.doUnlock() — skip re-insertion to avoid duplicates.
            if (lootbox?.directSpecies == null) {
                val ecosystemState = _uiState.value.ecosystemByCategory[spec.category]
                if (ecosystemState != null) {
                    marineCreatureRepository.insert(
                        MarineCreature(
                            id = generateUUID(),
                            ecosystemId = ecosystemState.id,
                            species = spec.species,
                            nickname = nickname.trim().takeIf { it.isNotBlank() },
                            unlockedAtLevel = lootbox?.categoryLevel ?: ecosystemState.currentLevel,
                            experience = 0,
                            creatureLevel = 1,
                            unlockedAt = Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                        )
                    )
                }
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