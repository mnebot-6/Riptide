package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class DayTask(
    val id: String,
    val blockId: String,
    val date: LocalDate,
    val title: String,
    val estimatedMinutes: Int?,
    val isCompleted: Boolean,
    val completedAt: LocalDateTime?,
    val order: Int
)