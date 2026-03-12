package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalTime

data class RecurringTaskDef(
    val id: String,
    val blockId: String,              // las recurrentes siempre tienen bloque
    val title: String,
    val time: LocalTime,
    val recurrence: Recurrence,
    val isActive: Boolean
)