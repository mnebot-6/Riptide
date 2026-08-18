package com.mnebot.riptide.presentation.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mnebot.riptide.NightSummaryScheduler
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.presentation.components.DatePickerDialogWrapper
import com.mnebot.riptide.presentation.task.PostponeSheet
import com.mnebot.riptide.presentation.task.TaskFormSheet
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import com.mnebot.riptide.presentation.aquarium.CreatureIcon
import com.mnebot.riptide.presentation.theme.rememberAdaptiveCardColor
import com.mnebot.riptide.presentation.displayNameRes
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanLight = Color(0xFF2E5F9E)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)

private fun sortedBlocks(
    blocks: List<WorkBlock>,
    tasksByBlock: Map<String?, List<DayTask>>,
    date: LocalDate
): List<WorkBlock> {
    val dayOfWeek = date.dayOfWeek.isoDayNumber
    return blocks.sortedWith(compareBy(
        { block ->
            // Hora más temprana de tarea pendiente (no completada/pospuesta)
            val earliestPendingTime = tasksByBlock[block.id]
                ?.filter { it.status != TaskStatus.COMPLETED }
                ?.mapNotNull { (it.schedule as? TaskSchedule.OneTime)?.time }
                ?.minOrNull()
                ?.toSecondOfDay()

            if (earliestPendingTime != null) {
                earliestPendingTime
            } else {
                // Fallback: hora del slot del bloque ese día
                val recurrence = block.recurrence
                if (recurrence is Recurrence.Weekly) {
                    val slot = recurrence.slots.firstOrNull { it.dayOfWeek == dayOfWeek }
                    slot?.startTime?.toSecondOfDay() ?: Int.MAX_VALUE
                } else Int.MAX_VALUE
            }
        },
        { it.name }
    ))
}

private fun sortedTasks(tasks: List<DayTask>): List<DayTask> {
    // 4-bucket order for active tasks:
    //  0 = priority + time (asc by time)
    //  1 = priority no-time (creation order = stable existing order)
    //  2 = normal + time (asc by time)
    //  3 = normal no-time (creation order)
    // Completed always at the end.
    fun bucket(task: DayTask): Int {
        val time = (task.schedule as? TaskSchedule.OneTime)?.time
        return when {
            task.isPriority && time != null -> 0
            task.isPriority -> 1
            time != null -> 2
            else -> 3
        }
    }
    fun timeKey(task: DayTask): Int {
        val time = (task.schedule as? TaskSchedule.OneTime)?.time
        return time?.toSecondOfDay() ?: 0
    }
    val (completed, active) = tasks.partition { it.status == TaskStatus.COMPLETED }
    val sortedActive = active.sortedWith(compareBy({ bucket(it) }, { timeKey(it) }))
    val sortedCompleted = completed.sortedWith(compareBy({ bucket(it) }, { timeKey(it) }))
    return sortedActive + sortedCompleted
}

