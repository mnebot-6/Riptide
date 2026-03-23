package com.mnebot.riptide.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskSchedule
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.presentation.main.parseColor
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep      = Color(0xFF0A1628)
private val OceanMid       = Color(0xFF1B3A6B)
private val TextPrimary    = Color(0xFFFFFFFF)
private val TextSecondary  = Color(0xB3FFFFFF)
private val CardBackground = Color(0x22FFFFFF)

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onRangeSelected: (HistoryRange) -> Unit,
    onNavigateBack: () -> Unit
) {
    val blocksById = uiState.blocks.associateBy { it.id }

    val allDates = (uiState.tasksByDate.keys + uiState.summaryByDate.keys)
        .distinct()
        .sortedDescending()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(OceanDeep, OceanMid)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.btn_back),
                    color = TextSecondary,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { onNavigateBack() }
                        .padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
                )
                Text(
                    text = stringResource(Res.string.title_history),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Range toggle
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
                    .padding(4.dp)
            ) {
                HistoryRange.entries.forEach { range ->
                    val selected = uiState.range == range
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) Color(0xFF1A73E8) else Color.Transparent)
                            .clickable { onRangeSelected(range) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.history_range_days, range.days),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.msg_loading),
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                allDates.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBackground)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.history_no_data, uiState.range.days),
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allDates, key = { it.toString() }) { date ->
                            DayHistoryCard(
                                date = date,
                                summary = uiState.summaryByDate[date],
                                tasks = uiState.tasksByDate[date] ?: emptyList(),
                                blocksById = blocksById
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun localizedMonths(): Array<String> = arrayOf(
    stringResource(Res.string.month_jan),
    stringResource(Res.string.month_feb),
    stringResource(Res.string.month_mar),
    stringResource(Res.string.month_apr),
    stringResource(Res.string.month_may),
    stringResource(Res.string.month_jun),
    stringResource(Res.string.month_jul),
    stringResource(Res.string.month_aug),
    stringResource(Res.string.month_sep),
    stringResource(Res.string.month_oct),
    stringResource(Res.string.month_nov),
    stringResource(Res.string.month_dec)
)

@Composable
private fun DayHistoryCard(
    date: LocalDate,
    summary: DaySummary?,
    tasks: List<DayTask>,
    blocksById: Map<String, WorkBlock>
) {
    val months = localizedMonths()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
    ) {
        // Date header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date.formatDisplay(months),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (summary != null && summary.tasksTotal > 0) {
                val pct = summary.tasksCompleted.toFloat() / summary.tasksTotal
                val badgeColor = summaryColor(pct)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeColor.copy(alpha = 0.22f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${summary.tasksCompleted}/${summary.tasksTotal}",
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (tasks.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x1AFFFFFF))
            )
            tasks.forEach { task ->
                HistoryTaskRow(
                    task = task,
                    block = task.blockId?.let { blocksById[it] }
                )
            }
        }
    }
}

@Composable
private fun HistoryTaskRow(task: DayTask, block: WorkBlock?) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isCompleted) "✓" else "✗",
            color = if (isCompleted) Color(0xFF4DD0E1) else Color(0x55FFFFFF),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        if (block != null) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(parseColor(block.color))
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = task.title,
            color = if (isCompleted) TextPrimary else TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        val time = when (val s = task.schedule) {
            is TaskSchedule.OneTime -> s.time
            is TaskSchedule.Recurring -> s.time
        }
        if (time != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

private fun LocalDate.formatDisplay(months: Array<String>): String =
    "$dayOfMonth ${months[monthNumber - 1]} $year"

private fun summaryColor(pct: Float): Color = when {
    pct <= 0f  -> Color(0x66FFFFFF)
    pct < 0.5f -> Color(0xFFE57373)
    pct < 0.8f -> Color(0xFFFFB74D)
    pct < 1.0f -> Color(0xFF4DD0E1)
    else       -> Color(0xFF1A73E8)
}
