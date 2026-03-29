package com.mnebot.riptide.presentation.aquarium

/**
 * Configuration for the procedurally generated sea floor terrain and decorations.
 *
 * [seed] drives all randomness — same seed always produces the same layout.
 * [heightVariation] scales the Y-range of terrain hills (0=flat, 1=full range ~0.73..0.93).
 * [decorationDensity] multiplies the count of seaweed clusters, shells, corals, etc.
 */
data class AquariumTerrainConfig(
    val seed: Long = 42L,
    val heightVariation: Float = 1.0f,    // 0.0..1.0
    val decorationDensity: Float = 1.0f   // 0.5..2.0
)
