package com.mnebot.riptide.presentation.aquarium

// Misma curva que EcosystemLevelCalculator
fun xpRequiredForLevel(level: Int): Int {
    if (level <= 1) return 0
    var xp = 0
    var increment = 1
    for (i in 2..level) {
        xp += increment
        increment = (increment * 1.5f).toInt().coerceAtLeast(increment + 1)
    }
    return xp
}

val CreatureSpec.displayName: String
    get() = when (species) {
        com.mnebot.riptide.domain.model.CreatureSpecies.CLOWNFISH      -> "Pez payaso"
        com.mnebot.riptide.domain.model.CreatureSpecies.ANGELFISH      -> "Pez ángel"
        com.mnebot.riptide.domain.model.CreatureSpecies.PUFFERFISH     -> "Pez globo"
        com.mnebot.riptide.domain.model.CreatureSpecies.BRAIN_CORAL    -> "Coral cerebro"
        com.mnebot.riptide.domain.model.CreatureSpecies.ANEMONE        -> "Anémona"
        com.mnebot.riptide.domain.model.CreatureSpecies.KELP           -> "Kelp"
        com.mnebot.riptide.domain.model.CreatureSpecies.LOBSTER        -> "Langosta"
        com.mnebot.riptide.domain.model.CreatureSpecies.HERMIT_CRAB    -> "Cangrejo ermitaño"
        com.mnebot.riptide.domain.model.CreatureSpecies.SHRIMP         -> "Gamba"
        com.mnebot.riptide.domain.model.CreatureSpecies.SEA_URCHIN     -> "Erizo de mar"
        com.mnebot.riptide.domain.model.CreatureSpecies.STARFISH       -> "Estrella de mar"
        com.mnebot.riptide.domain.model.CreatureSpecies.OYSTER         -> "Ostra"
        com.mnebot.riptide.domain.model.CreatureSpecies.MANTA_RAY      -> "Manta raya"
        com.mnebot.riptide.domain.model.CreatureSpecies.MOON_JELLYFISH -> "Medusa luna"
        com.mnebot.riptide.domain.model.CreatureSpecies.WHALE_SHARK    -> "Tiburón ballena"
        com.mnebot.riptide.domain.model.CreatureSpecies.OCTOPUS        -> "Pulpo"
        com.mnebot.riptide.domain.model.CreatureSpecies.SQUID          -> "Calamar"
        com.mnebot.riptide.domain.model.CreatureSpecies.SEA_TURTLE     -> "Tortuga marina"
        com.mnebot.riptide.domain.model.CreatureSpecies.DOLPHIN        -> "Delfín"
        com.mnebot.riptide.domain.model.CreatureSpecies.SEAL           -> "Foca"
        com.mnebot.riptide.domain.model.CreatureSpecies.BLUE_WHALE     -> "Ballena azul"
        com.mnebot.riptide.domain.model.CreatureSpecies.TREASURE_CHEST -> "Cofre del tesoro"
        com.mnebot.riptide.domain.model.CreatureSpecies.ANCHOR         -> "Ancla"
        com.mnebot.riptide.domain.model.CreatureSpecies.SUNKEN_SHIP    -> "Barco hundido"
    }