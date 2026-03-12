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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.presentation.main.currentDate
import com.mnebot.riptide.presentation.main.parseColor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val CardBackground = Color(0x33FFFFFF)
private val CardBorder = Color(0x55FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val SectionLabel = Color(0x80FFFFFF)

private val days = listOf(
    1 to "L", 2 to "M", 3 to "X", 4 to "J",
    5 to "V", 6 to "S", 7 to "D"
)

@Composable
fun TaskFormSheet(
    blocks: List<WorkBlock>,
    initialDate: LocalDate = currentDate(),
    existingTask: DayTask? = null,
    onSaveOneTime: (title: String, blockId: String?, date: LocalDate, time: LocalTime?) -> Unit,
    onSaveRecurring: (title: String, blockId: String, time: LocalTime, recurrence: Recurrence) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var selectedBlockId by remember { mutableStateOf<String?>(existingTask?.blockId) }
    var isRecurring by remember { mutableStateOf(false) } // edición siempre como puntual por ahora

    // OneTime
    val initialSchedule = existingTask?.schedule as? TaskSchedule.OneTime
    var dateText by remember { mutableStateOf(initialSchedule?.date?.toString() ?: initialDate.toString()) }
    var timeText by remember { mutableStateOf(
        initialSchedule?.time?.let {
            "${it.hour.toString().padStart(2,'0')}${it.minute.toString().padStart(2,'0')}"
        } ?: ""
    ) }

    // Recurring
    var recurringTimeDigits by remember { mutableStateOf("") }
    val selectedDays = remember { mutableStateMapOf<Int, Unit>() }

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
            // Asa
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x55FFFFFF))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Título del sheet
            Text(
                "Nueva tarea",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Nombre
            SheetSectionLabel("NOMBRE")
            Spacer(modifier = Modifier.height(8.dp))
            SheetTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "¿Qué tienes que hacer?"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tipo: puntual / recurrente
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("¿Es una tarea recurrente?", color = TextSecondary, fontSize = 14.sp)
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = Color(0xFF1A73E8)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isRecurring) {
                // --- PUNTUAL ---
                SheetSectionLabel("FECHA")
                Spacer(modifier = Modifier.height(8.dp))
                SheetTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    placeholder = "YYYY-MM-DD"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SheetSectionLabel("HORA (opcional)")
                Spacer(modifier = Modifier.height(8.dp))
                SheetTimeField(
                    digits = timeText,
                    onDigitsChange = { timeText = it }
                )
            } else {
                // --- RECURRENTE ---
                SheetSectionLabel("HORA")
                Spacer(modifier = Modifier.height(8.dp))
                SheetTimeField(
                    digits = recurringTimeDigits,
                    onDigitsChange = { recurringTimeDigits = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SheetSectionLabel("DÍAS")
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

            // Bloque (opcional para puntual, obligatorio para recurrente)
            SheetSectionLabel(if (isRecurring) "BLOQUE" else "BLOQUE (opcional)")
            Spacer(modifier = Modifier.height(8.dp))

            // Sin bloque (solo para puntuales)
            if (!isRecurring) {
                BlockChip(
                    label = "Sin bloque",
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

            // Botones
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
                    Text("Cancelar", color = TextSecondary, fontSize = 15.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A73E8))
                        .clickable {
                            if (title.isBlank()) return@clickable
                            if (!isRecurring) {
                                val date = runCatching { LocalDate.parse(dateText) }
                                    .getOrDefault(currentDate())
                                val time = parseTimeDigits(timeText)
                                onSaveOneTime(title, selectedBlockId, date, time)
                            } else {
                                val blockId = selectedBlockId ?: return@clickable
                                val time = parseTimeDigits(recurringTimeDigits) ?: return@clickable
                                if (selectedDays.isEmpty()) return@clickable
                                val slots = selectedDays.keys.map { WeeklySlot(it, null, null) }
                                onSaveRecurring(title, blockId, time, Recurrence.Weekly(slots))
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Guardar", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
                    Text("Eliminar tarea", color = Color(0xFFEA4335), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun parseTimeDigits(digits: String): LocalTime? {
    if (digits.length < 4) return null
    val h = digits.substring(0, 2).toIntOrNull() ?: return null
    val m = digits.substring(2, 4).toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return LocalTime(h, m)
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

@Composable
private fun SheetTimeField(
    digits: String,
    onDigitsChange: (String) -> Unit
) {
    val display = when {
        digits.length >= 3 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
        else -> digits
    }
    BasicTextField(
        value = display,
        onValueChange = { input ->
            val newDigits = input.filter { it.isDigit() }.take(4)
            onDigitsChange(newDigits)
        },
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        textStyle = TextStyle(
            color = TextPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        decorationBox = { inner ->
            if (digits.isEmpty()) {
                Text(
                    "--:--",
                    color = SectionLabel,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            inner()
        }
    )
}