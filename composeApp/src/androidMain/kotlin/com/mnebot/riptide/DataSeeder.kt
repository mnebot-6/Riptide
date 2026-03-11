package com.mnebot.riptide

import com.mnebot.riptide.data.local.db.RiptideDatabase
import com.mnebot.riptide.data.local.mapper.toEntity
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.Recurrence
import com.mnebot.riptide.domain.model.WeeklySlot
import com.mnebot.riptide.domain.model.WorkBlock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.*

object DataSeeder {

    suspend fun seedIfEmpty(db: RiptideDatabase, assigner: MarineCategoryAssigner) {
        if (db.workBlockDao().getAll().isNotEmpty()) return

        val today = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())

        val trabajo = WorkBlock(
            id = UUID.randomUUID().toString(),
            name = "Trabajo",
            marineCategories = emptyList(),
            color = "#1A73E8",
            icon = "💼",
            recurrence = Recurrence.Weekly(
                slots = listOf(1, 2, 3, 4, 5).map { day ->
                    WeeklySlot(dayOfWeek = day, startTime = LocalTime(8, 0), endTime = LocalTime(17, 0))
                }
            ),
            isActive = true
        )

        val voleibol = WorkBlock(
            id = UUID.randomUUID().toString(),
            name = "Voleibol",
            marineCategories = emptyList(),
            color = "#E8711A",
            icon = "🏐",
            recurrence = Recurrence.Weekly(
                slots = listOf(2, 4).map { day ->
                    WeeklySlot(dayOfWeek = day, startTime = LocalTime(20, 0), endTime = LocalTime(22, 0))
                }
            ),
            isActive = true
        )

        val salud = WorkBlock(
            id = UUID.randomUUID().toString(),
            name = "Salud",
            marineCategories = emptyList(),
            color = "#34A853",
            icon = "💚",
            recurrence = Recurrence.None,
            isActive = true
        )

        listOf(trabajo, voleibol, salud).forEach { block ->
            db.workBlockDao().insert(block.toEntity())
        }

        listOf(
            DayTask(UUID.randomUUID().toString(), trabajo.id, today, "Revisar correos", 30, false, null, 0),
            DayTask(UUID.randomUUID().toString(), trabajo.id, today, "Reunión de equipo", 60, false, null, 1),
            DayTask(UUID.randomUUID().toString(), trabajo.id, today, "Documentar API", 90, false, null, 2),
            DayTask(UUID.randomUUID().toString(), voleibol.id, today, "Entrenamiento", 120, false, null, 3),
            DayTask(UUID.randomUUID().toString(), salud.id, today, "Salir a caminar", 45, false, null, 4),
        ).forEach { task ->
            db.dayTaskDao().insert(task.toEntity())
        }

        assigner.reassign()
    }
}