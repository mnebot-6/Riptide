package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalDateTime

enum class CreatureSpecies(val category: MarineCategory, val displayName: String) {
    // FISH (9)
    CLOWNFISH(MarineCategory.FISH, "Pez payaso"),
    ANGELFISH(MarineCategory.FISH, "Pez ángel"),
    PUFFERFISH(MarineCategory.FISH, "Pez globo"),
    SURGEONFISH(MarineCategory.FISH, "Pez cirujano"),
    LIONFISH(MarineCategory.FISH, "Pez león"),
    SUNFISH(MarineCategory.FISH, "Pez luna"),
    BUTTERFLYFISH(MarineCategory.FISH, "Pez mariposa"),
    SEAHORSE(MarineCategory.FISH, "Caballito de mar"),
    MORAY_EEL(MarineCategory.FISH, "Morena"),

    // FLORA (9)
    BRAIN_CORAL(MarineCategory.FLORA, "Coral cerebro"),
    ANEMONE(MarineCategory.FLORA, "Anémona"),
    KELP(MarineCategory.FLORA, "Alga kelp"),
    POSIDONIA(MarineCategory.FLORA, "Posidonia"),
    FAN_CORAL(MarineCategory.FLORA, "Coral abanico"),
    TUBE_SPONGE(MarineCategory.FLORA, "Esponja tubular"),
    SEA_GRASS(MarineCategory.FLORA, "Hierba marina"),
    FIRE_CORAL(MarineCategory.FLORA, "Coral de fuego"),
    STAGHORN_CORAL(MarineCategory.FLORA, "Coral cuerno de ciervo"),

    // CRUSTACEAN (9)
    LOBSTER(MarineCategory.CRUSTACEAN, "Langosta"),
    HERMIT_CRAB(MarineCategory.CRUSTACEAN, "Cangrejo ermitaño"),
    SHRIMP(MarineCategory.CRUSTACEAN, "Gamba"),
    SPIDER_CRAB(MarineCategory.CRUSTACEAN, "Cangrejo araña"),
    BARNACLE(MarineCategory.CRUSTACEAN, "Percebes"),
    KRILL(MarineCategory.CRUSTACEAN, "Krill"),
    HORSESHOE_CRAB(MarineCategory.CRUSTACEAN, "Cangrejo herradura"),
    MANTIS_SHRIMP(MarineCategory.CRUSTACEAN, "Gamba mantis"),
    COCONUT_CRAB(MarineCategory.CRUSTACEAN, "Cangrejo cocotero"),

    // MOLLUSK (9)
    SEA_URCHIN(MarineCategory.MOLLUSK, "Erizo de mar"),
    STARFISH(MarineCategory.MOLLUSK, "Estrella de mar"),
    OYSTER(MarineCategory.MOLLUSK, "Ostra"),
    NAUTILUS(MarineCategory.MOLLUSK, "Nautilus"),
    GIANT_CLAM(MarineCategory.MOLLUSK, "Almeja gigante"),
    CONCH(MarineCategory.MOLLUSK, "Caracola"),
    SCALLOP(MarineCategory.MOLLUSK, "Vieira"),
    SEA_SLUG(MarineCategory.MOLLUSK, "Nudibranquio"),
    SEA_CUCUMBER(MarineCategory.MOLLUSK, "Pepino de mar"),

    // PELAGIC (9)
    MANTA_RAY(MarineCategory.PELAGIC, "Raya manta"),
    MOON_JELLYFISH(MarineCategory.PELAGIC, "Medusa luna"),
    WHALE_SHARK(MarineCategory.PELAGIC, "Tiburón ballena"),
    HAMMERHEAD(MarineCategory.PELAGIC, "Pez martillo"),
    BARRACUDA(MarineCategory.PELAGIC, "Barracuda"),
    BLUEFIN_TUNA(MarineCategory.PELAGIC, "Atún rojo"),
    FLYING_FISH(MarineCategory.PELAGIC, "Pez volador"),
    LIONSMANE_JELLYFISH(MarineCategory.PELAGIC, "Medusa melena de león"),
    SWORDFISH(MarineCategory.PELAGIC, "Pez espada"),

    // CEPHALOPOD (6)
    OCTOPUS(MarineCategory.CEPHALOPOD, "Pulpo"),
    SQUID(MarineCategory.CEPHALOPOD, "Calamar"),
    CUTTLEFISH(MarineCategory.CEPHALOPOD, "Sepia"),
    BLUE_RINGED_OCTOPUS(MarineCategory.CEPHALOPOD, "Pulpo anillado"),
    CHAMBERED_NAUTILUS(MarineCategory.CEPHALOPOD, "Nautilus cámara"),
    GIANT_PACIFIC_OCTOPUS(MarineCategory.CEPHALOPOD, "Pulpo gigante del Pacífico"),

    // REPTILE (6)
    SEA_TURTLE(MarineCategory.REPTILE, "Tortuga marina"),
    MARINE_IGUANA(MarineCategory.REPTILE, "Iguana marina"),
    GREEN_SEA_TURTLE(MarineCategory.REPTILE, "Tortuga verde"),
    SEA_SNAKE(MarineCategory.REPTILE, "Serpiente marina"),
    LEATHERBACK_TURTLE(MarineCategory.REPTILE, "Tortuga laúd"),
    SALTWATER_CROCODILE(MarineCategory.REPTILE, "Cocodrilo marino"),

    // MAMMAL (6)
    DOLPHIN(MarineCategory.MAMMAL, "Delfín"),
    SEAL(MarineCategory.MAMMAL, "Foca"),
    BLUE_WHALE(MarineCategory.MAMMAL, "Ballena azul"),
    SEA_OTTER(MarineCategory.MAMMAL, "Nutria marina"),
    MANATEE(MarineCategory.MAMMAL, "Manatí"),
    NARWHAL(MarineCategory.MAMMAL, "Narval"),

    // DECORATION (6)
    TREASURE_CHEST(MarineCategory.DECORATION, "Cofre del tesoro"),
    ANCHOR(MarineCategory.DECORATION, "Ancla"),
    SUNKEN_SHIP(MarineCategory.DECORATION, "Barco hundido"),
    DIVING_HELMET(MarineCategory.DECORATION, "Escafandra"),
    CORAL_THRONE(MarineCategory.DECORATION, "Trono de coral"),
    GOLDEN_TRIDENT(MarineCategory.DECORATION, "Tridente dorado"),

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