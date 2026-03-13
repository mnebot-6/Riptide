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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.sin

private val OceanDeep = Color(0xFF0A1628)
private val OceanMid = Color(0xFF1B3A6B)
private val OceanLight = Color(0xFF2E5F9E)
// private val LightRay = Color(0x0A7EC8E3)
private val PlantDark = Color(0xFF0D4A3A)
private val PlantMid = Color(0xFF1A6B52)
private val BubbleColor = Color(0x337EC8E3)

private data class PlantData(
    val x: Float,
    val height: Float,
    val segments: Int,
    val color: Color,
    val swayOffset: Float
)

private data class BubbleData(
    val x: Float,
    val size: Float,
    val speed: Float,
    val startOffset: Float
)

private val plants = listOf(
    PlantData(0.05f, 0.22f, 5, PlantMid,  0.0f),
    PlantData(0.12f, 0.18f, 4, PlantDark, 0.8f),
    PlantData(0.20f, 0.28f, 6, PlantMid,  1.6f),
    PlantData(0.30f, 0.15f, 4, PlantDark, 0.3f),
    PlantData(0.68f, 0.20f, 5, PlantMid,  1.1f),
    PlantData(0.78f, 0.25f, 6, PlantDark, 0.5f),
    PlantData(0.88f, 0.17f, 4, PlantMid,  1.9f),
    PlantData(0.95f, 0.23f, 5, PlantDark, 0.2f),
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

    /*val lightAnim = rememberInfiniteTransition(label = "light")
    val lightAlpha by lightAnim.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lightAlpha"
    )*/

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
        // Fondo
        drawRect(
            brush = Brush.verticalGradient(
                listOf(OceanDeep, OceanMid, OceanLight),
                startY = 0f,
                endY = size.height
            )
        )

        // Rayos de luz
        /*val rayColor = LightRay.copy(alpha = LightRay.alpha * lightAlpha)
        val rayWidth = size.width * 0.08f
        listOf(0.2f, 0.45f, 0.65f, 0.85f).forEach { xFraction ->
            val centerX = size.width * xFraction
            val path = Path().apply {
                moveTo(centerX - rayWidth * 0.3f, 0f)
                lineTo(centerX + rayWidth * 0.3f, 0f)
                lineTo(centerX + rayWidth, size.height * 0.6f)
                lineTo(centerX - rayWidth, size.height * 0.6f)
                close()
            }
            drawPath(path, rayColor)
        }*/

        // Plantas
        plants.forEach { plant ->
            val baseX = size.width * plant.x
            val baseY = size.height
            val totalHeight = size.height * plant.height
            val segmentHeight = totalHeight / plant.segments
            val maxSway = 8.dp.toPx()
            val sway = swayAngle * maxSway * sin(plant.swayOffset + 1f)
            val strokeWidth = 3.dp.toPx()

            var currentX = baseX
            var currentY = baseY

            for (index in 0 until plant.segments) {
                val progress = (index + 1f) / plant.segments
                val nextX = baseX + sway * progress
                val nextY = baseY - segmentHeight * (index + 1)
                drawLine(
                    color = plant.color,
                    start = Offset(currentX, currentY),
                    end = Offset(nextX, nextY),
                    strokeWidth = strokeWidth * (1f - progress * 0.4f)
                )
                currentX = nextX
                currentY = nextY
            }
        }

        // Burbujas
        bubbles.forEach { bubble ->
            val adjustedProgress = (bubbleProgress + bubble.startOffset) % 1f
            val x = size.width * bubble.x + sin(adjustedProgress * 6.28f) * 8.dp.toPx()
            val y = size.height * (1f - adjustedProgress)
            val radius = bubble.size.dp.toPx()
            val alpha = if (adjustedProgress > 0.8f) (1f - adjustedProgress) / 0.2f else 1f

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
}