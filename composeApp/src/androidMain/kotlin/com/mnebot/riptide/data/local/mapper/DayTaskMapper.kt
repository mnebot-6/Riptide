package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.data.local.entity.DayTaskEntity
import com.mnebot.riptide.domain.model.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun DayTaskEntity.toDomain(): DayTask {
    val schedule = when (scheduleType) {
        "ONE_TIME" -> TaskSchedule.OneTime(
            date = LocalDate.parse(date!!),
            time = time?.let { LocalTime.parse(it) }
        )
        "RECURRING" -> TaskSchedule.Recurring(
            time = LocalTime.parse(time!!),
            recurrence = Json.decodeFromString(recurrence!!)
        )
        else -> throw IllegalArgumentException("Unknown scheduleType: $scheduleType")
    }
    return DayTask(
        id = id,
        blockId = blockId,
        title = title,
        schedule = schedule,
        status = TaskStatus.valueOf(status),
        completedAt = completedAt?.let { LocalDateTime.parse(it) },
        postponedTo = postponedTo?.let { LocalDateTime.parse(it) },
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
        is TaskSchedule.Recurring -> {
            scheduleType = "RECURRING"
            date = null
            time = schedule.time.toString()
            recurrence = Json.encodeToString(schedule.recurrence)
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
        postponedTo = postponedTo?.toString(),
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