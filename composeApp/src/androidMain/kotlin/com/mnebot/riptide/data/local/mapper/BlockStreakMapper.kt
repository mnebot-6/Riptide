package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.data.local.entity.BlockStreakEntity
import com.mnebot.riptide.domain.model.BlockStreak
import kotlinx.datetime.LocalDate

fun BlockStreakEntity.toDomain(): BlockStreak = BlockStreak(
    blockId = blockId,
    currentStreak = currentStreak,
    lastActiveDate = LocalDate.parse(lastActiveDate)
)

fun BlockStreak.toEntity(): BlockStreakEntity = BlockStreakEntity(
    blockId = blockId,
    currentStreak = currentStreak,
    lastActiveDate = lastActiveDate.toString()
)