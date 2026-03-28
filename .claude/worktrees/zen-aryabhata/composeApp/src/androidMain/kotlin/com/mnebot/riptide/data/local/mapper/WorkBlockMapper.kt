package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.data.local.entity.WorkBlockEntity
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.Recurrence
import com.mnebot.riptide.domain.model.WeeklySlot
import com.mnebot.riptide.domain.model.WorkBlock
import kotlinx.datetime.LocalTime

fun WorkBlockEntity.toDomain(): WorkBlock {
    return WorkBlock(
        id = id,
        name = name,
        marineCategories = emptyList(), // se rellena desde BlockCategoryRepository
        color = color,
        icon = icon,
        recurrence = parseRecurrence(recurrenceJson),
        isActive = isActive
    )
}

fun WorkBlock.toEntity(): WorkBlockEntity {
    return WorkBlockEntity(
        id = id,
        name = name,
        color = color,
        icon = icon,
        recurrenceJson = serializeRecurrence(recurrence),
        isActive = isActive
    )
}

private fun serializeRecurrence(recurrence: Recurrence): String {
    return when (recurrence) {
        is Recurrence.None -> "none"
        is Recurrence.Weekly -> recurrence.slots.joinToString("|") { slot ->
            "${slot.dayOfWeek},${slot.startTime},${slot.endTime}"
        }
    }
}

fun parseRecurrence(json: String): Recurrence {
    if (json == "none" || json.isBlank()) return Recurrence.None
    return try {
        val slots = json.split("|").mapNotNull { part ->
            val segments = part.split(",")
            if (segments.size < 3) return@mapNotNull null
            val day = segments[0].trim().toIntOrNull() ?: return@mapNotNull null
            val start = segments[1].trim().takeIf { it != "null" }?.let { parseTime(it) }
            val end = segments[2].trim().takeIf { it != "null" }?.let { parseTime(it) }
            WeeklySlot(dayOfWeek = day, startTime = start, endTime = end)
        }
        if (slots.isEmpty()) Recurrence.None else Recurrence.Weekly(slots)
    } catch (e: Exception) {
        Recurrence.None
    }
}

private fun parseTime(value: String): LocalTime? {
    return try {
        val parts = value.split(":")
        if (parts.size < 2) return null
        LocalTime(parts[0].toInt(), parts[1].toInt())
    } catch (e: Exception) {
        null
    }
}