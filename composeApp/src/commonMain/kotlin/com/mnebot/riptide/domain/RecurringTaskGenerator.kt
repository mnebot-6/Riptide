package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.RecurringTaskDefRepository
import com.mnebot.riptide.generateUUID
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

class RecurringTaskGenerator(
    private val recurringTaskDefRepository: RecurringTaskDefRepository,
    private val dayTaskRepository: DayTaskRepository
) {
    // Genera instancias para los próximos `daysAhead` días si no existen ya
    suspend fun generateUpTo(from: LocalDate, daysAhead: Int = 7) {
        val defs = recurringTaskDefRepository.getAll().filter { it.isActive }

        for (i in 0..daysAhead) {
            val date = from.plusDays(i)
            val existingTasks = dayTaskRepository.getByDate(date)
            val existingSourceIds = existingTasks.mapNotNull { it.sourceTaskId }.toSet()

            for (def in defs) {
                if (def.id in existingSourceIds) continue
                if (!shouldGenerateForDate(def, date)) continue

                dayTaskRepository.insert(
                    DayTask(
                        id = generateUUID(),
                        blockId = def.blockId,
                        title = def.title,
                        schedule = TaskSchedule.OneTime(
                            date = date,
                            time = def.time       // ya es LocalTime?, OneTime lo acepta
                        ),
                        status = TaskStatus.PENDING,
                        completedAt = null,
                        postponedTo = null,
                        sourceTaskId = def.id
                    )
                )
            }
        }
    }

    private fun shouldGenerateForDate(def: RecurringTaskDef, date: LocalDate): Boolean {
        return when (def.recurrence) {
            is Recurrence.None -> false
            is Recurrence.Weekly -> {
                val dayOfWeek = date.dayOfWeek.isoDayNumber // 1=Lunes, 7=Domingo
                def.recurrence.slots.any { it.dayOfWeek == dayOfWeek }
            }
        }
    }
}

private fun LocalDate.plusDays(days: Int): LocalDate {
    var result = this
    repeat(days) { result = result.plus(DatePeriod(days = 1)) }
    return result
}