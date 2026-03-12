package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

enum class TaskStatus { PENDING, COMPLETED, EXPIRED, POSTPONED }

sealed class TaskSchedule {
    data class OneTime(
        val date: LocalDate,
        val time: LocalTime?
    ) : TaskSchedule()

    data class Recurring(
        val time: LocalTime,
        val recurrence: Recurrence
    ) : TaskSchedule()
}

data class DayTask(
    val id: String,
    val blockId: String?,             // null = tarea suelta
    val title: String,
    val schedule: TaskSchedule,
    val status: TaskStatus,
    val completedAt: LocalDateTime?,
    val postponedTo: LocalDateTime?,  // solo puntuales pospuestas
    val sourceTaskId: String?         // para instancias recurrentes, apunta a la definición
)