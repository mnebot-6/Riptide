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

    /** Repeats every [intervalMonths] months on the given day-of-month (1..31). */
    @Serializable
    data class MonthlyDay(val day: Int, val intervalMonths: Int = 1) : Recurrence()

    /**
     * Nth weekday of the month (e.g. first Sunday). [nth] is 1..5 (5 = last).
     * [dayOfWeek] is ISO 1..7 (Mon..Sun).
     */
    @Serializable
    data class NthWeekdayOfMonth(
        val nth: Int,
        val dayOfWeek: Int,
        val intervalMonths: Int = 1
    ) : Recurrence()
}

@Serializable
data class WeeklySlot(
    val dayOfWeek: Int,
    val startTime: LocalTime?,
    val endTime: LocalTime?
)