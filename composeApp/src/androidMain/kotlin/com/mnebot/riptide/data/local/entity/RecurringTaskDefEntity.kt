package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_task_defs",
    foreignKeys = [
        ForeignKey(
            entity = WorkBlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecurringTaskDefEntity(
    @PrimaryKey val id: String,
    val blockId: String,
    val title: String,
    val time: String,                // LocalTime serializado
    val recurrence: String,          // JSON
    val isActive: Boolean
)