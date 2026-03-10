package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_blocks")
data class WorkBlockEntity(
    @PrimaryKey val id: String,
    val name: String,
    val marineCategory: String,
    val color: String,
    val icon: String,
    val recurrenceType: String,
    val recurrenceSlots: String,
    val isActive: Boolean
)