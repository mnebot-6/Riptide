package com.mnebot.riptide.presentation.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mnebot.riptide.NightSummaryScheduler
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.presentation.aquarium.AquariumBackground
import com.mnebot.riptide.presentation.aquarium.AquariumCreatures
import com.mnebot.riptide.presentation.components.DatePickerDialogWrapper
import com.mnebot.riptide.presentation.task.PostponeSheet
import com.mnebot.riptide.presentation.task.TaskFormSheet
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.LocalTime
import kotlin.collections.mapKeys
import com.mnebot.riptide.presentation.aquarium.CreatureDetailDialog
import com.mnebot.riptide.presentation.aquarium.CreatureFreezeState
import com.mnebot.riptide.presentation.aquarium.rememberCreatureFreezeState
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.presentation.aquarium.CreatureSpec

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanLight = Color(0xFF2E5F9E)
private val CardBackground = Color(0x33FFFFFF)
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
            // Hora de la tarea no completada más temprana con hora definida
            val earliestTaskTime = tasksByBlock[block.id]
                ?.filter { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.POSTPONED }
                ?.mapNotNull { (it.schedule as? TaskSchedule.OneTime)?.time }
                ?.minOrNull()
                ?.toSecondOfDay()

            if (earliestTaskTime != null) {
                earliestTaskTime
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
    fun taskSortKey(task: DayTask): Pair<Int, Int> {
        val time = (task.schedule as? TaskSchedule.OneTime)?.time
        return if (time != null) Pair(0, time.toSecondOfDay()) else Pair(1, 0)
    }
    val (completed, active) = tasks.partition { it.status == TaskStatus.COMPLETED }
    return active.sortedWith(compareBy({ taskSortKey(it).first }, { taskSortKey(it).second })) +
            completed.sortedWith(compareBy({ taskSortKey(it).first }, { taskSortKey(it).second }))
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
    onNavigateToEcosystem: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val nightSummaryTime by nightSummaryScheduler.getNightSummaryTime()
        .collectAsState(initial = LocalTime(23, 30))
    var showAquarium by remember { mutableStateOf(false) }
    var showDrawer by remember { mutableStateOf(false) }
    var showTaskSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DayTask?>(null) }
    var postponingTask by remember { mutableStateOf<DayTask?>(null) }
    var contextMenuTask by remember { mutableStateOf<DayTask?>(null) }
    var deletingRecurringTask by remember { mutableStateOf<DayTask?>(null) }
    var editingScopeTask by remember { mutableStateOf<DayTask?>(null) }
    var editingTaskDef by remember { mutableStateOf<RecurringTaskDef?>(null) }
    var quickTaskBlock by remember { mutableStateOf<WorkBlock?>(null) }
    var selectedCreature by remember { mutableStateOf<Pair<MarineCreature, CreatureSpec>?>(null) }
    val creatureFreezeState = rememberCreatureFreezeState()

    val drawerOffsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(showDrawer, uiState.selectedDate) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (!showDrawer && drawerOffsetY.value > 0.3f) {
                                drawerOffsetY.animateTo(1f, animationSpec = tween(250))
                                showDrawer = true
                            } else if (showDrawer && drawerOffsetY.value < 0.7f) {
                                drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                                showDrawer = false
                            } else {
                                drawerOffsetY.animateTo(
                                    if (showDrawer) 1f else 0f,
                                    animationSpec = tween(250)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            drawerOffsetY.animateTo(
                                if (showDrawer) 1f else 0f,
                                animationSpec = tween(250)
                            )
                        }
                    }
                ) { _, dragAmount ->
                    val isVertical = kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)
                    val isHorizontal = kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)
                    when {
                        isVertical && !showDrawer && dragAmount.y > 0 -> {
                            scope.launch {
                                drawerOffsetY.snapTo(
                                    (drawerOffsetY.value + dragAmount.y / 600f).coerceIn(0f, 1f)
                                )
                            }
                        }
                        isVertical && showDrawer && dragAmount.y < 0 -> {
                            scope.launch {
                                drawerOffsetY.snapTo(
                                    (drawerOffsetY.value + dragAmount.y / 600f).coerceIn(0f, 1f)
                                )
                            }
                        }
                        isHorizontal && !showDrawer && dragAmount.x < -40 -> {
                            scope.launch {
                                viewModel.selectDate(uiState.selectedDate.plus(1, DateTimeUnit.DAY))
                            }
                        }
                        isHorizontal && !showDrawer && dragAmount.x > 40 -> {
                            scope.launch {
                                viewModel.selectDate(uiState.selectedDate.minus(1, DateTimeUnit.DAY))
                            }
                        }
                    }
                }
            }
    ) {
        AquariumBackground()
        AquariumCreatures(
            ecosystemByCategory = uiState.ecosystemByCategory,
            creatureLevelBySpecies = uiState.creatureLevelBySpecies,
            creaturesData = uiState.creaturesData,
            freezeState = creatureFreezeState,
            onCreatureTap = { creature, spec ->
                selectedCreature = creature to spec
            }
        )

        when {
            showAquarium -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    FloatingActionButton(
                        onClick = { showAquarium = false },
                        modifier = Modifier.padding(24.dp),
                        containerColor = CardBackground,
                        contentColor = TextPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) { Text("✕", fontSize = 20.sp) }
                }
            }
            else -> {
                val sorted = sortedBlocks(uiState.blocks, uiState.tasksByBlock, uiState.selectedDate)

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header fijo
                    MainHeader(
                        selectedDate = uiState.selectedDate,
                        today = currentDate(),
                        tasksByBlock = uiState.tasksByBlock,
                        onCalendarClick = { showDatePicker = true },
                        onTodayClick = { viewModel.selectDate(currentDate()) },
                        onAddTaskClick = { showTaskSheet = true },
                        onDrawerClick = {
                            scope.launch {
                                drawerOffsetY.animateTo(1f, animationSpec = tween(250))
                                showDrawer = true
                            }
                        },
                        onDateSelected = { viewModel.selectDate(it) },
                        onWeekChange = { viewModel.selectDate(it) }
                    )

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
                        streaksByBlock = uiState.streaksByBlock,
                        onAquariumClick = { showAquarium = true }
                    )
                }
            }
        }

        // Menú contextual
        contextMenuTask?.let { task ->
            AlertDialog(
                onDismissRequest = { contextMenuTask = null },
                containerColor = OceanMid,
                title = {
                    Text(task.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ContextMenuItem("✏️ Editar") {
                            if (task.sourceTaskId != null) {
                                editingScopeTask = task
                            } else {
                                editingTask = task
                            }
                            contextMenuTask = null
                        }
                        if (task.status != TaskStatus.COMPLETED && task.status != TaskStatus.EXPIRED) {
                            ContextMenuItem("⏰ Posponer") {
                                postponingTask = task
                                contextMenuTask = null
                            }
                        }
                        ContextMenuItem("🗑️ Eliminar", tint = Color(0xFFEA4335)) {
                            if (task.sourceTaskId != null) {
                                deletingRecurringTask = task
                            } else {
                                viewModel.deleteTask(task)
                            }
                            contextMenuTask = null
                        }
                    }
                },
                confirmButton = {}
            )
        }

        selectedCreature?.let { (creature, spec) ->
            CreatureDetailDialog(
                creature = creature,
                spec = spec,
                onDismiss = {
                    creatureFreezeState.unfreeze(creature.species)
                    selectedCreature = null
                },
                onNicknameChanged = { nickname ->
                    viewModel.updateCreatureNickname(creature.id, nickname)
                }
            )
        }

        // Diálogo scope edición recurrente
        editingScopeTask?.let { task ->
            AlertDialog(
                onDismissRequest = { editingScopeTask = null },
                containerColor = OceanMid,
                title = {
                    Text("¿Qué quieres editar?", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ContextMenuItem("Solo esta ocurrencia") {
                            editingTask = task
                            editingScopeTask = null
                        }
                        ContextMenuItem("Esta y las futuras") {
                            // Marcamos con una convención: guardamos en editingTask pero
                            // el onSaveRecurring usará updateRecurringTask
                            editingTask = task.copy(sourceTaskId = task.sourceTaskId + "_future")
                            editingScopeTask = null
                        }
                        ContextMenuItem("Todas las ocurrencias") {
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
                    Text("¿Qué quieres eliminar?", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ContextMenuItem("Solo esta ocurrencia") {
                            viewModel.deleteRecurringTaskInstance(task)
                            deletingRecurringTask = null
                        }
                        ContextMenuItem("Esta y las futuras") {
                            viewModel.deleteRecurringTaskFromDate(task)
                            deletingRecurringTask = null
                        }
                        ContextMenuItem("Todas las ocurrencias") {
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
                    Text("Resumen de ayer", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(summary.feedbackMessage, color = TextPrimary, fontSize = 15.sp)
                        Text(
                            "${summary.tasksCompleted} de ${summary.tasksTotal} tareas completadas",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissSummary() }) {
                        Text("Cerrar", color = Color(0xFF7EC8E3))
                    }
                }
            )
        }

        val currentUnlock = uiState.pendingUnlocks.firstOrNull()
        if (currentUnlock != null && uiState.pendingSummary == null) {
            var nickname by remember(currentUnlock) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = {},
                containerColor = OceanMid,
                title = {
                    Text("¡Algo nuevo en el estanque!", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentUnlock.emoji, fontSize = 48.sp)
                        Text(
                            "Un nuevo habitante ha llegado al estanque.\n¿Cómo quieres llamarle?",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            placeholder = { Text("Nombre...", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = Color(0xFF7EC8E3),
                                unfocusedBorderColor = TextSecondary
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmUnlock(currentUnlock, nickname) },
                        enabled = nickname.isNotBlank()
                    ) {
                        Text("Bienvenido al estanque 🌊", color = Color(0xFF7EC8E3))
                    }
                }
            )
        }

        // Drawer
        if (showDrawer || drawerOffsetY.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f * drawerOffsetY.value))
                    .clickable {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = -size.height * (1f - drawerOffsetY.value)
                    }
                    .pointerInput(showDrawer) {
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (drawerOffsetY.value < 0.7f) {
                                        drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                                        showDrawer = false
                                    } else {
                                        drawerOffsetY.animateTo(1f, animationSpec = tween(250))
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    drawerOffsetY.animateTo(
                                        if (showDrawer) 1f else 0f,
                                        animationSpec = tween(250)
                                    )
                                }
                            }
                        ) { _, dragAmount ->
                            val isVertical = kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)
                            if (isVertical && dragAmount.y < 0) {
                                scope.launch {
                                    drawerOffsetY.snapTo(
                                        (drawerOffsetY.value + dragAmount.y / 600f).coerceIn(0f, 1f)
                                    )
                                }
                            }
                        }
                    }
            ) {
                MainDrawer(
                    blocks = uiState.blocks,
                    nightSummaryTime = nightSummaryTime,
                    onAddBlock = {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        onNavigateToCreateBlock()
                    },
                    onEditBlock = { blockId ->
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        onNavigateToEditBlock(blockId)
                    },
                    onNightSummaryTimeChanged = { time ->
                        nightSummaryScheduler.scheduleWorker(time)
                        viewModel.updateNightSummaryTime(time)
                    },
                    onNavigateToEcosystem = {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        onNavigateToEcosystem()
                    }
                )
            }
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    TaskFormSheet(
                        blocks = uiState.blocks,
                        initialDate = uiState.selectedDate,
                        initialBlockId = quickTaskBlock?.id,
                        onSaveOneTime = { title, blockId, date, time ->
                            viewModel.addOneTimeTask(title, blockId, date, time)
                            showTaskSheet = false
                            quickTaskBlock = null
                        },
                        onSaveRecurring = { title, blockId, time, recurrence ->
                            viewModel.addRecurringTask(title, blockId, time, recurrence)
                            showTaskSheet = false
                            quickTaskBlock = null
                        },
                        onDismiss = { showTaskSheet = false; quickTaskBlock = null }
                    )
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    TaskFormSheet(
                        blocks = uiState.blocks,
                        initialDate = uiState.selectedDate,
                        existingTask = taskForForm,
                        existingDef = if (isRecurringEdit) editingTaskDef else null,
                        forceRecurring = isRecurringEdit,
                        onSaveOneTime = { title, blockId, date, time ->
                            viewModel.updateOneTimeTask(taskForForm, title, blockId, date, time)
                            editingTask = null
                            editingTaskDef = null
                        },
                        onSaveRecurring = { title, blockId, time, recurrence ->
                            val sourceId = realSourceId ?: return@TaskFormSheet
                            viewModel.updateRecurringTask(sourceId, title, blockId, time, recurrence)
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
    tasksByBlock: Map<String?, List<DayTask>>,
    onCalendarClick: () -> Unit,
    onTodayClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onDrawerClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onWeekChange: (LocalDate) -> Unit
) {
    val tasksByDate = remember(tasksByBlock) {
        tasksByBlock.values
            .flatten()
            .groupBy { task ->
                when (val schedule = task.schedule) {
                    is TaskSchedule.OneTime -> schedule.date
                    is TaskSchedule.Recurring -> null
                }
            }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 8.dp)
    ) {
        // Fila superior: nombre + acciones
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌊", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Riptide",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Volver a hoy (solo visible si no estamos en hoy)
            if (selectedDate != today) {
                HeaderIconButton(icon = "⟳", onClick = onTodayClick)
                Spacer(modifier = Modifier.width(4.dp))
            }

            HeaderIconButton(icon = "📅", onClick = onCalendarClick)
            Spacer(modifier = Modifier.width(4.dp))
            HeaderIconButton(icon = "➕", onClick = onAddTaskClick)
            Spacer(modifier = Modifier.width(4.dp))
            HeaderIconButton(icon = "☰", onClick = onDrawerClick)
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
private fun HeaderIconButton(icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(CardBackground)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 16.sp)
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
    onAquariumClick: () -> Unit,
    streaksByBlock: Map<String, Int>,
) {
    val blocksWithTasks = blocks.filter { tasksByBlock[it.id]?.isNotEmpty() == true }
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Color(0x33FFFFFF)))

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = TextPrimary)
            }
            error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Error: $error", color = TextPrimary)
            }
            blocksWithTasks.isEmpty() && tasksByBlock[null].isNullOrEmpty() -> Box(
                Modifier.fillMaxSize(), Alignment.Center
            ) {
                Text("No hay tareas para hoy 🌊", color = TextSecondary, fontSize = 16.sp)
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val unassignedTasks = sortedTasks(tasksByBlock[null] ?: emptyList())
                if (unassignedTasks.isNotEmpty()) {
                    item {
                        UnassignedSection(
                            tasks = unassignedTasks,
                            onTaskToggle = onTaskToggle,
                            onTaskLongPress = onTaskLongPress
                        )
                    }
                }
                items(blocksWithTasks) { block ->
                    val tasks = sortedTasks(tasksByBlock[block.id] ?: emptyList())
                    val streak = streaksByBlock[block.id] ?: 0
                    BlockSection(
                        block = block,
                        tasks = tasks,
                        selectedDate = selectedDate,
                        streak = streak,
                        onTaskToggle = onTaskToggle,
                        onTaskLongPress = onTaskLongPress,
                        onHeaderLongPress = onBlockHeaderLongPress
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAquariumClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = CardBackground,
            contentColor = TextPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) { Text("🐟", fontSize = 20.sp) }
    }
}

