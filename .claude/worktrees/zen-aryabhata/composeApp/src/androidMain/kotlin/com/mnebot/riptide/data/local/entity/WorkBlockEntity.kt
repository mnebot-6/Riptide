package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_blocks")
data class WorkBlockEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val recurrenceJson: String,
    val isActive: Boolean
)