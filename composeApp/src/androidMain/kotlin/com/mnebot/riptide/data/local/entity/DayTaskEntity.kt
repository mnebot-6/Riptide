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
    val sourceTaskId: String?
)