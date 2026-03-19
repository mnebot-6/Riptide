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
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.sin

// ── Paleta del agua ────────────────────────────────────────────────────────────
private val OceanShallow = Color(0xFF2E5F9E)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanDeep = Color(0xFF0A1628)

// ── Paleta del cielo según hora ────────────────────────────────────────────────
private data class SkyColors(val top: Color, val horizon: Color)

private fun skyForHour(hour: Int): SkyColors = when {
    hour < 5 || hour >= 23  -> SkyColors(Color(0xFF060B1A), Color(0xFF0D1B3A))        // noche
    hour < 7                 -> SkyColors(Color(0xFFB85C2A), Color(0xFFE8A048))        // amanecer
    hour < 9                 -> SkyColors(Color(0xFFD4824A), Color(0xFFF0C060))        // mañana dorada
    hour < 17                -> SkyColors(Color(0xFF4A90D9), Color(0xFF88C8F0))        // día
    hour < 19                -> SkyColors(Color(0xFFCC5530), Color(0xFFE88840))        // atardecer
    hour < 21                -> SkyColors(Color(0xFF6A2878), Color(0xFF9A5098))        // crepúsculo
    else                     -> SkyColors(Color(0xFF0D0820), Color(0xFF1A1040))        // noche tardía
}

// ── Fondo marino ───────────────────────────────────────────────────────────────
private val SandLight = Color(0xFFD4BC8C)
private val SandMid = Color(0xFFB89C70)
private val SandDark = Color(0xFF8C7858)
private val RockLight = Color(0xFF6A5E48)
private val RockMid = Color(0xFF504434)
private val RockDark = Color(0xFF352E22)
private val WaveCrest = Color(0x40FFFFFF)
private val BubbleColor = Color(0x337EC8E3)

// ── Rocas del fondo (variadas en tamaño y forma) ───────────────────────────────
private data class RockData(val cx: Float, val wFrac: Float, val hFrac: Float, val style: Int = 0)

private val rocks = listOf(
    // Rocas grandes
    RockData(0.08f, 0.11f, 0.042f, 1),
    RockData(0.52f, 0.14f, 0.050f, 2),
    RockData(0.88f, 0.12f, 0.044f, 1),
    // Rocas medianas
    RockData(0.22f, 0.07f, 0.028f, 0),
    RockData(0.38f, 0.06f, 0.022f, 2),
    RockData(0.68f, 0.08f, 0.032f, 0),
    RockData(0.79f, 0.05f, 0.018f, 1),
    // Rocas pequeñas
    RockData(0.14f, 0.035f, 0.012f, 0),
    RockData(0.45f, 0.030f, 0.010f, 2),
    RockData(0.61f, 0.040f, 0.014f, 1),
    RockData(0.93f, 0.032f, 0.011f, 0),
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
    // Hora actual para cielo dinámico
    val currentHour = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    }
    val sky = remember(currentHour) { skyForHour(currentHour) }

    val swayAnim = rememberInfiniteTransition(label = "sway")
    val swayAngle by swayAnim.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
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

    Canvas(modifier = modifier.fillMaxSize()) {
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

        // 2. Cielo sobre la superficie (gradiente dinámico por hora del día)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(sky.top, sky.horizon),
                startY = 0f,
                endY = surfaceY
            )
        )

        // 3. Fondo marino elaborado
        drawSeaFloor(floorY)

        // 4. Superficie del agua (olas más animadas)
        drawWaterSurface(surfaceY, swayAngle, sky.horizon)

        // 5. Burbujas (nacen del suelo, se desvanecen antes de la superficie)
        drawBubbles(bubbleProgress, surfaceY, floorY)
    }
}

