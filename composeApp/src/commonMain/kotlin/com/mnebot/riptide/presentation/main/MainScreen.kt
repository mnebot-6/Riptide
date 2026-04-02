package com.mnebot.riptide.presentation.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mnebot.riptide.NightSummaryScheduler
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.presentation.aquarium.AquariumBackground
import com.mnebot.riptide.presentation.aquarium.AquariumCreatures
import com.mnebot.riptide.presentation.components.DatePickerDialogWrapper
import com.mnebot.riptide.presentation.task.PostponeSheet
import com.mnebot.riptide.presentation.task.TaskFormSheet
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.LocalTime
import kotlin.collections.mapKeys
import com.mnebot.riptide.presentation.aquarium.CreatureDetailDialog
import com.mnebot.riptide.presentation.aquarium.CreatureFreezeState
import com.mnebot.riptide.presentation.aquarium.rememberCreatureFreezeState
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import com.mnebot.riptide.presentation.displayNameRes
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanLight = Color(0xFF2E5F9E)
private val CardBackground = Color(0x55FFFFFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)

private fun sortedBlocks(
    blocks: List<WorkBlock>,
    tasksByBlock: Map<String?, List<DayTask>>,
    date: LocalDate
): List<WorkBlock> {
    val dayOfWeek = date.dayOfWeek.isoDayNumber
    return blocks.sortedWith(compareBy(
        { block ->
            // Hora de la tarea no completada más temprana con hora definida
            val earliestTaskTime = tasksByBlock[block.id]
                ?.filter { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.POSTPONED }
                ?.mapNotNull { (it.schedule as? TaskSchedule.OneTime)?.time }
                ?.minOrNull()
                ?.toSecondOfDay()

            if (earliestTaskTime != null) {
                earliestTaskTime
            } else {
                // Fallback: hora del slot del bloque ese día
                val recurrence = block.recurrence
                if (recurrence is Recurrence.Weekly) {
                    val slot = recurrence.slots.firstOrNull { it.dayOfWeek == dayOfWeek }
                    slot?.startTime?.toSecondOfDay() ?: Int.MAX_VALUE
                } else Int.MAX_VALUE
            }
        },
        { it.name }
    ))
}

private fun sortedTasks(tasks: List<DayTask>): List<DayTask> {
    fun taskSortKey(task: DayTask): Pair<Int, Int> {
        val time = (task.schedule as? TaskSchedule.OneTime)?.time
        return if (time != null) Pair(0, time.toSecondOfDay()) else Pair(1, 0)
    }
    val (completed, active) = tasks.partition { it.status == TaskStatus.COMPLETED }
    return active.sortedWith(compareBy({ taskSortKey(it).first }, { taskSortKey(it).second })) +
            completed.sortedWith(compareBy({ taskSortKey(it).first }, { taskSortKey(it).second }))
}

