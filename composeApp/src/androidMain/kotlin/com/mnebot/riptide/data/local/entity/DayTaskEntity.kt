package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_tasks")
data class DayTaskEntity(
    @PrimaryKey val id: String,
    val blockId: String,
    val date: String,                  // formato ISO: "2025-03-10"
    val title: String,
    val estimatedMinutes: Int?,
    val isCompleted: Boolean,
    val completedAt: String?,          // formato ISO: "2025-03-10T22:00:00"
    val order: Int
)