package com.mnebot.riptide.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalTime

private val CardBackground = Color(0x33FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val SectionLabel = Color(0x80FFFFFF)

@Composable
fun TimeInputField(
    value: LocalTime?,
    onValueChange: (LocalTime?) -> Unit,
    nullable: Boolean = true,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }

    val fieldWidth = if (compact) 60.dp else 80.dp
    val fontSize = if (compact) 14.sp else 16.sp
    val hPad = if (compact) 8.dp else 10.dp
    val vPad = if (compact) 6.dp else 10.dp

    val displayText = value?.let {
        "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
    } ?: "--:--"

    Box(
        modifier = modifier
            .width(fieldWidth)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .clickable { showPicker = true }
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            color = if (value != null) TextPrimary else SectionLabel,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium
        )
    }

    if (showPicker) {
        TimePickerDialogWrapper(
            initial = value,
            onConfirm = { picked ->
                onValueChange(picked)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
expect fun TimePickerDialogWrapper(
    initial: LocalTime?,
    onConfirm: (LocalTime?) -> Unit,
    onDismiss: () -> Unit
)