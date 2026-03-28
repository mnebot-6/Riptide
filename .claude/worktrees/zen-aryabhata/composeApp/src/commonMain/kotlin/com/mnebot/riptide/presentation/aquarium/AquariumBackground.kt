package com.mnebot.riptide.presentation.aquarium

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mnebot.riptide.presentation.aquarium.weather.RandomWeatherProvider
import com.mnebot.riptide.presentation.aquarium.weather.WeatherState
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.sin
import androidx.compose.runtime.withFrameMillis

// ── Paleta del agua ────────────────────────────────────────────────────────────
private val OceanShallow = Color(0xFF2E5F9E)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanDeep = Color(0xFF0A1628)

// ── Cielo con interpolación continua ─────────────────────────────────────────
internal data class SkyColors(val top: Color, val mid: Color, val horizon: Color)

private data class SkyKeyframe(val hour: Float, val top: Color, val mid: Color, val horizon: Color)

private val skyKeyframes = listOf(
    //                            hour    top (zenith)            mid                      horizon
    SkyKeyframe( 0.0f, Color(0xFF060B1A), Color(0xFF0A1228), Color(0xFF0D1B3A)),  // medianoche
    SkyKeyframe( 5.0f, Color(0xFF0A1030), Color(0xFF1A1840), Color(0xFF2A2050)),  // pre-amanecer
    SkyKeyframe( 6.0f, Color(0xFF4A3060), Color(0xFFB85C2A), Color(0xFFE8A048)),  // amanecer pico
    SkyKeyframe( 7.0f, Color(0xFF6A80C0), Color(0xFFD4824A), Color(0xFFF0C060)),  // mañana dorada
    SkyKeyframe(10.0f, Color(0xFF3A78CC), Color(0xFF4A90D9), Color(0xFF88C8F0)),  // día pleno
    SkyKeyframe(17.5f, Color(0xFF4A88D0), Color(0xFF7090C0), Color(0xFFA0B0D0)),  // pre-atardecer
    SkyKeyframe(18.5f, Color(0xFF6A3050), Color(0xFFCC5530), Color(0xFFE88840)),  // atardecer pico
    SkyKeyframe(19.5f, Color(0xFF3A1848), Color(0xFF6A2878), Color(0xFF9A5098)),  // crepúsculo
    SkyKeyframe(21.0f, Color(0xFF0D0820), Color(0xFF121030), Color(0xFF1A1040)),  // noche temprana
    SkyKeyframe(24.0f, Color(0xFF060B1A), Color(0xFF0A1228), Color(0xFF0D1B3A)),  // = medianoche
)

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f
    )
}

internal fun interpolateSky(hourFraction: Float): SkyColors {
    val h = hourFraction.coerceIn(0f, 24f)
    // Encontrar los dos keyframes adyacentes
    var lo = skyKeyframes.last { it.hour <= h }
    var hi = skyKeyframes.first { it.hour > h }
    // Caso borde: si estamos exactamente en 24.0
    if (lo.hour == hi.hour) return SkyColors(lo.top, lo.mid, lo.horizon)
    val t = (h - lo.hour) / (hi.hour - lo.hour)
    return SkyColors(
        top = lerpColor(lo.top, hi.top, t),
        mid = lerpColor(lo.mid, hi.mid, t),
        horizon = lerpColor(lo.horizon, hi.horizon, t)
    )
}

internal fun getCurrentHourFraction(): Float {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return now.hour + now.minute / 60f
}

// ── Fondo marino (paleta desaturada) ──────────────────────────────────────────
private val SandLight = Color(0xFFCBB99A)
private val SandMid = Color(0xFFA89070)
private val SandDark = Color(0xFF7A6A50)
private val RockLight = Color(0xFF6A5E48)
private val RockMid = Color(0xFF504434)
private val RockDark = Color(0xFF352E22)
private val WaveCrest = Color(0x40FFFFFF)
private val BubbleColor = Color(0x337EC8E3)

