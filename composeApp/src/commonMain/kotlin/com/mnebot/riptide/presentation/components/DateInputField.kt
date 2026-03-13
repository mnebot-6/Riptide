// commonMain/presentation/components/DateInputField.kt
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.presentation.main.currentDate
import kotlinx.datetime.LocalDate

private val CardBackground = Color(0x33FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val SectionLabel = Color(0x80FFFFFF)
private val ErrorRed = Color(0xFFEA4335)

/**
 * Input de fecha estandarizado.
 *
 * El usuario escribe dígitos puros (máx 8). El formato YYYY-MM-DD se muestra
 * como overlay — el cursor nunca cruza los guiones.
 * Los dígitos del usuario reemplazan los de la fecha de hoy por la derecha:
 *   escribir "14"    → toma hoy "20260313", sustituye últimos 2 → "20260314" → "2026-03-14"
 *   escribir "70601" → sustituye últimos 5 → "20270601" → "2027-06-01"
 *
 * [nullable] permite dejarlo vacío.
 */
@Composable
fun DateInputField(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    nullable: Boolean = false,
    modifier: Modifier = Modifier
) {
    val today = currentDate()
    val todayDigits = "${today.year}${today.monthNumber.toString().padStart(2, '0')}${today.dayOfMonth.toString().padStart(2, '0')}"

    var userDigits by remember(value) {
        mutableStateOf(
            value?.let {
                "${it.year}${it.monthNumber.toString().padStart(2, '0')}${it.dayOfMonth.toString().padStart(2, '0')}"
            } ?: ""
        )
    }

    var showPicker by remember { mutableStateOf(false) }

    fun mergedDigits(): String {
        if (userDigits.isEmpty()) return todayDigits
        return (todayDigits.dropLast(userDigits.length) + userDigits).take(8)
    }

    fun formatDisplay(digits: String): String {
        val d = digits.padEnd(8, '-')
        return "${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}"
    }

    fun parseDate(digits: String): LocalDate? {
        if (digits.length != 8) return null
        val y = digits.substring(0, 4).toIntOrNull() ?: return null
        val m = digits.substring(4, 6).toIntOrNull() ?: return null
        val d = digits.substring(6, 8).toIntOrNull() ?: return null
        return runCatching { LocalDate(y, m, d) }.getOrNull()
    }

    val merged = mergedDigits()
    val parsedDate = parseDate(merged)
    val isValid = userDigits.isEmpty() || parsedDate != null

    fun onInput(newRaw: String) {
        val newDigits = newRaw.filter { it.isDigit() }.take(8)
        userDigits = newDigits
        onValueChange(parseDate(mergedDigits()))
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isValid) CardBackground else Color(0x33EA4335))
                    .then(
                        if (!isValid) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(12.dp))
                        else Modifier
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Input invisible — solo captura dígitos
                BasicTextField(
                    value = userDigits,
                    onValueChange = { onInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Transparent, fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                // Overlay visible formateado
                Text(
                    text = formatDisplay(merged),
                    color = when {
                        userDigits.isEmpty() -> SectionLabel
                        !isValid -> ErrorRed
                        else -> TextPrimary
                    },
                    fontSize = 15.sp
                )
            }

            Text(
                "📅",
                fontSize = 22.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showPicker = true }
                    .padding(4.dp)
            )
        }

        if (!isValid) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Fecha inválida", color = ErrorRed, fontSize = 11.sp)
        }
    }

    if (showPicker) {
        DatePickerDialogWrapper(
            initial = value ?: today,
            onConfirm = { picked ->
                if (picked != null) {
                    userDigits = "${picked.year}${picked.monthNumber.toString().padStart(2, '0')}${picked.dayOfMonth.toString().padStart(2, '0')}"
                }
                onValueChange(picked)
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
