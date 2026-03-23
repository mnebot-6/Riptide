// presentation/task/TaskFormSheet.kt
package com.mnebot.riptide.presentation.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.presentation.components.DateInputField
import com.mnebot.riptide.presentation.components.TimeInputField
import com.mnebot.riptide.presentation.localizedDays
import com.mnebot.riptide.presentation.main.currentDate
import com.mnebot.riptide.presentation.main.parseColor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val CardBackground = Color(0x33FFFFFF)
private val CardBorder = Color(0x55FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val SectionLabel = Color(0x80FFFFFF)

@Composable
fun TaskFormSheet(
    blocks: List<WorkBlock>,
    initialDate: LocalDate = currentDate(),
    initialBlockId: String? = null,
    existingTask: DayTask? = null,
    existingDef: RecurringTaskDef? = null,
    forceRecurring: Boolean = false,
    onSaveOneTime: (title: String, blockId: String?, date: LocalDate, time: LocalTime?, notificationsEnabled: Boolean) -> Unit,
    onSaveRecurring: (title: String, blockId: String, time: LocalTime?, recurrence: Recurrence, notificationsEnabled: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var selectedBlockId by remember { mutableStateOf<String?>(initialBlockId ?: existingTask?.blockId) }
    var isRecurring by remember { mutableStateOf(forceRecurring) }

    val initialSchedule = existingTask?.schedule as? TaskSchedule.OneTime
    var selectedDate by remember { mutableStateOf<LocalDate?>(initialSchedule?.date ?: initialDate) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(initialSchedule?.time) }

    var recurringTime by remember(existingDef) { mutableStateOf<LocalTime?>(existingDef?.time) }
    var notificationsEnabled by remember(existingTask, existingDef) {
        mutableStateOf(existingTask?.notificationsEnabled ?: existingDef?.notificationsEnabled ?: false)
    }

    // Reset notification toggle when time is cleared
    LaunchedEffect(selectedTime) { if (selectedTime == null) notificationsEnabled = false }
    LaunchedEffect(recurringTime) { if (recurringTime == null) notificationsEnabled = false }

    val selectedDays = remember(existingDef) {
        mutableStateMapOf<Int, Unit>().also { map ->
            val slots = (existingDef?.recurrence as? Recurrence.Weekly)?.slots
            slots?.forEach { map[it.dayOfWeek] = Unit }
        }
    }
    val days = localizedDays()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(OceanDeep, OceanMid)),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x55FFFFFF))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(if (existingTask == null) Res.string.title_new_task else Res.string.title_edit_task),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            SheetSectionLabel(stringResource(Res.string.label_name))
            Spacer(modifier = Modifier.height(8.dp))
            SheetTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = stringResource(Res.string.placeholder_task_title)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(Res.string.label_recurring_toggle), color = TextSecondary, fontSize = 14.sp)
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { if (!forceRecurring) isRecurring = it },
                    enabled = !forceRecurring,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = Color(0xFF1A73E8)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isRecurring) {
                SheetSectionLabel(stringResource(Res.string.label_date))
                Spacer(modifier = Modifier.height(8.dp))
                DateInputField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    nullable = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                SheetSectionLabel(stringResource(Res.string.label_time_optional))
                Spacer(modifier = Modifier.height(8.dp))
                TimeInputField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    nullable = true
                )
                if (selectedTime != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "🔔 ${stringResource(Res.string.label_notify_at_time)}",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = Color(0xFF1A73E8)
                            )
                        )
                    }
                }
            } else {
                SheetSectionLabel(stringResource(Res.string.label_time_optional))
                Spacer(modifier = Modifier.height(8.dp))
                TimeInputField(
                    value = recurringTime,
                    onValueChange = { recurringTime = it },
                    nullable = true
                )
                if (recurringTime != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "🔔 ${stringResource(Res.string.label_notify_at_time)}",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = Color(0xFF1A73E8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SheetSectionLabel(stringResource(Res.string.label_days))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEach { (dayNum, dayLabel) ->
                        val isSelected = selectedDays.containsKey(dayNum)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF1A73E8) else CardBackground)
                                .border(1.dp, if (isSelected) Color(0xFF1A73E8) else CardBorder, CircleShape)
                                .clickable {
                                    if (isSelected) selectedDays.remove(dayNum)
                                    else selectedDays[dayNum] = Unit
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(dayLabel, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SheetSectionLabel(stringResource(if (isRecurring) Res.string.label_block else Res.string.label_block_optional))
            Spacer(modifier = Modifier.height(8.dp))

            if (!isRecurring) {
                BlockChip(
                    label = stringResource(Res.string.chip_no_block),
                    color = Color(0x44FFFFFF),
                    isSelected = selectedBlockId == null,
                    onClick = { selectedBlockId = null }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            blocks.forEach { block ->
                BlockChip(
                    label = "${block.icon} ${block.name}",
                    color = parseColor(block.color),
                    isSelected = selectedBlockId == block.id,
                    onClick = { selectedBlockId = block.id }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(Res.string.btn_cancel), color = TextSecondary, fontSize = 15.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A73E8))
                        .clickable {
                            if (title.isBlank()) return@clickable
                            if (!isRecurring) {
                                val date = selectedDate ?: currentDate()
                                onSaveOneTime(title, selectedBlockId, date, selectedTime, notificationsEnabled)
                            } else {
                                val blockId = selectedBlockId ?: return@clickable
                                if (selectedDays.isEmpty()) return@clickable
                                val slots = selectedDays.keys.map { WeeklySlot(it, null, null) }
                                onSaveRecurring(title, blockId, recurringTime, Recurrence.Weekly(slots), notificationsEnabled)
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(Res.string.btn_save), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (existingTask != null && onDelete != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33EA4335))
                        .clickable { onDelete() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(Res.string.btn_delete_task), color = Color(0xFFEA4335), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun BlockChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) color.copy(alpha = 0.25f) else CardBackground)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) color else CardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        color = SectionLabel,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, color = SectionLabel, fontSize = 15.sp)
            inner()
        }
    )
}