// ── Rocas del fondo con capas de profundidad ─────────────────────────────────
private data class RockData(
    val cx: Float,         // x fraction [0..1]
    val wFrac: Float,      // width as fraction of screen width
    val hFrac: Float,      // height as fraction of screen height
    val style: Int = 0,    // 0=smooth, 1=angular, 2=irregular
    val layer: Int = 1     // 0=background, 1=mid, 2=foreground
)

// Background layer: small, muted, higher Y (further from viewer)
private val bgRocks = listOf(
    RockData(0.06f, 0.055f, 0.016f, 0, 0),
    RockData(0.30f, 0.050f, 0.014f, 2, 0),
    RockData(0.55f, 0.060f, 0.018f, 1, 0),
    RockData(0.78f, 0.048f, 0.015f, 0, 0),
    RockData(0.95f, 0.052f, 0.013f, 2, 0),
)

// Mid layer: medium rocks at terrain level
private val midRocks = listOf(
    RockData(0.12f, 0.07f, 0.028f, 0, 1),
    RockData(0.38f, 0.06f, 0.022f, 2, 1),
    RockData(0.60f, 0.08f, 0.032f, 0, 1),
    RockData(0.82f, 0.07f, 0.026f, 1, 1),
    RockData(0.48f, 0.05f, 0.018f, 1, 1),
)

// Foreground layer: large, slightly darker, drawn last
private val fgRocks = listOf(
    RockData(0.08f, 0.11f, 0.042f, 1, 2),
    RockData(0.52f, 0.14f, 0.050f, 2, 2),
    RockData(0.72f, 0.10f, 0.038f, 0, 2),
    RockData(0.90f, 0.12f, 0.044f, 1, 2),
)

// ── Burbujas ───────────────────────────────────────────────────────────────────
private data class BubbleData(val x: Float, val size: Float, val speed: Float, val startOffset: Float)

private val bubbles = listOf(
    BubbleData(0.15f, 4f, 6000f, 0.0f),
    BubbleData(0.28f, 3f, 5000f, 0.3f),
    BubbleData(0.42f, 6f, 7000f, 0.6f),
    BubbleData(0.55f, 4f, 5500f, 0.1f),
    BubbleData(0.67f, 5f, 6500f, 0.8f),
    BubbleData(0.80f, 3f, 4500f, 0.4f),
    BubbleData(0.90f, 6f, 7500f, 0.7f),
)

@Composable
fun AquariumBackground(modifier: Modifier = Modifier) {
    // Hora fraccionaria con actualización cada 60s
    var hourFraction by remember { mutableFloatStateOf(getCurrentHourFraction()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            hourFraction = getCurrentHourFraction()
        }
    }
    val sky = remember(hourFraction) { interpolateSky(hourFraction) }

    // Weather provider (session-unique random weather)
    val weatherProvider = remember { RandomWeatherProvider() }

    // Elapsed time tracking via frame clock
    var elapsedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val startTime = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameTime ->
                elapsedMs = frameTime - startTime
            }
        }
    }

    val swayAnim = rememberInfiniteTransition(label = "sway")
    val swayAngle by swayAnim.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swayAngle"
    )

    val bubbleAnim = rememberInfiniteTransition(label = "bubbles")
    val bubbleProgress by bubbleAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleProgress"
    )

    val weather = remember(elapsedMs / 1000) { weatherProvider.currentWeather(elapsedMs) }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawAquariumBackground(swayAngle, bubbleProgress, sky, weather, elapsedMs, hourFraction)
    }
}

/**
 * Draws the full aquarium background: sky, ocean gradient, sea floor, water surface, and bubbles.
 * Extracted as a standalone DrawScope function so it can be reused from WallpaperService via CanvasDrawScope.
 *
 * @param swayAngle oscillating value for wave animation
 * @param bubbleProgress 0..1 progress for bubble cycle
 * @param sky interpolated sky colors for current time
 * @param weather current weather state (clouds, rain, wind, wave amplitude)
 * @param elapsedMs elapsed time in milliseconds for particle/lighting animation
 * @param hourFraction current hour as float (0-24) for sun ray/caustic visibility
 */
