package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.graphics.Path

/**
 * Provides an undulating terrain curve for the sea floor.
 *
 * Control points are generated procedurally from a seed in [AquariumTerrainConfig],
 * producing alternating hills (odd indices) and valleys (even interior indices).
 * Y-fraction range: ~0.73 (hill peaks) to ~0.93 (valley floors) at heightVariation=1.0.
 *
 * Uses Catmull-Rom spline interpolation for smooth, natural curves.
 */
object AquariumTerrain {

    private data class GeneratedTerrain(
        val seed: Long,
        val heightVariation: Float,
        val controlPoints: List<Pair<Float, Float>>
    )

    private var cached: GeneratedTerrain = generateTerrain(AquariumTerrainConfig())

    /**
     * Reconfigures the terrain if the seed or heightVariation differs from the cached version.
     * Should be called once before the first draw (e.g. from the cachedDecorations lazy block).
     */
    fun configure(config: AquariumTerrainConfig) {
        if (cached.seed != config.seed || cached.heightVariation != config.heightVariation) {
            cached = generateTerrain(config)
        }
    }

    private fun generateTerrain(config: AquariumTerrainConfig): GeneratedTerrain {
        val rng = kotlin.random.Random(config.seed)
        val xFracs = listOf(0.00f, 0.12f, 0.25f, 0.38f, 0.50f, 0.63f, 0.76f, 0.87f, 1.00f)
        val hv = config.heightVariation.coerceIn(0f, 1f)
        val base = 0.84f
        val amplitude = 0.022f * hv
        val points = xFracs.mapIndexed { i, x ->
            val y = when (i) {
                0, 8 -> base + amplitude * 0.3f           // edges: slight valley
                else -> if (i % 2 == 1)
                    base - rng.nextFloat() * amplitude     // odd = hill (smaller Y = higher on screen)
                else
                    base + rng.nextFloat() * amplitude     // even = valley
            }
            x to y.coerceIn(0.80f, 0.90f)
        }
        return GeneratedTerrain(config.seed, config.heightVariation, points)
    }

    /**
     * Returns the Y coordinate of the terrain at the given horizontal fraction.
     *
     * Uses Catmull-Rom spline interpolation for smooth curves between control points.
     * The result is in screen coordinates (pixels).
     *
     * @param xFraction horizontal position as a fraction of screen width [0..1]
     * @param screenHeight total screen height in pixels
     */
    fun terrainY(xFraction: Float, screenHeight: Float): Float {
        val x = xFraction.coerceIn(0f, 1f)
        val pts = cached.controlPoints
        val n = pts.size

        var segIndex = 0
        for (i in 0 until n - 1) {
            if (x >= pts[i].first) segIndex = i
        }

        val p0x = pts[segIndex].first
        val p1x = if (segIndex + 1 < n) pts[segIndex + 1].first else p0x
        val segLen = p1x - p0x
        val t = if (segLen > 0f) ((x - p0x) / segLen).coerceIn(0f, 1f) else 0f

        val y0 = pts[(segIndex - 1).coerceAtLeast(0)].second
        val y1 = pts[segIndex].second
        val y2 = pts[(segIndex + 1).coerceAtMost(n - 1)].second
        val y3 = pts[(segIndex + 2).coerceAtMost(n - 1)].second

        val yFraction = catmullRom(t, y0, y1, y2, y3)
        return yFraction * screenHeight
    }

    /**
     * Returns a closed Path following the terrain curve from left to right,
     * then down to the bottom-right corner, across to bottom-left, and closed.
     * Suitable for filling with a sand gradient.
     *
     * @param screenWidth total screen width in pixels
     * @param screenHeight total screen height in pixels
     * @param steps number of horizontal sample points for smoothness (default 100)
     */
    fun terrainPath(screenWidth: Float, screenHeight: Float, steps: Int = 100): Path {
        return Path().apply {
            val startY = terrainY(0f, screenHeight)
            moveTo(0f, startY)

            for (i in 1..steps) {
                val xFrac = i.toFloat() / steps
                val px = xFrac * screenWidth
                val py = terrainY(xFrac, screenHeight)
                lineTo(px, py)
            }

            lineTo(screenWidth, screenHeight)
            lineTo(0f, screenHeight)
            close()
        }
    }

    /**
     * Catmull-Rom spline interpolation between p1 and p2,
     * using p0 and p3 as tangent guides. t in [0..1].
     */
    private fun catmullRom(t: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * (
            (2f * p1) +
            (-p0 + p2) * t +
            (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
            (-p0 + 3f * p1 - 3f * p2 + p3) * t3
        )
    }
}
