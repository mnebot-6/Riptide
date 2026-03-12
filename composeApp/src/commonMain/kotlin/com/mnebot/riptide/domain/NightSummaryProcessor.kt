package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.generateUUID
import kotlinx.datetime.LocalDate

class NightSummaryProcessor(
    private val dayTaskRepository: DayTaskRepository,
    private val daySummaryRepository: DaySummaryRepository
) {
    suspend fun processDay(date: LocalDate) {
        // Si ya existe resumen para este día, no hacer nada
        if (daySummaryRepository.getByDate(date) != null) return

        // Obtener todas las tareas del día
        val tasks = dayTaskRepository.getByDate(date)
        if (tasks.isEmpty()) return

        // Expirar las PENDING
        tasks.filter { it.status == TaskStatus.PENDING }.forEach { task ->
            dayTaskRepository.updateStatus(task.id, TaskStatus.EXPIRED)
        }

        // Calcular score
        val total = tasks.size
        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
        val score = if (total > 0) completed.toFloat() / total.toFloat() else 0f

        // Generar mensaje emocional
        val message = when {
            total == 0 -> "El mar estuvo tranquilo hoy."
            score == 0f -> "Las corrientes cambian. Mañana el mar sigue ahí."
            score < 0.4f -> "Algo se movió hoy. Eso cuenta."
            score < 0.7f -> "Buen empuje hoy."
            score < 1f -> "El estanque está vivo."
            else -> "Hoy el estanque brilló."
        }

        // Guardar DaySummary
        daySummaryRepository.insert(
            DaySummary(
                id = generateUUID(),
                date = date,
                score = score,
                tasksTotal = total,
                tasksCompleted = completed,
                streakDay = 0, // streaks en v2
                feedbackMessage = message
            )
        )
    }
}