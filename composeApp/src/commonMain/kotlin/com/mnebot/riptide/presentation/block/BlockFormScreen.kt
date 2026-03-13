// presentation/block/BlockFormScreen.kt
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.Recurrence
import com.mnebot.riptide.domain.model.WeeklySlot
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.presentation.components.TimeInputField
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
                Spacer(modifier = Modifier.height(16.dp))
                ScheduleSection(selectedDays = selectedDays)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón eliminar
            if (existingBlock != null && onDelete != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33EA4335))
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
private fun ScheduleSection(
    selectedDays: androidx.compose.runtime.snapshots.SnapshotStateMap<Int, Pair<LocalTime?, LocalTime?>>
) {
    var sameTimeForAll by remember { mutableStateOf(false) }
    var sharedStart by remember { mutableStateOf<LocalTime?>(null) }
    var sharedEnd by remember { mutableStateOf<LocalTime?>(null) }

    // Selector de días
    SectionTitle("DÍAS", horizontalPadding = true)
    Spacer(modifier = Modifier.height(8.dp))
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
                    .background(if (isSelected) Color(0xFF1A73E8) else Color(0x33FFFFFF))
                    .border(
                        1.dp,
                        if (isSelected) Color(0xFF1A73E8) else Color(0x55FFFFFF),
                        CircleShape
                    )
                    .clickable {
                        if (isSelected) selectedDays.remove(dayNum)
                        else selectedDays[dayNum] = Pair(null, null)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dayLabel,
                    color = Color(0xFFFFFFFF),
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
            Text("Mismo horario todos los días", color = Color(0xB3FFFFFF), fontSize = 13.sp)
            Switch(
                checked = sameTimeForAll,
                onCheckedChange = { sameTimeForAll = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFFFFF),
                    checkedTrackColor = Color(0xFF1A73E8)
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
        TimeInputField(value = start, onValueChange = onStartChange, nullable = true, compact = true)
        Spacer(modifier = Modifier.width(16.dp))
        Text("Fin", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TimeInputField(value = end, onValueChange = onEndChange, nullable = true, compact = true)
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
        TimeInputField(value = startTime, onValueChange = onStartChange, nullable = true, compact = true)
        Spacer(modifier = Modifier.width(12.dp))
        Text("Fin", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TimeInputField(value = endTime, onValueChange = onEndChange, nullable = true, compact = true)
    }
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
        decorationBox = { inner ->
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
