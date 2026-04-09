package com.mnebot.riptide.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.PersonalDate
import com.mnebot.riptide.domain.model.TaskStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val CardBackground = Color(0x22FFFFFF)
private val AccentBlue = Color(0xFF1A73E8)
private val DotTask = Color(0xFF4DD0E1)
private val DotPersonal = Color(0xFF64B5F6)
private val DotHoliday = Color(0xFFE57373)

data class CalendarUiState(
    val tasksByDate: Map<LocalDate, List<DayTask>> = emptyMap(),
    val personalDates: List<PersonalDate> = emptyList(),
    val holidays: Map<LocalDate, String> = emptyMap()
)

@Composable
fun CalendarTabContent(
    uiState: CalendarUiState,
    onMonthChanged: (year: Int, month: Int) -> Unit,
    onAddPersonalDate: () -> Unit,
    onDeletePersonalDate: (String) -> Unit
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var displayYear by remember { mutableIntStateOf(today.year) }
    var displayMonth by remember { mutableIntStateOf(today.monthNumber) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(displayYear, displayMonth) {
        onMonthChanged(displayYear, displayMonth)
    }

    val months = localizedMonths()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(OceanDeep, OceanMid)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Month header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    if (displayMonth == 1) {
                        displayMonth = 12; displayYear--
                    } else displayMonth--
                }) {
                    Text("\u25C0", color = TextSecondary, fontSize = 16.sp)
                }

                Text(
                    text = "${months[displayMonth - 1]} $displayYear",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    if (displayMonth == 12) {
                        displayMonth = 1; displayYear++
                    } else displayMonth++
                }) {
                    Text("\u25B6", color = TextSecondary, fontSize = 16.sp)
                }
            }

            // Today button + Add date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .clickable {
                            displayYear = today.year
                            displayMonth = today.monthNumber
                            selectedDate = today
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.btn_today),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .clickable { onAddPersonalDate() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_plus),
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.btn_add_date),
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Weekday headers
            val dayHeaders = listOf("L", "M", "X", "J", "V", "S", "D")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                dayHeaders.forEach { d ->
                    Text(
                        text = d,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Month grid
            val personalDatesByDate = uiState.personalDates.associateBy { it.date }
            MonthGrid(
                year = displayYear,
                month = displayMonth,
                today = today,
                selectedDate = selectedDate,
                taskDates = uiState.tasksByDate.keys,
                personalDates = personalDatesByDate.keys,
                holidays = uiState.holidays.keys,
                onDateSelected = { selectedDate = it }
            )

            Spacer(Modifier.height(12.dp))

            // Selected date details
            if (selectedDate != null) {
                val date = selectedDate!!
                val tasks = uiState.tasksByDate[date] ?: emptyList()
                val personal = personalDatesByDate[date]
                val holiday = uiState.holidays[date]

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${date.day} ${months[date.month.ordinal]}",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (holiday != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Dot(DotHoliday)
                            Spacer(Modifier.width(6.dp))
                            Text(holiday, color = DotHoliday, fontSize = 13.sp)
                        }
                    }

                    if (personal != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Dot(DotPersonal)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                personal.title,
                                color = DotPersonal,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(Res.drawable.ic_trash),
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onDeletePersonalDate(personal.id) }
                            )
                        }
                    }

                    if (tasks.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
                        Text(
                            "$completed/${tasks.size}",
                            color = DotTask,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        tasks.forEach { task ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (task.status == TaskStatus.COMPLETED) "\u2713" else "\u2717",
                                    color = if (task.status == TaskStatus.COMPLETED) DotTask else TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(task.title, color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }

                    if (holiday == null && personal == null && tasks.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "-",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
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
