package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.TaskSchedule
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class NightSummaryProcessor(
    private val dayTaskRepository: DayTaskRepository,
    private val daySummaryRepository: DaySummaryRepository,
    private val blockStreakProcessor: BlockStreakProcessor? = null,
    private val ecosystemProcessor: EcosystemProcessor? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
) {
    // En la firma de processDay, añadir summaryTime:
    suspend fun processDay(
        date: LocalDate,
        blockNames: Map<String, String> = emptyMap(),
        blockCategories: Map<String, List<MarineCategory>> = emptyMap(),
        summaryTime: LocalTime? = null
    ) {
        if (daySummaryRepository.getByDate(date) != null) return

        val allTasks = dayTaskRepository.getByDate(date)
        if (allTasks.isEmpty()) return

        // Excluir pospuestas
        val nonPostponed = allTasks.filter {
            it.status != TaskStatus.POSTPONED && it.status != TaskStatus.CANCELLED
        }

        // Tareas evaluables: las que tienen fecha <= fecha del resumen, o las que no tienen
        // fecha pero ya están completadas. Las sin fecha y sin completar se ignoran hoy.
        val evaluable = nonPostponed.filter { task ->
            // Ya completada → siempre evaluable
            if (task.status == TaskStatus.COMPLETED) return@filter true

            when (val schedule = task.schedule) {
                is TaskSchedule.OneTime -> {
                    // Solo tareas de HOY (no futuras ni pasadas sin resolver)
                    if (schedule.date != date) return@filter false
                    val taskTime = schedule.time
                    // Tiene hora y es anterior o igual al resumen → evaluable
                    taskTime != null && summaryTime != null && taskTime <= summaryTime
                }
                is TaskSchedule.Recurring -> false
            }
        }

        if (evaluable.isEmpty()) return

        // Expirar las PENDING evaluables
        evaluable.filter { it.status == TaskStatus.PENDING }.forEach { task ->
            dayTaskRepository.updateStatus(task.id, TaskStatus.EXPIRED)
        }

        val total = evaluable.size
        val completed = evaluable.count { it.status == TaskStatus.COMPLETED }
        val score = if (total > 0) completed.toFloat() / total.toFloat() else 0f

        val blockIds = evaluable.mapNotNull { it.blockId }.distinct()
        val streaks = blockStreakProcessor?.processDay(date, blockIds) ?: emptyMap()

        val message = buildMessage(score, total, completed, streaks, blockNames)

        daySummaryRepository.insert(
            DaySummary(
                id = generateUUID(),
                date = date,
                score = score,
                tasksTotal = total,
                tasksCompleted = completed,
                streakDay = 0,
                feedbackMessage = message
            )
        )

        val bestStreak = streaks.values.maxOrNull() ?: 0
        val allCategories = evaluable
            .mapNotNull { it.blockId }
            .flatMap { blockId -> blockCategories[blockId] ?: emptyList() }
            .distinct()

        val newUnlocks: List<CreatureSpec> =
            ecosystemProcessor?.addNightBonus(score, bestStreak, allCategories) ?: emptyList()

        if (newUnlocks.isNotEmpty() && userPreferencesRepository != null) {
            val existing = userPreferencesRepository.getPendingUnlocks()
            userPreferencesRepository.setPendingUnlocks(existing + newUnlocks.map { it.emoji })
        }
    }

    private fun buildMessage(
        score: Float,
        total: Int,
        completed: Int,
        streaks: Map<String, Int>,
        blockNames: Map<String, String>
    ): String {
        val baseMessage = when {
            score == 0f -> "Las corrientes cambian. Mañana el mar sigue ahí."
            score < 0.4f -> "Algo se movió hoy. Eso cuenta."
            score < 0.7f -> "Buen empuje hoy."
            score < 1f -> "El estanque está vivo."
            else -> "Hoy el estanque brilló."
        }

        val topEntry = streaks.entries
            .filter { it.value >= 3 }
            .maxByOrNull { it.value }

        val streakSuffix = if (topEntry != null) {
            val name = blockNames[topEntry.key] ?: "ese bloque"
            " $name lleva ${topEntry.value} días seguidos 🔥"
        } else null

        val progressPrefix = if (score > 0f && score < 1f) "$completed de $total tareas. " else null

        return buildString {
            if (progressPrefix != null) append(progressPrefix)
            append(baseMessage)
            if (streakSuffix != null) append(streakSuffix)
        }
    }
}