package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope

// ── Placement data classes ─────────────────────────────────────────────────────

data class PebblePlacement(
    val xFrac: Float,
    val yOffsetFrac: Float,
    val radiusFrac: Float,
    val colorIndex: Int,
    val alpha: Float
)

data class TerrainDecorations(
    val seed: Long,
    val density: Float,
    val pebbles: List<PebblePlacement>,
)

// ── Generator ─────────────────────────────────────────────────────────────────

internal fun generateDecorations(config: AquariumTerrainConfig): TerrainDecorations {
    val rng = kotlin.random.Random(config.seed + 1L)
    val d = config.decorationDensity.coerceIn(0.5f, 2.0f)

    val pebbleCount = (25 * d).toInt()
    val pebbles = (0 until pebbleCount).map {
        PebblePlacement(
            xFrac = rng.nextFloat(),
            yOffsetFrac = rng.nextFloat() * 0.006f,
            radiusFrac = 0.002f + rng.nextFloat() * 0.005f,
            colorIndex = rng.nextInt(3),
            alpha = 0.25f + rng.nextFloat() * 0.20f
        )
    }

    return TerrainDecorations(
        seed = config.seed,
        density = config.decorationDensity,
        pebbles = pebbles,
    )
}

// ── Drawing functions ─────────────────────────────────────────────────────────

/**
 * Draws subtle pebbles scattered along the sea floor.
 */
internal fun DrawScope.drawFloorDecorations(decorations: TerrainDecorations) {
    val w = size.width
    val h = size.height

    decorations.pebbles.forEach { p ->
        val px = p.xFrac * w
        val py = AquariumTerrain.terrainY(p.xFrac, h) + p.yOffsetFrac * h
        val radius = p.radiusFrac * h
        val pebbleColor = when (p.colorIndex) {
            0 -> SandDark.copy(alpha = p.alpha)
            1 -> RockLight.copy(alpha = p.alpha)
            else -> SandMid.copy(alpha = p.alpha + 0.1f)
        }
        drawCircle(color = pebbleColor, radius = radius, center = Offset(px, py))
    }
}