private fun shouldShowBlockTime(block: WorkBlock, date: LocalDate): Boolean {
    val recurrence = block.recurrence
    if (recurrence !is Recurrence.Weekly) return false
    val dayOfWeek = date.dayOfWeek.isoDayNumber
    val slot = recurrence.slots.firstOrNull { it.dayOfWeek == dayOfWeek } ?: return false
    return slot.startTime != null && slot.endTime != null
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    nightSummaryScheduler: NightSummaryScheduler,
    onNavigateToCreateBlock: () -> Unit,
    onNavigateToEditBlock: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onSignIn: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    var showTaskSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showBlocksSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DayTask?>(null) }
    var postponingTask by remember { mutableStateOf<DayTask?>(null) }
    var contextMenuTask by remember { mutableStateOf<DayTask?>(null) }
    var deletingRecurringTask by remember { mutableStateOf<DayTask?>(null) }
    var editingScopeTask by remember { mutableStateOf<DayTask?>(null) }
    var editingTaskDef by remember { mutableStateOf<RecurringTaskDef?>(null) }
    var quickTaskBlock by remember { mutableStateOf<WorkBlock?>(null) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Aquarium background is rendered by MainShellScreen

        val sorted = sortedBlocks(uiState.blocks, uiState.tasksByBlock, uiState.selectedDate)

        Column(modifier = Modifier.fillMaxSize()) {
            // Header fijo
            MainHeader(
                selectedDate = uiState.selectedDate,
                today = currentDate(),
                tasksByDate = uiState.tasksByDate,
                globalStreak = uiState.globalStreak,
                onCalendarClick = { showDatePicker = true },
                onTodayClick = { viewModel.selectDate(currentDate()) },
                onAddTaskClick = { showTaskSheet = true },
                onBlocksClick = { showBlocksSheet = true },
                onSettingsClick = onNavigateToSettings,
                onDateSelected = { viewModel.selectDate(it) },
                onWeekChange = { viewModel.selectDate(it) }
            )

            val timerStates by viewModel.timerStates.collectAsState()

            // Contenido scrollable
            MainContent(
                blocks = sorted,
                tasksByBlock = uiState.tasksByBlock,
                selectedDate = uiState.selectedDate,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onTaskToggle = { viewModel.toggleTaskCompleted(it) },
                onTaskLongPress = { contextMenuTask = it },
                onBlockHeaderLongPress = { block ->
                    quickTaskBlock = block
                    showTaskSheet = true
                },
                onIncrement = { viewModel.incrementTaskCount(it) },
                onDecrement = { viewModel.decrementTaskCount(it) },
                onPriorityToggle = { viewModel.toggleTaskPriority(it) },
                timerStates = timerStates,
                onTimerStart = { task ->
                    task.timerDurationMinutes?.let { viewModel.startTimer(task.id, it) }
                },
                onTimerPause = { viewModel.pauseTimer(it) },
                onTimerResume = { viewModel.resumeTimer(it) },
                onTimerCancel = { viewModel.cancelTimer(it) },
                onNotesChanged = { task, notes -> viewModel.updateTaskNotes(task, notes) }
            )
        }

        // Menú contextual — BottomSheet
        contextMenuTask?.let { task ->
            Dialog(
                onDismissRequest = { contextMenuTask = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { contextMenuTask = null },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    var sheetDragY by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, sheetDragY.roundToInt().coerceAtLeast(0)) }
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        if (sheetDragY > 100f) contextMenuTask = null
                                        sheetDragY = 0f
                                    }
                                ) { _, dragAmount ->
                                    sheetDragY = (sheetDragY + dragAmount.y).coerceAtLeast(0f)
                                }
                            }
                            .background(
                                Brush.verticalGradient(listOf(OceanDeep, OceanMid)),
                                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp, bottom = 32.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x55FFFFFF))
                                    .align(Alignment.CenterHorizontally)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = task.title,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(16.dp))
                            ContextMenuItem(
                                label = stringResource(Res.string.menu_edit),
                                painter = painterResource(Res.drawable.ic_pencil)
                            ) {
                                if (task.sourceTaskId != null) {
                                    editingScopeTask = task
                                } else {
                                    editingTask = task
                                }
                                contextMenuTask = null
                            }
                            if (task.status != TaskStatus.COMPLETED && task.status != TaskStatus.EXPIRED) {
                                ContextMenuItem(
                                    label = stringResource(Res.string.menu_postpone),
                                    painter = painterResource(Res.drawable.ic_clock)
                                ) {
                                    postponingTask = task
                                    contextMenuTask = null
                                }
                            }
                            ContextMenuItem(
                                label = stringResource(Res.string.menu_delete),
                                painter = painterResource(Res.drawable.ic_trash),
                                tint = Color(0xFFEA4335)
                            ) {
                                if (task.sourceTaskId != null) {
                                    deletingRecurringTask = task
                                } else {
                                    viewModel.deleteTask(task)
                                }
                                contextMenuTask = null
                            }
                        }
                    }
                }
            }
        }

        // Diálogo scope edición recurrente
        editingScopeTask?.let { task ->
            AlertDialog(
                onDismissRequest = { editingScopeTask = null },
                containerColor = OceanMid,
                title = {
                    Text(stringResource(Res.string.dialog_edit_scope_title), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ContextMenuItem(stringResource(Res.string.dialog_edit_one)) {
                            editingTask = task
                            editingScopeTask = null
                        }
                        ContextMenuItem(stringResource(Res.string.dialog_edit_future)) {
                            // Marcamos con una convención: guardamos en editingTask pero
                            // el onSaveRecurring usará updateRecurringTask
                            editingTask = task.copy(sourceTaskId = task.sourceTaskId + "_future")
                            editingScopeTask = null
                        }
                        ContextMenuItem(stringResource(Res.string.dialog_edit_all)) {
                            editingTask = task.copy(sourceTaskId = task.sourceTaskId + "_all")
                            editingScopeTask = null
                        }
                    }
                },
                confirmButton = {}
            )
        }

        deletingRecurringTask?.let { task ->
            AlertDialog(
                onDismissRequest = { deletingRecurringTask = null },
                containerColor = OceanMid,
                title = {
                    Text(stringResource(Res.string.dialog_delete_scope_title), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ContextMenuItem(stringResource(Res.string.dialog_delete_one)) {
                            viewModel.deleteRecurringTaskInstance(task)
                            deletingRecurringTask = null
                        }
                        ContextMenuItem(stringResource(Res.string.dialog_delete_future)) {
                            viewModel.deleteRecurringTaskFromDate(task)
                            deletingRecurringTask = null
                        }
                        ContextMenuItem(stringResource(Res.string.dialog_delete_all)) {
                            viewModel.deleteRecurringTaskAll(task)
                            deletingRecurringTask = null
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // Resumen nocturno
        uiState.pendingSummary?.let { summary ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSummary() },
                containerColor = OceanMid,
                title = {
                    Text(stringResource(Res.string.title_night_summary), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(summary.feedbackMessage, color = TextPrimary, fontSize = 15.sp)
                        Text(
                            stringResource(Res.string.msg_tasks_completed, summary.tasksCompleted, summary.tasksTotal),
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissSummary() }) {
                        Text(stringResource(Res.string.btn_close), color = Color(0xFF7EC8E3))
                    }
                }
            )
        }

        // ── Lootbox Dialog ────────────────────────────────────────────────────
        val currentLootbox = uiState.pendingLootboxes.firstOrNull()
        val revealedSpecies = uiState.revealedSpecies
        if (currentLootbox != null && uiState.pendingSummary == null) {
            if (revealedSpecies == null) {
                // Estado 1: Lootbox cerrada
                val categoryName = stringResource(currentLootbox.category.displayNameRes())
                AlertDialog(
                    onDismissRequest = {},
                    containerColor = OceanMid,
                    title = {
                        Text(
                            stringResource(Res.string.title_lootbox, categoryName),
                            color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (currentLootbox.directSpecies != null) "✨" else "🎁",
                                fontSize = 56.sp
                            )
                            Text(
                                if (currentLootbox.directSpecies != null)
                                    stringResource(Res.string.msg_decoration_unlock)
                                else
                                    stringResource(Res.string.msg_lootbox_level, currentLootbox.categoryLevel),
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openLootbox()
                        }) {
                            Text(stringResource(Res.string.btn_open_lootbox), color = Color(0xFF7EC8E3))
                        }
                    }
                )
            } else {
                // Estado 2: Especie revelada → pedir nombre (sólo fauna/flora, no decoración)
                var nickname by remember(revealedSpecies) { mutableStateOf("") }
                val isDecoration = revealedSpecies.category == MarineCategory.DECORATION
                val rarityColor = when (revealedSpecies.rarity) {
                    com.mnebot.riptide.domain.model.CreatureRarity.COMMON -> Color(0xFF9E9E9E)
                    com.mnebot.riptide.domain.model.CreatureRarity.UNCOMMON -> Color(0xFF4CAF50)
                    com.mnebot.riptide.domain.model.CreatureRarity.RARE -> Color(0xFF2196F3)
                    com.mnebot.riptide.domain.model.CreatureRarity.EPIC -> Color(0xFF9C27B0)
                    com.mnebot.riptide.domain.model.CreatureRarity.LEGENDARY -> Color(0xFFFF9800)
                }
                AlertDialog(
                    onDismissRequest = {},
                    containerColor = OceanMid,
                    title = {
                        Text(stringResource(Res.string.title_creature_unlock), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CreatureIcon(
                                spec = revealedSpecies,
                                level = 1,
                                modifier = Modifier.size(96.dp)
                            )
                            Text(
                                stringResource(revealedSpecies.species.displayNameRes()),
                                color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(revealedSpecies.rarity.displayNameRes()),
                                color = rarityColor, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                            if (!isDecoration) {
                                Text(
                                    stringResource(Res.string.msg_creature_name_prompt),
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                OutlinedTextField(
                                    value = nickname,
                                    onValueChange = { nickname = it },
                                    placeholder = { Text(stringResource(Res.string.placeholder_nickname), color = TextSecondary) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = Color(0xFF7EC8E3),
                                        unfocusedBorderColor = TextSecondary
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.confirmUnlock(revealedSpecies, nickname) },
                            enabled = isDecoration || nickname.isNotBlank()
                        ) {
                            Text(stringResource(Res.string.btn_welcome_creature), color = Color(0xFF7EC8E3))
                        }
                    }
                )
            }
        }

        // Blocks management bottom sheet
        if (showBlocksSheet) {
            BlocksBottomSheet(
                blocks = uiState.blocks,
                onAddBlock = {
                    showBlocksSheet = false
                    onNavigateToCreateBlock()
                },
                onEditBlock = { blockId ->
                    showBlocksSheet = false
                    onNavigateToEditBlock(blockId)
                },
                onDismiss = { showBlocksSheet = false }
            )
        }

        // DatePicker para ir a día concreto
        if (showDatePicker) {
            DatePickerDialogWrapper(
                initial = uiState.selectedDate,
                onConfirm = { date ->
                    if (date != null) viewModel.selectDate(date)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        // TaskFormSheet — nueva tarea desde header
        if (showTaskSheet) {
            Dialog(
                onDismissRequest = { showTaskSheet = false; quickTaskBlock = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { showTaskSheet = false; quickTaskBlock = null },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    var formDragY by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, formDragY.roundToInt().coerceAtLeast(0)) }
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (formDragY > 100f) { showTaskSheet = false; quickTaskBlock = null }
                                        formDragY = 0f
                                    }
                                ) { _, dragAmount ->
                                    formDragY = (formDragY + dragAmount).coerceAtLeast(0f)
                                }
                            }
                    ) {
                    TaskFormSheet(
                        blocks = uiState.blocks,
                        initialDate = uiState.selectedDate,
                        initialBlockId = quickTaskBlock?.id,
                        onSaveOneTime = { title, blockId, date, time, notificationsEnabled, targetCount, notes, timerDuration, isPriority ->
                            viewModel.addOneTimeTask(title, blockId, date, time, notificationsEnabled, targetCount, notes, timerDuration, isPriority)
                            showTaskSheet = false
                            quickTaskBlock = null
                        },
                        onSaveRecurring = { title, blockId, time, recurrence, notificationsEnabled, targetCount, noteTemplate, timerDuration, isPriority ->
                            viewModel.addRecurringTask(title, blockId, time, recurrence, notificationsEnabled, targetCount, noteTemplate, timerDuration, isPriority)
                            showTaskSheet = false
                            quickTaskBlock = null
                        },
                        onDismiss = { showTaskSheet = false; quickTaskBlock = null }
                    )
                    }
                }
            }
        }

        // TaskFormSheet — editar tarea existente
        editingTask?.let { task ->
            val isFutureEdit = task.sourceTaskId?.endsWith("_future") == true
            val isAllEdit = task.sourceTaskId?.endsWith("_all") == true
            val isRecurringEdit = isFutureEdit || isAllEdit
            val realSourceId = task.sourceTaskId
                ?.removeSuffix("_future")
                ?.removeSuffix("_all")
            val taskForForm = if (isRecurringEdit) task.copy(sourceTaskId = realSourceId) else task

            // Cargar def si es edición recurrente y aún no la tenemos
            LaunchedEffect(task.id) {
                editingTaskDef = realSourceId?.let { viewModel.getRecurringTaskDef(it) }
            }

            Dialog(
                onDismissRequest = { editingTask = null; editingTaskDef = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { editingTask = null; editingTaskDef = null },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    var editDragY by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, editDragY.roundToInt().coerceAtLeast(0)) }
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (editDragY > 100f) { editingTask = null; editingTaskDef = null }
                                        editDragY = 0f
                                    }
                                ) { _, dragAmount ->
                                    editDragY = (editDragY + dragAmount).coerceAtLeast(0f)
                                }
                            }
                    ) {
                    TaskFormSheet(
                        blocks = uiState.blocks,
                        initialDate = uiState.selectedDate,
                        existingTask = taskForForm,
                        existingDef = if (isRecurringEdit) editingTaskDef else null,
                        forceRecurring = isRecurringEdit,
                        onSaveOneTime = { title, blockId, date, time, notificationsEnabled, targetCount, notes, timerDuration, isPriority ->
                            viewModel.updateOneTimeTask(taskForForm, title, blockId, date, time, notificationsEnabled, targetCount, notes, timerDuration, isPriority)
                            editingTask = null
                            editingTaskDef = null
                        },
                        onSaveRecurring = { title, blockId, time, recurrence, notificationsEnabled, targetCount, noteTemplate, timerDuration, isPriority ->
                            val sourceId = realSourceId ?: return@TaskFormSheet
                            viewModel.updateRecurringTask(sourceId, title, blockId, time, recurrence, notificationsEnabled, targetCount, noteTemplate, timerDuration, isPriority)
                            editingTask = null
                            editingTaskDef = null
                        },
                        onDelete = {
                            if (taskForForm.sourceTaskId != null) {
                                deletingRecurringTask = taskForForm
                            } else {
                                viewModel.deleteTask(taskForForm)
                            }
                            editingTask = null
                            editingTaskDef = null
                        },
                        onDismiss = { editingTask = null; editingTaskDef = null }
                    )
                    }
                }
            }
        }

        // PostponeSheet
        postponingTask?.let { task ->
            Dialog(
                onDismissRequest = { postponingTask = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    PostponeSheet(
                        task = task,
                        onPostpone = { date, time ->
                            viewModel.postponeTask(task, date, time)
                            postponingTask = null
                        },
                        onDismiss = { postponingTask = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainHeader(
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<DayTask>>,
    globalStreak: Int = 0,
    onCalendarClick: () -> Unit,
    onTodayClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onBlocksClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onWeekChange: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Fila superior: nombre + acciones
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Volver a hoy (solo visible si no estamos en hoy)
            if (selectedDate != today) {
                HeaderIconButton(painter = painterResource(Res.drawable.ic_history), contentDescription = stringResource(Res.string.a11y_go_to_today), onClick = onTodayClick)
                Spacer(modifier = Modifier.width(4.dp))
            } else // Racha global (solo visible si >= 2 días consecutivos)
            if (globalStreak >= 2) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(rememberAdaptiveCardColor())
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_flame),
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$globalStreak",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            HeaderIconButton(painter = painterResource(Res.drawable.ic_calendar), contentDescription = stringResource(Res.string.a11y_open_calendar), onClick = onCalendarClick)
            Spacer(modifier = Modifier.width(4.dp))
            HeaderIconButton(painter = painterResource(Res.drawable.ic_plus), contentDescription = stringResource(Res.string.a11y_add_task), onClick = onAddTaskClick)
            Spacer(modifier = Modifier.width(4.dp))
            HeaderIconButton(painter = painterResource(Res.drawable.ic_box), contentDescription = stringResource(Res.string.a11y_manage_blocks), onClick = onBlocksClick)
            Spacer(modifier = Modifier.width(4.dp))
            HeaderIconButton(painter = painterResource(Res.drawable.ic_settings), contentDescription = stringResource(Res.string.a11y_open_settings), onClick = onSettingsClick)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // WeekCalendar
        WeekCalendar(
            selectedDate = selectedDate,
            today = today,
            tasksByDate = tasksByDate,
            onDateSelected = onDateSelected,
            onWeekChange = onWeekChange
        )
    }
}

@Composable
private fun HeaderIconButton(painter: Painter, contentDescription: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(rememberAdaptiveCardColor())
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MainContent(
    blocks: List<WorkBlock>,
    tasksByBlock: Map<String?, List<DayTask>>,
    selectedDate: LocalDate,
    isLoading: Boolean,
    error: String?,
    onTaskToggle: (DayTask) -> Unit,
    onTaskLongPress: (DayTask) -> Unit,
    onBlockHeaderLongPress: (WorkBlock) -> Unit,
    onIncrement: (DayTask) -> Unit = {},
    onDecrement: (DayTask) -> Unit = {},
    onPriorityToggle: (DayTask) -> Unit = {},
    timerStates: Map<String, MainViewModel.TimerState> = emptyMap(),
    onTimerStart: (DayTask) -> Unit = {},
    onTimerPause: (String) -> Unit = {},
    onTimerResume: (String) -> Unit = {},
    onTimerCancel: (String) -> Unit = {},
    onNotesChanged: (DayTask, String?) -> Unit = { _, _ -> },
) {
    // Which blocks were already finished when this day was loaded. Snapshot on purpose:
    // if a block jumped into the collapsed "completed" section the moment the user ticked
    // its last task, the LazyColumn would re-anchor on the moved key and drag the scroll
    // along with it. The block stays put and gets tidied on the next load of the day.
    val preCompletedBlockIds = remember(selectedDate, blocks, tasksByBlock.keys) {
        blocks.filter { block ->
            tasksByBlock[block.id]?.let { tasks ->
                tasks.isNotEmpty() && tasks.all { it.status == TaskStatus.COMPLETED }
            } == true
        }.map { it.id }.toSet()
    }
    val (activeBlocks, completedBlocks) = remember(blocks, tasksByBlock, preCompletedBlockIds) {
        val withTasks = blocks.filter { tasksByBlock[it.id]?.isNotEmpty() == true }
        withTasks.partition { block -> block.id !in preCompletedBlockIds }
    }
    var completedBlocksExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Solo el día en curso se puede marcar/desmarcar. El pasado ya está cerrado y
    // el futuro aún no ha ocurrido: ambos son de lectura.
    val isEditable = selectedDate == currentDate()

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Color(0x33FFFFFF)))

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = TextPrimary)
            }
            error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(Res.string.msg_error, error), color = TextPrimary)
            }
            activeBlocks.isEmpty() && completedBlocks.isEmpty() && tasksByBlock[null].isNullOrEmpty() -> Box(
                Modifier.fillMaxSize(), Alignment.Center
            ) {
                Text(stringResource(Res.string.msg_no_tasks), color = TextSecondary, fontSize = 16.sp)
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val allUnassigned = sortedTasks(tasksByBlock[null] ?: emptyList())
                val activeUnassigned = allUnassigned.filter { it.status != TaskStatus.COMPLETED }
                val completedUnassigned = allUnassigned.filter { it.status == TaskStatus.COMPLETED }

                // Active unassigned tasks first
                if (activeUnassigned.isNotEmpty()) {
                    item {
                        UnassignedSection(
                            tasks = activeUnassigned,
                            isEditable = isEditable,
                            onTaskToggle = onTaskToggle,
                            onTaskLongPress = onTaskLongPress,
                            onIncrement = onIncrement,
                            onDecrement = onDecrement,
                            onPriorityToggle = onPriorityToggle,
                            timerStates = timerStates,
                            onTimerStart = onTimerStart,
                            onTimerPause = onTimerPause,
                            onTimerResume = onTimerResume,
                            onTimerCancel = onTimerCancel,
                            onNotesChanged = onNotesChanged
                        )
                    }
                }
                // Active blocks (with at least one pending task)
                items(activeBlocks, key = { it.id }) { block ->
                    val tasks = sortedTasks(tasksByBlock[block.id] ?: emptyList())
                    BlockSection(
                        block = block,
                        tasks = tasks,
                        selectedDate = selectedDate,
                        isEditable = isEditable,
                        onTaskToggle = onTaskToggle,
                        onTaskLongPress = onTaskLongPress,
                        onHeaderLongPress = onBlockHeaderLongPress,
                        onIncrement = onIncrement,
                        onDecrement = onDecrement,
                        onPriorityToggle = onPriorityToggle,
                        timerStates = timerStates,
                        onTimerStart = onTimerStart,
                        onTimerPause = onTimerPause,
                        onTimerResume = onTimerResume,
                        onTimerCancel = onTimerCancel,
                        onNotesChanged = onNotesChanged
                    )
                }

                // Collapsed completed-blocks section
                if (completedBlocks.isNotEmpty()) {
                    item {
                        CompletedBlocksHeader(
                            count = completedBlocks.size,
                            expanded = completedBlocksExpanded,
                            onToggle = { completedBlocksExpanded = !completedBlocksExpanded }
                        )
                    }
                    if (completedBlocksExpanded) {
                        items(completedBlocks, key = { "done_${it.id}" }) { block ->
                            val tasks = sortedTasks(tasksByBlock[block.id] ?: emptyList())
                            BlockSection(
                                block = block,
                                tasks = tasks,
                                selectedDate = selectedDate,
                                isEditable = isEditable,
                                onTaskToggle = onTaskToggle,
                                onTaskLongPress = onTaskLongPress,
                                onHeaderLongPress = onBlockHeaderLongPress,
                                onIncrement = onIncrement,
                                onDecrement = onDecrement,
                                onPriorityToggle = onPriorityToggle,
                                timerStates = timerStates,
                                onTimerStart = onTimerStart,
                                onTimerPause = onTimerPause,
                                onTimerResume = onTimerResume,
                                onTimerCancel = onTimerCancel,
                                onNotesChanged = onNotesChanged
                            )
                        }
                    }
                }
                // Completed unassigned tasks at the very bottom
                if (completedUnassigned.isNotEmpty()) {
                    item {
                        UnassignedSection(
                            tasks = completedUnassigned,
                            isEditable = isEditable,
                            onTaskToggle = onTaskToggle,
                            onTaskLongPress = onTaskLongPress,
                            onIncrement = onIncrement,
                            onDecrement = onDecrement,
                            onPriorityToggle = onPriorityToggle,
                            timerStates = timerStates,
                            onTimerStart = onTimerStart,
                            onTimerPause = onTimerPause,
                            onTimerResume = onTimerResume,
                            onTimerCancel = onTimerCancel,
                            onNotesChanged = onNotesChanged
                        )
                    }
                }
                item {
                    Text(
                        text = stringResource(Res.string.hint_long_press),
                        color = Color(0x44FFFFFF),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

    }
}

@Composable
private fun ContextMenuItem(
    label: String,
    painter: Painter? = null,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(text = label, color = tint, fontSize = 15.sp)
    }
}

