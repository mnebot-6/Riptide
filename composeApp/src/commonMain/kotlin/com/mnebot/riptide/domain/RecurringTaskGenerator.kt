package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.*
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.RecurringTaskDefRepository
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
            val existingSourceIds = dayTaskRepository.getSourceIdsForDate(date).toSet()

            for (def in defs) {
                if (def.id in existingSourceIds) continue
                if (!shouldGenerateForDate(def, date)) continue

                dayTaskRepository.insert(
                    DayTask(
                        id = instanceId(def.id, date),
                        blockId = def.blockId,
                        title = def.title,
                        schedule = TaskSchedule.OneTime(
                            date = date,
                            time = def.time       // ya es LocalTime?, OneTime lo acepta
                        ),
                        status = TaskStatus.PENDING,
                        completedAt = null,

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

    /**
     * Propaga un cambio de definición a las instancias PENDING de [from] en adelante,
     * conservando lo que el usuario escribió en cada una (notas y progreso contable).
     * Las instancias que ya no encajan con la nueva recurrencia se borran; las que
     * faltan las crea [generateUpTo].
     */
    suspend fun applyDefinitionChange(
        def: RecurringTaskDef,
        from: LocalDate,
        daysAhead: Int = 7
    ) {
        val instances = dayTaskRepository.getBySourceTask(def.id)
            .filter { it.status == TaskStatus.PENDING }
            .filter { (it.schedule as? TaskSchedule.OneTime)?.date?.let { d -> d >= from } == true }

        for (task in instances) {
            val date = (task.schedule as TaskSchedule.OneTime).date
            if (!def.isActive || !shouldGenerateForDate(def, date)) {
                dayTaskRepository.delete(task.id)
                continue
            }
            dayTaskRepository.update(
                task.copy(
                    blockId = def.blockId,
                    title = def.title,
                    schedule = TaskSchedule.OneTime(date = date, time = def.time),
                    notificationsEnabled = def.notificationsEnabled,
                    targetCount = def.targetCount,
                    timerDurationMinutes = def.timerDurationMinutes,
                    isPriority = def.isPriority
                    // notes y currentCount son del usuario: no se tocan
                )
            )
        }

        if (def.isActive) generateUpTo(from, daysAhead)
    }

    /**
     * Id determinista: la misma definición y el mismo día producen siempre el mismo id.
     * Con un UUID aleatorio, dos móviles (o una reinstalación que genera antes de bajarse
     * el servidor) creaban dos filas distintas para la misma tarea y el sync las mostraba
     * duplicadas. Con esto el upsert las colapsa en una sola.
     *
     * Tiene forma de UUID porque el backend valida `id` como UUID (y la columna es
     * varchar(36)): un "$defId:$date" en claro tumbaba el sync entero.
     */
    internal fun instanceId(defId: String, date: LocalDate): String {
        val name = "$defId:$date".encodeToByteArray()
        // FNV-1a de 64 bits con dos semillas → 128 bits, idénticos en Android e iOS.
        fun fnv(seed: Long) = name.fold(seed) { h, b -> (h xor (b.toLong() and 0xff)) * 0x100000001b3L }
        val hex = listOf(fnv(-0x340d631b7bdddcdbL), fnv(0x5bd1e9955bd1e995L))
            .joinToString("") { it.toULong().toString(16).padStart(16, '0') }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
            "${hex.substring(16, 20)}-${hex.substring(20)}"
    }

    internal fun shouldGenerateForDate(def: RecurringTaskDef, date: LocalDate): Boolean {
        return when (val r = def.recurrence) {
            is Recurrence.None -> false
            is Recurrence.Weekly -> {
                val dayOfWeek = date.dayOfWeek.isoDayNumber // 1=Mon..7=Sun
                r.slots.any { it.dayOfWeek == dayOfWeek }
            }
            is Recurrence.Yearly -> (date.month.ordinal + 1) == r.month && date.day == r.day
            is Recurrence.MonthlyDay -> date.day == r.day
        }
    }
}

private fun LocalDate.plusDays(days: Int): LocalDate {
    var result = this
    repeat(days) { result = result.plus(DatePeriod(days = 1)) }
    return result
}