internal fun DrawScope.drawAquariumBackground(
    swayAngle: Float,
    bubbleProgress: Float,
    sky: SkyColors,
    weather: WeatherState = WeatherState.Clear,
    elapsedMs: Long = 0L,
    hourFraction: Float = 12f
) {
    val surfaceY = AquariumBounds.surfaceY(size.height)
    val floorY = AquariumBounds.floorY(size.height)

    // 1. Gradiente de agua (del cielo hacia la profundidad)
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to sky.horizon,
                AquariumBounds.SURFACE_FRACTION to OceanShallow,
                0.45f to OceanMid,
                AquariumBounds.FLOOR_FRACTION to OceanDeep,
            ),
            startY = 0f,
            endY = floorY
        )
    )

    // 2. Cielo sobre la superficie (gradiente dinámico de 3 colores)
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to sky.top,
                0.45f to sky.mid,
                1.00f to sky.horizon
            ),
            startY = 0f,
            endY = surfaceY
        )
    )

    // 3. Clouds above the water surface
    drawClouds(elapsedMs, surfaceY, weather)

    // 4. Rain from clouds to surface
    drawRain(elapsedMs, surfaceY, weather)

    // 5. Fondo marino elaborado
    drawSeaFloor(floorY)

    // 6. Sun rays (after floor, before creatures)
    drawSunRays(elapsedMs, surfaceY, floorY, hourFraction, weather)

    // 7. Caustic light patterns on sea floor
    drawCaustics(elapsedMs, surfaceY, floorY, hourFraction, weather)

    // 8. Ambient particles (plankton, sediment, current streaks)
    drawAmbientParticles(elapsedMs, surfaceY, floorY, weather)

    // 9. Superficie del agua (ola con amplitud modulada por clima)
    drawWaterSurface(surfaceY, swayAngle, sky.horizon, weather)

    // 10. Burbujas (nacen del suelo, se desvanecen antes de la superficie)
    drawBubbles(bubbleProgress, surfaceY, floorY)
}

