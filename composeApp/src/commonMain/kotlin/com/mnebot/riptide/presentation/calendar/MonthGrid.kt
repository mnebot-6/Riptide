package com.mnebot.riptide.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.datetime.*

private val TodayBg = Color(0xFF1A73E8)
private val SelectedBg = Color(0x44FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val TextMuted = Color(0x55FFFFFF)
private val DotTask = Color(0xFF4DD0E1)
private val DotPersonal = Color(0xFF64B5F6)
private val DotHoliday = Color(0xFFE57373)

@Composable
fun MonthGrid(
    year: Int,
    month: Int,
    today: LocalDate,
    selectedDate: LocalDate?,
    taskDates: Set<LocalDate>,
    personalDates: Set<LocalDate>,
    holidays: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstOfMonth = LocalDate(year, month, 1)
    val daysInMonth = when (month) {
        2 -> if (isLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
    // ISO day of week: 1=Monday, 7=Sunday
    val startDayOfWeek = firstOfMonth.dayOfWeek.isoDayNumber

    // We need startDayOfWeek-1 empty cells before day 1
    val cells = mutableListOf<Int?>()
    repeat(startDayOfWeek - 1) { cells.add(null) }
    for (d in 1..daysInMonth) { cells.add(d) }
    // Pad to fill complete weeks
    while (cells.size % 7 != 0) { cells.add(null) }

    val weeks = cells.chunked(7)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .then(
                                if (day != null) Modifier.clickable {
                                    onDateSelected(LocalDate(year, month, day))
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val date = LocalDate(year, month, day)
                            val isToday = date == today
                            val isSelected = date == selectedDate

                            // Background circle
                            if (isToday || isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isToday) TodayBg else SelectedBg)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day.toString(),
                                    color = when {
                                        isToday -> TextPrimary
                                        date > today -> TextMuted
                                        else -> TextSecondary
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )

                                // Indicator dots
                                val hasTasks = date in taskDates
                                val hasPersonal = date in personalDates
                                val hasHoliday = date in holidays
                                if (hasTasks || hasPersonal || hasHoliday) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(top = 1.dp)
                                    ) {
                                        if (hasTasks) Dot(DotTask)
                                        if (hasPersonal) Dot(DotPersonal)
                                        if (hasHoliday) Dot(DotHoliday)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