@Composable
private fun ContextMenuItem(label: String, tint: Color = TextPrimary, onClick: () -> Unit) {
    Text(
        text = label,
        color = tint,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    )
}

@Composable
private fun BlockSection(
    block: WorkBlock,
    tasks: List<DayTask>,
    selectedDate: LocalDate,
    streak: Int,
    onTaskToggle: (DayTask) -> Unit,
    onTaskLongPress: (DayTask) -> Unit,
    onHeaderLongPress: (WorkBlock) -> Unit
) {
    Column {
        BlockHeader(
            block = block,
            selectedDate = selectedDate,
            streak = streak,
            onLongPress = { onHeaderLongPress(block) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        tasks.forEach { task ->
            TaskCard(
                task = task,
                blockColor = parseColor(block.color),
                onToggle = { onTaskToggle(task) },
                onLongPress = { onTaskLongPress(task) }
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
    streak: Int,
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
            if (streak >= 2) {
                Text(
                    text = "🔥 $streak días",
                    color = Color(0xFFFFB347),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(
    task: DayTask,
    blockColor: Color,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    val isExpired = task.status == TaskStatus.EXPIRED
    val isPostponed = task.status == TaskStatus.POSTPONED
    val taskTime = (task.schedule as? TaskSchedule.OneTime)?.time

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isExpired) Color(0x66FFFFFF) else blockColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                color = when {
                    isCompleted || isExpired || isPostponed -> TextSecondary
                    else -> TextPrimary
                },
                fontSize = 14.sp,
                fontWeight = if (isCompleted || isExpired || isPostponed) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
            if (taskTime != null) {
                Text(
                    text = "${taskTime.hour.toString().padStart(2, '0')}:${
                        taskTime.minute.toString().padStart(2, '0')
                    }",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            if (isPostponed && task.postponedTo != null) {
                Text(
                    text = "→ ${task.postponedTo.date} ${
                        task.postponedTo.time.hour.toString().padStart(2, '0')
                    }:${task.postponedTo.time.minute.toString().padStart(2, '0')}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        when {
            isPostponed -> Text("⏰", fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExpired) {
                        Text("⌛", fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
                    }
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = { onToggle() },
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
}

@Composable
private fun UnassignedSection(
    tasks: List<DayTask>,
    onTaskToggle: (DayTask) -> Unit,
    onTaskLongPress: (DayTask) -> Unit
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
            ) { Text("📋", fontSize = 18.sp) }
            Spacer(modifier = Modifier.width(10.dp))
            Text("Sin bloque", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        tasks.forEach { task ->
            TaskCard(
                task = task,
                blockColor = Color(0x66FFFFFF),
                onToggle = { onTaskToggle(task) },
                onLongPress = { onTaskLongPress(task) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

expect fun parseColor(hex: String): Color