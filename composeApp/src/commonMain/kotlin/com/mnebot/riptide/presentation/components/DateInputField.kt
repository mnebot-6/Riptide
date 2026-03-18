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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.presentation.main.currentDate
import kotlinx.datetime.LocalDate

private val CardBackground = Color(0x33FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val SectionLabel = Color(0x80FFFFFF)

@Composable
fun DateInputField(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    nullable: Boolean = false,
    modifier: Modifier = Modifier
) {
    val today = currentDate()
    var showPicker by remember { mutableStateOf(false) }

    val displayText = value?.let {
        "${it.year}-${it.monthNumber.toString().padStart(2, '0')}-${it.dayOfMonth.toString().padStart(2, '0')}"
    } ?: "----/--/--"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .clickable { showPicker = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = displayText,
            color = if (value != null) TextPrimary else SectionLabel,
            fontSize = 15.sp
        )
    }

    if (showPicker) {
        DatePickerDialogWrapper(
            initial = value ?: today,
            onConfirm = { picked ->
                if (picked != null) onValueChange(picked)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
expect fun DatePickerDialogWrapper(
    initial: LocalDate,
    onConfirm: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
)