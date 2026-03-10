package com.mnebot.riptide

import com.mnebot.riptide.data.local.db.RiptideDatabase
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.data.repository.WorkBlockRepositoryImpl
import com.mnebot.riptide.domain.model.*
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock.System
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
suspend fun seedDatabaseIfEmpty(database: RiptideDatabase) {
    val workBlockRepository = WorkBlockRepositoryImpl(database.workBlockDao())
    val dayTaskRepository = DayTaskRepositoryImpl(database.dayTaskDao())

    if (workBlockRepository.getAll().isNotEmpty()) return

    val today = System.todayIn(TimeZone.currentSystemDefault())

    val blockTrabajo = WorkBlock(
        id = Uuid.random().toString(),
        name = "Trabajo",
        marineCategory = MarineCategory.FISH,
        color = "#1A73E8",
        recurrence = Recurrence.Weekly(
            slots = listOf(
                WeeklySlot(1, LocalTime(8, 0), LocalTime(17, 0)),
                WeeklySlot(2, LocalTime(8, 0), LocalTime(17, 0)),
                WeeklySlot(3, LocalTime(8, 0), LocalTime(17, 0)),
                WeeklySlot(4, LocalTime(8, 0), LocalTime(17, 0)),
                WeeklySlot(5, LocalTime(8, 0), LocalTime(17, 0))
            )
        ),
        icon = "💼",
        isActive = true
    )

    val blockVoleibol = WorkBlock(
        id = Uuid.random().toString(),
        name = "Voleibol",
        marineCategory = MarineCategory.CRUSTACEAN,
        color = "#E8711A",
        recurrence = Recurrence.Weekly(
            slots = listOf(
                WeeklySlot(2, LocalTime(20, 0), LocalTime(22, 0)),
                WeeklySlot(4, LocalTime(20, 0), LocalTime(22, 0))
            )
        ),
        icon = "🏐",
        isActive = true
    )

    val blockSalud = WorkBlock(
        id = Uuid.random().toString(),
        name = "Salud",
        marineCategory = MarineCategory.FLORA,
        color = "#34A853",
        recurrence = Recurrence.None,
        icon = "💚",
        isActive = true
    )

    workBlockRepository.insert(blockTrabajo)
    workBlockRepository.insert(blockVoleibol)
    workBlockRepository.insert(blockSalud)

    listOf(
        DayTask(Uuid.random().toString(), blockTrabajo.id, today, "Revisar PR módulo NFC", 30, false, null, 0),
        DayTask(Uuid.random().toString(), blockTrabajo.id, today, "Daily con el equipo", 15, true, null, 1),
        DayTask(Uuid.random().toString(), blockTrabajo.id, today, "Documentar endpoint", 45, false, null, 2),
        DayTask(Uuid.random().toString(), blockVoleibol.id, today, "Estirar después", 10, false, null, 0),
        DayTask(Uuid.random().toString(), blockSalud.id, today, "Beber 2L de agua", null, false, null, 0)
    ).forEach { dayTaskRepository.insert(it) }
}