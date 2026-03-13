// androidMain/presentation/components/InputFieldDialogs.android.kt
package com.mnebot.riptide.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val Accent = Color(0xFF7EC8E3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun TimePickerDialogWrapper(
    initial: LocalTime?,
    onConfirm: (LocalTime?) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: 8,
        initialMinute = initial?.minute ?: 0,
        is24Hour = true
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(OceanMid)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Seleccionar hora",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Colorear el TimePicker con tema marino
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        primary = Accent,
                        onPrimary = OceanDeep,
                        surface = Color(0x33FFFFFF),
                        onSurface = TextPrimary,
                        surfaceVariant = Color(0x22FFFFFF),
                        onSurfaceVariant = TextSecondary,
                        outline = Color(0x55FFFFFF),
                        secondaryContainer = Color(0x44FFFFFF),
                        onSecondaryContainer = TextPrimary,
                        tertiaryContainer = Accent.copy(alpha = 0.2f),
                        onTertiaryContainer = Accent
                    )
                ) {
                    TimePicker(state = state)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        onConfirm(LocalTime(state.hour, state.minute))
                    }) {
                        Text("Aceptar", color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun DatePickerDialogWrapper(
    initial: LocalDate,
    onConfirm: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = run {
            val days = initial.toEpochDays().toLong()
            days * 24 * 60 * 60 * 1000L
        }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp))
                .background(OceanMid)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        primary = Accent,
                        onPrimary = OceanDeep,
                        surface = OceanMid,
                        onSurface = TextPrimary,
                        surfaceVariant = Color(0x22FFFFFF),
                        onSurfaceVariant = TextSecondary,
                        outline = Color(0x55FFFFFF),
                        secondaryContainer = Color(0x44FFFFFF),
                        onSecondaryContainer = TextPrimary,
                        tertiaryContainer = Accent.copy(alpha = 0.2f),
                        onTertiaryContainer = Accent
                    )
                ) {
                    DatePicker(
                        state = state,
                        colors = DatePickerDefaults.colors(
                            containerColor = OceanMid,
                            titleContentColor = TextSecondary,
                            headlineContentColor = TextPrimary,
                            weekdayContentColor = TextSecondary,
                            subheadContentColor = TextSecondary,
                            navigationContentColor = TextPrimary,
                            yearContentColor = TextPrimary,
                            currentYearContentColor = Accent,
                            selectedYearContentColor = OceanDeep,
                            selectedYearContainerColor = Accent,
                            dayContentColor = TextPrimary,
                            selectedDayContentColor = OceanDeep,
                            selectedDayContainerColor = Accent,
                            todayContentColor = Accent,
                            todayDateBorderColor = Accent,
                            dayInSelectionRangeContentColor = TextPrimary,
                            dayInSelectionRangeContainerColor = Accent.copy(alpha = 0.2f)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        val millis = state.selectedDateMillis
                        if (millis != null) {
                            val days = (millis / (24 * 60 * 60 * 1000L)).toInt()
                            onConfirm(LocalDate.fromEpochDays(days))
                        } else {
                            onConfirm(null)
                        }
                    }) {
                        Text("Aceptar", color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
