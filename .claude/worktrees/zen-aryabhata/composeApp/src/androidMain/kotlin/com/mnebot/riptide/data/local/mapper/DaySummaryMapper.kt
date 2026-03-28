package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.data.local.entity.DaySummaryEntity
import com.mnebot.riptide.domain.model.DaySummary
import kotlinx.datetime.LocalDate

fun DaySummaryEntity.toDomain(): DaySummary = DaySummary(
    id = id,
    date = LocalDate.parse(date),
    score = score,
    tasksTotal = tasksTotal,
    tasksCompleted = tasksCompleted,
    streakDay = streakDay,
    feedbackMessage = feedbackMessage
)

fun DaySummary.toEntity(): DaySummaryEntity = DaySummaryEntity(
    id = id,
    date = date.toString(),
    score = score,
    tasksTotal = tasksTotal,
    tasksCompleted = tasksCompleted,
    streakDay = streakDay,
    feedbackMessage = feedbackMessage
)