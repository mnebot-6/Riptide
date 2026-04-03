package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "day_tasks",
    foreignKeys = [
        ForeignKey(
            entity = WorkBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class DayTaskEntity(
    @PrimaryKey val id: String,
    val blockId: String?,
    val title: String,
    val scheduleType: String,        // "ONE_TIME" | "RECURRING"
    val date: String?,               // LocalDate serializado, solo ONE_TIME
    val time: String?,               // LocalTime serializado, nullable en ONE_TIME
    val recurrence: String?,         // JSON, solo RECURRING
    val status: String,              // TaskStatus.name
    val completedAt: String?,        // LocalDateTime serializado
    val postponedTo: String?,        // LocalDateTime serializado
    val sourceTaskId: String?,
    val hasBeenRewarded: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val updatedAt: String = "",
    val isDeleted: Boolean = false,
    val targetCount: Int? = null,
    val currentCount: Int = 0,
    val notes: String? = null,
    val timerDurationMinutes: Int? = null,
    val isPriority: Boolean = false
)