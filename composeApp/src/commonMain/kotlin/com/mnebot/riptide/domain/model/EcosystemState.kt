package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDateTime

data class EcosystemState(
    val id: String,
    val category: MarineCategory,
    val totalExperience: Int,
    val currentLevel: Int,
    val lastUpdated: LocalDateTime
)