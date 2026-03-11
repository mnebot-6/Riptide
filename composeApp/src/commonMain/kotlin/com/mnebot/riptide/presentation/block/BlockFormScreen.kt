package com.mnebot.riptide.presentation.block

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
import com.mnebot.riptide.domain.model.Recurrence
import com.mnebot.riptide.domain.model.WeeklySlot
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.presentation.main.parseColor
import kotlinx.datetime.LocalTime

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanLight = Color(0xFF2E5F9E)
private val CardBackground = Color(0x33FFFFFF)
private val CardBorder = Color(0x55FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val SectionLabel = Color(0x80FFFFFF)

private val colorPalette = listOf(
    "#1A73E8", "#E8711A", "#34A853", "#EA4335",
    "#9C27B0", "#00BCD4", "#FF9800", "#607D8B",
    "#E91E63", "#795548", "#009688", "#F5C842"
)

private val days = listOf(
    1 to "L", 2 to "M", 3 to "X", 4 to "J",
    5 to "V", 6 to "S", 7 to "D"
)

@Composable
fun BlockFormScreen(
    existingBlock: WorkBlock? = null,
    onSave: (WorkBlock) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(existingBlock?.name ?: "") }
    var icon by remember { mutableStateOf(existingBlock?.icon ?: "") }
    var selectedColor by remember { mutableStateOf(existingBlock?.color ?: colorPalette.first()) }
    var hasSchedule by remember {
        mutableStateOf(existingBlock?.recurrence is Recurrence.Weekly)
    }

    val initialSlots = if (existingBlock?.recurrence is Recurrence.Weekly) {
        (existingBlock.recurrence as Recurrence.Weekly).slots.associate {
            it.dayOfWeek to Pair(it.startTime, it.endTime)
        }
    } else emptyMap()

    val selectedDays = remember {
        mutableStateMapOf<Int, Pair<LocalTime?, LocalTime?>>().apply { putAll(initialSlots) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(OceanDeep, OceanMid, OceanLight)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Cabecera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardBackground)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = TextPrimary, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (existingBlock == null) "Nuevo bloque" else "Editar bloque",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .clickable {
                            if (name.isNotBlank()) {
                                onSave(
                                    buildBlock(
                                        existingBlock?.id,
                                        name,
                                        icon,
                                        selectedColor,
                                        hasSchedule,
                                        selectedDays
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Guardar",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nombre
            SectionTitle("NOMBRE", horizontalPadding = true)
            Spacer(modifier = Modifier.height(8.dp))
            FormTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Ej: Trabajo, Deporte, Lectura...",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Icono
            SectionTitle("ICONO", horizontalPadding = true)
            Spacer(modifier = Modifier.height(8.dp))
            FormTextField(
                value = icon,
                onValueChange = { if (it.length <= 2) icon = it },
                placeholder = "Escribe un emoji",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Color
            SectionTitle("COLOR", horizontalPadding = true)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(colorPalette) { hex ->
                    val color = parseColor(hex)
                    val isSelected = hex == selectedColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(3.dp, TextPrimary, CircleShape)
                                else Modifier.border(1.dp, CardBorder, CircleShape)
                            )
                            .clickable { selectedColor = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Horario
            SectionTitle("HORARIO", horizontalPadding = true)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("¿Tiene días y horario fijo?", color = TextSecondary, fontSize = 14.sp)
                Switch(
                    checked = hasSchedule,
                    onCheckedChange = {
                        hasSchedule = it
                        if (!it) selectedDays.clear()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = parseColor(selectedColor)
                    )
                )
            }

            if (hasSchedule) {
                Spacer(modifier = Modifier.height(12.dp))
                DaySelector(
                    selectedDays = selectedDays,
                    blockColor = parseColor(selectedColor)
                )
            }

            // Eliminar
            if (existingBlock != null && onDelete != null) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33EA4335))
                        .border(1.dp, Color(0x55EA4335), RoundedCornerShape(12.dp))
                        .clickable { onDelete(existingBlock.id) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Eliminar bloque",
                        color = Color(0xFFEA4335),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    selectedDays: MutableMap<Int, Pair<LocalTime?, LocalTime?>>,
    blockColor: Color
) {
    var sameTimeForAll by remember { mutableStateOf(false) }
    var sharedStart by remember { mutableStateOf<LocalTime?>(null) }
    var sharedEnd by remember { mutableStateOf<LocalTime?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (dayNum, dayLabel) ->
            val isSelected = selectedDays.containsKey(dayNum)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) blockColor else CardBackground)
                    .border(1.dp, if (isSelected) blockColor else CardBorder, CircleShape)
                    .clickable {
                        if (isSelected) {
                            selectedDays.remove(dayNum)
                        } else {
                            selectedDays[dayNum] = if (sameTimeForAll)
                                Pair(sharedStart, sharedEnd)
                            else
                                Pair(null, null)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dayLabel,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (selectedDays.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mismo horario para todos", color = TextSecondary, fontSize = 13.sp)
            Switch(
                checked = sameTimeForAll,
                onCheckedChange = { checked ->
                    sameTimeForAll = checked
                    if (checked) {
                        val keys = selectedDays.keys.toList()
                        keys.forEach { selectedDays[it] = Pair(sharedStart, sharedEnd) }
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = blockColor
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (sameTimeForAll) {
            SharedTimeRow(
                start = sharedStart,
                end = sharedEnd,
                onStartChange = { t ->
                    sharedStart = t
                    val keys = selectedDays.keys.toList()
                    keys.forEach { selectedDays[it] = Pair(t, sharedEnd) }
                },
                onEndChange = { t ->
                    sharedEnd = t
                    val keys = selectedDays.keys.toList()
                    keys.forEach { selectedDays[it] = Pair(sharedStart, t) }
                }
            )
        } else {
            selectedDays.keys.sorted().forEach { dayNum ->
                val dayLabel = days.first { it.first == dayNum }.second
                val slot = selectedDays[dayNum] ?: Pair(null, null)
                DayTimeRow(
                    dayLabel = dayLabel,
                    startTime = slot.first,
                    endTime = slot.second,
                    onStartChange = { selectedDays[dayNum] = slot.copy(first = it) },
                    onEndChange = { selectedDays[dayNum] = slot.copy(second = it) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SharedTimeRow(
    start: LocalTime?,
    end: LocalTime?,
    onStartChange: (LocalTime?) -> Unit,
    onEndChange: (LocalTime?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("Inicio", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TimeTextField(time = start, onChange = onStartChange)
        Spacer(modifier = Modifier.width(16.dp))
        Text("Fin", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TimeTextField(time = end, onChange = onEndChange)
    }
}

@Composable
private fun DayTimeRow(
    dayLabel: String,
    startTime: LocalTime?,
    endTime: LocalTime?,
    onStartChange: (LocalTime?) -> Unit,
    onEndChange: (LocalTime?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dayLabel,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text("Inicio", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TimeTextField(time = startTime, onChange = onStartChange)
        Spacer(modifier = Modifier.width(12.dp))
        Text("Fin", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TimeTextField(time = endTime, onChange = onEndChange)
    }
}

@Composable
private fun TimeTextField(
    time: LocalTime?,
    onChange: (LocalTime?) -> Unit
) {
    var text by remember(time) {
        mutableStateOf(
            time?.let {
                "${it.hour.toString().padStart(2, '0')}${it.minute.toString().padStart(2, '0')}"
            } ?: ""
        )
    }

    BasicTextField(
        value = text,
        onValueChange = { input: String ->
            val digits = input.filter { c: Char -> c.isDigit() }.take(4)
            text = digits
            if (digits.length == 4) {
                val h = digits.substring(0, 2).toIntOrNull()
                val m = digits.substring(2, 4).toIntOrNull()
                if (h != null && m != null && h in 0..23 && m in 0..59) {
                    onChange(LocalTime(h, m))
                } else {
                    onChange(null)
                }
            } else {
                onChange(null)
            }
        },
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x22FFFFFF))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        textStyle = TextStyle(
            color = TextPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = { inner: @Composable () -> Unit ->
            if (text.isEmpty()) {
                Text(
                    "--:--",
                    color = SectionLabel,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            inner()
        }
    )
}

@Composable
private fun SectionTitle(title: String, horizontalPadding: Boolean = false) {
    Text(
        text = title,
        color = SectionLabel,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = if (horizontalPadding) Modifier.padding(horizontal = 16.dp) else Modifier
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        textStyle = TextStyle(
            color = TextPrimary,
            fontSize = 15.sp
        ),
        decorationBox = { inner: @Composable () -> Unit ->
            if (value.isEmpty()) {
                Text(placeholder, color = SectionLabel, fontSize = 15.sp)
            }
            inner()
        }
    )
}

private fun buildBlock(
    id: String?,
    name: String,
    icon: String,
    color: String,
    hasSchedule: Boolean,
    selectedDays: Map<Int, Pair<LocalTime?, LocalTime?>>
): WorkBlock {
    val recurrence = if (hasSchedule && selectedDays.isNotEmpty()) {
        Recurrence.Weekly(
            slots = selectedDays.map { (day, times) ->
                WeeklySlot(dayOfWeek = day, startTime = times.first, endTime = times.second)
            }
        )
    } else {
        Recurrence.None
    }
    return WorkBlock(
        id = id ?: generateUUID(),
        name = name,
        marineCategories = emptyList(),
        color = color,
        icon = icon.ifBlank { "📦" },
        recurrence = recurrence,
        isActive = true
    )
}