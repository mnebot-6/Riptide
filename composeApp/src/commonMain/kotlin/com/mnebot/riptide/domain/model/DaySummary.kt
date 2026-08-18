package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDate

data class DaySummary(
    val id: String,
    val date: LocalDate,
    val score: Float,
    val tasksTotal: Int,
    val tasksCompleted: Int,
    val feedbackMessage: String
)