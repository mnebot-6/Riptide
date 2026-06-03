package com.mnebot.riptide.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mnebot.riptide.MainActivity
import com.mnebot.riptide.R
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate as JavaLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Clock

private const val TAG = "RiptideWidget"

// Marine palette, translucent but legible (~70% alpha) so the live wallpaper still peeks through.
private val WidgetBackground = Color(0xB3061826)         // ~70% alpha deep ocean
private val HeaderSeam = Color(0x33FFFFFF)
private val ProgressTrack = Color(0x33FFFFFF)
private val ProgressFill = Color(0xCC4FC3F7)
private val ProgressFillDone = Color(0xCC81C784)
private val RowBg = Color(0x33FFFFFF)
private val NoBlockStripe = Color(0x44FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val TextCompleted = Color(0x8AFFFFFF)
private val PriorityAmber = Color(0xFFFFD54F)

class RiptideWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = WidgetSnapshotStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            val dataStore = WidgetSnapshotStateDefinition.getDataStore(context, "")
            val current = dataStore.data.first()
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            if (current.dateIso != today || current.generatedAtIso.isBlank()) {
                val fresh = WidgetDataLoader.load(context)
                updateAppWidgetState(context, WidgetSnapshotStateDefinition, id) { fresh }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing widget state", e)
        }

        provideContent {
            val snapshot = currentState<WidgetSnapshot>()
            WidgetContent(snapshot)
        }
    }
}

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val items = snapshot.items
    val total = items.size
    val completed = items.count { it.isCompleted }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(ColorProvider(WidgetBackground))
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                DateHeader(
                    dateIso = snapshot.dateIso,
                    completed = completed,
                    total = total
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                ProgressHeader(completed = completed, total = total)
            }
            // Seam: thin divider, only as wide as the task list, under which tasks appear to slide.
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorProvider(HeaderSeam))
                ) {}
            }
            if (total == 0) {
                EmptyState(text = context.getString(R.string.widget_no_tasks))
            } else {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    // Top spacer lives inside the scroll, so tasks slide under the seam.
                    item { Spacer(modifier = GlanceModifier.height(8.dp)) }
                    items(items, itemId = { it.id.hashCode().toLong() }) { item ->
                        TaskRow(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(dateIso: String, completed: Int, total: Int) {
    val locale = Locale.getDefault()
    val dateText = formatDateHeader(dateIso, locale)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateText,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(TextPrimary),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
        if (total > 0) {
            Text(
                text = "$completed/$total",
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

private fun formatDateHeader(dateIso: String, locale: Locale): String {
    val date = try {
        if (dateIso.isBlank()) JavaLocalDate.now() else JavaLocalDate.parse(dateIso)
    } catch (_: Exception) {
        JavaLocalDate.now()
    }
    val day = date.format(DateTimeFormatter.ofPattern("EEEE", locale))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    val month = date.format(DateTimeFormatter.ofPattern("MMMM", locale))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    return "$day ${date.dayOfMonth} - $month"
}

@Composable
private fun ProgressHeader(completed: Int, total: Int) {
    if (total == 0) {
        Box(modifier = GlanceModifier.fillMaxWidth().height(4.dp)) {}
        return
    }
    val progress = completed.toFloat() / total
    val done = completed == total
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(4.dp)
            .cornerRadius(2.dp)
    ) {
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
            color = ColorProvider(if (done) ProgressFillDone else ProgressFill),
            backgroundColor = ColorProvider(ProgressTrack)
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(TextSecondary),
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun TaskRow(item: WidgetTaskItem) {
    val stripeColor = parseHexColor(item.blockColorHex) ?: NoBlockStripe
    val textColor = if (item.isCompleted) TextCompleted else TextPrimary

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(8.dp)
                .background(ColorProvider(RowBg))
                .clickable(
                    actionRunCallback<TaskClickAction>(
                        actionParametersOf(TaskClickAction.TaskIdKey to item.id)
                    )
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical block-color stripe
            Box(
                modifier = GlanceModifier
                    .width(3.dp)
                    .height(22.dp)
                    .cornerRadius(2.dp)
                    .background(ColorProvider(stripeColor))
            ) {}

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Title (with optional priority star prefix) and optional count line
            Column(modifier = GlanceModifier.defaultWeight()) {
                val titleText = buildString {
                    if (item.isPriority) {
                        append("★ ")
                    }
                    append(item.title)
                }
                Text(
                    text = titleText,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(if (item.isPriority && !item.isCompleted) PriorityAmber else textColor),
                        fontSize = 13.sp,
                        fontWeight = if (item.isPriority) FontWeight.Medium else FontWeight.Normal,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
                if (item.isCountable && item.targetCount != null) {
                    Text(
                        text = "${item.currentCount}/${item.targetCount}",
                        style = TextStyle(
                            color = ColorProvider(TextSecondary),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex).toLong() or 0x100000000L)
    } catch (_: Exception) {
        null
    }
}
