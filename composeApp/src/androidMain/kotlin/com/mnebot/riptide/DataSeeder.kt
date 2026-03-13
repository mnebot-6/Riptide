package com.mnebot.riptide

import com.mnebot.riptide.data.local.db.RiptideDatabase
import com.mnebot.riptide.data.repository.BlockCategoryRepositoryImpl
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.data.repository.WorkBlockRepositoryImpl
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.model.*
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DataSeeder {
    suspend fun seedIfEmpty(db: RiptideDatabase, assigner: MarineCategoryAssigner) {
        val workBlockRepo = WorkBlockRepositoryImpl(db.workBlockDao())
        val dayTaskRepo = DayTaskRepositoryImpl(db.dayTaskDao())

        if (workBlockRepo.getAll().isNotEmpty()) return

        val now = kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val today = now.date

        val blockTrabajo = WorkBlock(
            id = generateUUID(),
            name = "Trabajo",
            marineCategories = emptyList(),
            color = "#1A73E8",
            icon = "💼",
            recurrence = Recurrence.Weekly(
                slots = listOf(1, 2, 3, 4, 5).map {
                    WeeklySlot(it, LocalTime(9, 0), LocalTime(18, 0))
                }
            ),
            isActive = true
        )
        val blockPersonal = WorkBlock(
            id = generateUUID(),
            name = "Personal",
            marineCategories = emptyList(),
            color = "#9C27B0",
            icon = "🌱",
            recurrence = Recurrence.None,
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
        workBlockRepo.insert(blockPersonal)
        workBlockRepo.insert(blockSalud)

        assigner.reassign()

        val tasks = listOf(
            DayTask(generateUUID(), blockTrabajo.id, "Revisar correos",
                TaskSchedule.OneTime(today, LocalTime(9, 0)), TaskStatus.PENDING, null, null, null),
            DayTask(generateUUID(), blockTrabajo.id, "Tarea importante del día",
                TaskSchedule.OneTime(today, null), TaskStatus.PENDING, null, null, null),
            DayTask(generateUUID(), blockPersonal.id, "Algo para ti hoy",
                TaskSchedule.OneTime(today, null), TaskStatus.PENDING, null, null, null),
            DayTask(generateUUID(), blockSalud.id, "Beber 2L de agua",
                TaskSchedule.OneTime(today, null), TaskStatus.PENDING, null, null, null),
        )
        tasks.forEach { dayTaskRepo.insert(it) }
    }
}