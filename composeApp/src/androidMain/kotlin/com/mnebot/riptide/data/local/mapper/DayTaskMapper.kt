package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.data.local.entity.DayTaskEntity
import com.mnebot.riptide.domain.model.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

fun DayTaskEntity.toDomain(): DayTask {
    val schedule = TaskSchedule.OneTime(
        date = LocalDate.parse(date!!),
        time = time?.let { LocalTime.parse(it) }
    )
    return DayTask(
        id = id,
        blockId = blockId,
        title = title,
        schedule = schedule,
        // Los estados legacy (POSTPONED/CANCELLED) los convierte MIGRATION_15_16,
        // pero un cliente antiguo puede colarlos por sync: no reventamos por eso.
        status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.PENDING),
        completedAt = completedAt?.let { LocalDateTime.parse(it) },
        sourceTaskId = sourceTaskId,
        hasBeenRewarded = hasBeenRewarded,
        notificationsEnabled = notificationsEnabled,
        targetCount = targetCount,
        currentCount = currentCount,
        notes = notes,
        timerDurationMinutes = timerDurationMinutes,
        isPriority = isPriority
    )
}

fun DayTask.toEntity(): DayTaskEntity {
    val scheduleType: String
    val date: String?
    val time: String?
    val recurrence: String?

    when (schedule) {
        is TaskSchedule.OneTime -> {
            scheduleType = "ONE_TIME"
            date = schedule.date.toString()
            time = schedule.time?.toString()
            recurrence = null
        }
    }

    return DayTaskEntity(
        id = id,
        blockId = blockId,
        title = title,
        scheduleType = scheduleType,
        date = date,
        time = time,
        recurrence = recurrence,
        status = status.name,
        completedAt = completedAt?.toString(),
        // ponytail: columna legacy, ya no se usa. Se cae en la próxima recreación de day_tasks.
        postponedTo = null,
        sourceTaskId = sourceTaskId,
        hasBeenRewarded = hasBeenRewarded,
        notificationsEnabled = notificationsEnabled,
        targetCount = targetCount,
        currentCount = currentCount,
        notes = notes,
        timerDurationMinutes = timerDurationMinutes,
        isPriority = isPriority
    )
}