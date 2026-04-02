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
import org.jetbrains.compose.resources.painterResource
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
import com.mnebot.riptide.presentation.localizedDays
import com.mnebot.riptide.presentation.main.parseColor
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

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

    val initialSlots = (existingBlock?.recurrence as? Recurrence.Weekly)?.slots?.associate {
        it.dayOfWeek to Pair(it.startTime, it.endTime)
    } ?: emptyMap()

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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CardBackground)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_left),
                        contentDescription = stringResource(Res.string.a11y_back),
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(if (existingBlock == null) Res.string.title_new_block else Res.string.title_edit_block),
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
                        stringResource(Res.string.btn_save),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nombre
            SectionTitle(stringResource(Res.string.label_name), horizontalPadding = true)
            Spacer(modifier = Modifier.height(8.dp))
            FormTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(Res.string.placeholder_block_name),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Icono
            SectionTitle(stringResource(Res.string.label_icon), horizontalPadding = true)
            Spacer(modifier = Modifier.height(8.dp))
            FormTextField(
                value = icon,
                onValueChange = { if (it.length <= 2) icon = it },
                placeholder = stringResource(Res.string.placeholder_icon),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Color
            SectionTitle(stringResource(Res.string.label_color), horizontalPadding = true)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(colorPalette, key = { it }) { hex ->
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
            SectionTitle(stringResource(Res.string.label_schedule), horizontalPadding = true)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(Res.string.label_schedule_toggle), color = TextSecondary, fontSize = 14.sp)
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
                        stringResource(Res.string.btn_delete_block),
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
    val days = localizedDays()

    // Selector de días
    SectionTitle(stringResource(Res.string.label_days), horizontalPadding = true)
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
            Text(stringResource(Res.string.label_same_schedule), color = Color(0xB3FFFFFF), fontSize = 13.sp)
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
        Text(stringResource(Res.string.label_start_time), color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TimeInputField(value = start, onValueChange = onStartChange, nullable = true, compact = true)
        Spacer(modifier = Modifier.width(16.dp))
        Text(stringResource(Res.string.label_end_time), color = TextSecondary, fontSize = 13.sp)
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
        Text(stringResource(Res.string.label_start_time), color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        TimeInputField(value = startTime, onValueChange = onStartChange, nullable = true, compact = true)
        Spacer(modifier = Modifier.width(12.dp))
        Text(stringResource(Res.string.label_end_time), color = TextSecondary, fontSize = 12.sp)
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
