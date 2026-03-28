package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mnebot.riptide.presentation.aquarium.weather.WeatherState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Particle data ─────────────────────────────────────────────────────────────

private data class PlanktonParticle(
    val baseX: Float,     // 0..1 fraction of width
    val baseY: Float,     // 0..1 fraction of water column
    val size: Float,      // dp
    val speed: Float,     // drift speed multiplier
    val phase: Float,     // animation phase offset
    val alpha: Float      // base opacity
)

private data class SedimentParticle(
    val baseX: Float,
    val fallSpeed: Float, // fall speed multiplier
    val size: Float,
    val phase: Float,
    val alpha: Float
)

// ── Deterministic particle pools ──────────────────────────────────────────────

private val planktonParticles: List<PlanktonParticle> = (0 until 20).map { i ->
    val hash = (i * 7919 + 1013) // deterministic pseudo-random
    PlanktonParticle(
        baseX = (hash % 1000) / 1000f,
        baseY = ((hash * 3) % 1000) / 1000f,
        size = 2f + (hash % 3),
        speed = 0.6f + ((hash * 7) % 100) / 200f,
        phase = (hash % 628) / 100f,
        alpha = 0.15f + ((hash * 11) % 100) / 400f  // 0.15..0.40
    )
}

private val sedimentParticles: List<SedimentParticle> = (0 until 12).map { i ->
    val hash = (i * 6271 + 2017)
    SedimentParticle(
        baseX = (hash % 1000) / 1000f,
        fallSpeed = 0.3f + ((hash * 3) % 100) / 200f,
        size = 1f + (hash % 2),
        phase = (hash % 628) / 100f,
        alpha = 0.12f + ((hash * 7) % 100) / 500f  // 0.12..0.32
    )
}

// ── Colors ────────────────────────────────────────────────────────────────────

private val PlanktonColor = Color(0xFF7EC8E3)    // bright cyan-blue
private val SedimentColor = Color(0xFFB89C70)    // sandy
private val CurrentColor = Color(0xFF4A8EB0)     // muted blue

// ── Drawing functions ─────────────────────────────────────────────────────────

/**
 * Draws ambient particles: plankton, sediment, and current streaks.
 * Call after background and before creatures.
 *
 * @param elapsedMs elapsed time in milliseconds
 * @param surfaceY Y coordinate of water surface
 * @param floorY Y coordinate of average floor level
 * @param weather current weather state (windStrength affects currents)
 */
internal fun DrawScope.drawAmbientParticles(
    elapsedMs: Long,
    surfaceY: Float,
    floorY: Float,
    weather: WeatherState
) {
    val waterRange = floorY - surfaceY
    if (waterRange <= 0f) return

    drawPlankton(elapsedMs, surfaceY, waterRange)
    drawSediment(elapsedMs, surfaceY, waterRange)
    drawCurrentStreaks(elapsedMs, surfaceY, waterRange, weather)
}

private fun DrawScope.drawPlankton(
    elapsedMs: Long,
    surfaceY: Float,
    waterRange: Float
) {
    val t = elapsedMs / 1000.0
    val w = size.width

    planktonParticles.forEach { p ->
        // Brownian drift: slow, organic movement
        val driftX = sin(t * 0.3 * p.speed + p.phase).toFloat() * w * 0.04f
        val driftY = cos(t * 0.2 * p.speed + p.phase * 1.7f).toFloat() * waterRange * 0.03f

        val x = (p.baseX * w + driftX) % w
        val y = surfaceY + p.baseY * waterRange + driftY

        // Stay within water bounds
        if (y < surfaceY || y > surfaceY + waterRange) return@forEach

        val radius = p.size.dp.toPx()

        // Subtle glow effect: outer circle + inner bright dot
        drawCircle(
            color = PlanktonColor.copy(alpha = p.alpha * 0.4f),
            radius = radius * 1.8f,
            center = Offset(x, y)
        )
        drawCircle(
            color = PlanktonColor.copy(alpha = p.alpha),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawSediment(
    elapsedMs: Long,
    surfaceY: Float,
    waterRange: Float
) {
    val t = elapsedMs / 1000.0
    val w = size.width
    val cycleLength = 25.0 // seconds for a full fall cycle

    sedimentParticles.forEach { p ->
        // Slow downward drift with slight horizontal wobble
        val fallProgress = ((t * p.fallSpeed / cycleLength) + p.phase / 6.28f) % 1.0
        val wobbleX = sin(t * 0.5 + p.phase).toFloat() * 6.dp.toPx()

        val x = p.baseX * w + wobbleX
        val y = surfaceY + fallProgress.toFloat() * waterRange

        if (y < surfaceY || y > surfaceY + waterRange) return@forEach

        val radius = p.size.dp.toPx()

        // Fade in at top, fade out at bottom
        val verticalFade = when {
            fallProgress < 0.1 -> (fallProgress / 0.1).toFloat()
            fallProgress > 0.85 -> ((1.0 - fallProgress) / 0.15).toFloat()
            else -> 1f
        }

        drawCircle(
            color = SedimentColor.copy(alpha = p.alpha * verticalFade),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawCurrentStreaks(
    elapsedMs: Long,
    surfaceY: Float,
    waterRange: Float,
    weather: WeatherState
) {
    // Current streaks are subtle and tied to wind strength
    if (weather.windStrength < 0.15f) return

    val t = elapsedMs / 1000.0
    val w = size.width
    val streakAlpha = (weather.windStrength * 0.12f).coerceAtMost(0.10f)
    val streakLength = 20.dp.toPx() + weather.windStrength * 30.dp.toPx()

    // 4-6 current streaks at various depths
    val streakCount = (3 + (weather.windStrength * 4).toInt()).coerceAtMost(6)

    for (i in 0 until streakCount) {
        val hash = (i * 4919 + 773)
        val baseY = surfaceY + (hash % 1000) / 1000f * waterRange * 0.8f
        val speed = 0.03f + ((hash * 3) % 100) / 2000f
        val phase = (hash % 628) / 100f

        // Horizontal position cycles across screen
        val xProgress = ((t * speed + phase / 6.28) % 1.2 - 0.1).toFloat()
        val x = xProgress * w

        // Slight vertical undulation
        val yOffset = sin(t * 0.4 + phase).toFloat() * 8.dp.toPx()
        val y = baseY + yOffset

        if (x < -streakLength || x > w + streakLength) continue
        if (y < surfaceY || y > surfaceY + waterRange) continue

        // Draw as a thin line
        drawLine(
            color = CurrentColor.copy(alpha = streakAlpha),
            start = Offset(x, y),
            end = Offset(x + streakLength, y + 1.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
