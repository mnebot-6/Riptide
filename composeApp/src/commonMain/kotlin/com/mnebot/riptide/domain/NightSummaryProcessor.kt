package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import com.mnebot.riptide.domain.model.PendingLootbox
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class NightSummaryProcessor(
    private val dayTaskRepository: DayTaskRepository,
    private val daySummaryRepository: DaySummaryRepository,
    private val ecosystemProcessor: EcosystemProcessor? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val decorationUnlockChecker: DecorationUnlockChecker? = null,
) {
    /**
     * Cierra [date]. Todas las tareas del día cuentan, tengan hora o no: el cierre
     * ocurre a las 23:59:59, así que no queda tiempo por delante.
     *
     * Un día sin tareas se guarda igualmente con `tasksTotal = 0` (día neutral):
     * no suma ni rompe racha, pero deja constancia de que el día fue procesado.
     */
    suspend fun processDay(
        date: LocalDate,
        blockCategories: Map<String, List<MarineCategory>> = emptyMap()
    ) {
        if (daySummaryRepository.getByDate(date) != null) return

        val tasks = dayTaskRepository.getByDate(date)

        // Lo que quedó pendiente al cerrar el día, expira
        tasks.filter { it.status == TaskStatus.PENDING }.forEach { task ->
            dayTaskRepository.updateStatus(task.id, TaskStatus.EXPIRED)
        }

        val total = tasks.size
        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
        val score = if (total == 0) 0f else completed.toFloat() / total

        // La racha se deriva de los resúmenes: se calcula antes y después de cerrar hoy
        val history = daySummaryRepository.getRange(date.minus(STREAK_LOOKBACK_DAYS, DateTimeUnit.DAY), date)
        val streakBefore = DayStreak.currentFrom(history, date.minus(1, DateTimeUnit.DAY))

        val summary = DaySummary(
            id = generateUUID(),
            date = date,
            score = score,
            tasksTotal = total,
            tasksCompleted = completed,
            feedbackMessage = ""      // se rellena abajo, ya con la racha calculada
        )
        val streakAfter = DayStreak.currentFrom(history + summary, date)

        daySummaryRepository.insert(
            summary.copy(feedbackMessage = buildMessage(score, total, completed, streakAfter))
        )

        // Día neutral: nada que recompensar
        if (total == 0) return

        val allCategories = tasks
            .mapNotNull { it.blockId }
            .flatMap { blockId -> blockCategories[blockId] ?: emptyList() }
            .distinct()

        // Bonus discreto por bloque terminado del todo: incentivo sin estado que mantener
        val fullBlocks = tasks
            .filter { it.blockId != null }
            .groupBy { it.blockId }
            .count { (_, blockTasks) -> blockTasks.all { it.status == TaskStatus.COMPLETED } }

        val ecosystemLootboxes: List<PendingLootbox> =
            ecosystemProcessor?.addNightBonus(score, streakAfter, fullBlocks, allCategories) ?: emptyList()

        // Lootbox por hito de racha. La racha crece de uno en uno, así que basta con
        // mirar si el valor de hoy es exactamente un hito.
        val streakLootboxes: List<PendingLootbox> =
            if (streakAfter > streakBefore && streakAfter in STREAK_MILESTONES) {
                allCategories.firstOrNull()
                    ?.let { listOf(PendingLootbox(it, streakAfter)) }
                    ?: emptyList()
            } else emptyList()

        val newLootboxes = ecosystemLootboxes + streakLootboxes

        if (newLootboxes.isNotEmpty() && userPreferencesRepository != null) {
            val existing = userPreferencesRepository.getPendingLootboxes()
            userPreferencesRepository.setPendingLootboxes(existing + newLootboxes)
        }

        // Comprobar condiciones de desbloqueo de DECORATION al finalizar el día
        decorationUnlockChecker?.checkAll()
    }

    /**
     * Red de seguridad para cuando el worker de las 23:59 no llegó a ejecutarse
     * (móvil apagado, Doze, app forzada a parar). Cierra todos los días desde el
     * último resumen guardado hasta ayer.
     *
     * Sin resúmenes previos no hay nada que recuperar: se empieza a contar hoy.
     */
    suspend fun processPendingDays(
        today: LocalDate,
        blockCategories: Map<String, List<MarineCategory>> = emptyMap()
    ) {
        val lastClosed = daySummaryRepository.getLatestN(1).firstOrNull()?.date ?: return
        var date = lastClosed.plus(1, DateTimeUnit.DAY)
        while (date < today) {
            processDay(date, blockCategories)
            date = date.plus(1, DateTimeUnit.DAY)
        }
    }

    companion object {
        /** El día se cierra cuando ya no queda día por delante. */
        val DAY_CLOSE_TIME = LocalTime(23, 59, 59)

        /** Umbral único de "día conseguido". De aquí cuelgan racha y decoraciones. */
        const val DAY_ACHIEVED_THRESHOLD = 0.80f

        /** Historial suficiente para resolver el hito de racha más largo (30 días). */
        private const val STREAK_LOOKBACK_DAYS = 60
    }

    private fun buildMessage(score: Float, total: Int, completed: Int, streak: Int): String {
        if (total == 0) return "Un día tranquilo. El estanque sigue ahí."

        val baseMessage = when {
            score == 0f -> "Las corrientes cambian. Mañana el mar sigue ahí."
            score < 0.4f -> "Algo se movió hoy. Eso cuenta."
            score < DAY_ACHIEVED_THRESHOLD -> "Buen empuje hoy."
            score < 1f -> "El estanque está vivo."
            else -> "Hoy el estanque brilló."
        }

        val streakSuffix = if (streak >= 3) " Llevas $streak días seguidos 🔥" else null
        val progressPrefix = if (score > 0f && score < 1f) "$completed de $total tareas. " else null

        return buildString {
            if (progressPrefix != null) append(progressPrefix)
            append(baseMessage)
            if (streakSuffix != null) append(streakSuffix)
        }
    }
}
