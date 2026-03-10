package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.data.local.entity.DayTaskEntity
import com.mnebot.riptide.domain.model.DayTask
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

fun DayTaskEntity.toDomain(): DayTask = DayTask(
    id = id,
    blockId = blockId,
    date = LocalDate.parse(date),
    title = title,
    estimatedMinutes = estimatedMinutes,
    isCompleted = isCompleted,
    completedAt = completedAt?.let { LocalDateTime.parse(it) },
    order = order
)

fun DayTask.toEntity(): DayTaskEntity = DayTaskEntity(
    id = id,
    blockId = blockId,
    date = date.toString(),
    title = title,
    estimatedMinutes = estimatedMinutes,
    isCompleted = isCompleted,
    completedAt = completedAt?.toString(),
    order = order
)