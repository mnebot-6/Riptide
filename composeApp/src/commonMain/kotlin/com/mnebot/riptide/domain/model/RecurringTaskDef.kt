package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalTime

data class RecurringTaskDef(
    val id: String,
    val blockId: String,
    val title: String,
    val time: LocalTime?,              // nullable — la hora es opcional en recurrentes
    val recurrence: Recurrence,
    val isActive: Boolean
)