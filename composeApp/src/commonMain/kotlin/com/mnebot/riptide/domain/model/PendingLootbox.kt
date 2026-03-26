package com.mnebot.riptide.domain.model

data class PendingLootbox(
    val category: MarineCategory,
    val categoryLevel: Int,
    val directSpecies: CreatureSpecies? = null
)
