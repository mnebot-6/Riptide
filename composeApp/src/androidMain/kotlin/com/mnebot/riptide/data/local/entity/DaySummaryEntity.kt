package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_summaries")
data class DaySummaryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val score: Float,
    val tasksTotal: Int,
    val tasksCompleted: Int,
    val streakDay: Int,
    val feedbackMessage: String
)