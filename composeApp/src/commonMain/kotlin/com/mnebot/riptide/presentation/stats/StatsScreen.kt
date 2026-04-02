package com.mnebot.riptide.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.presentation.main.currentDate
import com.mnebot.riptide.presentation.main.parseColor
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep  = Color(0xFF0A1628)
private val OceanMid   = Color(0xFF1B3A6B)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val CardBackground = Color(0x22FFFFFF)

private fun completionColor(pct: Float): Color = when {
    pct <= 0f   -> Color(0x44FFFFFF)
    pct < 0.5f  -> Color(0xFFE57373)
    pct < 0.8f  -> Color(0xFFFFB74D)
    pct < 1.0f  -> Color(0xFF4DD0E1)
    else        -> Color(0xFF1A73E8)
}

@Composable
fun StatsScreen(
    uiState: StatsUiState,
    onRangeSelected: (StatsRange) -> Unit,
    onNavigateBack: () -> Unit
) {
    val today = currentDate()
    val days = when (uiState.range) {
        StatsRange.WEEK -> 7
        StatsRange.MONTH -> 30
        StatsRange.ALL_TIME -> 0 // not used for ALL_TIME
    }
    val dates = if (uiState.range != StatsRange.ALL_TIME) {
        (0 until days).map { today.minus(days - 1 - it, DateTimeUnit.DAY) }
    } else emptyList()
    val summaryByDate = uiState.summaries.associateBy { it.date }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(OceanDeep, OceanMid)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 40.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.btn_back),
                    color = TextSecondary,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { onNavigateBack() }
                        .padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
                )
                Text(
                    text = stringResource(Res.string.title_stats),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))

            // Range toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBackground)
                    .padding(4.dp)
            ) {
                StatsRange.entries.forEach { range ->
                    val selected = uiState.range == range
                    val label = when (range) {
                        StatsRange.WEEK -> stringResource(Res.string.stats_range_week)
                        StatsRange.MONTH -> stringResource(Res.string.stats_range_month)
                        StatsRange.ALL_TIME -> stringResource(Res.string.stats_range_all_time)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) Color(0xFF1A73E8) else Color.Transparent)
                            .clickable { onRangeSelected(range) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Chart + Summary section
            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.msg_loading), color = TextSecondary, fontSize = 14.sp)
                }
            } else if (uiState.range == StatsRange.ALL_TIME) {
                // ── ALL TIME: Monthly trend chart ──
                if (uiState.monthlyTrend.isNotEmpty()) {
                    MonthlyTrendChart(
                        trend = uiState.monthlyTrend,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // KPI Grid 2x2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = stringResource(Res.string.stats_longest_streak),
                        value = "${uiState.longestStreakEver}d",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(Res.string.stats_avg_daily),
                        value = "${(uiState.avgDailyCompletion * 100).toInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = stringResource(Res.string.stats_active_days),
                        value = uiState.totalDaysActive.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(Res.string.stats_total_completed),
                        value = uiState.totalCompleted.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Month-over-month comparison
                Spacer(Modifier.height(16.dp))
                MonthComparisonRow(
                    currentRate = uiState.currentMonthRate,
                    previousRate = uiState.previousMonthRate
                )

                // Best week
                if (uiState.bestWeekLabel.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBackground)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(Res.string.stats_best_week),
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${uiState.bestWeekLabel}  •  ${(uiState.bestWeekAvg * 100).toInt()}%",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            } else {
                // ── WEEK / MONTH: Daily bar chart ──
                CompletionBarChart(
                    dates = dates,
                    summaryByDate = summaryByDate,
                    range = uiState.range,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Leyenda de colores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChartLegendChip(color = Color(0xFFE57373), label = stringResource(Res.string.stats_legend_low))
                    ChartLegendChip(color = Color(0xFFFFB74D), label = stringResource(Res.string.stats_legend_medium))
                    ChartLegendChip(color = Color(0xFF4DD0E1), label = stringResource(Res.string.stats_legend_high))
                    ChartLegendChip(color = Color(0xFF1A73E8), label = stringResource(Res.string.stats_legend_perfect))
                }

                Spacer(Modifier.height(20.dp))

                // Summary numbers
                val activeDays = uiState.summaries.count { it.tasksCompleted > 0 }
                val totalCompleted = uiState.summaries.sumOf { it.tasksCompleted }
                val bestDay = uiState.summaries.maxByOrNull { it.tasksCompleted }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = stringResource(Res.string.stats_active_days),
                        value = activeDays.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(Res.string.stats_total_completed),
                        value = totalCompleted.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    if (bestDay != null && bestDay.tasksTotal > 0) {
                        StatCard(
                            label = stringResource(Res.string.stats_best_day),
                            value = "${bestDay.tasksCompleted}/${bestDay.tasksTotal}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Streaks per block
            if (uiState.streaksByBlock.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.stats_streaks_title),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))

                uiState.streaksByBlock.forEach { (block, streak) ->
                    StreakRow(block = block, streak = streak.currentStreak)
                    Spacer(Modifier.height(8.dp))
                }
            } else if (!uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.stats_no_streaks),
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionBarChart(
    dates: List<LocalDate>,
    summaryByDate: Map<LocalDate, DaySummary>,
    range: StatsRange,
    modifier: Modifier = Modifier
) {
    val today = currentDate()
    val dowLabels = listOf(
        stringResource(Res.string.day_mon),
        stringResource(Res.string.day_tue),
        stringResource(Res.string.day_wed),
        stringResource(Res.string.day_thu),
        stringResource(Res.string.day_fri),
        stringResource(Res.string.day_sat),
        stringResource(Res.string.day_sun)
    )

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Eje Y (S1): etiquetas 100% / 50% / 0%
            Column(
                modifier = Modifier.width(30.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("100%", color = Color(0x55FFFFFF), fontSize = 8.sp)
                Text("50%",  color = Color(0x55FFFFFF), fontSize = 8.sp)
                Text("0%",   color = Color(0x55FFFFFF), fontSize = 8.sp)
            }

            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val barCount = dates.size
                val totalWidth = size.width
                val chartHeight = size.height
                val barWidth = (totalWidth / barCount) * 0.55f
                val barSpacing = totalWidth / barCount

                // Líneas de referencia horizontales (S1)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = Offset(0f, 0f),
                    end = Offset(totalWidth, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = Color(0x22FFFFFF),
                    start = Offset(0f, chartHeight * 0.5f),
                    end = Offset(totalWidth, chartHeight * 0.5f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                dates.forEachIndexed { i, date ->
                    val summary = summaryByDate[date]
                    val pct = if (summary != null && summary.tasksTotal > 0)
                        summary.tasksCompleted.toFloat() / summary.tasksTotal
                    else 0f

                    val barColor = completionColor(pct)
                    val barHeight = if (pct > 0f) (chartHeight * pct).coerceAtLeast(6.dp.toPx())
                                   else 4.dp.toPx()

                    val left = i * barSpacing + (barSpacing - barWidth) / 2f
                    val top = chartHeight - barHeight

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }

        // Day labels row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(top = 4.dp, start = 30.dp)
        ) {
            dates.forEach { date ->
                val label = when (range) {
                    StatsRange.WEEK  -> dowLabels[date.dayOfWeek.ordinal]
                    StatsRange.MONTH -> if (date.day % 5 == 0) date.day.toString() else ""
                    StatsRange.ALL_TIME -> ""
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (label.isNotEmpty()) {
                        Text(
                            text = label,
                            color = if (date == today) TextPrimary else TextSecondary,
                            fontSize = if (range == StatsRange.WEEK) 10.sp else 8.sp,
                            fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StreakRow(block: WorkBlock, streak: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(parseColor(block.color))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${block.icon} ${block.name}",
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Res.drawable.ic_flame),
                contentDescription = stringResource(Res.string.a11y_streak_count),
                tint = Color(0xFFFFB347),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$streak",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MonthlyTrendChart(
    trend: List<MonthStat>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Y-axis labels
            Column(
                modifier = Modifier.fillMaxHeight().width(28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("100%", color = Color(0x55FFFFFF), fontSize = 8.sp)
                Text("50%",  color = Color(0x55FFFFFF), fontSize = 8.sp)
                Text("0%",   color = Color(0x55FFFFFF), fontSize = 8.sp)
            }

            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val barCount = trend.size
                if (barCount == 0) return@Canvas
                val totalWidth = size.width
                val chartHeight = size.height
                val barWidth = (totalWidth / barCount) * 0.6f
                val barSpacing = totalWidth / barCount

                // Reference lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                drawLine(Color(0x33FFFFFF), Offset(0f, 0f), Offset(totalWidth, 0f), 1.dp.toPx(), pathEffect = dashEffect)
                drawLine(Color(0x22FFFFFF), Offset(0f, chartHeight * 0.5f), Offset(totalWidth, chartHeight * 0.5f), 1.dp.toPx(), pathEffect = dashEffect)

                trend.forEachIndexed { i, stat ->
                    val barColor = completionColor(stat.completionRate)
                    val barHeight = if (stat.completionRate > 0f) (chartHeight * stat.completionRate).coerceAtLeast(6.dp.toPx())
                                    else 4.dp.toPx()
                    val left = i * barSpacing + (barSpacing - barWidth) / 2f
                    val top = chartHeight - barHeight

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }

        // Month labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(start = 28.dp)
        ) {
            trend.forEach { stat ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = stat.label,
                        color = TextSecondary,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthComparisonRow(
    currentRate: Float,
    previousRate: Float
) {
    val diff = currentRate - previousRate
    val arrow = if (diff >= 0) "\u2191" else "\u2193"   // ↑ or ↓
    val arrowColor = if (diff >= 0) Color(0xFF4DD0E1) else Color(0xFFE57373)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.stats_this_month),
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${(currentRate * 100).toInt()}%",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(Res.string.stats_vs_last_month),
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$arrow ${kotlin.math.abs((diff * 100).toInt())}%",
                color = arrowColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