private fun DrawScope.drawSeaFloor(floorY: Float) {
    val w = size.width
    val h = size.height

    // 1. Sand fill following the terrain curve
    val sandPath = AquariumTerrain.terrainPath(w, h)
    drawPath(
        sandPath,
        brush = Brush.verticalGradient(
            listOf(SandLight, SandMid, SandDark),
            startY = AquariumTerrain.terrainY(0.35f, h) - h * 0.02f, // start near highest hill
            endY = h
        )
    )

    // 2. Shadow transition zone along the terrain contour
    val shadowSteps = 60
    val shadowPath = Path().apply {
        val startY = AquariumTerrain.terrainY(0f, h)
        moveTo(0f, startY - h * 0.025f)
        for (i in 1..shadowSteps) {
            val xFrac = i.toFloat() / shadowSteps
            val ty = AquariumTerrain.terrainY(xFrac, h)
            lineTo(xFrac * w, ty - h * 0.025f)
        }
        // Bottom part: terrain + offset downward
        for (i in shadowSteps downTo 0) {
            val xFrac = i.toFloat() / shadowSteps
            val ty = AquariumTerrain.terrainY(xFrac, h)
            lineTo(xFrac * w, ty + h * 0.015f)
        }
        close()
    }
    drawPath(shadowPath, OceanDeep.copy(alpha = 0.45f))

    // 3. Background rocks (small, muted, behind everything)
    bgRocks.sortedBy { it.cx }.forEach { rock ->
        val cx = w * rock.cx
        val rw = w * rock.wFrac
        val rh = h * rock.hFrac
        val baseY = AquariumTerrain.terrainY(rock.cx, h) + h * 0.008f
        drawRock(cx, baseY, rw, rh, rock.style, alpha = 0.40f, tint = -1)
    }

    // 4. Texture lines that follow the terrain contour (offset downward)
    val sandLineColor = SandMid.copy(alpha = 0.35f)
    val lineStroke = 1.dp.toPx()
    for (i in 1..3) {
        val yOffset = (h - floorY) * i / 4f
        val linePath = Path().apply {
            val startY = AquariumTerrain.terrainY(0f, h) + yOffset
            moveTo(0f, startY)
            for (seg in 0 until 8) {
                val xMid = (seg + 0.5f) / 8f
                val xEnd = (seg + 1f) / 8f
                val terrainMid = AquariumTerrain.terrainY(xMid, h) + yOffset
                val terrainEnd = AquariumTerrain.terrainY(xEnd, h) + yOffset
                val wobble = if (seg % 2 == 0) 2.dp.toPx() else -2.dp.toPx()
                quadraticTo(xMid * w, terrainMid + wobble, xEnd * w, terrainEnd)
            }
        }
        drawPath(linePath, sandLineColor, style = Stroke(width = lineStroke, cap = StrokeCap.Round))
    }

    // 5. Pebbles scattered along the terrain surface (deterministic)
    val pebbleCount = 25
    for (i in 0 until pebbleCount) {
        // Deterministic pseudo-random based on index
        val seed = (i * 7 + 13)
        val xFrac = (seed * 0.0397f) % 1f
        val px = xFrac * w
        val py = AquariumTerrain.terrainY(xFrac, h) + ((seed * 3) % 5) * 0.5f.dp.toPx()
        val radius = (1f + (seed % 3)).dp.toPx()
        val pebbleAlpha = 0.25f + (seed % 4) * 0.05f
        val pebbleColor = when (seed % 3) {
            0 -> SandDark.copy(alpha = pebbleAlpha)
            1 -> RockLight.copy(alpha = pebbleAlpha)
            else -> SandMid.copy(alpha = pebbleAlpha + 0.1f)
        }
        drawCircle(color = pebbleColor, radius = radius, center = Offset(px, py))
    }

    // 6. Mid-layer rocks at terrain level
    midRocks.sortedBy { it.cx }.forEach { rock ->
        val cx = w * rock.cx
        val rw = w * rock.wFrac
        val rh = h * rock.hFrac
        val baseY = AquariumTerrain.terrainY(rock.cx, h)
        drawRock(cx, baseY, rw, rh, rock.style)
    }

    // 7. Foreground rocks (large, slightly darker, drawn last)
    fgRocks.sortedBy { it.cx }.forEach { rock ->
        val cx = w * rock.cx
        val rw = w * rock.wFrac
        val rh = h * rock.hFrac
        val baseY = AquariumTerrain.terrainY(rock.cx, h) - h * 0.005f
        drawRock(cx, baseY, rw, rh, rock.style, tint = 1)
    }
}

/**
 * Draws a single rock at the given position.
 * @param alpha overall opacity (0.4 for background layer, 1.0 for mid/foreground)
 * @param tint -1 = lighter/muted (background), 0 = normal (mid), 1 = darker (foreground)
 */
