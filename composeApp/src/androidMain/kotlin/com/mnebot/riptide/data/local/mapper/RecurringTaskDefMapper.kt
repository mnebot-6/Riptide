@file:UseSerializers(LocalTimeSerializer::class)

package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.LocalTimeSerializer
import com.mnebot.riptide.data.local.entity.RecurringTaskDefEntity
import com.mnebot.riptide.domain.model.RecurringTaskDef
import com.mnebot.riptide.domain.model.Recurrence
import kotlinx.datetime.LocalTime
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun RecurringTaskDefEntity.toDomain(): RecurringTaskDef = RecurringTaskDef(
    id = id,
    blockId = blockId,
    title = title,
    time = time?.let { LocalTime.parse(it) },
    // NthWeekdayOfMonth desapareció en v16; MIGRATION_15_16 desactiva las que lo usaban,
    // pero una fila que llegue por sync desde un cliente antiguo no debe reventar.
    recurrence = runCatching { Json.decodeFromString<Recurrence>(recurrence) }
        .getOrDefault(Recurrence.None),
    isActive = isActive,
    notificationsEnabled = notificationsEnabled,
    targetCount = targetCount,
    noteTemplate = noteTemplate,
    timerDurationMinutes = timerDurationMinutes,
    isPriority = isPriority
)

fun RecurringTaskDef.toEntity(): RecurringTaskDefEntity = RecurringTaskDefEntity(
    id = id,
    blockId = blockId,
    title = title,
    time = time?.toString(),
    recurrence = Json.encodeToString<Recurrence>(this.recurrence),
    isActive = isActive,
    notificationsEnabled = notificationsEnabled,
    targetCount = targetCount,
    noteTemplate = noteTemplate,
    timerDurationMinutes = timerDurationMinutes,
    isPriority = isPriority
)