private fun DrawScope.drawSeaFloor(floorY: Float) {
    val w = size.width
    val h = size.height

    // Banda de arena principal con gradiente más pronunciado
    drawRect(
        brush = Brush.verticalGradient(
            listOf(SandLight, SandMid, SandDark),
            startY = floorY,
            endY = h
        ),
        topLeft = Offset(0f, floorY),
        size = androidx.compose.ui.geometry.Size(w, h - floorY)
    )

    // Líneas de textura de arena (olas de arena suave)
    val sandLineColor = SandMid.copy(alpha = 0.35f)
    val lineStroke = 1.dp.toPx()
    for (i in 1..4) {
        val lineY = floorY + (h - floorY) * i / 5f
        val path = Path().apply {
            moveTo(0f, lineY)
            for (seg in 0 until 8) {
                val x0 = w * seg / 8f
                val x1 = w * (seg + 0.5f) / 8f
                val x2 = w * (seg + 1f) / 8f
                val offset = if (seg % 2 == 0) 2.dp.toPx() else -2.dp.toPx()
                quadraticTo(x1, lineY + offset, x2, lineY)
            }
        }
        drawPath(path, sandLineColor, style = Stroke(width = lineStroke, cap = StrokeCap.Round))
    }

    // Sombra de transición agua→arena
    drawRect(
        brush = Brush.verticalGradient(
            listOf(OceanDeep.copy(alpha = 0.45f), Color.Transparent),
            startY = floorY - h * 0.025f,
            endY = floorY + h * 0.015f
        ),
        topLeft = Offset(0f, floorY - h * 0.025f),
        size = androidx.compose.ui.geometry.Size(w, h * 0.04f)
    )

    // Rocas del fondo con variedad de formas
    rocks.sortedBy { it.cx }.forEach { rock ->
        val cx = w * rock.cx
        val rw = w * rock.wFrac
        val rh = h * rock.hFrac
        drawRock(cx, floorY, rw, rh, rock.style)
    }
}

private fun DrawScope.drawRock(cx: Float, floorY: Float, rw: Float, rh: Float, style: Int) {
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
    drawPath(path, RockMid)

    // Highlight en la parte superior izquierda (luz)
    val hlPath = Path().apply {
        moveTo(cx - rw * 0.15f, floorY - rh * 0.85f)
        quadraticTo(cx - rw * 0.3f, floorY - rh * 0.6f, cx - rw * 0.25f, floorY - rh * 0.4f)
    }
    drawPath(hlPath, RockLight.copy(alpha = 0.5f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

    // Sombra en la base
    val shadowPath = Path().apply {
        moveTo(cx - rw * 0.4f, floorY)
        quadraticTo(cx, floorY + 3.dp.toPx(), cx + rw * 0.4f, floorY)
    }
    drawPath(shadowPath, RockDark.copy(alpha = 0.5f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawWaterSurface(surfaceY: Float, swayAngle: Float, skyHorizon: Color) {
    val w = size.width
    val waveAmplitude = 9.dp.toPx()   // olas más animadas
    val segments = 8

    // Área sobre la ola (cielo reflejado)
    val wavePath = Path().apply {
        moveTo(0f, 0f)
        lineTo(0f, surfaceY)
        val segWidth = w / segments
        for (i in 0 until segments) {
            val x1 = segWidth * (i + 0.5f)
            val x2 = segWidth * (i + 1)
            val phase = i * 0.7f
            val offset = swayAngle * waveAmplitude * sin(phase + 1f)
            quadraticTo(x1, surfaceY + offset, x2, surfaceY)
        }
        lineTo(w, 0f)
        close()
    }
    drawPath(wavePath, skyHorizon.copy(alpha = 0.55f))

    // Cresta de ola principal (blanco más pronunciado)
    val crestPath = Path().apply {
        moveTo(0f, surfaceY)
        val segWidth = w / segments
        for (i in 0 until segments) {
            val x1 = segWidth * (i + 0.5f)
            val x2 = segWidth * (i + 1)
            val phase = i * 0.7f
            val offset = swayAngle * waveAmplitude * sin(phase + 1f)
            quadraticTo(x1, surfaceY + offset, x2, surfaceY)
        }
    }
    drawPath(crestPath, WaveCrest, style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round))

    // Cresta secundaria (más abajo, menor amplitud — efecto profundidad)
    val crest2Path = Path().apply {
        val offset2Y = surfaceY + waveAmplitude * 0.6f
        moveTo(0f, offset2Y)
        val segWidth = w / segments
        for (i in 0 until segments) {
            val x1 = segWidth * (i + 0.5f)
            val x2 = segWidth * (i + 1)
            val phase = i * 0.7f + 0.4f
            val offset = swayAngle * waveAmplitude * 0.4f * sin(phase + 1.5f)
            quadraticTo(x1, offset2Y + offset, x2, offset2Y)
        }
    }
    drawPath(crest2Path, WaveCrest.copy(alpha = 0.25f), style = Stroke(width = 1.2f.dp.toPx(), cap = StrokeCap.Round))
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
