// commonMain/presentation/components/TimeInputField.kt
package com.mnebot.riptide.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalTime

private val CardBackground = Color(0x33FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val SectionLabel = Color(0x80FFFFFF)
private val ErrorRed = Color(0xFFEA4335)

/**
 * Input de hora estandarizado.
 *
 * El usuario escribe dígitos puros (máx 4). El formato HH:mm se muestra
 * como overlay — el cursor nunca cruza el ":".
 * Los dígitos se añaden por la izquierda: 1→8→3→0 → "18:30"
 * Al borrar también se eliminan por la izquierda.
 *
 * [nullable] permite dejarlo vacío.
 * [showPickerIcon] controla si aparece el icono del reloj.
 */
@Composable
fun TimeInputField(
    value: LocalTime?,
    onValueChange: (LocalTime?) -> Unit,
    nullable: Boolean = true,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showPickerIcon: Boolean = true
) {
    var digits by remember(value) {
        mutableStateOf(
            value?.let {
                "${it.hour.toString().padStart(2, '0')}${it.minute.toString().padStart(2, '0')}"
            } ?: ""
        )
    }

    var showPicker by remember { mutableStateOf(false) }

    val isValid = digits.isEmpty() || (digits.length == 4 && run {
        val h = digits.substring(0, 2).toIntOrNull() ?: -1
        val m = digits.substring(2, 4).toIntOrNull() ?: -1
        h in 0..23 && m in 0..59
    })

    fun formatDisplay(): String {
        val padded = digits.padStart(4, '-')
        return "${padded.substring(0, 2)}:${padded.substring(2, 4)}"
    }

    fun parseAndEmit(d: String) {
        if (d.isEmpty()) { onValueChange(null); return }
        if (d.length == 4) {
            val h = d.substring(0, 2).toIntOrNull()
            val m = d.substring(2, 4).toIntOrNull()
            if (h != null && m != null && h in 0..23 && m in 0..59) {
                onValueChange(LocalTime(h, m)); return
            }
        }
        onValueChange(null)
    }

    val fieldWidth = if (compact) 60.dp else 80.dp
    val fontSize = if (compact) 14.sp else 16.sp
    val hPad = if (compact) 8.dp else 10.dp
    val vPad = if (compact) 6.dp else 10.dp

    Column(horizontalAlignment = Alignment.Start, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(fieldWidth)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isValid) CardBackground else Color(0x33EA4335))
                    .then(
                        if (!isValid) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .padding(horizontal = hPad, vertical = vPad),
                contentAlignment = Alignment.Center
            ) {
                // Input invisible — solo captura dígitos
                BasicTextField(
                    value = digits,
                    onValueChange = { input ->
                        val newDigits = input.filter { it.isDigit() }.take(4)
                        digits = newDigits
                        parseAndEmit(newDigits)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Transparent, fontSize = fontSize),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                // Overlay visible formateado
                Text(
                    text = if (digits.isEmpty()) "--:--" else formatDisplay(),
                    color = when {
                        digits.isEmpty() -> SectionLabel
                        !isValid -> ErrorRed
                        else -> TextPrimary
                    },
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium
                )
            }

            if (showPickerIcon) {
                Text(
                    "🕐",
                    fontSize = if (compact) 18.sp else 20.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showPicker = true }
                        .padding(4.dp)
                )
            }
        }

        if (!isValid && digits.length == 4) {
            Spacer(modifier = Modifier.height(2.dp))
            Text("Hora inválida", color = ErrorRed, fontSize = 10.sp)
        }
    }

    if (showPicker) {
        TimePickerDialogWrapper(
            initial = value,
            onConfirm = { picked ->
                if (picked != null) {
                    digits = "${picked.hour.toString().padStart(2, '0')}${picked.minute.toString().padStart(2, '0')}"
                }
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
