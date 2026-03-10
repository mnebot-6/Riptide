package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalTime

data class WorkBlock(
    val id: String,
    val name: String,
    val marineCategory: MarineCategory,
    val color: String,
    val icon: String,
    val recurrence: Recurrence,
    val isActive: Boolean
)

sealed class Recurrence {
    object None : Recurrence()
    data class Weekly(val slots: List<WeeklySlot>) : Recurrence()
}

data class WeeklySlot(
    val dayOfWeek: Int,
    val startTime: LocalTime?,
    val endTime: LocalTime?
)