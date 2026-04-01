package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_streaks")
data class BlockStreakEntity(
    @PrimaryKey val blockId: String,
    val currentStreak: Int,
    val lastActiveDate: String,
    val longestStreak: Int = 0,
    val updatedAt: String = ""
)