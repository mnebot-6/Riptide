package com.mnebot.riptide

import com.mnebot.riptide.data.local.db.RiptideDatabase
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.data.repository.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

object DataSeeder {
    suspend fun seedIfEmpty(db: RiptideDatabase, assigner: MarineCategoryAssigner) {
        val workBlockRepo = WorkBlockRepositoryImpl(db.workBlockDao())
        val blockCategoryRepo = BlockCategoryRepositoryImpl(db.blockCategoryDao())
        val dayTaskRepo = DayTaskRepositoryImpl(db.dayTaskDao())

        if (workBlockRepo.getAll().isNotEmpty()) return

        val today = kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        val blockTrabajo = WorkBlock(
            id = generateUUID(),
            name = "Trabajo",
            marineCategories = emptyList(),
            color = "#1A73E8",
            icon = "💼",
            recurrence = Recurrence.Weekly(
                slots = listOf(1, 2, 3, 4, 5).map {
                    WeeklySlot(it, LocalTime(8, 0), LocalTime(17, 0))
                }
            ),
            isActive = true
        )
        val blockVoleibol = WorkBlock(
            id = generateUUID(),
            name = "Voleibol",
            marineCategories = emptyList(),
            color = "#E8711A",
            icon = "🏐",
            recurrence = Recurrence.Weekly(
                slots = listOf(2, 4).map {
                    WeeklySlot(it, LocalTime(20, 0), LocalTime(22, 0))
                }
            ),
            isActive = true
        )
        val blockSalud = WorkBlock(
            id = generateUUID(),
            name = "Salud",
            marineCategories = emptyList(),
            color = "#34A853",
            icon = "💚",
            recurrence = Recurrence.None,
            isActive = true
        )

        workBlockRepo.insert(blockTrabajo)
        workBlockRepo.insert(blockVoleibol)
        workBlockRepo.insert(blockSalud)

        assigner.reassign()

        val tasks = listOf(
            DayTask(generateUUID(), blockTrabajo.id, "Revisar PRs", TaskSchedule.OneTime(today, LocalTime(9, 0)), TaskStatus.PENDING, null, null, null),
            DayTask(generateUUID(), blockTrabajo.id, "Daily con el equipo", TaskSchedule.OneTime(today, LocalTime(10, 0)), TaskStatus.PENDING, null, null, null),
            DayTask(generateUUID(), blockTrabajo.id, "Documentar endpoint", TaskSchedule.OneTime(today, null), TaskStatus.PENDING, null, null, null),
            DayTask(generateUUID(), blockVoleibol.id, "Estirar después", TaskSchedule.OneTime(today, LocalTime(22, 0)), TaskStatus.PENDING, null, null, null),
            DayTask(generateUUID(), blockSalud.id, "Beber 2L de agua", TaskSchedule.OneTime(today, null), TaskStatus.PENDING, null, null, null)
        )
        tasks.forEach { dayTaskRepo.insert(it) }
    }
}