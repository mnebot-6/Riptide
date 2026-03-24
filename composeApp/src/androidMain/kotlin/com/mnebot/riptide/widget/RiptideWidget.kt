package com.mnebot.riptide.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mnebot.riptide.MainActivity
import com.mnebot.riptide.R
import com.mnebot.riptide.data.local.db.DatabaseProvider
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val TAG = "RiptideWidget"

// Semi-transparent marine palette
private val WidgetBackground = Color(0xCC0A1628)
private val OceanMidTranslucent = Color(0x881B3A6B)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val Accent = Color(0xFF4FC3F7)
private val CompletedGreen = Color(0xFF81C784)
private val CheckCyan = Color(0xFF4DD0E1)
private val CrossMuted = Color(0x55FFFFFF)
private val RowBg = Color(0x22FFFFFF)
private val NoBlockStripe = Color(0x33FFFFFF)

class RiptideWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetTasks: List<WidgetTask>
        val completed: Int
        val total: Int
        val today: LocalDate

        try {
            today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date

            val db = DatabaseProvider.getDatabase(context)
            val tasks = db.dayTaskDao().getByDate(today.toString())
            val blocks = db.workBlockDao().getAll()
            val blockMap = blocks.associate { it.id to it }

            widgetTasks = tasks
                .filter { it.status != "POSTPONED" }
                .map { entity ->
                    val rawColor = entity.blockId?.let { blockMap[it]?.color }
                    val parsedColor = rawColor?.let {
                        try {
                            Color(android.graphics.Color.parseColor(it).toLong() or 0x100000000L)
                        } catch (_: Exception) { null }
                    } ?: NoBlockStripe

                    WidgetTask(
                        id = entity.id,
                        title = entity.title,
                        blockName = entity.blockId?.let { blockMap[it]?.name },
                        blockColor = parsedColor,
                        isCompleted = entity.status == "COMPLETED",
                        time = entity.time,
                        sourceTaskId = entity.sourceTaskId
                    )
                }
                .sortedWith(compareBy({ it.isCompleted }, { it.time ?: "99:99" }, { it.title }))

            completed = widgetTasks.count { it.isCompleted }
            total = widgetTasks.size
        } catch (e: Exception) {
            Log.e(TAG, "Error loading widget data", e)
            provideContent { ErrorContent() }
            return
        }

        provideContent {
            WidgetContent(
                tasks = widgetTasks,
                completed = completed,
                total = total,
                today = today,
                context = context
            )
        }
    }
}

data class WidgetTask(
    val id: String,
    val title: String,
    val blockName: String?,
    val blockColor: Color,
    val isCompleted: Boolean,
    val time: String?,
    val sourceTaskId: String?
)

@Composable
private fun ErrorContent() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Riptide",
            style = TextStyle(
                color = ColorProvider(TextPrimary),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun formatDayOfWeek(date: LocalDate): String {
    val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$dayName ${date.day}"
}

@Composable
private fun WidgetContent(
    tasks: List<WidgetTask>,
    completed: Int,
    total: Int,
    today: LocalDate,
    context: Context
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .cornerRadius(16.dp)
            .padding(14.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header: clickable to open app
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Riptide",
                        style = TextStyle(
                            color = ColorProvider(TextPrimary),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = formatDayOfWeek(today),
                        style = TextStyle(
                            color = ColorProvider(TextSecondary),
                            fontSize = 11.sp
                        )
                    )
                }
                if (total > 0) {
                    val allDone = completed == total
                    Text(
                        text = if (allDone) context.getString(R.string.widget_all_done) else "$completed / $total",
                        style = TextStyle(
                            color = ColorProvider(if (allDone) CompletedGreen else Accent),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Progress bar
            if (total > 0) {
                val progress = completed.toFloat() / total
                val barColor = if (completed == total) CompletedGreen else Accent
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .cornerRadius(3.dp)
                        .background(ColorProvider(OceanMidTranslucent))
                ) {
                    if (progress > 0f) {
                        Box(
                            modifier = GlanceModifier
                                .width((progress * 200).dp.coerceAtLeast(6.dp))
                                .height(6.dp)
                                .cornerRadius(3.dp)
                                .background(ColorProvider(barColor))
                        ) {}
                    }
                }
                Spacer(modifier = GlanceModifier.height(10.dp))
            }

            // Task list or empty state
            if (total == 0) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.widget_no_tasks),
                        style = TextStyle(
                            color = ColorProvider(TextSecondary),
                            fontSize = 14.sp
                        )
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                        WidgetTaskRow(task)
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetTaskRow(task: WidgetTask) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .cornerRadius(8.dp)
            .background(ColorProvider(RowBg))
            .clickable(
                actionRunCallback<ToggleTaskAction>(
                    actionParametersOf(ToggleTaskAction.TaskIdKey to task.id)
                )
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator: ✓ or ✗
        Text(
            text = if (task.isCompleted) "✓" else "✗",
            style = TextStyle(
                color = ColorProvider(if (task.isCompleted) CheckCyan else CrossMuted),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.width(6.dp))

        // Block color stripe
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(32.dp)
                .cornerRadius(2.dp)
                .background(ColorProvider(task.blockColor))
        ) {}

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Task title + block name
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = task.title,
                style = TextStyle(
                    color = ColorProvider(if (task.isCompleted) TextSecondary else TextPrimary),
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
            if (task.blockName != null) {
                Text(
                    text = task.blockName,
                    style = TextStyle(
                        color = ColorProvider(task.blockColor),
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
        }

        // Recurring indicator
        if (task.sourceTaskId != null) {
            Text(
                text = "\u21BB",
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
        }

        // Time (at the end)
        val timeDisplay = task.time?.take(5)
        if (timeDisplay != null) {
            Text(
                text = timeDisplay,
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 11.sp
                )
            )
        }
    }
}
