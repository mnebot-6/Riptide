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
private val OceanShallow = Color(0xFF5ABCE8)
private val OceanMid = Color(0xFF2880C8)
private val OceanDeep = Color(0xFF0E4A90)

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

// ── Fondo marino ──────────────────────────────────────────────────────────────
internal val SandLight = Color(0xFFE2C898)
internal val SandMid   = Color(0xFFC8A872)
internal val SandDark  = Color(0xFF9A7A50)
internal val RockLight = Color(0xFF7A8CA2)
internal val RockMid   = Color(0xFF586070)
internal val RockDark  = Color(0xFF3A4858)
private val WaveCrest   = Color(0x50FFFFFF)
private val BubbleColor = Color(0x407EC8E3)


// ── Cache procedural de terreno y decoraciones ────────────────────────────────
private val defaultTerrainConfig = AquariumTerrainConfig(decorationDensity = 0.65f)
private val cachedDecorations: TerrainDecorations by lazy {
    AquariumTerrain.configure(defaultTerrainConfig)
    generateDecorations(defaultTerrainConfig)
}

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

    // 3. Viñeta lateral — oscurece bordes izquierdo/derecho para dar sensación de cuenca
    drawRect(
        brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0.00f to OceanDeep.copy(alpha = 0.42f),
                0.14f to Color.Transparent,
                0.86f to Color.Transparent,
                1.00f to OceanDeep.copy(alpha = 0.42f),
            )
        )
    )
    // Viñeta inferior — el fondo es más oscuro que el agua del medio
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.55f to Color.Transparent,
                1.00f to OceanDeep.copy(alpha = 0.35f),
            )
        )
    )

    // 4. Clouds above the water surface
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
    val decs = cachedDecorations

    // ── LAYER 1: Sand fill following the terrain curve ────────────────────────
    val sandPath = AquariumTerrain.terrainPath(w, h)
    drawPath(
        sandPath,
        brush = Brush.verticalGradient(
            listOf(SandLight, SandMid, SandDark),
            startY = AquariumTerrain.terrainY(0.35f, h) - h * 0.02f,
            endY = h
        )
    )

    // ── LAYER 2: Sand ripple texture lines ────────────────────────────────────
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

    // ── LAYER 3: Floor pebbles ────────────────────────────────────────────────
    drawFloorDecorations(decs)
}

// ── Rock drawing (kept for potential future use) ──────────────────────────────

/**
 * Draws the rock geometry with a gradient fill, soft highlight, contact shadow, and texture line.
 * All shapes use smooth bezier curves for an organic, natural look.
 */