private fun shouldShowBlockTime(block: WorkBlock, date: LocalDate): Boolean {
    val recurrence = block.recurrence
    if (recurrence !is Recurrence.Weekly) return false
    val dayOfWeek = date.dayOfWeek.isoDayNumber
    val slot = recurrence.slots.firstOrNull { it.dayOfWeek == dayOfWeek } ?: return false
    return slot.startTime != null && slot.endTime != null
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    nightSummaryScheduler: NightSummaryScheduler,
    onNavigateToCreateBlock: () -> Unit,
    onNavigateToEditBlock: (String) -> Unit,
    onNavigateToEcosystem: () -> Unit,
    onSetLiveWallpaper: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onSignIn: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val nightSummaryTime by nightSummaryScheduler.getNightSummaryTime()
        .collectAsState(initial = LocalTime(23, 30))
    val morningReminderTime by nightSummaryScheduler.getMorningReminderTime()
        .collectAsState(initial = null)
    var showAquarium by remember { mutableStateOf(false) }
    var showDrawer by remember { mutableStateOf(false) }
    var showTaskSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DayTask?>(null) }
    var postponingTask by remember { mutableStateOf<DayTask?>(null) }
    var contextMenuTask by remember { mutableStateOf<DayTask?>(null) }
    var deletingRecurringTask by remember { mutableStateOf<DayTask?>(null) }
    var editingScopeTask by remember { mutableStateOf<DayTask?>(null) }
    var editingTaskDef by remember { mutableStateOf<RecurringTaskDef?>(null) }
    var quickTaskBlock by remember { mutableStateOf<WorkBlock?>(null) }
    var selectedCreature by remember { mutableStateOf<Pair<MarineCreature, CreatureSpec>?>(null) }
    val creatureFreezeState = rememberCreatureFreezeState()

    val drawerOffsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(showDrawer, uiState.selectedDate) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (!showDrawer && drawerOffsetY.value > 0.3f) {
                                drawerOffsetY.animateTo(1f, animationSpec = tween(250))
                                showDrawer = true
                            } else if (showDrawer && drawerOffsetY.value < 0.7f) {
                                drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                                showDrawer = false
                            } else {
                                drawerOffsetY.animateTo(
                                    if (showDrawer) 1f else 0f,
                                    animationSpec = tween(250)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            drawerOffsetY.animateTo(
                                if (showDrawer) 1f else 0f,
                                animationSpec = tween(250)
                            )
                        }
                    }
                ) { _, dragAmount ->
                    val isVertical = kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)
                    val isHorizontal = kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y)
                    when {
                        isVertical && !showDrawer && dragAmount.y > 0 -> {
                            scope.launch {
                                drawerOffsetY.snapTo(
                                    (drawerOffsetY.value + dragAmount.y / 600f).coerceIn(0f, 1f)
                                )
                            }
                        }
                        isVertical && showDrawer && dragAmount.y < 0 -> {
                            scope.launch {
                                drawerOffsetY.snapTo(
                                    (drawerOffsetY.value + dragAmount.y / 600f).coerceIn(0f, 1f)
                                )
                            }
                        }
                        isHorizontal && !showDrawer && dragAmount.x < -40 -> {
                            scope.launch {
                                viewModel.selectDate(uiState.selectedDate.plus(1, DateTimeUnit.DAY))
                            }
                        }
                        isHorizontal && !showDrawer && dragAmount.x > 40 -> {
                            scope.launch {
                                viewModel.selectDate(uiState.selectedDate.minus(1, DateTimeUnit.DAY))
                            }
                        }
                    }
                }
            }
    ) {
        AquariumBackground()
        AquariumCreatures(
            ecosystemByCategory = uiState.ecosystemByCategory,
            creatureLevelBySpecies = uiState.creatureLevelBySpecies,
            creaturesData = uiState.creaturesData,
            freezeState = creatureFreezeState,
            onCreatureTap = { creature, spec ->
                selectedCreature = creature to spec
            }
        )

        when {
            showAquarium -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    FloatingActionButton(
                        onClick = { showAquarium = false },
                        modifier = Modifier.padding(24.dp),
                        containerColor = CardBackground,
                        contentColor = TextPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) { Icon(painter = painterResource(Res.drawable.ic_x), contentDescription = stringResource(Res.string.a11y_close_aquarium), modifier = Modifier.size(20.dp)) }
                }
            }
            else -> {
                val sorted = sortedBlocks(uiState.blocks, uiState.tasksByBlock, uiState.selectedDate)

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header fijo
                    MainHeader(
                        selectedDate = uiState.selectedDate,
                        today = currentDate(),
                        tasksByBlock = uiState.tasksByBlock,
                        globalStreak = uiState.globalStreak,
                        onCalendarClick = { showDatePicker = true },
                        onTodayClick = { viewModel.selectDate(currentDate()) },
                        onAddTaskClick = { showTaskSheet = true },
                        onDrawerClick = {
                            scope.launch {
                                drawerOffsetY.animateTo(1f, animationSpec = tween(250))
                                showDrawer = true
                            }
                        },
                        onDateSelected = { viewModel.selectDate(it) },
                        onWeekChange = { viewModel.selectDate(it) }
                    )

                    // Contenido scrollable
                    MainContent(
                        blocks = sorted,
                        tasksByBlock = uiState.tasksByBlock,
                        selectedDate = uiState.selectedDate,
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        onTaskToggle = { viewModel.toggleTaskCompleted(it) },
                        onTaskLongPress = { contextMenuTask = it },
                        onBlockHeaderLongPress = { block ->
                            quickTaskBlock = block
                            showTaskSheet = true
                        },
                        streaksByBlock = uiState.streaksByBlock,
                        onAquariumClick = { showAquarium = true }
                    )
                }
            }
        }

        // Menú contextual — BottomSheet
        contextMenuTask?.let { task ->
            Dialog(
                onDismissRequest = { contextMenuTask = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(OceanDeep, OceanMid)),
                                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp, bottom = 32.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x55FFFFFF))
                                    .align(Alignment.CenterHorizontally)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = task.title,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(16.dp))
                            ContextMenuItem(
                                label = stringResource(Res.string.menu_edit),
                                painter = painterResource(Res.drawable.ic_pencil)
                            ) {
                                if (task.sourceTaskId != null) {
                                    editingScopeTask = task
                                } else {
                                    editingTask = task
                                }
                                contextMenuTask = null
                            }
                            if (task.status != TaskStatus.COMPLETED && task.status != TaskStatus.EXPIRED) {
                                ContextMenuItem(
                                    label = stringResource(Res.string.menu_postpone),
                                    painter = painterResource(Res.drawable.ic_clock)
                                ) {
                                    postponingTask = task
                                    contextMenuTask = null
                                }
                            }
                            ContextMenuItem(
                                label = stringResource(Res.string.menu_delete),
                                painter = painterResource(Res.drawable.ic_trash),
                                tint = Color(0xFFEA4335)
                            ) {
                                if (task.sourceTaskId != null) {
                                    deletingRecurringTask = task
                                } else {
                                    viewModel.deleteTask(task)
                                }
                                contextMenuTask = null
                            }
                        }
                    }
                }
            }
        }

        selectedCreature?.let { (creature, spec) ->
            CreatureDetailDialog(
                creature = creature,
                spec = spec,
                onDismiss = {
                    creatureFreezeState.unfreeze(creature.species)
                    selectedCreature = null
                },
                onNicknameChanged = { nickname ->
                    viewModel.updateCreatureNickname(creature.id, nickname)
                }
            )
        }

        // Diálogo scope edición recurrente
        editingScopeTask?.let { task ->
            AlertDialog(
                onDismissRequest = { editingScopeTask = null },
                containerColor = OceanMid,
                title = {
                    Text(stringResource(Res.string.dialog_edit_scope_title), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ContextMenuItem(stringResource(Res.string.dialog_edit_one)) {
                            editingTask = task
                            editingScopeTask = null
                        }
                        ContextMenuItem(stringResource(Res.string.dialog_edit_future)) {
                            // Marcamos con una convención: guardamos en editingTask pero
                            // el onSaveRecurring usará updateRecurringTask
                            editingTask = task.copy(sourceTaskId = task.sourceTaskId + "_future")
                            editingScopeTask = null
                        }
                        ContextMenuItem(stringResource(Res.string.dialog_edit_all)) {
                            editingTask = task.copy(sourceTaskId = task.sourceTaskId + "_all")
                            editingScopeTask = null
                        }
                    }
                },
                confirmButton = {}
            )
        }

        deletingRecurringTask?.let { task ->
            AlertDialog(
                onDismissRequest = { deletingRecurringTask = null },
                containerColor = OceanMid,
                title = {
                    Text(stringResource(Res.string.dialog_delete_scope_title), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ContextMenuItem(stringResource(Res.string.dialog_delete_one)) {
                            viewModel.deleteRecurringTaskInstance(task)
                            deletingRecurringTask = null
                        }
                        ContextMenuItem(stringResource(Res.string.dialog_delete_future)) {
                            viewModel.deleteRecurringTaskFromDate(task)
                            deletingRecurringTask = null
                        }
                        ContextMenuItem(stringResource(Res.string.dialog_delete_all)) {
                            viewModel.deleteRecurringTaskAll(task)
                            deletingRecurringTask = null
                        }
                    }
                },
                confirmButton = {}
            )
        }

        // Resumen nocturno
        uiState.pendingSummary?.let { summary ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSummary() },
                containerColor = OceanMid,
                title = {
                    Text(stringResource(Res.string.title_night_summary), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(summary.feedbackMessage, color = TextPrimary, fontSize = 15.sp)
                        Text(
                            stringResource(Res.string.msg_tasks_completed, summary.tasksCompleted, summary.tasksTotal),
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissSummary() }) {
                        Text(stringResource(Res.string.btn_close), color = Color(0xFF7EC8E3))
                    }
                }
            )
        }

        // ── Lootbox Dialog ────────────────────────────────────────────────────
        val currentLootbox = uiState.pendingLootboxes.firstOrNull()
        val revealedSpecies = uiState.revealedSpecies
        if (currentLootbox != null && uiState.pendingSummary == null) {
            if (revealedSpecies == null) {
                // Estado 1: Lootbox cerrada
                val categoryName = stringResource(currentLootbox.category.displayNameRes())
                AlertDialog(
                    onDismissRequest = {},
                    containerColor = OceanMid,
                    title = {
                        Text(
                            stringResource(Res.string.title_lootbox, categoryName),
                            color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (currentLootbox.directSpecies != null) "✨" else "🎁",
                                fontSize = 56.sp
                            )
                            Text(
                                if (currentLootbox.directSpecies != null)
                                    stringResource(Res.string.msg_decoration_unlock)
                                else
                                    stringResource(Res.string.msg_lootbox_level, currentLootbox.categoryLevel),
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openLootbox()
                        }) {
                            Text(stringResource(Res.string.btn_open_lootbox), color = Color(0xFF7EC8E3))
                        }
                    }
                )
            } else {
                // Estado 2: Especie revelada → pedir nombre (sólo fauna/flora, no decoración)
                var nickname by remember(revealedSpecies) { mutableStateOf("") }
                val isDecoration = revealedSpecies.category == MarineCategory.DECORATION
                val rarityColor = when (revealedSpecies.rarity) {
                    com.mnebot.riptide.domain.model.CreatureRarity.COMMON -> Color(0xFF9E9E9E)
                    com.mnebot.riptide.domain.model.CreatureRarity.UNCOMMON -> Color(0xFF4CAF50)
                    com.mnebot.riptide.domain.model.CreatureRarity.RARE -> Color(0xFF2196F3)
                    com.mnebot.riptide.domain.model.CreatureRarity.EPIC -> Color(0xFF9C27B0)
                    com.mnebot.riptide.domain.model.CreatureRarity.LEGENDARY -> Color(0xFFFF9800)
                }
                AlertDialog(
                    onDismissRequest = {},
                    containerColor = OceanMid,
                    title = {
                        Text(stringResource(Res.string.title_creature_unlock), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(revealedSpecies.emoji, fontSize = 48.sp)
                            Text(
                                stringResource(revealedSpecies.species.displayNameRes()),
                                color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(revealedSpecies.rarity.displayNameRes()),
                                color = rarityColor, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                            if (!isDecoration) {
                                Text(
                                    stringResource(Res.string.msg_creature_name_prompt),
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                OutlinedTextField(
                                    value = nickname,
                                    onValueChange = { nickname = it },
                                    placeholder = { Text(stringResource(Res.string.placeholder_nickname), color = TextSecondary) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = Color(0xFF7EC8E3),
                                        unfocusedBorderColor = TextSecondary
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.confirmUnlock(revealedSpecies, nickname) },
                            enabled = isDecoration || nickname.isNotBlank()
                        ) {
                            Text(stringResource(Res.string.btn_welcome_creature), color = Color(0xFF7EC8E3))
                        }
                    }
                )
            }
        }

        // Drawer
        if (showDrawer || drawerOffsetY.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f * drawerOffsetY.value))
                    .clickable {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = -size.height * (1f - drawerOffsetY.value)
                    }
                    .pointerInput(showDrawer) {
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (drawerOffsetY.value < 0.7f) {
                                        drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                                        showDrawer = false
                                    } else {
                                        drawerOffsetY.animateTo(1f, animationSpec = tween(250))
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    drawerOffsetY.animateTo(
                                        if (showDrawer) 1f else 0f,
                                        animationSpec = tween(250)
                                    )
                                }
                            }
                        ) { _, dragAmount ->
                            val isVertical = kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)
                            if (isVertical && dragAmount.y < 0) {
                                scope.launch {
                                    drawerOffsetY.snapTo(
                                        (drawerOffsetY.value + dragAmount.y / 600f).coerceIn(0f, 1f)
                                    )
                                }
                            }
                        }
                    }
            ) {
                MainDrawer(
                    blocks = uiState.blocks,
                    nightSummaryTime = nightSummaryTime,
                    morningReminderTime = morningReminderTime,
                    onAddBlock = {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        onNavigateToCreateBlock()
                    },
                    onEditBlock = { blockId ->
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        onNavigateToEditBlock(blockId)
                    },
                    onNightSummaryTimeChanged = { time ->
                        nightSummaryScheduler.scheduleWorker(time)
                        viewModel.updateNightSummaryTime(time)
                    },
                    onMorningReminderTimeChanged = { time ->
                        scope.launch { nightSummaryScheduler.setMorningReminderTime(time) }
                        nightSummaryScheduler.scheduleMorningReminder(time)
                    },
                    onNavigateToEcosystem = {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        onNavigateToEcosystem()
                    },
                    onSetLiveWallpaper = {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        onSetLiveWallpaper()
                    },
                    onNavigateToStats = {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        onNavigateToStats()
                    },
                    onNavigateToHistory = {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        onNavigateToHistory()
                    },
                    loggedInUser = uiState.loggedInUser,
                    syncStatus = uiState.syncStatus,
                    onSignIn = {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        onSignIn()
                    },
                    onSignOut = {
                        scope.launch {
                            drawerOffsetY.animateTo(0f, animationSpec = tween(250))
                            showDrawer = false
                        }
                        viewModel.signOut()
                    },
                    onSyncNow = { viewModel.syncNow() }
                )
            }
        }

        // DatePicker para ir a día concreto
        if (showDatePicker) {
            DatePickerDialogWrapper(
                initial = uiState.selectedDate,
                onConfirm = { date ->
                    if (date != null) viewModel.selectDate(date)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        // TaskFormSheet — nueva tarea desde header
        if (showTaskSheet) {
            Dialog(
                onDismissRequest = { showTaskSheet = false; quickTaskBlock = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    TaskFormSheet(
                        blocks = uiState.blocks,
                        initialDate = uiState.selectedDate,
                        initialBlockId = quickTaskBlock?.id,
                        onSaveOneTime = { title, blockId, date, time, notificationsEnabled ->
                            viewModel.addOneTimeTask(title, blockId, date, time, notificationsEnabled)
                            showTaskSheet = false
                            quickTaskBlock = null
                        },
                        onSaveRecurring = { title, blockId, time, recurrence, notificationsEnabled ->
                            viewModel.addRecurringTask(title, blockId, time, recurrence, notificationsEnabled)
                            showTaskSheet = false
                            quickTaskBlock = null
                        },
                        onDismiss = { showTaskSheet = false; quickTaskBlock = null }
                    )
                }
            }
        }

        // TaskFormSheet — editar tarea existente
        editingTask?.let { task ->
            val isFutureEdit = task.sourceTaskId?.endsWith("_future") == true
            val isAllEdit = task.sourceTaskId?.endsWith("_all") == true
            val isRecurringEdit = isFutureEdit || isAllEdit
            val realSourceId = task.sourceTaskId
                ?.removeSuffix("_future")
                ?.removeSuffix("_all")
            val taskForForm = if (isRecurringEdit) task.copy(sourceTaskId = realSourceId) else task

            // Cargar def si es edición recurrente y aún no la tenemos
            LaunchedEffect(task.id) {
                editingTaskDef = realSourceId?.let { viewModel.getRecurringTaskDef(it) }
            }

            Dialog(
                onDismissRequest = { editingTask = null; editingTaskDef = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    TaskFormSheet(
                        blocks = uiState.blocks,
                        initialDate = uiState.selectedDate,
                        existingTask = taskForForm,
                        existingDef = if (isRecurringEdit) editingTaskDef else null,
                        forceRecurring = isRecurringEdit,
                        onSaveOneTime = { title, blockId, date, time, notificationsEnabled ->
                            viewModel.updateOneTimeTask(taskForForm, title, blockId, date, time, notificationsEnabled)
                            editingTask = null
                            editingTaskDef = null
                        },
                        onSaveRecurring = { title, blockId, time, recurrence, notificationsEnabled ->
                            val sourceId = realSourceId ?: return@TaskFormSheet
                            viewModel.updateRecurringTask(sourceId, title, blockId, time, recurrence, notificationsEnabled)
                            editingTask = null
                            editingTaskDef = null
                        },
                        onDelete = {
                            if (taskForForm.sourceTaskId != null) {
                                deletingRecurringTask = taskForForm
                            } else {
                                viewModel.deleteTask(taskForForm)
                            }
                            editingTask = null
                            editingTaskDef = null
                        },
                        onDismiss = { editingTask = null; editingTaskDef = null }
                    )
                }
            }
        }

        // PostponeSheet
        postponingTask?.let { task ->
            Dialog(
                onDismissRequest = { postponingTask = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    PostponeSheet(
                        task = task,
                        onPostpone = { date, time ->
                            viewModel.postponeTask(task, date, time)
                            postponingTask = null
                        },
                        onDismiss = { postponingTask = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainHeader(
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByBlock: Map<String?, List<DayTask>>,
    globalStreak: Int = 0,
    onCalendarClick: () -> Unit,
    onTodayClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onDrawerClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onWeekChange: (LocalDate) -> Unit
) {
    val allTasksToday = remember(tasksByBlock) { tasksByBlock.values.flatten() }
    val tasksByDate = remember(allTasksToday) {
        allTasksToday
            .groupBy { task ->
                when (val schedule = task.schedule) {
                    is TaskSchedule.OneTime -> schedule.date
                    is TaskSchedule.Recurring -> null
                }
            }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }
    val totalTasks = allTasksToday.size
    val completedTasks = allTasksToday.count { it.status == TaskStatus.COMPLETED }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Fila superior: nombre + acciones
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_riptide_logo),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    stringResource(Res.string.app_name),
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                // Progreso del día (X/Y hechas)
                /* if (totalTasks > 0) {
                    val allDone = completedTasks == totalTasks
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (allDone) Color(0x331A73E8) else CardBackground)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.msg_day_progress, completedTasks, totalTasks),
                            color = if (allDone) Color(0xFF7EC8E3) else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                } */
            }

            // Volver a hoy (solo visible si no estamos en hoy)
            if (selectedDate != today) {
                HeaderIconButton(painter = painterResource(Res.drawable.ic_history), contentDescription = stringResource(Res.string.a11y_go_to_today), onClick = onTodayClick)
                Spacer(modifier = Modifier.width(4.dp))
            } else // Racha global (solo visible si >= 2 días consecutivos)
            if (globalStreak >= 2) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_flame),
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$globalStreak",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            HeaderIconButton(painter = painterResource(Res.drawable.ic_calendar), contentDescription = stringResource(Res.string.a11y_open_calendar), onClick = onCalendarClick)
            Spacer(modifier = Modifier.width(4.dp))
            HeaderIconButton(painter = painterResource(Res.drawable.ic_plus), contentDescription = stringResource(Res.string.a11y_add_task), onClick = onAddTaskClick)
            Spacer(modifier = Modifier.width(4.dp))
            HeaderIconButton(painter = painterResource(Res.drawable.ic_waves), contentDescription = stringResource(Res.string.a11y_open_menu), onClick = onDrawerClick)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // WeekCalendar
        WeekCalendar(
            selectedDate = selectedDate,
            today = today,
            tasksByDate = tasksByDate,
            onDateSelected = onDateSelected,
            onWeekChange = onWeekChange
        )
    }
}

@Composable
private fun HeaderIconButton(painter: Painter, contentDescription: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CardBackground)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MainContent(
    blocks: List<WorkBlock>,
    tasksByBlock: Map<String?, List<DayTask>>,
    selectedDate: LocalDate,
    isLoading: Boolean,
    error: String?,
    onTaskToggle: (DayTask) -> Unit,
    onTaskLongPress: (DayTask) -> Unit,
    onBlockHeaderLongPress: (WorkBlock) -> Unit,
    onAquariumClick: () -> Unit,
    streaksByBlock: Map<String, Int>,
) {
    val blocksWithTasks = remember(blocks, tasksByBlock) {
        blocks.filter { tasksByBlock[it.id]?.isNotEmpty() == true }
    }
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Color(0x33FFFFFF)))

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = TextPrimary)
            }
            error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(Res.string.msg_error, error), color = TextPrimary)
            }
            blocksWithTasks.isEmpty() && tasksByBlock[null].isNullOrEmpty() -> Box(
                Modifier.fillMaxSize(), Alignment.Center
            ) {
                Text(stringResource(Res.string.msg_no_tasks), color = TextSecondary, fontSize = 16.sp)
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val unassignedTasks = sortedTasks(tasksByBlock[null] ?: emptyList())
                if (unassignedTasks.isNotEmpty()) {
                    item {
                        UnassignedSection(
                            tasks = unassignedTasks,
                            onTaskToggle = onTaskToggle,
                            onTaskLongPress = onTaskLongPress
                        )
                    }
                }
                items(blocksWithTasks, key = { it.id }) { block ->
                    val tasks = sortedTasks(tasksByBlock[block.id] ?: emptyList())
                    val streak = streaksByBlock[block.id] ?: 0
                    BlockSection(
                        block = block,
                        tasks = tasks,
                        selectedDate = selectedDate,
                        streak = streak,
                        onTaskToggle = onTaskToggle,
                        onTaskLongPress = onTaskLongPress,
                        onHeaderLongPress = onBlockHeaderLongPress
                    )
                }
                item {
                    Text(
                        text = stringResource(Res.string.hint_long_press),
                        color = Color(0x44FFFFFF),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAquariumClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = CardBackground,
            contentColor = TextPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) { Icon(painter = painterResource(Res.drawable.ic_fish), contentDescription = stringResource(Res.string.a11y_view_aquarium), modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun ContextMenuItem(
    label: String,
    painter: Painter? = null,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(text = label, color = tint, fontSize = 15.sp)
    }
}

@Composable
private fun BlockSection(
    block: WorkBlock,
    tasks: List<DayTask>,
    selectedDate: LocalDate,
    streak: Int,
    onTaskToggle: (DayTask) -> Unit,
    onTaskLongPress: (DayTask) -> Unit,
    onHeaderLongPress: (WorkBlock) -> Unit
) {
    Column {
        BlockHeader(
            block = block,
            selectedDate = selectedDate,
            streak = streak,
            onLongPress = { onHeaderLongPress(block) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        tasks.forEach { task ->
            TaskCard(
                task = task,
                blockColor = parseColor(block.color),
                onToggle = { onTaskToggle(task) },
                onLongPress = { onTaskLongPress(task) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlockHeader(
    block: WorkBlock,
    selectedDate: LocalDate,
    streak: Int,
    onLongPress: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(parseColor(block.color).copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) { Text(block.icon, fontSize = 18.sp) }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(block.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (shouldShowBlockTime(block, selectedDate)) {
                val recurrence = block.recurrence as Recurrence.Weekly
                val slot = recurrence.slots.first { it.dayOfWeek == selectedDate.dayOfWeek.isoDayNumber }
                Text("${slot.startTime} - ${slot.endTime}", color = TextSecondary, fontSize = 12.sp)
            }
            if (streak >= 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_flame),
                        contentDescription = null,
                        tint = Color(0xFFFFB347),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = stringResource(Res.string.msg_streak, streak),
                        color = Color(0xFFFFB347),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(
    task: DayTask,
    blockColor: Color,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isCompleted = task.status == TaskStatus.COMPLETED
    val isExpired = task.status == TaskStatus.EXPIRED
    val isPostponed = task.status == TaskStatus.POSTPONED
    val taskTime = (task.schedule as? TaskSchedule.OneTime)?.time

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isExpired) Color(0x66FFFFFF) else blockColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                color = when {
                    isCompleted || isExpired || isPostponed -> TextSecondary
                    else -> TextPrimary
                },
                fontSize = 14.sp,
                fontWeight = if (isCompleted || isExpired || isPostponed) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
            if (taskTime != null) {
                Text(
                    text = "${taskTime.hour.toString().padStart(2, '0')}:${
                        taskTime.minute.toString().padStart(2, '0')
                    }",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            if (isPostponed && task.postponedTo != null) {
                Text(
                    text = "→ ${task.postponedTo.date} ${
                        task.postponedTo.time.hour.toString().padStart(2, '0')
                    }:${task.postponedTo.time.minute.toString().padStart(2, '0')}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        when {
            isPostponed -> Icon(
                painter = painterResource(Res.drawable.ic_clock),
                contentDescription = stringResource(Res.string.a11y_task_postponed),
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExpired) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_clock),
                            contentDescription = stringResource(Res.string.a11y_task_expired),
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle()
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = blockColor,
                            uncheckedColor = TextSecondary,
                            checkmarkColor = TextPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun UnassignedSection(
    tasks: List<DayTask>,
    onTaskToggle: (DayTask) -> Unit,
    onTaskLongPress: (DayTask) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_box),
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(stringResource(Res.string.section_no_block), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        tasks.forEach { task ->
            TaskCard(
                task = task,
                blockColor = Color(0x66FFFFFF),
                onToggle = { onTaskToggle(task) },
                onLongPress = { onTaskLongPress(task) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

expect fun parseColor(hex: String): Color