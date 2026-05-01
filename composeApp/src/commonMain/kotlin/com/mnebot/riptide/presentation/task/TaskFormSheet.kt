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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.datetime.number
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

private data class NoteTemplate(val label: String, val content: String)

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
    var daysError by remember { mutableStateOf(false) }
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

    // Note: notifications without time fire at the morning reminder digest.

    val selectedDays = remember(existingDef) {
        mutableStateMapOf<Int, Unit>().also { map ->
            val slots = (existingDef?.recurrence as? Recurrence.Weekly)?.slots
            slots?.forEach { map[it.dayOfWeek] = Unit }
        }
    }
    val days = localizedDays()

    // ── Extended recurrence types ────────────────────────────────────────
    var recurrenceType by remember(existingDef) {
        mutableStateOf(
            when (existingDef?.recurrence) {
                is Recurrence.Yearly -> RecurrenceTypeUI.YEARLY
                is Recurrence.MonthlyDay -> RecurrenceTypeUI.MONTHLY_DAY
                is Recurrence.NthWeekdayOfMonth -> RecurrenceTypeUI.NTH_WEEKDAY
                else -> RecurrenceTypeUI.WEEKLY
            }
        )
    }
    var monthlyDay by remember(existingDef) {
        mutableIntStateOf((existingDef?.recurrence as? Recurrence.MonthlyDay)?.day ?: 1)
    }
    var monthlyInterval by remember(existingDef) {
        mutableIntStateOf((existingDef?.recurrence as? Recurrence.MonthlyDay)?.intervalMonths ?: 1)
    }
    var yearlyMonth by remember(existingDef) {
        mutableIntStateOf((existingDef?.recurrence as? Recurrence.Yearly)?.month ?: 1)
    }
    var yearlyDay by remember(existingDef) {
        mutableIntStateOf((existingDef?.recurrence as? Recurrence.Yearly)?.day ?: 1)
    }
    var nthOccurrence by remember(existingDef) {
        mutableIntStateOf((existingDef?.recurrence as? Recurrence.NthWeekdayOfMonth)?.nth ?: 1)
    }
    var nthDayOfWeek by remember(existingDef) {
        mutableIntStateOf((existingDef?.recurrence as? Recurrence.NthWeekdayOfMonth)?.dayOfWeek ?: 1)
    }
    var nthInterval by remember(existingDef) {
        mutableIntStateOf((existingDef?.recurrence as? Recurrence.NthWeekdayOfMonth)?.intervalMonths ?: 1)
    }

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
                onValueChange = { if (it.length <= 500) { title = it; titleError = false } },
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
            } else {
                SheetSectionLabel(stringResource(Res.string.label_time_optional))
                Spacer(modifier = Modifier.height(8.dp))
                TimeInputField(
                    value = recurringTime,
                    onValueChange = { recurringTime = it },
                    nullable = true
                )
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

                Spacer(modifier = Modifier.height(16.dp))

                // ── Recurrence type picker — equal-width chips ───────────────
                SheetSectionLabel(stringResource(Res.string.label_recurrence_type))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecurrenceTypeChip(
                        label = stringResource(Res.string.recurrence_weekly),
                        selected = recurrenceType == RecurrenceTypeUI.WEEKLY,
                        modifier = Modifier.weight(1f)
                    ) { recurrenceType = RecurrenceTypeUI.WEEKLY; daysError = false }
                    RecurrenceTypeChip(
                        label = stringResource(Res.string.recurrence_monthly_day),
                        selected = recurrenceType == RecurrenceTypeUI.MONTHLY_DAY,
                        modifier = Modifier.weight(1f)
                    ) { recurrenceType = RecurrenceTypeUI.MONTHLY_DAY; daysError = false }
                    RecurrenceTypeChip(
                        label = stringResource(Res.string.recurrence_nth_weekday),
                        selected = recurrenceType == RecurrenceTypeUI.NTH_WEEKDAY,
                        modifier = Modifier.weight(1f)
                    ) { recurrenceType = RecurrenceTypeUI.NTH_WEEKDAY; daysError = false }
                    RecurrenceTypeChip(
                        label = stringResource(Res.string.recurrence_yearly),
                        selected = recurrenceType == RecurrenceTypeUI.YEARLY,
                        modifier = Modifier.weight(1f)
                    ) { recurrenceType = RecurrenceTypeUI.YEARLY; daysError = false }
                }
                Spacer(modifier = Modifier.height(16.dp))

                when (recurrenceType) {
                    RecurrenceTypeUI.WEEKLY -> {
                        SheetSectionLabel(stringResource(Res.string.label_days))
                        Spacer(modifier = Modifier.height(8.dp))
                        WeekdayCircleRow(
                            days = days,
                            isSelected = { selectedDays.containsKey(it) },
                            onToggle = { dayNum ->
                                if (selectedDays.containsKey(dayNum)) selectedDays.remove(dayNum)
                                else selectedDays[dayNum] = Unit
                                daysError = false
                            }
                        )
                    }
                    RecurrenceTypeUI.MONTHLY_DAY -> {
                        // System date picker → user picks a date, we extract the day-of-month.
                        SheetSectionLabel(stringResource(Res.string.label_day_of_month))
                        Spacer(modifier = Modifier.height(8.dp))
                        DateInputField(
                            value = LocalDate(currentDate().year, currentDate().month, monthlyDay.coerceIn(1, 28)),
                            onValueChange = { picked -> picked?.let { monthlyDay = it.day } },
                            nullable = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SheetSectionLabel(stringResource(Res.string.label_every_n_months))
                        Spacer(modifier = Modifier.height(8.dp))
                        IntervalNumberField(value = monthlyInterval, onChange = { monthlyInterval = it })
                    }
                    RecurrenceTypeUI.YEARLY -> {
                        // One picker for both month and day-of-month — the year is ignored.
                        SheetSectionLabel(stringResource(Res.string.label_yearly_date))
                        Spacer(modifier = Modifier.height(8.dp))
                        DateInputField(
                            value = LocalDate(currentDate().year, yearlyMonth.coerceIn(1, 12), yearlyDay.coerceIn(1, 28)),
                            onValueChange = { picked ->
                                picked?.let {
                                    yearlyMonth = it.month.number
                                    yearlyDay = it.day
                                }
                            },
                            nullable = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    RecurrenceTypeUI.NTH_WEEKDAY -> {
                        SheetSectionLabel(stringResource(Res.string.label_nth_occurrence))
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val occurrenceLabels = listOf(
                                1 to stringResource(Res.string.nth_1),
                                2 to stringResource(Res.string.nth_2),
                                3 to stringResource(Res.string.nth_3),
                                4 to stringResource(Res.string.nth_4),
                                5 to stringResource(Res.string.nth_last)
                            )
                            occurrenceLabels.forEachIndexed { index, (n, label) ->
                                SegmentedButton(
                                    selected = nthOccurrence == n,
                                    onClick = { nthOccurrence = n },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = occurrenceLabels.size
                                    ),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = ChipAccent,
                                        activeContentColor = TextPrimary,
                                        activeBorderColor = ChipAccent,
                                        inactiveContainerColor = CardBackground,
                                        inactiveContentColor = TextSecondary,
                                        inactiveBorderColor = CardBorder
                                    ),
                                    label = { Text(label, fontSize = 13.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        SheetSectionLabel(stringResource(Res.string.label_days))
                        Spacer(modifier = Modifier.height(8.dp))
                        WeekdayCircleRow(
                            days = days,
                            isSelected = { nthDayOfWeek == it },
                            onToggle = { nthDayOfWeek = it }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SheetSectionLabel(stringResource(Res.string.label_every_n_months))
                        Spacer(modifier = Modifier.height(8.dp))
                        IntervalNumberField(value = nthInterval, onChange = { nthInterval = it })
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
            if (daysError) {
                Text(
                    text = stringResource(Res.string.error_days_required),
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
                                val recurrence: Recurrence = when (recurrenceType) {
                                    RecurrenceTypeUI.WEEKLY -> {
                                        daysError = selectedDays.isEmpty()
                                        if (selectedDays.isEmpty()) return@clickable
                                        val slots = selectedDays.keys.map { WeeklySlot(it, null, null) }
                                        Recurrence.Weekly(slots)
                                    }
                                    RecurrenceTypeUI.MONTHLY_DAY -> Recurrence.MonthlyDay(monthlyDay, monthlyInterval)
                                    RecurrenceTypeUI.YEARLY -> Recurrence.Yearly(yearlyMonth, yearlyDay)
                                    RecurrenceTypeUI.NTH_WEEKDAY -> Recurrence.NthWeekdayOfMonth(nthOccurrence, nthDayOfWeek, nthInterval)
                                }
                                onSaveRecurring(title, selectedBlockId!!, recurringTime, recurrence, notificationsEnabled, finalTargetCount, finalNotes, finalTimerDuration, isPriority)
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
        singleLine = true,
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

private enum class RecurrenceTypeUI { WEEKLY, MONTHLY_DAY, YEARLY, NTH_WEEKDAY }

private val ChipAccent = Color(0xFF1A73E8)

/** Standard chip used for recurrence type selection and similar single-line choice rows. */
@Composable
private fun RecurrenceTypeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ChipAccent else CardBackground)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) ChipAccent else CardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** Single-row weekday selector (Mon..Sun). Reused by Weekly and NthWeekday recurrences. */
@Composable
private fun WeekdayCircleRow(
    days: List<Pair<Int, String>>,
    isSelected: (Int) -> Boolean,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (dayNum, dayLabel) ->
            val selected = isSelected(dayNum)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) ChipAccent else CardBackground)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) ChipAccent else CardBorder,
                        shape = CircleShape
                    )
                    .clickable { onToggle(dayNum) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayLabel,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Numeric input for "every N months". Uses Material3 [OutlinedTextField] with the numeric
 * keyboard — a stock component the user already recognises from form inputs elsewhere.
 */
@Composable
private fun IntervalNumberField(value: Int, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            // Accept up to 2 digits; clamp 1..12. Empty string while user is editing is fine.
            val sanitized = input.filter { it.isDigit() }.take(2)
            text = sanitized
            sanitized.toIntOrNull()?.let { parsed ->
                onChange(parsed.coerceIn(1, 12))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground,
            focusedBorderColor = ChipAccent,
            unfocusedBorderColor = CardBorder,
            cursorColor = ChipAccent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    )
}