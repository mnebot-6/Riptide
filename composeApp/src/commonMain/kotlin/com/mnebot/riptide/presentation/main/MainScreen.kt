package com.mnebot.riptide.presentation.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.WorkBlock
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanLight = Color(0xFF2E5F9E)
private val CardBackground = Color(0x33FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToCreateBlock: () -> Unit,
    onNavigateToEditBlock: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAquarium by remember { mutableStateOf(false) }
    var showDrawer by remember { mutableStateOf(false) }
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
        OceanBackground()

        when {
            showAquarium -> {
                AquariumFullScreen(onClose = { showAquarium = false })
            }
            else -> {
                MainContent(
                    blocks = uiState.blocks,
                    tasksByBlock = uiState.tasksByBlock,
                    selectedDate = uiState.selectedDate,
                    today = currentDate(),
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onDateSelected = { viewModel.selectDate(it) },
                    onTaskToggle = { viewModel.toggleTaskCompleted(it) },
                    onAquariumClick = { showAquarium = true }
                )
            }
        }

        if (showDrawer || drawerOffsetY.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x00000000).copy(alpha = 0.6f * drawerOffsetY.value))
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
                    onAddTask = { },
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
                    }
                )
            }
        }
    }
}

@Composable
private fun OceanBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(OceanDeep, OceanMid, OceanLight)
                )
            )
    )
}

@Composable
private fun AquariumFullScreen(onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("🐟 🪸 🦞 🐚", fontSize = 48.sp, color = TextPrimary)
        FloatingActionButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = CardBackground,
            contentColor = TextPrimary,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Text("✕", fontSize = 20.sp)
        }
    }
}

@Composable
private fun MainContent(
    blocks: List<WorkBlock>,
    tasksByBlock: Map<String, List<DayTask>>,
    selectedDate: LocalDate,
    today: LocalDate,
    isLoading: Boolean,
    error: String?,
    onDateSelected: (LocalDate) -> Unit,
    onTaskToggle: (DayTask) -> Unit,
    onAquariumClick: () -> Unit
) {
    val blocksWithTasks = blocks.filter { block ->
        tasksByBlock[block.id]?.isNotEmpty() == true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 40.dp)
        ) {
            WeekCalendar(
                selectedDate = selectedDate,
                today = today,
                onDateSelected = onDateSelected,
                onWeekChange = onDateSelected
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x33FFFFFF))
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TextPrimary)
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Error: $error", color = TextPrimary)
                    }
                }
                blocksWithTasks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay tareas para hoy 🌊", color = TextSecondary, fontSize = 16.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                        items(blocksWithTasks) { block ->
                            BlockSection(
                                block = block,
                                tasks = tasksByBlock[block.id] ?: emptyList(),
                                onTaskToggle = onTaskToggle
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAquariumClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = CardBackground,
            contentColor = TextPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Text("🐟", fontSize = 20.sp)
        }
    }
}

@Composable
private fun BlockSection(
    block: WorkBlock,
    tasks: List<DayTask>,
    onTaskToggle: (DayTask) -> Unit
) {
    Column {
        BlockHeader(block = block)
        Spacer(modifier = Modifier.height(8.dp))
        tasks.forEach { task ->
            TaskCard(
                task = task,
                blockColor = parseColor(block.color),
                onToggle = { onTaskToggle(task) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun BlockHeader(block: WorkBlock) {
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
        ) {
            Text(block.icon, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = block.name,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            val timeLabel = buildTimeLabel(block)
            if (timeLabel != null) {
                Text(text = timeLabel, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: DayTask,
    blockColor: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(blockColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = task.title,
            color = if (task.isCompleted) TextSecondary else TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = blockColor,
                uncheckedColor = TextSecondary,
                checkmarkColor = TextPrimary
            )
        )
    }
}

private fun buildTimeLabel(block: WorkBlock): String? {
    val recurrence = block.recurrence
    if (recurrence !is com.mnebot.riptide.domain.model.Recurrence.Weekly) return null
    val slot = recurrence.slots.firstOrNull() ?: return null
    if (slot.startTime == null || slot.endTime == null) return null
    return "${slot.startTime} - ${slot.endTime}"
}

expect fun parseColor(hex: String): Color