@Composable
private fun BlocksBottomSheet(
    blocks: List<WorkBlock>,
    onAddBlock: () -> Unit,
    onEditBlock: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                    .background(
                        Brush.verticalGradient(listOf(OceanDeep, OceanMid)),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 32.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0x55FFFFFF))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.section_blocks),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    blocks.forEach { block ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditBlock(block.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(block.icon, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = block.name, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text("\u203A", color = TextSecondary, fontSize = 20.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddBlock() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_plus),
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(Res.string.btn_add_block), color = TextSecondary, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockSection(
    block: WorkBlock,
    tasks: List<DayTask>,
    selectedDate: LocalDate,
    isEditable: Boolean = true,
    onTaskToggle: (DayTask) -> Unit,
    onTaskLongPress: (DayTask) -> Unit,
    onHeaderLongPress: (WorkBlock) -> Unit,
    onIncrement: (DayTask) -> Unit = {},
    onDecrement: (DayTask) -> Unit = {},
    onPriorityToggle: (DayTask) -> Unit = {},
    timerStates: Map<String, MainViewModel.TimerState> = emptyMap(),
    onTimerStart: (DayTask) -> Unit = {},
    onTimerPause: (String) -> Unit = {},
    onTimerResume: (String) -> Unit = {},
    onTimerCancel: (String) -> Unit = {},
    onNotesChanged: (DayTask, String?) -> Unit = { _, _ -> }
) {
    Column {
        BlockHeader(
            block = block,
            selectedDate = selectedDate,
            onLongPress = { onHeaderLongPress(block) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        tasks.forEach { task ->
            SwipeableTaskCard(
                task = task,
                blockColor = parseColor(block.color),
                isEditable = isEditable,
                onToggle = { onTaskToggle(task) },
                onLongPress = { onTaskLongPress(task) },
                onIncrement = { onIncrement(task) },
                onDecrement = { onDecrement(task) },
                onPriorityToggle = { onPriorityToggle(task) },
                timerState = timerStates[task.id],
                onTimerStart = { onTimerStart(task) },
                onTimerPause = { onTimerPause(task.id) },
                onTimerResume = { onTimerResume(task.id) },
                onTimerCancel = { onTimerCancel(task.id) },
                onNotesChanged = { onNotesChanged(task, it) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlockHeader(
    block: WorkBlock,
    selectedDate: LocalDate,
    onLongPress: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(parseColor(block.color).copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) { Text(block.icon, fontSize = 18.sp) }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(block.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (shouldShowBlockTime(block, selectedDate)) {
                val recurrence = block.recurrence as Recurrence.Weekly
                val slot = recurrence.slots.first { it.dayOfWeek == selectedDate.dayOfWeek.isoDayNumber }
                Text("${slot.startTime} - ${slot.endTime}", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SwipeableTaskCard(
    task: DayTask,
    blockColor: Color,
    isEditable: Boolean = true,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onPriorityToggle: () -> Unit,
    timerState: MainViewModel.TimerState?,
    onTimerStart: () -> Unit,
    onTimerPause: () -> Unit,
    onTimerResume: () -> Unit,
    onTimerCancel: () -> Unit = {},
    onNotesChanged: (String?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val swipeThreshold = 300f // px

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
    ) {
        // Background color behind the card during swipe
        val swipeBg = when {
            offsetX.value > 30f -> Color(0xFF4CAF50).copy(alpha = (offsetX.value / swipeThreshold).coerceIn(0f, 0.4f))
            offsetX.value < -30f -> Color(0xFF9E9E9E).copy(alpha = (-offsetX.value / swipeThreshold).coerceIn(0f, 0.4f))
            else -> Color.Transparent
        }
        Box(
            modifier = Modifier.matchParentSize().background(swipeBg, RoundedCornerShape(12.dp)),
            contentAlignment = if (offsetX.value > 0) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            if (kotlin.math.abs(offsetX.value) > 30f) {
                Text(
                    text = if (offsetX.value > 0) {
                        if (task.isCountable) "+1" else "✓"
                    } else {
                        if (task.isCountable) "-1" else "✗"
                    },
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(task.id, isEditable) {
                    if (!isEditable) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val isCompleted = task.status == TaskStatus.COMPLETED
                            if (offsetX.value > swipeThreshold) {
                                // Right: +1 or complete (noop if already completed)
                                if (!isCompleted) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (task.isCountable) onIncrement() else onToggle()
                                }
                            } else if (offsetX.value < -swipeThreshold) {
                                // Left: -1 or uncomplete (noop if not completed for non-countable)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (task.isCountable) onDecrement() else if (isCompleted) onToggle()
                            }
                            scope.launch { offsetX.animateTo(0f, animationSpec = tween(200)) }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, animationSpec = tween(200)) }
                        }
                    ) { _, dragAmount ->
                        scope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-400f, 400f)) }
                    }
                }
        ) {
            TaskCard(
                task = task,
                isEditable = isEditable,
                blockColor = blockColor,
                onToggle = onToggle,
                onLongPress = onLongPress,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
                onPriorityToggle = onPriorityToggle,
                timerState = timerState,
                onTimerStart = onTimerStart,
                onTimerPause = onTimerPause,
                onTimerResume = onTimerResume,
                onTimerCancel = onTimerCancel,
                onNotesChanged = onNotesChanged
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(
    task: DayTask,
    blockColor: Color,
    isEditable: Boolean = true,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {},
    onPriorityToggle: () -> Unit = {},
    timerState: MainViewModel.TimerState? = null,
    onTimerStart: () -> Unit = {},
    onTimerPause: () -> Unit = {},
    onTimerResume: () -> Unit = {},
    onTimerCancel: () -> Unit = {},
    onNotesChanged: (String?) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val isCompleted = task.status == TaskStatus.COMPLETED
    val isExpired = task.status == TaskStatus.EXPIRED
    val taskTime = (task.schedule as? TaskSchedule.OneTime)?.time
    var showNotesDialog by remember { mutableStateOf(false) }
    val taskCardBg = rememberAdaptiveCardColor()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(taskCardBg)
            .combinedClickable(onClick = {
                if (task.isCountable && !isCompleted && isEditable) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIncrement()
                }
            }, onLongClick = onLongPress)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority star (not clickable — read-only indicator)
        if (task.isPriority) {
            Icon(
                painter = painterResource(Res.drawable.ic_star),
                contentDescription = null,
                tint = Color(0xFFFFB347),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Content column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                color = when {
                    isCompleted || isExpired -> TextSecondary
                    else -> TextPrimary
                },
                fontSize = 14.sp,
                fontWeight = if (isCompleted || isExpired) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1
            )

            // Time display
            if (taskTime != null) {
                Text(
                    text = "${taskTime.hour.toString().padStart(2, '0')}:${
                        taskTime.minute.toString().padStart(2, '0')
                    }",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Notes icon — opens NotesDialog
        if (task.notes != null) {
            Icon(
                painter = painterResource(Res.drawable.ic_file_text),
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { showNotesDialog = true }
                    .padding(6.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
        }

        // Right side: all applicable indicators shown simultaneously
        run {
            // Timer controls (if task has timer and not completed)
            if (task.timerDurationMinutes != null && !isCompleted && isEditable) {
                if (timerState != null) {
                    // Timer running or paused
                    val mins = timerState.remainingSeconds / 60
                    val secs = timerState.remainingSeconds % 60
                    val progress = 1f - timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(28.dp),
                                color = blockColor,
                                trackColor = Color(0x33FFFFFF),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "${mins}:${secs.toString().padStart(2, '0')}",
                                color = TextPrimary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(
                                if (timerState.isRunning) Res.drawable.ic_pause else Res.drawable.ic_play
                            ),
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    if (timerState.isRunning) onTimerPause() else onTimerResume()
                                }
                                .padding(5.dp)
                        )
                        Icon(
                            painter = painterResource(Res.drawable.ic_x),
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onTimerCancel() }
                                .padding(6.dp)
                        )
                    }
                } else {
                    // Timer not started — show play button with duration
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FFFFFF))
                            .clickable { onTimerStart() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_play),
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${task.timerDurationMinutes}m",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                if (task.isCountable) Spacer(Modifier.width(6.dp))
            }
            // Countable progress (if applicable)
            if (task.isCountable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExpired && task.timerDurationMinutes == null) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_clock),
                            contentDescription = stringResource(Res.string.a11y_task_expired),
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = "${task.currentCount}/${task.targetCount}",
                        color = if (isCompleted) blockColor else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // Checkbox (only for simple tasks: not countable AND not timer)
            if (!task.isCountable && task.timerDurationMinutes == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExpired) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_clock),
                            contentDescription = stringResource(Res.string.a11y_task_expired),
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Checkbox(
                        checked = isCompleted,
                        enabled = isEditable,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle()
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = blockColor,
                            uncheckedColor = TextSecondary,
                            checkmarkColor = TextPrimary
                        )
                    )
                }
            }
        }
    }

    // NotesDialog
    if (showNotesDialog && task.notes != null) {
        NotesDialog(
            notes = task.notes,
            onNotesChanged = onNotesChanged,
            onDismiss = { showNotesDialog = false }
        )
    }
}

@Composable
private fun NotesDialog(
    notes: String,
    onNotesChanged: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(notes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A2A4A))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) stringResource(Res.string.label_edit) else stringResource(Res.string.label_notes),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
                        Icon(
                            painter = painterResource(if (isEditing) Res.drawable.ic_check else Res.drawable.ic_pencil),
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp).clickable {
                                if (isEditing) {
                                    onNotesChanged(editText.ifBlank { null })
                                    isEditing = false
                                } else {
                                    editText = notes
                                    isEditing = true
                                }
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            painter = painterResource(Res.drawable.ic_x),
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp).clickable { onDismiss() }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (isEditing) {
                    // Markdown helper buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val helpers = listOf(
                            "- [ ] " to "☐",
                            "- " to "•",
                            "**" to "B",
                            "# " to "H"
                        )
                        helpers.forEach { (insert, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33FFFFFF))
                                    .clickable {
                                        editText = if (editText.isNotEmpty() && !editText.endsWith("\n")) {
                                            "$editText\n$insert"
                                        } else {
                                            "$editText$insert"
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    BasicTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TextPrimary,
                            fontSize = 13.sp
                        ),
                        cursorBrush = Brush.verticalGradient(listOf(TextPrimary, TextPrimary))
                    )
                } else {
                    InteractiveMarkdownText(
                        text = notes,
                        onToggleCheckbox = { lineIndex ->
                            val lines = notes.lines().toMutableList()
                            if (lineIndex in lines.indices) {
                                val line = lines[lineIndex]
                                lines[lineIndex] = when {
                                    line.startsWith("- [ ] ") -> line.replaceFirst("- [ ] ", "- [x] ")
                                    line.startsWith("- [x] ") || line.startsWith("- [X] ") ->
                                        line.replaceFirst(Regex("- \\[[xX]] "), "- [ ] ")
                                    else -> line
                                }
                                onNotesChanged(lines.joinToString("\n"))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveMarkdownText(
    text: String,
    onToggleCheckbox: (lineIndex: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        text.lines().forEachIndexed { index, line ->
            when {
                line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleCheckbox(index) }
                    ) {
                        Text("☑ ", color = Color(0xFF4CAF50), fontSize = 13.sp)
                        Text(line.removePrefix("- [x] ").removePrefix("- [X] "), color = TextSecondary, fontSize = 13.sp, textDecoration = TextDecoration.LineThrough)
                    }
                }
                line.startsWith("- [ ] ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleCheckbox(index) }
                    ) {
                        Text("☐ ", color = TextSecondary, fontSize = 13.sp)
                        Text(line.removePrefix("- [ ] "), color = TextPrimary, fontSize = 13.sp)
                    }
                }
                line.startsWith("- ") -> {
                    Row {
                        Text("• ", color = TextSecondary, fontSize = 13.sp)
                        Text(line.removePrefix("- "), color = TextPrimary, fontSize = 13.sp)
                    }
                }
                line.startsWith("## ") -> {
                    Text(line.removePrefix("## "), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                line.startsWith("**") && line.endsWith("**") -> {
                    Text(line.removeSurrounding("**"), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                line.contains("**") -> {
                    Row {
                        var remaining = line
                        while (remaining.contains("**")) {
                            val before = remaining.substringBefore("**")
                            if (before.isNotEmpty()) Text(before, color = TextPrimary, fontSize = 13.sp)
                            remaining = remaining.substringAfter("**")
                            val bold = remaining.substringBefore("**", "")
                            if (bold.isNotEmpty()) Text(bold, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            remaining = remaining.substringAfter("**", "")
                        }
                        if (remaining.isNotEmpty()) Text(remaining, color = TextPrimary, fontSize = 13.sp)
                    }
                }
                line.isBlank() -> Spacer(modifier = Modifier.height(4.dp))
                else -> Text(line, color = TextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun UnassignedSection(
    tasks: List<DayTask>,
    isEditable: Boolean = true,
    onTaskToggle: (DayTask) -> Unit,
    onTaskLongPress: (DayTask) -> Unit,
    onIncrement: (DayTask) -> Unit = {},
    onDecrement: (DayTask) -> Unit = {},
    onPriorityToggle: (DayTask) -> Unit = {},
    timerStates: Map<String, MainViewModel.TimerState> = emptyMap(),
    onTimerStart: (DayTask) -> Unit = {},
    onTimerPause: (String) -> Unit = {},
    onTimerResume: (String) -> Unit = {},
    onTimerCancel: (String) -> Unit = {},
    onNotesChanged: (DayTask, String?) -> Unit = { _, _ -> }
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_box),
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(stringResource(Res.string.section_no_block), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        tasks.forEach { task ->
            SwipeableTaskCard(
                task = task,
                isEditable = isEditable,
                blockColor = Color(0x66FFFFFF),
                onToggle = { onTaskToggle(task) },
                onLongPress = { onTaskLongPress(task) },
                onIncrement = { onIncrement(task) },
                onDecrement = { onDecrement(task) },
                onPriorityToggle = { onPriorityToggle(task) },
                timerState = timerStates[task.id],
                onTimerStart = { onTimerStart(task) },
                onTimerPause = { onTimerPause(task.id) },
                onTimerResume = { onTimerResume(task.id) },
                onTimerCancel = { onTimerCancel(task.id) },
                onNotesChanged = { onNotesChanged(task, it) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CompletedBlocksHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = tween(durationMillis = 200),
        label = "completed_chevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_down),
            contentDescription = null,
            tint = Color(0x99FFFFFF),
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = rotation }
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(Res.string.label_completed_blocks, count),
            color = Color(0xCCFFFFFF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

expect fun parseColor(hex: String): Color