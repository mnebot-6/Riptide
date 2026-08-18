package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * Tres estados, sin solapes:
 * - PENDING: aún se puede hacer (solo hoy y días futuros)
 * - COMPLETED: hecha
 * - EXPIRED: el día se cerró sin completarla
 *
 * Posponer mueve la fecha de la propia tarea; borrar usa el soft delete de sync.
 */
enum class TaskStatus { PENDING, COMPLETED, EXPIRED }

/**
 * Toda instancia vive en un día concreto: la recurrencia pertenece a la definición
 * ([RecurringTaskDef]), no a la instancia. Antes había un caso `Recurring` que nunca
 * se llegaba a escribir en base de datos.
 */
sealed class TaskSchedule {
    data class OneTime(
        val date: LocalDate,
        val time: LocalTime?
    ) : TaskSchedule()
}

data class DayTask(
    val id: String,
    val blockId: String?,
    val title: String,
    val schedule: TaskSchedule,
    val status: TaskStatus,
    val completedAt: LocalDateTime?,
    val sourceTaskId: String?,
    val hasBeenRewarded: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val targetCount: Int? = null,
    val currentCount: Int = 0,
    val notes: String? = null,
    val timerDurationMinutes: Int? = null,
    val isPriority: Boolean = false
) {
    val isCountable: Boolean get() = targetCount != null && targetCount > 0
}
