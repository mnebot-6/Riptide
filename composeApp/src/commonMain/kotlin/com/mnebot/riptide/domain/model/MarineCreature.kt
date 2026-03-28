package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDateTime

enum class CreatureSpecies(val category: MarineCategory, val displayName: String) {
    // FISH
    CLOWNFISH(MarineCategory.FISH, "Pez payaso"),
    ANGELFISH(MarineCategory.FISH, "Pez ángel"),
    PUFFERFISH(MarineCategory.FISH, "Pez globo"),
    SURGEONFISH(MarineCategory.FISH, "Pez cirujano"),
    LIONFISH(MarineCategory.FISH, "Pez león"),
    SUNFISH(MarineCategory.FISH, "Pez luna"),

    // FLORA
    BRAIN_CORAL(MarineCategory.FLORA, "Coral cerebro"),
    ANEMONE(MarineCategory.FLORA, "Anémona"),
    KELP(MarineCategory.FLORA, "Alga kelp"),
    POSIDONIA(MarineCategory.FLORA, "Posidonia"),
    FAN_CORAL(MarineCategory.FLORA, "Coral abanico"),

    // CRUSTACEAN
    LOBSTER(MarineCategory.CRUSTACEAN, "Langosta"),
    HERMIT_CRAB(MarineCategory.CRUSTACEAN, "Cangrejo ermitaño"),
    SHRIMP(MarineCategory.CRUSTACEAN, "Gamba"),
    SPIDER_CRAB(MarineCategory.CRUSTACEAN, "Cangrejo araña"),
    BARNACLE(MarineCategory.CRUSTACEAN, "Percebes"),

    // MOLLUSK
    SEA_URCHIN(MarineCategory.MOLLUSK, "Erizo de mar"),
    STARFISH(MarineCategory.MOLLUSK, "Estrella de mar"),
    OYSTER(MarineCategory.MOLLUSK, "Ostra"),
    NAUTILUS(MarineCategory.MOLLUSK, "Nautilus"),
    GIANT_CLAM(MarineCategory.MOLLUSK, "Almeja gigante"),

    // PELAGIC
    MANTA_RAY(MarineCategory.PELAGIC, "Raya manta"),
    MOON_JELLYFISH(MarineCategory.PELAGIC, "Medusa luna"),
    WHALE_SHARK(MarineCategory.PELAGIC, "Tiburón ballena"),
    HAMMERHEAD(MarineCategory.PELAGIC, "Pez martillo"),
    BARRACUDA(MarineCategory.PELAGIC, "Barracuda"),

    // CEPHALOPOD
    OCTOPUS(MarineCategory.CEPHALOPOD, "Pulpo"),
    SQUID(MarineCategory.CEPHALOPOD, "Calamar"),
    CUTTLEFISH(MarineCategory.CEPHALOPOD, "Sepia"),
    BLUE_RINGED_OCTOPUS(MarineCategory.CEPHALOPOD, "Pulpo anillado"),

    // REPTILE
    SEA_TURTLE(MarineCategory.REPTILE, "Tortuga marina"),
    MARINE_IGUANA(MarineCategory.REPTILE, "Iguana marina"),

    // MAMMAL
    DOLPHIN(MarineCategory.MAMMAL, "Delfín"),
    SEAL(MarineCategory.MAMMAL, "Foca"),
    BLUE_WHALE(MarineCategory.MAMMAL, "Ballena azul"),
    SEA_OTTER(MarineCategory.MAMMAL, "Nutria marina"),
    MANATEE(MarineCategory.MAMMAL, "Manatí"),

    // DECORATION
    TREASURE_CHEST(MarineCategory.DECORATION, "Cofre del tesoro"),
    ANCHOR(MarineCategory.DECORATION, "Ancla"),
    SUNKEN_SHIP(MarineCategory.DECORATION, "Barco hundido"),

    // COMPANION — easter egg oculto
    BIMBA(MarineCategory.COMPANION, "Bimba")
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