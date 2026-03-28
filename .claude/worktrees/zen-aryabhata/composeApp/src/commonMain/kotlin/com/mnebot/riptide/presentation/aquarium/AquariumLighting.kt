package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

// ── Sunlight ray data ─────────────────────────────────────────────────────────

private data class SunRay(
    val xFraction: Float,  // position at surface (0..1)
    val width: Float,      // ray width at surface (fraction of screen width)
    val spreadFactor: Float, // how much the ray widens as it descends
    val angle: Float,      // slight tilt in radians (-0.15..0.15)
    val phase: Float,      // oscillation phase offset
    val alpha: Float       // base opacity (0.03..0.08)
)

private val sunRays = listOf(
    SunRay(0.18f, 0.06f, 1.8f, -0.08f, 0.0f, 0.06f),
    SunRay(0.38f, 0.08f, 2.2f, 0.05f, 1.3f, 0.07f),
    SunRay(0.55f, 0.05f, 1.6f, -0.12f, 2.8f, 0.05f),
    SunRay(0.72f, 0.07f, 2.0f, 0.10f, 4.1f, 0.065f),
    SunRay(0.88f, 0.04f, 1.5f, -0.06f, 5.5f, 0.045f),
)

// ── Caustic colors ────────────────────────────────────────────────────────────

private val CausticColor = Color(0xFFB0E0FF)

/**
 * Draws crepuscular light rays penetrating the water from the surface.
 * Visible only during daytime (roughly 7:00-18:00).
 * Intensity modulated by cloud coverage.
 *
 * @param elapsedMs elapsed time for animation
 * @param surfaceY Y coordinate of water surface
 * @param floorY Y coordinate of average floor level
 * @param hourFraction current hour as float (0-24)
 * @param weather current weather state
 */
internal fun DrawScope.drawSunRays(
    elapsedMs: Long,
    surfaceY: Float,
    floorY: Float,
    hourFraction: Float,
    weather: WeatherState
) {
    // Only visible during daytime
    if (hourFraction < 6.5f || hourFraction > 19f) return

    // Intensity curve: ramp up 6:30-8, full 8-17, ramp down 17-19
    val timeIntensity = when {
        hourFraction < 8f -> (hourFraction - 6.5f) / 1.5f
        hourFraction > 17f -> (19f - hourFraction) / 2f
        else -> 1f
    }.coerceIn(0f, 1f)

    // Cloud coverage reduces ray visibility
    val cloudFactor = (1f - weather.cloudCoverage * 0.85f).coerceIn(0.1f, 1f)
    val baseAlpha = timeIntensity * cloudFactor

    if (baseAlpha < 0.01f) return

    val t = elapsedMs / 1000.0
    val w = size.width
    val waterDepth = floorY - surfaceY

    sunRays.forEach { ray ->
        // Slow angular oscillation
        val angleOffset = sin(t * 0.15 + ray.phase).toFloat() * 0.04f
        val currentAngle = ray.angle + angleOffset

        val topWidth = w * ray.width
        val bottomWidth = topWidth * ray.spreadFactor
        val xShift = sin(currentAngle) * waterDepth

        // Ray top (at surface)
        val topLeft = Offset(w * ray.xFraction - topWidth / 2f, surfaceY)
        val topRight = Offset(w * ray.xFraction + topWidth / 2f, surfaceY)
        // Ray bottom (at floor)
        val bottomLeft = Offset(w * ray.xFraction - bottomWidth / 2f + xShift, floorY)
        val bottomRight = Offset(w * ray.xFraction + bottomWidth / 2f + xShift, floorY)

        val path = Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(topRight.x, topRight.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        }

        // Vertical gradient: visible at top, fades toward bottom
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = ray.alpha * baseAlpha),
                    0.3f to Color.White.copy(alpha = ray.alpha * baseAlpha * 0.5f),
                    0.7f to Color.White.copy(alpha = ray.alpha * baseAlpha * 0.15f),
                    1.0f to Color.Transparent
                ),
                startY = surfaceY,
                endY = floorY
            )
        )
    }
}

/**
 * Draws animated caustic light patterns on the sea floor.
 * Creates a network of bright lines that shift and morph over time.
 * Only visible during daytime when there's enough light.
 *
 * @param elapsedMs elapsed time for animation
 * @param surfaceY Y coordinate of water surface
 * @param floorY Y coordinate of average floor level
 * @param hourFraction current hour as float (0-24)
 * @param weather current weather state
 */
internal fun DrawScope.drawCaustics(
    elapsedMs: Long,
    surfaceY: Float,
    floorY: Float,
    hourFraction: Float,
    weather: WeatherState
) {
    // Only visible during daytime
    if (hourFraction < 7f || hourFraction > 18f) return

    // Intensity based on time and clouds
    val timeIntensity = when {
        hourFraction < 8.5f -> (hourFraction - 7f) / 1.5f
        hourFraction > 16.5f -> (18f - hourFraction) / 1.5f
        else -> 1f
    }.coerceIn(0f, 1f)

    val cloudFactor = (1f - weather.cloudCoverage * 0.75f).coerceIn(0.05f, 1f)
    val intensity = timeIntensity * cloudFactor

    if (intensity < 0.02f) return

    val t = elapsedMs / 1000.0
    val w = size.width

    // Caustic band sits on the floor region (floorY to floorY + 12% of water depth)
    val causticTop = floorY - (floorY - surfaceY) * 0.05f
    val causticBottom = floorY + (size.height - floorY) * 0.3f
    val causticHeight = causticBottom - causticTop

    // Draw overlapping sinusoidal bright lines
    val lineCount = 8
    val baseAlpha = 0.06f * intensity

    for (i in 0 until lineCount) {
        val hash = (i * 3571 + 997)
        val yBase = causticTop + (hash % 1000) / 1000f * causticHeight
        val period1 = 180f + (hash % 60)
        val period2 = 130f + ((hash * 7) % 80)
        val amplitude = 3f + (hash % 4)
        val phaseOff = (hash % 628) / 100f
        val speed1 = 0.3 + ((hash * 3) % 100) / 300.0
        val speed2 = 0.4 + ((hash * 11) % 100) / 250.0

        val path = Path().apply {
            val steps = 20
            val stepW = w / steps
            for (s in 0..steps) {
                val x = s * stepW
                // Two overlapping sine waves with different frequencies
                val y1 = sin(x / period1 * 2.0 * PI + t * speed1 + phaseOff).toFloat() * amplitude
                val y2 = sin(x / period2 * 2.0 * PI + t * speed2 + phaseOff * 1.7f).toFloat() * amplitude * 0.6f
                val y = yBase + y1 + y2

                if (s == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        // Alpha fades from center outward (depth-based)
        val depthFade = 1f - ((yBase - causticTop) / causticHeight).coerceIn(0f, 1f) * 0.6f

        drawPath(
            path = path,
            color = CausticColor.copy(alpha = baseAlpha * depthFade),
            style = Stroke(
                width = (1.2f + (hash % 2) * 0.5f).dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}
