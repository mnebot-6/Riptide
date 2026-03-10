package com.mnebot.riptide.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.WorkBlock

// Colores del tema marino
private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanLight = Color(0xFF2E5F9E)
private val CardBackground = Color(0x33FFFFFF)
private val CardBorder = Color(0x55FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAquarium by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        OceanBackground()

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextPrimary)
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.error}", color = TextPrimary)
                }
            }
            showAquarium -> {
                AquariumFullScreen(onClose = { showAquarium = false })
            }
            else -> {
                MainContent(
                    blocks = uiState.blocks,
                    tasksByBlock = uiState.tasksByBlock,
                    onTaskToggle = { viewModel.toggleTaskCompleted(it) },
                    onAquariumClick = { showAquarium = true }
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
    onTaskToggle: (DayTask) -> Unit,
    onAquariumClick: () -> Unit
) {
    val blocksWithTasks = blocks.filter { block ->
        tasksByBlock[block.id]?.isNotEmpty() == true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (blocksWithTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay tareas para hoy 🌊", color = TextSecondary, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 48.dp, bottom = 96.dp)
            ) {
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
            TaskCard(task = task, blockColor = parseColor(block.color), onToggle = { onTaskToggle(task) })
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
                Text(
                    text = timeLabel,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
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