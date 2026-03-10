package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDateTime

enum class CreatureSpecies(val category: MarineCategory, val displayName: String) {

    // FISH
    CLOWNFISH(MarineCategory.FISH, "Pez payaso"),
    ANGELFISH(MarineCategory.FISH, "Pez ángel"),

    // FLORA
    BRAIN_CORAL(MarineCategory.FLORA, "Coral cerebro"),
    ANEMONE(MarineCategory.FLORA, "Anémona"),

    // CRUSTACEAN
    HERMIT_CRAB(MarineCategory.CRUSTACEAN, "Cangrejo ermitaño"),
    LOBSTER(MarineCategory.CRUSTACEAN, "Langosta"),

    // MOLLUSK
    STARFISH(MarineCategory.MOLLUSK, "Estrella de mar"),
    SEA_URCHIN(MarineCategory.MOLLUSK, "Erizo de mar"),

    // PELAGIC
    MOON_JELLYFISH(MarineCategory.PELAGIC, "Medusa luna"),
    MANTA_RAY(MarineCategory.PELAGIC, "Raya manta")
}

data class MarineCreature(
    val id: String,
    val ecosystemId: String,
    val species: CreatureSpecies,
    val nickname: String?,
    val unlockedAtLevel: Int,
    val experience: Int,
    val creatureLevel: Int,
    val unlockedAt: LocalDateTime
)