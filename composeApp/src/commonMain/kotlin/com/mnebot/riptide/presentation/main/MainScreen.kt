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
            val recurrence = block.recurrence
            if (recurrence is Recurrence.Weekly) {
                val slot = recurrence.slots.firstOrNull { it.dayOfWeek == dayOfWeek }
                if (slot?.startTime != null) slot.startTime.toSecondOfDay() else Int.MAX_VALUE
            } else Int.MAX_VALUE
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
    onNavigateToEditBlock: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val nightSummaryTime by nightSummaryScheduler.getNightSummaryTime()
        .collectAsState(initial = LocalTime(23, 30))
    var showAquarium by remember { mutableStateOf(false) }
    var showDrawer by remember { mutableStateOf(false) }
    var showTaskSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DayTask?>(null) }
    var postponingTask by remember { mutableStateOf<DayTask?>(null) }
    var contextMenuTask by remember { mutableStateOf<DayTask?>(null) }
    var deletingRecurringTask by remember { mutableStateOf<DayTask?>(null) }
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
        // Capa 1 — fondo siempre visible
        AquariumBackground()

        // Capa 2 — criaturas siempre visibles (si nivel >= 2)
        AquariumCreatures(
            ecosystemByCategory = uiState.ecosystemByCategory
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
                MainContent(
                    blocks = sorted,
                    tasksByBlock = uiState.tasksByBlock,
                    selectedDate = uiState.selectedDate,
                    today = currentDate(),
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onDateSelected = { viewModel.selectDate(it) },
                    onTaskToggle = { viewModel.toggleTaskCompleted(it) },
                    onTaskLongPress = { contextMenuTask = it },
                    streaksByBlock = uiState.streaksByBlock,
                    onAquariumClick = { showAquarium = true }
                )
            }
        }

        // Menú contextual
        contextMenuTask?.let { task ->
            AlertDialog(
                onDismissRequest = { contextMenuTask = null },
                containerColor = Color(0xFF1B3A6B),
                title = {
                    Text(
                        task.title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ContextMenuItem("✏️ Editar") {
                            editingTask = task
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

        deletingRecurringTask?.let { task ->
            AlertDialog(
                onDismissRequest = { deletingRecurringTask = null },
                containerColor = Color(0xFF1B3A6B),
                title = {
                    Text(
                        "¿Qué quieres eliminar?",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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
                containerColor = Color(0xFF1B3A6B),
                title = {
                    Text(
                        "Resumen de ayer",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            summary.feedbackMessage,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
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
                containerColor = Color(0xFF1B3A6B),
                title = {
                    Text(
                        "¡Algo nuevo en el estanque!",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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
            ) {
                MainDrawer(
                    blocks = uiState.blocks,
                    nightSummaryTime = nightSummaryTime,
                    onAddTask = {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        showTaskSheet = true
                    },
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
                    onNightSummaryTimeChanged = { newTime ->
                        scope.launch {
                            nightSummaryScheduler.setNightSummaryTime(newTime)
                            nightSummaryScheduler.scheduleWorker(newTime)
                        }
                    }
                )
            }
        }

        if (showTaskSheet) {
            Dialog(
                onDismissRequest = { showTaskSheet = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    TaskFormSheet(
                        blocks = uiState.blocks,
                        initialDate = uiState.selectedDate,
                        onSaveOneTime = { title, blockId, date, time ->
                            viewModel.addOneTimeTask(title, blockId, date, time)
                            showTaskSheet = false
                        },
                        onSaveRecurring = { title, blockId, time, recurrence ->
                            viewModel.addRecurringTask(title, blockId, time, recurrence)
                            showTaskSheet = false
                        },
                        onDismiss = { showTaskSheet = false }
                    )
                }
            }
        }

        editingTask?.let { task ->
            val isRecurringInstance = task.sourceTaskId != null
            var editingRecurringScope by remember(task) { mutableStateOf<String?>(null) }
            // "instance" = solo esta, "all" = todas las futuras

            if (isRecurringInstance && editingRecurringScope == null) {
                AlertDialog(
                    onDismissRequest = { editingTask = null },
                    containerColor = Color(0xFF1B3A6B),
                    title = {
                        Text(
                            "¿Qué quieres editar?",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ContextMenuItem("Solo esta ocurrencia") {
                                editingRecurringScope = "instance"
                            }
                            ContextMenuItem("Esta y todas las futuras") {
                                editingRecurringScope = "all"
                            }
                        }
                    },
                    confirmButton = {}
                )
            } else if (!isRecurringInstance || editingRecurringScope != null) {
                Dialog(
                    onDismissRequest = { editingTask = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        TaskFormSheet(
                            blocks = uiState.blocks,
                            initialDate = uiState.selectedDate,
                            existingTask = task,
                            onSaveOneTime = { title, blockId, date, time ->
                                viewModel.updateOneTimeTask(task, title, blockId, date, time)
                                editingTask = null
                            },
                            onSaveRecurring = { title, blockId, time, recurrence ->
                                val sourceId = task.sourceTaskId
                                if (editingRecurringScope == "all" && sourceId != null) {
                                    viewModel.updateRecurringTask(sourceId, title, blockId, time, recurrence)
                                } else {
                                    viewModel.updateOneTimeTask(
                                        task,
                                        title,
                                        blockId,
                                        (task.schedule as? TaskSchedule.OneTime)?.date ?: currentDate(),
                                        time
                                    )
                                }
                                editingTask = null
                            },
                            onDelete = {
                                if (isRecurringInstance) {
                                    deletingRecurringTask = task
                                } else {
                                    viewModel.deleteTask(task)
                                }
                                editingTask = null
                            },
                            onDismiss = { editingTask = null }
                        )
                    }
                }
            }
        }

        postponingTask?.let { task ->
            Dialog(
                onDismissRequest = { postponingTask = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    PostponeSheet(
                        task = task,
                        onPostpone = { postponedTo ->
                            viewModel.postponeTask(task, postponedTo)
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
private fun ContextMenuItem(
    label: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
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
private fun MainContent(
    blocks: List<WorkBlock>,
    tasksByBlock: Map<String?, List<DayTask>>,
    selectedDate: LocalDate,
    today: LocalDate,
    isLoading: Boolean,
    error: String?,
    onDateSelected: (LocalDate) -> Unit,
    onTaskToggle: (DayTask) -> Unit,
    onTaskLongPress: (DayTask) -> Unit,
    onAquariumClick: () -> Unit,
    streaksByBlock: Map<String, Int>,
) {
    val blocksWithTasks = blocks.filter { tasksByBlock[it.id]?.isNotEmpty() == true }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 40.dp)
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

            WeekCalendar(
                selectedDate = selectedDate,
                today = today,
                tasksByDate = tasksByDate,
                onDateSelected = onDateSelected,
                onWeekChange = onDateSelected
            )

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x33FFFFFF)))

            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = TextPrimary)
                }
                error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Error: $error", color = TextPrimary)
                }
                blocksWithTasks.isEmpty() && tasksByBlock[null].isNullOrEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    Alignment.Center
                ) {
                    Text("No hay tareas para hoy 🌊", color = TextSecondary, fontSize = 16.sp)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    val unassignedTasks = sortedTasks(tasksByBlock[null] ?: emptyList())
                    if (unassignedTasks.isNotEmpty()) {
                        item {
                            UnassignedSection(
                                tasks = unassignedTasks,
                                onTaskToggle = onTaskToggle,
                                onTaskLongPress = onTaskLongPress
                            )
                            Spacer(modifier = Modifier.height(16.dp))
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
                            onTaskLongPress = onTaskLongPress
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
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
private fun BlockSection(
    block: WorkBlock,
    tasks: List<DayTask>,
    selectedDate: LocalDate,
    streak: Int,
    onTaskToggle: (DayTask) -> Unit,
    onTaskLongPress: (DayTask) -> Unit
) {
    Column {
        BlockHeader(block = block, selectedDate = selectedDate, streak = streak)
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

@Composable
private fun BlockHeader(block: WorkBlock, selectedDate: LocalDate, streak: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
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
            isExpired -> Text("⌛", fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
            isPostponed -> Text("⏰", fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
            else -> Checkbox(
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
            Text(
                "Sin bloque",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
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