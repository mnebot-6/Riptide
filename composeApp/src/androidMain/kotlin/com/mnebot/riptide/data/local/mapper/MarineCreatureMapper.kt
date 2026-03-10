package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.data.local.entity.MarineCreatureEntity
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.MarineCreature
import kotlinx.datetime.LocalDateTime

fun MarineCreatureEntity.toDomain(): MarineCreature = MarineCreature(
    id = id,
    ecosystemId = ecosystemId,
    species = CreatureSpecies.valueOf(species),
    nickname = nickname,
    unlockedAtLevel = unlockedAtLevel,
    experience = experience,
    creatureLevel = creatureLevel,
    unlockedAt = LocalDateTime.parse(unlockedAt)
)

fun MarineCreature.toEntity(): MarineCreatureEntity = MarineCreatureEntity(
    id = id,
    ecosystemId = ecosystemId,
    category = species.category.name,
    species = species.name,
    nickname = nickname,
    unlockedAtLevel = unlockedAtLevel,
    experience = experience,
    creatureLevel = creatureLevel,
    unlockedAt = unlockedAt.toString()
)