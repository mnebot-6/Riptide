package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.data.local.entity.WorkBlockEntity
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.Recurrence
import com.mnebot.riptide.domain.model.WeeklySlot
import com.mnebot.riptide.domain.model.WorkBlock
import kotlinx.datetime.LocalTime

fun WorkBlockEntity.toDomain(): WorkBlock = WorkBlock(
    id = id,
    name = name,
    marineCategory = MarineCategory.valueOf(marineCategory),
    color = color,
    icon = icon,
    recurrence = parseRecurrence(recurrenceType, recurrenceSlots),
    isActive = isActive
)

fun WorkBlock.toEntity(): WorkBlockEntity = WorkBlockEntity(
    id = id,
    name = name,
    marineCategory = marineCategory.name,
    color = color,
    icon = icon,
    recurrenceType = when (recurrence) {
        is Recurrence.None -> "NONE"
        is Recurrence.Weekly -> "WEEKLY"
    },
    recurrenceSlots = serializeSlots(recurrence),
    isActive = isActive
)

private fun parseRecurrence(type: String, slotsJson: String): Recurrence {
    if (type == "NONE" || slotsJson.isBlank() || slotsJson == "[]") return Recurrence.None
    return try {
        val slots = mutableListOf<WeeklySlot>()
        val content = slotsJson.trim().removePrefix("[").removeSuffix("]")
        val objects = splitJsonObjects(content)
        for (obj in objects) {
            val fields = obj.trim().removePrefix("{").removeSuffix("}")
            val map = mutableMapOf<String, String>()
            for (field in fields.split(",")) {
                val colonIndex = field.indexOf(":")
                if (colonIndex == -1) continue
                val key = field.substring(0, colonIndex).trim().removeSurrounding("\"")
                val value = field.substring(colonIndex + 1).trim().removeSurrounding("\"")
                map[key] = value
            }
            val day = map["day"]?.toIntOrNull() ?: continue
            val start = map["start"]?.let { if (it == "null") null else LocalTime.parse(it) }
            val end = map["end"]?.let { if (it == "null") null else LocalTime.parse(it) }
            slots.add(WeeklySlot(day, start, end))
        }
        if (slots.isEmpty()) Recurrence.None else Recurrence.Weekly(slots)
    } catch (e: Exception) {
        Recurrence.None
    }
}

private fun splitJsonObjects(content: String): List<String> {
    val objects = mutableListOf<String>()
    var depth = 0
    var start = 0
    for (i in content.indices) {
        when (content[i]) {
            '{' -> { if (depth == 0) start = i; depth++ }
            '}' -> { depth--; if (depth == 0) objects.add(content.substring(start, i + 1)) }
        }
    }
    return objects
}

private fun serializeSlots(recurrence: Recurrence): String {
    if (recurrence !is Recurrence.Weekly) return "[]"
    val slots = recurrence.slots.joinToString(",") { slot ->
        "{\"day\":${slot.dayOfWeek},\"start\":\"${slot.startTime}\",\"end\":\"${slot.endTime}\"}"
    }
    return "[$slots]"
}