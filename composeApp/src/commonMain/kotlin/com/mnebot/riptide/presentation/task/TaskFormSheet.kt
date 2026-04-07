// presentation/task/TaskFormSheet.kt
package com.mnebot.riptide.presentation.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.presentation.components.DateInputField
import com.mnebot.riptide.presentation.components.TimeInputField
import com.mnebot.riptide.presentation.localizedDays
import com.mnebot.riptide.presentation.main.currentDate
import com.mnebot.riptide.presentation.main.parseColor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val CardBackground = Color(0x33FFFFFF)
private val CardBorder = Color(0x55FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)
private val SectionLabel = Color(0xB3FFFFFF)

@Composable
fun TaskFormSheet(
    blocks: List<WorkBlock>,
    initialDate: LocalDate = currentDate(),
    initialBlockId: String? = null,
    existingTask: DayTask? = null,
    existingDef: RecurringTaskDef? = null,
    forceRecurring: Boolean = false,
    onSaveOneTime: (title: String, blockId: String?, date: LocalDate, time: LocalTime?, notificationsEnabled: Boolean, targetCount: Int?, notes: String?, timerDurationMinutes: Int?, isPriority: Boolean) -> Unit,
    onSaveRecurring: (title: String, blockId: String, time: LocalTime?, recurrence: Recurrence, notificationsEnabled: Boolean, targetCount: Int?, noteTemplate: String?, timerDurationMinutes: Int?, isPriority: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var selectedBlockId by remember { mutableStateOf<String?>(initialBlockId ?: existingTask?.blockId) }
    var isRecurring by remember { mutableStateOf(forceRecurring) }
    var titleError by remember { mutableStateOf(false) }
    var blockError by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val initialSchedule = existingTask?.schedule as? TaskSchedule.OneTime
    var selectedDate by remember { mutableStateOf<LocalDate?>(initialSchedule?.date ?: initialDate) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(initialSchedule?.time) }

    var recurringTime by remember(existingDef) { mutableStateOf<LocalTime?>(existingDef?.time) }
    var notificationsEnabled by remember(existingTask, existingDef) {
        mutableStateOf(existingTask?.notificationsEnabled ?: existingDef?.notificationsEnabled ?: false)
    }

    // New feature states
    var targetCountEnabled by remember { mutableStateOf(existingTask?.targetCount != null || existingDef?.targetCount != null) }
    var targetCount by remember { mutableIntStateOf(existingTask?.targetCount ?: existingDef?.targetCount ?: 3) }
    var timerEnabled by remember { mutableStateOf(existingTask?.timerDurationMinutes != null || existingDef?.timerDurationMinutes != null) }
    var timerDuration by remember { mutableIntStateOf(existingTask?.timerDurationMinutes ?: existingDef?.timerDurationMinutes ?: 15) }
    var isPriority by remember { mutableStateOf(existingTask?.isPriority ?: existingDef?.isPriority ?: false) }
    var notesText by remember { mutableStateOf(existingTask?.notes ?: existingDef?.noteTemplate ?: "") }

    // Reset notification toggle when time is cleared
    LaunchedEffect(selectedTime) { if (selectedTime == null) notificationsEnabled = false }
    LaunchedEffect(recurringTime) { if (recurringTime == null) notificationsEnabled = false }

    val selectedDays = remember(existingDef) {
        mutableStateMapOf<Int, Unit>().also { map ->
            val slots = (existingDef?.recurrence as? Recurrence.Weekly)?.slots
            slots?.forEach { map[it.dayOfWeek] = Unit }
        }
    }
    val days = localizedDays()

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
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x55FFFFFF))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(if (existingTask == null) Res.string.title_new_task else Res.string.title_edit_task),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            SheetSectionLabel(stringResource(Res.string.label_name))
            Spacer(modifier = Modifier.height(8.dp))
            SheetTextField(
                value = title,
                onValueChange = { if (it.length <= 500) title = it },
                placeholder = stringResource(Res.string.placeholder_task_title)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(Res.string.label_recurring_toggle), color = TextSecondary, fontSize = 14.sp)
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { if (!forceRecurring) isRecurring = it },
                    enabled = !forceRecurring,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = Color(0xFF1A73E8)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isRecurring) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SheetSectionLabel(stringResource(Res.string.label_date))
                        Spacer(modifier = Modifier.height(8.dp))
                        DateInputField(
                            value = selectedDate,
                            onValueChange = { selectedDate = it },
                            nullable = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        SheetSectionLabel(stringResource(Res.string.label_time_optional))
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeInputField(
                            value = selectedTime,
                            onValueChange = { selectedTime = it },
                            nullable = true
                        )
                    }
                }
                if (selectedTime != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_bell),
                                contentDescription = stringResource(Res.string.a11y_notification),
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.label_notify_at_time),
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = Color(0xFF1A73E8)
                            )
                        )
                    }
                }
            } else {
                SheetSectionLabel(stringResource(Res.string.label_time_optional))
                Spacer(modifier = Modifier.height(8.dp))
                TimeInputField(
                    value = recurringTime,
                    onValueChange = { recurringTime = it },
                    nullable = true
                )
                if (recurringTime != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_bell),
                                contentDescription = stringResource(Res.string.a11y_notification),
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.label_notify_at_time),
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = Color(0xFF1A73E8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SheetSectionLabel(stringResource(Res.string.label_days))
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

            // ── Feature toggles 2×2 grid ────────────────────────────────
            var showCountDialog by remember { mutableStateOf(false) }
            var showTimerDialog by remember { mutableStateOf(false) }
            var showNotesDialog by remember { mutableStateOf(false) }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FeatureToggleChip(
                        icon = Res.drawable.ic_star,
                        label = stringResource(Res.string.label_priority),
                        isActive = isPriority,
                        activeColor = Color(0xFFFFB347),
                        modifier = Modifier.weight(1f),
                        onClick = { isPriority = !isPriority }
                    )
                    FeatureToggleChip(
                        icon = Res.drawable.ic_hash,
                        label = stringResource(Res.string.label_countable),
                        isActive = targetCountEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = { showCountDialog = true }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FeatureToggleChip(
                        icon = Res.drawable.ic_timer,
                        label = stringResource(Res.string.label_timer),
                        isActive = timerEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = { showTimerDialog = true }
                    )
                    FeatureToggleChip(
                        icon = Res.drawable.ic_file_text,
                        label = stringResource(Res.string.label_notes),
                        isActive = notesText.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { showNotesDialog = true }
                    )
                }
            }

            // Countable config dialog
            if (showCountDialog) {
                AlertDialog(
                    onDismissRequest = { showCountDialog = false },
                    containerColor = Color(0xFF1B3A6B),
                    title = { Text(stringResource(Res.string.label_countable), color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                    text = {
                        Column {
                            SheetSectionLabel(stringResource(Res.string.label_target))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(2, 3, 5, 10).forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (targetCount == preset) Color(0xFF1A73E8) else CardBackground)
                                            .border(1.dp, if (targetCount == preset) Color(0xFF1A73E8) else CardBorder, RoundedCornerShape(8.dp))
                                            .clickable { targetCount = preset; targetCountEnabled = true }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$preset", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            BasicTextField(
                                value = if (targetCount in listOf(2, 3, 5, 10)) "" else targetCount.toString(),
                                onValueChange = { text ->
                                    text.toIntOrNull()?.let { if (it in 1..999) { targetCount = it; targetCountEnabled = true } }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardBackground)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (targetCount in listOf(2, 3, 5, 10)) Text(stringResource(Res.string.label_other), color = SectionLabel, fontSize = 14.sp)
                                    inner()
                                }
                            )
                            if (targetCountEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(Res.string.btn_remove_config),
                                    color = Color(0xFFEA4335),
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { targetCountEnabled = false; showCountDialog = false }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCountDialog = false }) {
                            Text(stringResource(Res.string.btn_accept), color = Color(0xFF1A73E8), fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {}
                )
            }

            // Timer config dialog
            if (showTimerDialog) {
                AlertDialog(
                    onDismissRequest = { showTimerDialog = false },
                    containerColor = Color(0xFF1B3A6B),
                    title = { Text(stringResource(Res.string.label_timer), color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                    text = {
                        Column {
                            SheetSectionLabel(stringResource(Res.string.label_duration))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(5, 10, 15, 25, 45).forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (timerDuration == preset) Color(0xFF1A73E8) else CardBackground)
                                            .border(1.dp, if (timerDuration == preset) Color(0xFF1A73E8) else CardBorder, RoundedCornerShape(8.dp))
                                            .clickable { timerDuration = preset; timerEnabled = true }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${preset}m", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            BasicTextField(
                                value = if (timerDuration in listOf(5, 10, 15, 25, 45)) "" else timerDuration.toString(),
                                onValueChange = { text ->
                                    text.toIntOrNull()?.let { if (it in 1..999) { timerDuration = it; timerEnabled = true } }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardBackground)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (timerDuration in listOf(5, 10, 15, 25, 45)) Text(stringResource(Res.string.label_other), color = SectionLabel, fontSize = 14.sp)
                                    inner()
                                }
                            )
                            if (timerEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(Res.string.btn_remove_config),
                                    color = Color(0xFFEA4335),
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { timerEnabled = false; showTimerDialog = false }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showTimerDialog = false }) {
                            Text(stringResource(Res.string.btn_accept), color = Color(0xFF1A73E8), fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {}
                )
            }

            // Notes config dialog
            if (showNotesDialog) {
                AlertDialog(
                    onDismissRequest = { showNotesDialog = false },
                    containerColor = Color(0xFF1B3A6B),
                    title = { Text(stringResource(Res.string.label_notes), color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                    text = {
                        Column {
                            SheetSectionLabel(stringResource(Res.string.label_templates))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                data class NoteTemplate(val label: String, val content: String)
                                val templates = listOf(
                                    NoteTemplate(stringResource(Res.string.tmpl_list), "- [ ] \n- [ ] \n- [ ] \n"),
                                    NoteTemplate(stringResource(Res.string.tmpl_description), "## ${stringResource(Res.string.tmpl_description)}\n\n"),
                                    NoteTemplate(stringResource(Res.string.tmpl_journal), "**${stringResource(Res.string.tmpl_date)}:** ${currentDate()}\n**${stringResource(Res.string.tmpl_mood)}:**\n**${stringResource(Res.string.tmpl_reflection)}:**\n"),
                                    NoteTemplate(stringResource(Res.string.tmpl_info), "**${stringResource(Res.string.tmpl_key)}:** ${stringResource(Res.string.tmpl_value)}\n**${stringResource(Res.string.tmpl_key)}:** ${stringResource(Res.string.tmpl_value)}\n")
                                )
                                templates.forEach { tmpl ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CardBackground)
                                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                            .clickable { notesText = tmpl.content }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(tmpl.label, color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            BasicTextField(
                                value = notesText,
                                onValueChange = { if (it.length <= 5000) notesText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardBackground)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                                decorationBox = { inner ->
                                    if (notesText.isEmpty()) Text(stringResource(Res.string.placeholder_notes), color = SectionLabel, fontSize = 14.sp)
                                    inner()
                                }
                            )
                            if (notesText.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(Res.string.btn_remove_config),
                                    color = Color(0xFFEA4335),
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { notesText = ""; showNotesDialog = false }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showNotesDialog = false }) {
                            Text(stringResource(Res.string.btn_accept), color = Color(0xFF1A73E8), fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {}
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SheetSectionLabel(stringResource(if (isRecurring) Res.string.label_block else Res.string.label_block_optional))
            Spacer(modifier = Modifier.height(8.dp))

            val allBlockOptions = buildList {
                if (!isRecurring) add(null to null) // "No block" option
                blocks.forEach { add(it.id to it) }
            }
            allBlockOptions.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { (id, block) ->
                        if (block == null) {
                            BlockChip(
                                label = stringResource(Res.string.chip_no_block),
                                color = Color(0x44FFFFFF),
                                isSelected = selectedBlockId == null,
                                onClick = { selectedBlockId = null },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            BlockChip(
                                label = "${block.icon} ${block.name}",
                                color = parseColor(block.color),
                                isSelected = selectedBlockId == block.id,
                                onClick = { selectedBlockId = block.id },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Validation error messages
            if (titleError) {
                Text(
                    text = stringResource(Res.string.error_field_required),
                    color = Color(0xFFEA4335),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (blockError) {
                Text(
                    text = stringResource(Res.string.error_block_required),
                    color = Color(0xFFEA4335),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

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
                    Text(stringResource(Res.string.btn_cancel), color = TextSecondary, fontSize = 15.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A73E8))
                        .clickable {
                            titleError = title.isBlank()
                            val finalTargetCount = if (targetCountEnabled) targetCount else null
                            val finalTimerDuration = if (timerEnabled) timerDuration else null
                            val finalNotes = notesText.takeIf { it.isNotBlank() }

                            if (!isRecurring) {
                                blockError = false
                                if (title.isBlank()) return@clickable
                                val date = selectedDate ?: currentDate()
                                onSaveOneTime(title, selectedBlockId, date, selectedTime, notificationsEnabled, finalTargetCount, finalNotes, finalTimerDuration, isPriority)
                            } else {
                                blockError = selectedBlockId == null
                                if (title.isBlank() || selectedBlockId == null) return@clickable
                                if (selectedDays.isEmpty()) return@clickable
                                val slots = selectedDays.keys.map { WeeklySlot(it, null, null) }
                                onSaveRecurring(title, selectedBlockId!!, recurringTime, Recurrence.Weekly(slots), notificationsEnabled, finalTargetCount, finalNotes, finalTimerDuration, isPriority)
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(Res.string.btn_save), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (existingTask != null && onDelete != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33EA4335))
                        .clickable { showDeleteConfirm = true }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(Res.string.btn_delete_task), color = Color(0xFFEA4335), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    containerColor = Color(0xFF1B3A6B),
                    title = {
                        Text(stringResource(Res.string.dialog_confirm_delete_title), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    },
                    text = {
                        Text(stringResource(Res.string.dialog_confirm_delete_body), color = TextSecondary, fontSize = 14.sp)
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteConfirm = false
                            onDelete?.invoke()
                        }) {
                            Text(stringResource(Res.string.menu_delete), color = Color(0xFFEA4335), fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(stringResource(Res.string.btn_cancel), color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BlockChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
private fun FeatureToggleChip(
    icon: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    isActive: Boolean,
    activeColor: Color = Color(0xFF1A73E8),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.2f) else CardBackground)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) activeColor else CardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (isActive) activeColor else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = if (isActive) TextPrimary else TextSecondary, fontSize = 13.sp)
    }
}