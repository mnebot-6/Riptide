@file:UseSerializers(LocalTimeSerializer::class)

package com.mnebot.riptide.domain.model

import com.mnebot.riptide.LocalTimeSerializer
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

data class WorkBlock(
    val id: String,
    val name: String,
    val marineCategories: List<MarineCategory>,
    val color: String,
    val icon: String,
    val recurrence: Recurrence,
    val isActive: Boolean
)

@Serializable
sealed class Recurrence {
    @Serializable
    object None : Recurrence()

    @Serializable
    data class Weekly(val slots: List<WeeklySlot>) : Recurrence()

    /** Annual recurrence on a fixed month/day (e.g. birthdays). */
    @Serializable
    data class Yearly(val month: Int, val day: Int) : Recurrence()

    /** Repeats on the given day-of-month (1..31). */
    @Serializable
    data class MonthlyDay(val day: Int) : Recurrence()
}

@Serializable
data class WeeklySlot(
    val dayOfWeek: Int,
    val startTime: LocalTime?,
    val endTime: LocalTime?
)