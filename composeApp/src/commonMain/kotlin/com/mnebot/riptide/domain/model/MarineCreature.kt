package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDateTime

enum class CreatureSpecies(val category: MarineCategory, val displayName: String) {
    // FISH
    CLOWNFISH(MarineCategory.FISH, "Pez payaso"),
    ANGELFISH(MarineCategory.FISH, "Pez ángel"),
    PUFFERFISH(MarineCategory.FISH, "Pez globo"),

    // FLORA
    BRAIN_CORAL(MarineCategory.FLORA, "Coral cerebro"),
    ANEMONE(MarineCategory.FLORA, "Anémona"),
    KELP(MarineCategory.FLORA, "Alga kelp"),

    // CRUSTACEAN
    LOBSTER(MarineCategory.CRUSTACEAN, "Langosta"),
    HERMIT_CRAB(MarineCategory.CRUSTACEAN, "Cangrejo ermitaño"),
    SHRIMP(MarineCategory.CRUSTACEAN, "Gamba"),

    // MOLLUSK
    SEA_URCHIN(MarineCategory.MOLLUSK, "Erizo de mar"),
    STARFISH(MarineCategory.MOLLUSK, "Estrella de mar"),
    OYSTER(MarineCategory.MOLLUSK, "Ostra"),

    // PELAGIC
    MANTA_RAY(MarineCategory.PELAGIC, "Raya manta"),
    MOON_JELLYFISH(MarineCategory.PELAGIC, "Medusa luna"),
    WHALE_SHARK(MarineCategory.PELAGIC, "Tiburón ballena"),

    // CEPHALOPOD
    OCTOPUS(MarineCategory.CEPHALOPOD, "Pulpo"),
    SQUID(MarineCategory.CEPHALOPOD, "Calamar"),

    // REPTILE
    SEA_TURTLE(MarineCategory.REPTILE, "Tortuga marina"),

    // MAMMAL
    DOLPHIN(MarineCategory.MAMMAL, "Delfín"),
    SEAL(MarineCategory.MAMMAL, "Foca"),
    BLUE_WHALE(MarineCategory.MAMMAL, "Ballena azul"),

    // DECORATION
    TREASURE_CHEST(MarineCategory.DECORATION, "Cofre del tesoro"),
    ANCHOR(MarineCategory.DECORATION, "Ancla"),
    SUNKEN_SHIP(MarineCategory.DECORATION, "Barco hundido")
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