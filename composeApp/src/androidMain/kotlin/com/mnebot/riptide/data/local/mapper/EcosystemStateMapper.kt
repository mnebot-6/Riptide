package com.mnebot.riptide.data.local.mapper

import com.mnebot.riptide.data.local.entity.EcosystemStateEntity
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import kotlinx.datetime.LocalDateTime

fun EcosystemStateEntity.toDomain(): EcosystemState = EcosystemState(
    id = id,
    category = MarineCategory.valueOf(category),
    totalExperience = totalExperience,
    currentLevel = currentLevel,
    lastUpdated = LocalDateTime.parse(lastUpdated)
)

fun EcosystemState.toEntity(): EcosystemStateEntity = EcosystemStateEntity(
    id = id,
    category = category.name,
    totalExperience = totalExperience,
    currentLevel = currentLevel,
    lastUpdated = lastUpdated.toString()
)