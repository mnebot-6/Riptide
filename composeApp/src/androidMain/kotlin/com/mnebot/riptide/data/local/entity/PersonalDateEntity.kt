package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_dates")
data class PersonalDateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,          // ISO LocalDate
    val type: String,          // PersonalDateType name
    val recurrence: String,    // PersonalDateRecurrence name
    val notificationsEnabled: Boolean = false,
    val updatedAt: String = ""
)
