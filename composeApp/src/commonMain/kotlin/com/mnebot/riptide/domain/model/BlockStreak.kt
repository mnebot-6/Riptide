package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDate

data class BlockStreak(
    val blockId: String,
    val currentStreak: Int,
    val lastActiveDate: LocalDate,
    val longestStreak: Int = 0
)