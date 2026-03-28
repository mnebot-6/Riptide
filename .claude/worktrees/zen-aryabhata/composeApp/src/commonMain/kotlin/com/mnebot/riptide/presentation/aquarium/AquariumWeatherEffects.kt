package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mnebot.riptide.presentation.aquarium.weather.WeatherState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Cloud data ────────────────────────────────────────────────────────────────

private data class CloudShape(
    val xFraction: Float,   // base horizontal position (0..1)
    val yFraction: Float,   // vertical position within sky zone (0..0.8)
    val width: Float,       // cloud width as fraction of screen
    val height: Float,      // cloud height as fraction of sky zone
    val driftSpeed: Float,  // horizontal drift speed multiplier
    val blobCount: Int,     // number of overlapping ellipses
    val phase: Float        // animation phase offset
)

private val clouds = listOf(
    CloudShape(0.10f, 0.20f, 0.18f, 0.35f, 0.8f, 4, 0.0f),
    CloudShape(0.35f, 0.45f, 0.22f, 0.30f, 1.1f, 5, 1.5f),
    CloudShape(0.58f, 0.15f, 0.15f, 0.28f, 0.6f, 3, 3.2f),
    CloudShape(0.78f, 0.55f, 0.20f, 0.32f, 0.9f, 4, 4.8f),
    CloudShape(0.92f, 0.30f, 0.16f, 0.25f, 1.3f, 3, 6.0f),
)

// ── Rain drop data ────────────────────────────────────────────────────────────

private data class RainDrop(
    val xFraction: Float,
    val speed: Float,       // fall speed multiplier
    val length: Float,      // drop length in dp
    val phase: Float        // start offset
)

private val rainDrops: List<RainDrop> = (0 until 40).map { i ->
    val hash = (i * 8191 + 2039)
    RainDrop(
        xFraction = (hash % 1000) / 1000f,
        speed = 0.7f + ((hash * 3) % 100) / 200f,
        length = 4f + ((hash * 7) % 100) / 20f,
        phase = (hash % 1000) / 1000f
    )
}

// ── Colors ────────────────────────────────────────────────────────────────────

private val CloudColor = Color(0xFFCCDDEE)
private val RainColor = Color(0xFF8EBBDD)
private val RippleColor = Color(0xFF7EC8E3)

/**
 * Draws clouds above the water surface.
 * Cloud opacity and count depend on weather.cloudCoverage.
 *
 * @param elapsedMs elapsed time for animation
 * @param surfaceY Y coordinate of water surface
 * @param weather current weather state
 */
internal fun DrawScope.drawClouds(
    elapsedMs: Long,
    surfaceY: Float,
    weather: WeatherState
) {
    if (weather.cloudCoverage < 0.05f) return

    val t = elapsedMs / 1000.0
    val w = size.width
    val skyHeight = surfaceY

    // Number of visible clouds based on coverage
    val visibleCount = (clouds.size * weather.cloudCoverage * 1.2f).toInt().coerceIn(1, clouds.size)
    val baseAlpha = (weather.cloudCoverage * 0.55f).coerceIn(0.05f, 0.45f)

    clouds.take(visibleCount).forEach { cloud ->
        // Horizontal drift based on wind
        val driftOffset = (t * weather.windStrength * cloud.driftSpeed * 0.015).toFloat()
        val baseX = ((cloud.xFraction + driftOffset) % 1.3f - 0.15f) * w

        val cloudW = w * cloud.width
        val cloudH = skyHeight * cloud.height
        val cloudY = skyHeight * cloud.yFraction

        // Draw cloud as overlapping ellipses (blobs)
        for (b in 0 until cloud.blobCount) {
            val blobFraction = b.toFloat() / cloud.blobCount
            val blobX = baseX + (blobFraction - 0.5f) * cloudW * 0.8f
            val blobW = cloudW * (0.4f + blobFraction * 0.2f)
            val blobH = cloudH * (0.5f + (1f - blobFraction) * 0.3f)
            val blobY = cloudY - blobH * 0.3f * sin(blobFraction * PI.toFloat())

            drawOval(
                color = CloudColor.copy(alpha = baseAlpha * (0.7f + blobFraction * 0.3f)),
                topLeft = Offset(blobX - blobW / 2, blobY),
                size = androidx.compose.ui.geometry.Size(blobW, blobH)
            )
        }
    }
}

/**
 * Draws rain drops falling from the sky to the water surface.
 * Only active when weather.rainIntensity > 0.
 *
 * @param elapsedMs elapsed time for animation
 * @param surfaceY Y coordinate of water surface
 * @param weather current weather state
 */
internal fun DrawScope.drawRain(
    elapsedMs: Long,
    surfaceY: Float,
    weather: WeatherState
) {
    if (weather.rainIntensity < 0.05f) return

    val t = elapsedMs / 1000.0
    val w = size.width

    // Number of active drops based on intensity
    val activeDrops = (rainDrops.size * weather.rainIntensity).toInt().coerceIn(1, rainDrops.size)
    val dropAlpha = (weather.rainIntensity * 0.5f).coerceIn(0.1f, 0.4f)
    val fallDuration = 1.2 // seconds for a drop to fall from top to surface

    rainDrops.take(activeDrops).forEach { drop ->
        // Wind-influenced horizontal position
        val windOffset = weather.windStrength * 0.05f * surfaceY
        val x = drop.xFraction * w + windOffset

        // Vertical progress: cycle based on speed
        val progress = ((t * drop.speed / fallDuration + drop.phase) % 1.0).toFloat()

        val dropTop = progress * surfaceY
        val dropLength = drop.length.dp.toPx()
        val dropBottom = (dropTop + dropLength).coerceAtMost(surfaceY)

        // Only draw if within sky zone
        if (dropTop > surfaceY) return@forEach

        // Fade in at top
        val fadeAlpha = if (progress < 0.1f) progress / 0.1f else 1f

        drawLine(
            color = RainColor.copy(alpha = dropAlpha * fadeAlpha),
            start = Offset(x, dropTop),
            end = Offset(x + weather.windStrength * 3.dp.toPx(), dropBottom),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Ripple on surface when drop hits
        if (progress > 0.85f) {
            val rippleProgress = (progress - 0.85f) / 0.15f
            val rippleRadius = 2.dp.toPx() + rippleProgress * 4.dp.toPx()
            val rippleAlpha = (1f - rippleProgress) * dropAlpha * 0.6f

            drawCircle(
                color = RippleColor.copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = Offset(x, surfaceY),
                style = Stroke(width = 0.8f.dp.toPx())
            )
        }
    }
}
