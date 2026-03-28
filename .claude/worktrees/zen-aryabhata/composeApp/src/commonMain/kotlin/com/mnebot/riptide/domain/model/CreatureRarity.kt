package com.mnebot.riptide.domain.model

enum class CreatureRarity(val weight: Float, val displayName: String) {
    COMMON(0.40f, "Común"),
    UNCOMMON(0.30f, "Poco común"),
    RARE(0.20f, "Raro"),
    EPIC(0.08f, "Épico"),
    LEGENDARY(0.02f, "Legendario")
}
