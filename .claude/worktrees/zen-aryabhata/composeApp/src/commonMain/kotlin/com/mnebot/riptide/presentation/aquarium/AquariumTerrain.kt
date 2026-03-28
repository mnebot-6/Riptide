package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.graphics.Path

/**
 * Provides an undulating terrain curve for the sea floor, replacing the flat baseline.
 *
 * Uses Catmull-Rom spline interpolation between control points for smooth, natural hills
 * and valleys reminiscent of cartoon coral reef art.
 *
 * yFraction range: ~0.82 (hill peaks) to ~0.93 (valley floors).
 */
object AquariumTerrain {

    // Control points defining hills and valleys as (xFraction, yFraction) pairs.
    private val controlPoints = listOf(
        0.00f to 0.90f,   // left edge - mid height
        0.10f to 0.86f,   // small hill
        0.22f to 0.91f,   // valley
        0.35f to 0.83f,   // main hill (tallest)
        0.48f to 0.89f,   // valley
        0.58f to 0.85f,   // medium hill
        0.72f to 0.92f,   // deep valley
        0.82f to 0.84f,   // hill
        0.93f to 0.90f,   // gentle slope
        1.00f to 0.88f,   // right edge
    )

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
        val n = controlPoints.size

        // Find the segment: controlPoints[i] <= x < controlPoints[i+1]
        var segIndex = 0
        for (i in 0 until n - 1) {
            if (x >= controlPoints[i].first) segIndex = i
        }

        val p0x = controlPoints[segIndex].first
        val p1x = if (segIndex + 1 < n) controlPoints[segIndex + 1].first else p0x
        val segLen = p1x - p0x

        // Local t within this segment [0..1]
        val t = if (segLen > 0f) ((x - p0x) / segLen).coerceIn(0f, 1f) else 0f

        // Catmull-Rom needs 4 points: P_{i-1}, P_i, P_{i+1}, P_{i+2}
        val y0 = controlPoints[(segIndex - 1).coerceAtLeast(0)].second
        val y1 = controlPoints[segIndex].second
        val y2 = controlPoints[(segIndex + 1).coerceAtMost(n - 1)].second
        val y3 = controlPoints[(segIndex + 2).coerceAtMost(n - 1)].second

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
     * @param steps number of horizontal sample points for smoothness (default 80)
     */
    fun terrainPath(screenWidth: Float, screenHeight: Float, steps: Int = 80): Path {
        return Path().apply {
            // Start at left edge, terrain height
            val startY = terrainY(0f, screenHeight)
            moveTo(0f, startY)

            // Trace the terrain curve left to right
            for (i in 1..steps) {
                val xFrac = i.toFloat() / steps
                val px = xFrac * screenWidth
                val py = terrainY(xFrac, screenHeight)
                lineTo(px, py)
            }

            // Close the path: go to bottom-right, bottom-left, then back to start
            lineTo(screenWidth, screenHeight)
            lineTo(0f, screenHeight)
            close()
        }
    }

    /**
     * Catmull-Rom spline interpolation between p1 and p2,
     * using p0 and p3 as tangent guides. t in [0..1].
     *
     * Standard Catmull-Rom formula with alpha=0.5 (uniform):
     *   q(t) = 0.5 * ((2*p1) +
     *          (-p0 + p2) * t +
     *          (2*p0 - 5*p1 + 4*p2 - p3) * t^2 +
     *          (-p0 + 3*p1 - 3*p2 + p3) * t^3)
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
