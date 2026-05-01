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
                        sourceTaskId = def.id,
                        notificationsEnabled = def.notificationsEnabled,
                        targetCount = def.targetCount,
                        currentCount = 0,
                        notes = def.noteTemplate,
                        timerDurationMinutes = def.timerDurationMinutes,
                        isPriority = def.isPriority
                    )
                )
            }
        }
    }

    internal fun shouldGenerateForDate(def: RecurringTaskDef, date: LocalDate): Boolean {
        return when (val r = def.recurrence) {
            is Recurrence.None -> false
            is Recurrence.Weekly -> {
                val dayOfWeek = date.dayOfWeek.isoDayNumber // 1=Mon..7=Sun
                r.slots.any { it.dayOfWeek == dayOfWeek }
            }
            is Recurrence.Yearly -> (date.month.ordinal + 1) == r.month && date.day == r.day
            is Recurrence.MonthlyDay -> {
                if (date.day != r.day) false
                else {
                    val interval = r.intervalMonths.coerceAtLeast(1)
                    interval == 1 || ((date.month.ordinal + 1) - 1) % interval == 0
                }
            }
            is Recurrence.NthWeekdayOfMonth -> {
                val isoDow = date.dayOfWeek.isoDayNumber
                if (isoDow != r.dayOfWeek) false
                else {
                    val occurrence = (date.day - 1) / 7 + 1
                    val matchesNth = if (r.nth == 5) {
                        // Last occurrence: next week's same weekday is in next month
                        val nextWeek = date.plus(DatePeriod(days = 7))
                        nextWeek.month != date.month
                    } else occurrence == r.nth
                    if (!matchesNth) false
                    else {
                        val interval = r.intervalMonths.coerceAtLeast(1)
                        interval == 1 || ((date.month.ordinal + 1) - 1) % interval == 0
                    }
                }
            }
        }
    }
}

private fun LocalDate.plusDays(days: Int): LocalDate {
    var result = this
    repeat(days) { result = result.plus(DatePeriod(days = 1)) }
    return result
}