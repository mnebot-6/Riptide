package com.mnebot.riptide.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.PersonalDate
import com.mnebot.riptide.domain.model.PersonalDateRecurrence
import com.mnebot.riptide.domain.model.PersonalDateType
import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.presentation.components.DateInputField
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val AccentBlue = Color(0xFF1A73E8)
private val ChipSelected = Color(0xFF1A73E8)
private val ChipBg = Color(0x33FFFFFF)

@Composable
fun PersonalDateDialog(
    onDismiss: () -> Unit,
    onSave: (PersonalDate) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date) }
    var type by remember { mutableStateOf(PersonalDateType.CUSTOM) }
    var recurrence by remember { mutableStateOf(PersonalDateRecurrence.ONCE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B3A6B),
        title = {
            Text(
                stringResource(Res.string.dialog_personal_date_title),
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Title field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChipBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (title.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.dialog_personal_date_title),
                            color = TextSecondary.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(AccentBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Date picker
                DateInputField(
                    value = date,
                    onValueChange = { it?.let { d -> date = d } }
                )

                // Type selector
                Text(stringResource(Res.string.label_date_type), color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf(
                        PersonalDateType.BIRTHDAY to stringResource(Res.string.date_type_birthday),
                        PersonalDateType.APPOINTMENT to stringResource(Res.string.date_type_appointment),
                        PersonalDateType.HOLIDAY to stringResource(Res.string.date_type_holiday),
                        PersonalDateType.CUSTOM to stringResource(Res.string.date_type_custom)
                    )
                    types.forEach { (t, label) ->
                        MiniChip(label, selected = type == t) { type = t }
                    }
                }

                // Recurrence selector
                Text(stringResource(Res.string.label_recurrence), color = TextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val recurrences = listOf(
                        PersonalDateRecurrence.ONCE to stringResource(Res.string.recurrence_once),
                        PersonalDateRecurrence.YEARLY to stringResource(Res.string.recurrence_yearly),
                        PersonalDateRecurrence.MONTHLY to stringResource(Res.string.recurrence_monthly)
                    )
                    recurrences.forEach { (r, label) ->
                        MiniChip(label, selected = recurrence == r) { recurrence = r }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            PersonalDate(
                                id = generateUUID(),
                                title = title.trim(),
                                date = date,
                                type = type,
                                recurrence = recurrence,
                                notificationsEnabled = false
                            )
                        )
                    }
                }
            ) {
                Text(
                    stringResource(Res.string.btn_save),
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.btn_cancel), color = TextSecondary)
            }
        }
    )
}

@Composable
private fun MiniChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ChipSelected else ChipBg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
