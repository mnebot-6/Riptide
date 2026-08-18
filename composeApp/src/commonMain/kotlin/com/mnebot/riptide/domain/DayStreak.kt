package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.NightSummaryProcessor.Companion.DAY_ACHIEVED_THRESHOLD
import com.mnebot.riptide.domain.model.DaySummary
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Días de racha que otorgan una lootbox bonus. */
val STREAK_MILESTONES = listOf(7, 14, 30)

/**
 * La única racha de la app, derivada de los `DaySummary`. No se persiste nada:
 * si los resúmenes están bien, la racha está bien.
 *
 * Reglas:
 * - Día conseguido = `score >= 0.80`.
 * - Día neutral (`tasksTotal == 0`) se salta: ni suma ni rompe.
 * - Día sin resumen corta: ese día aún no se ha cerrado.
 */
object DayStreak {

    /** Racha viva contando hacia atrás desde [upTo] incluido (normalmente, ayer). */
    fun currentFrom(summaries: List<DaySummary>, upTo: LocalDate): Int {
        val byDate = summaries.associateBy { it.date }
        var date = upTo
        var streak = 0
        while (true) {
            val summary = byDate[date] ?: break
            if (summary.tasksTotal > 0) {
                if (summary.score < DAY_ACHIEVED_THRESHOLD) break
                streak++
            }
            date = date.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    /** Racha más larga alcanzada en [summaries]. */
    fun longest(summaries: List<DaySummary>): Int {
        if (summaries.isEmpty()) return 0
        val sorted = summaries.sortedBy { it.date }
        var best = 0
        var current = 0
        var previousDate: LocalDate? = null
        for (summary in sorted) {
            val isContiguous = previousDate == null ||
                summary.date.toEpochDays() - previousDate.toEpochDays() == 1L
            if (!isContiguous) current = 0
            previousDate = summary.date

            if (summary.tasksTotal == 0) continue          // neutral: no rompe ni suma
            if (summary.score >= DAY_ACHIEVED_THRESHOLD) {
                current++
                if (current > best) best = current
            } else {
                current = 0
            }
        }
        return best
    }
}