internal fun DrawScope.drawRockShape(
    cx: Float, floorY: Float, rw: Float, rh: Float, style: Int, fillColor: Color
) {
    val topY = floorY - rh
    val alpha = fillColor.alpha

    // ── Organic silhouette (all-bezier, 4 variants) ───────────────────────────
    val path = when (style) {
        0 -> Path().apply {  // smooth dome boulder
            moveTo(cx - rw * 0.50f, floorY)
            cubicTo(
                cx - rw * 0.54f, floorY - rh * 0.48f,
                cx - rw * 0.28f, topY - rh * 0.06f,
                cx + rw * 0.02f, topY
            )
            cubicTo(
                cx + rw * 0.26f, topY - rh * 0.04f,
                cx + rw * 0.54f, floorY - rh * 0.42f,
                cx + rw * 0.50f, floorY
            )
            close()
        }
        1 -> Path().apply {  // asymmetric boulder — broader on left, tapered right
            moveTo(cx - rw * 0.50f, floorY)
            cubicTo(
                cx - rw * 0.56f, floorY - rh * 0.52f,
                cx - rw * 0.22f, topY + rh * 0.04f,
                cx - rw * 0.04f, topY
            )
            cubicTo(
                cx + rw * 0.14f, topY + rh * 0.02f,
                cx + rw * 0.40f, floorY - rh * 0.65f,
                cx + rw * 0.48f, floorY - rh * 0.22f
            )
            cubicTo(
                cx + rw * 0.52f, floorY - rh * 0.08f,
                cx + rw * 0.50f, floorY,
                cx + rw * 0.50f, floorY
            )
            close()
        }
        2 -> Path().apply {  // wide low cobblestone with humped top
            moveTo(cx - rw * 0.50f, floorY)
            cubicTo(
                cx - rw * 0.56f, floorY - rh * 0.55f,
                cx - rw * 0.38f, topY,
                cx - rw * 0.08f, topY
            )
            cubicTo(
                cx + rw * 0.06f, topY - rh * 0.10f,
                cx + rw * 0.32f, topY + rh * 0.02f,
                cx + rw * 0.44f, topY + rh * 0.15f
            )
            cubicTo(
                cx + rw * 0.58f, floorY - rh * 0.40f,
                cx + rw * 0.52f, floorY,
                cx + rw * 0.50f, floorY
            )
            close()
        }
        else -> Path().apply {  // tall craggy formation — narrow waist, broad base
            moveTo(cx - rw * 0.48f, floorY)
            cubicTo(
                cx - rw * 0.50f, floorY - rh * 0.48f,
                cx - rw * 0.36f, floorY - rh * 0.82f,
                cx - rw * 0.18f, topY
            )
            cubicTo(
                cx - rw * 0.08f, topY - rh * 0.07f,
                cx + rw * 0.14f, topY - rh * 0.05f,
                cx + rw * 0.24f, topY + rh * 0.06f
            )
            cubicTo(
                cx + rw * 0.42f, floorY - rh * 0.78f,
                cx + rw * 0.50f, floorY - rh * 0.42f,
                cx + rw * 0.48f, floorY
            )
            close()
        }
    }

    // ── Gradient fill: lighter top → base → darker bottom ────────────────────
    val lightColor = lerpColor(fillColor, Color.White, 0.28f).copy(alpha = alpha)
    val darkColor  = lerpColor(fillColor, Color.Black, 0.32f).copy(alpha = alpha)
    drawPath(
        path,
        brush = Brush.verticalGradient(
            colors = listOf(lightColor, fillColor.copy(alpha = alpha), darkColor),
            startY = topY,
            endY = floorY
        )
    )

    // ── Soft highlight — upper-left quadrant, diffuse white glow ─────────────
    val hlR  = rw * 0.26f
    val hlCx = cx - rw * 0.16f
    val hlCy = floorY - rh * 0.70f
    drawCircle(
        color = Color.White.copy(alpha = 0.18f * alpha),
        radius = hlR,
        center = Offset(hlCx, hlCy)
    )

    // ── Contact shadow — soft ellipse pressed into the sand ──────────────────
    val shadowOval = Path().apply {
        val sy = floorY + 1.5f.dp.toPx()
        moveTo(cx - rw * 0.40f, floorY)
        cubicTo(cx - rw * 0.40f, sy, cx + rw * 0.40f, sy, cx + rw * 0.40f, floorY)
        cubicTo(cx + rw * 0.40f, floorY - 1.5f.dp.toPx(), cx - rw * 0.40f, floorY - 1.5f.dp.toPx(), cx - rw * 0.40f, floorY)
        close()
    }
    drawPath(shadowOval, RockDark.copy(alpha = 0.38f * alpha))

    // ── Subtle mid-body texture crease ────────────────────────────────────────
    val texPath = Path().apply {
        moveTo(cx - rw * 0.28f, floorY - rh * 0.46f)
        cubicTo(
            cx - rw * 0.08f, floorY - rh * 0.52f,
            cx + rw * 0.10f, floorY - rh * 0.44f,
            cx + rw * 0.26f, floorY - rh * 0.50f
        )
    }
    drawPath(texPath, RockDark.copy(alpha = 0.16f * alpha),
        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
}

/**
 * Draws a single rock using depth-tinted colors.
 * @param alpha overall opacity (0.4 for background layer, 1.0 for mid/foreground)
 * @param tint -1 = lighter/muted (background), 0 = normal (mid), 1 = darker (foreground)
 */
internal fun DrawScope.drawRock(cx: Float, floorY: Float, rw: Float, rh: Float, style: Int, alpha: Float = 1f, tint: Int = 0) {
    val rockColor = when (tint) {
        -1 -> RockLight.copy(alpha = alpha)
         1 -> RockDark.copy(alpha = alpha)
        else -> RockMid.copy(alpha = alpha)
    }
    drawRockShape(cx, floorY, rw, rh, style, rockColor)
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
