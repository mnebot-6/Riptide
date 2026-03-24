package com.mnebot.riptide.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class RiptideWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        val db = DatabaseProvider.getDatabase(context)
        val tasks = db.dayTaskDao().getByDate(today.toString())
        val blocks = db.workBlockDao().getAll()
        val blockMap = blocks.associate { it.id to it }

        val widgetTasks = tasks
            .filter { it.status != "POSTPONED" }
            .map { entity ->
                WidgetTask(
                    id = entity.id,
                    title = entity.title,
                    blockName = entity.blockId?.let { blockMap[it]?.name },
                    blockColor = entity.blockId?.let { blockMap[it]?.color },
                    isCompleted = entity.status == "COMPLETED",
                    time = entity.time
                )
            }
            .sortedWith(compareBy({ it.isCompleted }, { it.time ?: "99:99" }, { it.title }))

        val completed = widgetTasks.count { it.isCompleted }
        val total = widgetTasks.size

        provideContent {
            WidgetContent(
                tasks = widgetTasks,
                completed = completed,
                total = total,
                context = context
            )
        }
    }
}

data class WidgetTask(
    val id: String,
    val title: String,
    val blockName: String?,
    val blockColor: String?,
    val isCompleted: Boolean,
    val time: String?
)

@Composable
private fun WidgetContent(
    tasks: List<WidgetTask>,
    completed: Int,
    total: Int,
    context: Context
) {
    val oceanDeep = ColorProvider(android.graphics.Color.parseColor("#FF0A1628"))
    val oceanMid = ColorProvider(android.graphics.Color.parseColor("#FF1B3A6B"))
    val textPrimary = ColorProvider(android.graphics.Color.parseColor("#FFFFFFFF"))
    val textSecondary = ColorProvider(android.graphics.Color.parseColor("#B3FFFFFF"))
    val accent = ColorProvider(android.graphics.Color.parseColor("#FF4FC3F7"))
    val completedColor = ColorProvider(android.graphics.Color.parseColor("#FF81C784"))
    val cardBg = ColorProvider(android.graphics.Color.parseColor("#33FFFFFF"))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(oceanDeep)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header: title + progress
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Riptide",
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                if (total > 0) {
                    val allDone = completed == total
                    Text(
                        text = if (allDone) context.getString(R.string.widget_all_done) else "$completed / $total",
                        style = TextStyle(
                            color = if (allDone) completedColor else accent,
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
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .cornerRadius(3.dp)
                        .background(oceanMid)
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .cornerRadius(3.dp)
                            .background(if (completed == total) completedColor else accent)
                    ) {}
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
                            color = textSecondary,
                            fontSize = 14.sp
                        )
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                        WidgetTaskRow(task, textPrimary, textSecondary, cardBg)
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetTaskRow(
    task: WidgetTask,
    textPrimary: ColorProvider,
    textSecondary: ColorProvider,
    cardBg: ColorProvider
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        val indicatorColor = if (task.isCompleted) {
            ColorProvider(android.graphics.Color.parseColor("#FF81C784"))
        } else {
            ColorProvider(android.graphics.Color.parseColor("#66FFFFFF"))
        }

        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .cornerRadius(4.dp)
                .background(indicatorColor)
        ) {}

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Time (if any)
        if (task.time != null) {
            Text(
                text = task.time.substring(0, 5),
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
        }

        // Task title
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = task.title,
                style = TextStyle(
                    color = if (task.isCompleted) textSecondary else textPrimary,
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
            if (task.blockName != null) {
                Text(
                    text = task.blockName,
                    style = TextStyle(
                        color = if (task.blockColor != null) {
                            try {
                                ColorProvider(android.graphics.Color.parseColor(task.blockColor))
                            } catch (_: Exception) {
                                textSecondary
                            }
                        } else textSecondary,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
