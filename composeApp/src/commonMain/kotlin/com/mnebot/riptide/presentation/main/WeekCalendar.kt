// WeekCalendar.kt
package com.mnebot.riptide.presentation.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskStatus

private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val CardBackground = Color(0x33FFFFFF)
private val SelectedDay = Color(0x55FFFFFF)
private val TodayIndicator = Color(0xFF7EC8E3)
private val BarBackground = Color(0x33FFFFFF)
private val BarCompleted = Color(0xFF7EC8E3)
private val BarPending = Color(0x55FFFFFF)

@Composable
fun WeekCalendar(
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<DayTask>>,
    onDateSelected: (LocalDate) -> Unit,
    onWeekChange: (LocalDate) -> Unit
) {
    val weekStart = remember(selectedDate) { getWeekStart(selectedDate) }
    val days = remember(weekStart) { (0..6).map { weekStart.plus(it, DateTimeUnit.DAY) } }

    var isSwiping by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isSwiping) 0.4f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "calendarAlpha"
    )
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .alpha(alpha)
            .pointerInput(weekStart) {
                detectHorizontalDragGestures(
                    onDragEnd = { isSwiping = false },
                    onDragCancel = { isSwiping = false }
                ) { _, dragAmount ->
                    if (dragAmount < -40 && !isSwiping) {
                        isSwiping = true
                        scope.launch {
                            delay(150)
                            onWeekChange(weekStart.plus(7, DateTimeUnit.DAY))
                            isSwiping = false
                        }
                    } else if (dragAmount > 40 && !isSwiping) {
                        isSwiping = true
                        scope.launch {
                            delay(150)
                            onWeekChange(weekStart.minus(7, DateTimeUnit.DAY))
                            isSwiping = false
                        }
                    }
                }
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { date ->
            val tasksForDay = tasksByDate[date] ?: emptyList()
            val total = tasksForDay.size
            val completed = tasksForDay.count { it.status == TaskStatus.COMPLETED }

            DayCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                taskTotal = total,
                taskCompleted = completed,
                onSelected = { onDateSelected(date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    taskTotal: Int,
    taskCompleted: Int,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) SelectedDay else Color.Transparent)
            .clickable { onSelected() }
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(
            text = dayInitial(date.dayOfWeek),
            color = if (isToday) TodayIndicator else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = date.dayOfMonth.toString(),
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(6.dp))
        DayProgressBar(
            total = taskTotal,
            completed = taskCompleted,
            isToday = isToday
        )
    }
}

@Composable
private fun DayProgressBar(
    total: Int,
    completed: Int,
    isToday: Boolean
) {
    val barWidth = 28.dp
    val barHeight = 3.dp

    Box(
        modifier = Modifier
            .width(barWidth)
            .height(barHeight)
            .clip(RoundedCornerShape(2.dp))
            .background(if (total > 0) BarPending else Color.Transparent)
    ) {
        if (total > 0) {
            val fraction = completed.toFloat() / total.toFloat()
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(durationMillis = 300),
                label = "progressBar_${completed}_${total}"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isToday) TodayIndicator else BarCompleted)
            )
        }
    }
}

private fun dayInitial(dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "L"
        DayOfWeek.TUESDAY -> "M"
        DayOfWeek.WEDNESDAY -> "X"
        DayOfWeek.THURSDAY -> "J"
        DayOfWeek.FRIDAY -> "V"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "D"
        else -> ""
    }
}

private fun getWeekStart(date: LocalDate): LocalDate {
    val daysFromMonday = date.dayOfWeek.ordinal
    return date.minus(daysFromMonday, DateTimeUnit.DAY)
}