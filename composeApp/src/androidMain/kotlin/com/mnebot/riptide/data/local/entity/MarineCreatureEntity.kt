package com.mnebot.riptide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "marine_creatures")
data class MarineCreatureEntity(
    @PrimaryKey val id: String,
    val ecosystemId: String,
    val category: String,
    val species: String,
    val nickname: String?,
    val unlockedAtLevel: Int,
    val experience: Int,
    val creatureLevel: Int,
    val unlockedAt: String
)