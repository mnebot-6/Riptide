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

// displayName delegado al enum CreatureSpecies (que ya lo tiene como propiedad)
val CreatureSpec.displayName: String
    get() = species.displayName
