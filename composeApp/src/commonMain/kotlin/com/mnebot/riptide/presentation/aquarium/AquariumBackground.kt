package com.mnebot.riptide.presentation.aquarium

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import kotlin.math.sin

// --- Paleta ---
private val SurfaceLight = Color(0xFF4A90D9)
private val OceanShallow = Color(0xFF2E5F9E)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanDeep = Color(0xFF0A1628)

private val SandLight = Color(0xFFC2A882)
private val SandMid = Color(0xFF9E8866)
private val SandDark = Color(0xFF7A6B50)
private val RockColor = Color(0xFF5A4E3C)
private val RockShadow = Color(0xFF3D3528)

private val WaveCrest = Color(0x30FFFFFF)
private val BubbleColor = Color(0x337EC8E3)

// --- Datos de burbujas ---
private data class BubbleData(
    val x: Float,
    val size: Float,
    val speed: Float,
    val startOffset: Float
)

private val bubbles = listOf(
    BubbleData(0.15f, 4f, 6000f, 0.0f),
    BubbleData(0.28f, 3f, 5000f, 0.3f),
    BubbleData(0.42f, 6f, 7000f, 0.6f),
    BubbleData(0.55f, 4f, 5500f, 0.1f),
    BubbleData(0.67f, 5f, 6500f, 0.8f),
    BubbleData(0.80f, 3f, 4500f, 0.4f),
    BubbleData(0.90f, 6f, 7500f, 0.7f),
)

// --- Datos de rocas del fondo ---
private data class RockData(val cx: Float, val w: Float, val h: Float)

private val rocks = listOf(
    RockData(0.10f, 0.07f, 0.025f),
    RockData(0.35f, 0.05f, 0.018f),
    RockData(0.58f, 0.08f, 0.030f),
    RockData(0.82f, 0.06f, 0.022f),
    RockData(0.93f, 0.04f, 0.015f),
)

@Composable
fun AquariumBackground(modifier: Modifier = Modifier) {
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

    Canvas(modifier = modifier.fillMaxSize()) {
        val surfaceY = AquariumBounds.surfaceY(size.height)
        val floorY = AquariumBounds.floorY(size.height)

        // 1. Gradiente de fondo (claro arriba → oscuro abajo)
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to SurfaceLight,
                    AquariumBounds.SURFACE_FRACTION to OceanShallow,
                    0.45f to OceanMid,
                    AquariumBounds.FLOOR_FRACTION to OceanDeep,
                ),
                startY = 0f,
                endY = floorY
            )
        )

        // 2. Fondo marino (arena + rocas)
        drawSeaFloor(floorY)

        // 3. Superficie del agua (olas animadas)
        drawWaterSurface(surfaceY, swayAngle)

        // 4. Burbujas (nacen del suelo, se desvanecen antes de la superficie)
        drawBubbles(bubbleProgress, surfaceY, floorY)
    }
}

private fun DrawScope.drawSeaFloor(floorY: Float) {
    val w = size.width
    val h = size.height

    // Banda de arena principal
    drawRect(
        brush = Brush.verticalGradient(
            listOf(SandLight, SandMid, SandDark),
            startY = floorY,
            endY = h
        ),
        topLeft = Offset(0f, floorY),
        size = androidx.compose.ui.geometry.Size(w, h - floorY)
    )

    // Línea de transición agua→arena (sombra sutil)
    drawLine(
        color = RockShadow.copy(alpha = 0.3f),
        start = Offset(0f, floorY),
        end = Offset(w, floorY),
        strokeWidth = 2.dp.toPx()
    )

    // Rocas decorativas
    rocks.forEach { rock ->
        val cx = w * rock.cx
        val rw = w * rock.w
        val rh = h * rock.h
        val path = Path().apply {
            moveTo(cx - rw / 2f, floorY)
            quadraticTo(cx - rw * 0.3f, floorY - rh, cx, floorY - rh * 1.1f)
            quadraticTo(cx + rw * 0.3f, floorY - rh, cx + rw / 2f, floorY)
            close()
        }
        drawPath(path, RockColor)
        // Sombra inferior de la roca
        drawPath(
            Path().apply {
                moveTo(cx - rw * 0.35f, floorY)
                quadraticTo(cx, floorY - rh * 0.3f, cx + rw * 0.35f, floorY)
                close()
            },
            RockShadow.copy(alpha = 0.4f)
        )
    }
}

private fun DrawScope.drawWaterSurface(surfaceY: Float, swayAngle: Float) {
    val w = size.width
    val waveAmplitude = 4.dp.toPx()
    val segments = 6

    // Ola principal
    val wavePath = Path().apply {
        moveTo(0f, 0f)
        lineTo(0f, surfaceY)
        val segWidth = w / segments
        for (i in 0 until segments) {
            val x0 = segWidth * i
            val x1 = segWidth * (i + 0.5f)
            val x2 = segWidth * (i + 1)
            val offset = swayAngle * waveAmplitude * if (i % 2 == 0) 1f else -0.7f
            quadraticTo(x1, surfaceY + offset, x2, surfaceY)
        }
        lineTo(w, 0f)
        close()
    }

    // Zona sobre la superficie: luz filtrada
    drawPath(wavePath, SurfaceLight.copy(alpha = 0.35f))

    // Cresta de ola (línea blanca sutil)
    val crestPath = Path().apply {
        moveTo(0f, surfaceY)
        val segWidth = w / segments
        for (i in 0 until segments) {
            val x1 = segWidth * (i + 0.5f)
            val x2 = segWidth * (i + 1)
            val offset = swayAngle * waveAmplitude * if (i % 2 == 0) 1f else -0.7f
            quadraticTo(x1, surfaceY + offset, x2, surfaceY)
        }
    }
    drawPath(
        crestPath,
        WaveCrest,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
    )
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
        // Desvanecer al acercarse a la superficie
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