private fun DrawScope.drawRock(cx: Float, floorY: Float, rw: Float, rh: Float, style: Int, alpha: Float = 1f, tint: Int = 0) {
    val path = when (style) {
        0 -> Path().apply {  // forma suave redondeada
            moveTo(cx - rw / 2f, floorY)
            cubicTo(cx - rw * 0.5f, floorY - rh * 0.5f, cx - rw * 0.2f, floorY - rh, cx, floorY - rh * 1.05f)
            cubicTo(cx + rw * 0.2f, floorY - rh, cx + rw * 0.5f, floorY - rh * 0.5f, cx + rw / 2f, floorY)
            close()
        }
        1 -> Path().apply {  // forma angulosa (piedra más plana)
            moveTo(cx - rw / 2f, floorY)
            lineTo(cx - rw * 0.4f, floorY - rh * 0.7f)
            lineTo(cx - rw * 0.1f, floorY - rh)
            lineTo(cx + rw * 0.25f, floorY - rh * 0.9f)
            lineTo(cx + rw / 2f, floorY - rh * 0.4f)
            lineTo(cx + rw * 0.45f, floorY)
            close()
        }
        else -> Path().apply {  // forma irregular (canto rodado)
            moveTo(cx - rw / 2f, floorY)
            quadraticTo(cx - rw * 0.55f, floorY - rh * 0.6f, cx - rw * 0.15f, floorY - rh)
            quadraticTo(cx + rw * 0.1f, floorY - rh * 1.1f, cx + rw * 0.35f, floorY - rh * 0.85f)
            quadraticTo(cx + rw * 0.6f, floorY - rh * 0.3f, cx + rw / 2f, floorY)
            close()
        }
    }
    val rockColor = when (tint) {
        -1 -> RockLight.copy(alpha = alpha)   // background: lighter, muted
         1 -> RockDark.copy(alpha = alpha)     // foreground: darker
        else -> RockMid.copy(alpha = alpha)    // mid: normal
    }
    drawPath(path, rockColor)

    // Highlight en la parte superior izquierda (luz)
    val hlPath = Path().apply {
        moveTo(cx - rw * 0.15f, floorY - rh * 0.85f)
        quadraticTo(cx - rw * 0.3f, floorY - rh * 0.6f, cx - rw * 0.25f, floorY - rh * 0.4f)
    }
    drawPath(hlPath, RockLight.copy(alpha = 0.5f * alpha), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

    // Sombra en la base
    val shadowPath = Path().apply {
        moveTo(cx - rw * 0.4f, floorY)
        quadraticTo(cx, floorY + 3.dp.toPx(), cx + rw * 0.4f, floorY)
    }
    drawPath(shadowPath, RockDark.copy(alpha = 0.5f * alpha), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawWaterSurface(
    surfaceY: Float,
    swayAngle: Float,
    skyHorizon: Color,
    weather: WeatherState = WeatherState.Clear
) {
    val w = size.width
    val waveAmplitude = 4.dp.toPx() * weather.waveAmplitudeMultiplier
    val segments = 6

    // Área sobre la ola (cielo reflejado)
    val wavePath = Path().apply {
        moveTo(0f, 0f)
        lineTo(0f, surfaceY)
        val segWidth = w / segments
        for (i in 0 until segments) {
            val x1 = segWidth * (i + 0.5f)
            val x2 = segWidth * (i + 1)
            val dir = if (i % 2 == 0) 1f else -0.7f
            val offset = swayAngle * waveAmplitude * dir
            quadraticTo(x1, surfaceY + offset, x2, surfaceY)
        }
        lineTo(w, 0f)
        close()
    }
    drawPath(wavePath, skyHorizon.copy(alpha = 0.35f))

    // Cresta de ola (blanco suave)
    val crestPath = Path().apply {
        moveTo(0f, surfaceY)
        val segWidth = w / segments
        for (i in 0 until segments) {
            val x1 = segWidth * (i + 0.5f)
            val x2 = segWidth * (i + 1)
            val dir = if (i % 2 == 0) 1f else -0.7f
            val offset = swayAngle * waveAmplitude * dir
            quadraticTo(x1, surfaceY + offset, x2, surfaceY)
        }
    }
    drawPath(crestPath, WaveCrest, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawBubbles(
    bubbleProgress: Float,
    surfaceY: Float,
    floorY: Float
) {
    val waterRange = floorY - surfaceY
    bubbles.forEach { bubble ->
        val adjustedProgress = (bubbleProgress + bubble.startOffset) % 1f
        val x = size.width * bubble.x + sin(adjustedProgress * 6.28f) * 8.dp.toPx()
        val y = floorY - adjustedProgress * waterRange
        val radius = bubble.size.dp.toPx()
        val alpha = when {
            adjustedProgress > 0.85f -> (1f - adjustedProgress) / 0.15f
            adjustedProgress < 0.05f -> adjustedProgress / 0.05f
            else -> 1f
        }
        drawCircle(
            color = BubbleColor.copy(alpha = BubbleColor.alpha * alpha),
            radius = radius,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.15f * alpha),
            radius = radius * 0.4f,
            center = Offset(x - radius * 0.3f, y - radius * 0.3f)
        )
    }
}
