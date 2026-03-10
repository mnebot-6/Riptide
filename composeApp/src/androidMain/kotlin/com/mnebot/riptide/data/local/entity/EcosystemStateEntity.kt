package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ecosystem_states")
data class EcosystemStateEntity(
    @PrimaryKey val id: String,
    val category: String,
    val totalExperience: Int,
    val currentLevel: Int,
    val lastUpdated: String
)