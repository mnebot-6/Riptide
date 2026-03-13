package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import kotlinx.datetime.LocalDate

class NightSummaryProcessor(
    private val dayTaskRepository: DayTaskRepository,
    private val daySummaryRepository: DaySummaryRepository,
    private val blockStreakProcessor: BlockStreakProcessor? = null,
    private val ecosystemProcessor: EcosystemProcessor? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
) {
    suspend fun processDay(
        date: LocalDate,
        blockNames: Map<String, String> = emptyMap(),
        blockCategories: Map<String, List<MarineCategory>> = emptyMap()
    ) {
        if (daySummaryRepository.getByDate(date) != null) return

        val tasks = dayTaskRepository.getByDate(date)
        if (tasks.isEmpty()) return

        tasks.filter { it.status == TaskStatus.PENDING }.forEach { task ->
            dayTaskRepository.updateStatus(task.id, TaskStatus.EXPIRED)
        }

        val total = tasks.size
        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
        val score = if (total > 0) completed.toFloat() / total.toFloat() else 0f

        val blockIds = tasks.mapNotNull { it.blockId }.distinct()
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
        val allCategories = tasks
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