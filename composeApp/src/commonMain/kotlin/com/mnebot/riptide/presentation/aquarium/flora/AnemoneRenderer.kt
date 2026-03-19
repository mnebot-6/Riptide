package com.mnebot.riptide.presentation.aquarium.flora

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.cos
import kotlin.math.sin

private const val TAU = 6.28318530f

private val AnemoneBase = Color(0xFFAA45A0)
private val AnemoneDark = Color(0xFF882080)
private val AnemoneTip = Color(0xFFDA70D6)
private val AnemoneBright = Color(0xFFEE99E8)

object AnemoneRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val scale = size / 28f

        // Base (disco)
        val baseRadius = (6f + level * 1.5f) * scale
        drawCircle(
            color = AnemoneDark,
            radius = baseRadius,
            center = Offset(x, y - baseRadius * 0.3f)
        )

        // Tentáculos
        val tentacleCount = when {
            level <= 2 -> 5
            level <= 5 -> 9
            else -> 14
        }
        val maxHeight = (20f + level * 5f) * scale
        val strokeWidth = (2.5f * scale).coerceAtLeast(1.2f)

        for (i in 0 until tentacleCount) {
            val angle = (i.toFloat() / tentacleCount - 0.5f) * 1.4f // spread [-0.7, 0.7] radians
            val tentacleHeight = maxHeight * (0.7f + 0.3f * cos(angle * 2f))

            // Ondulación interna con animTimeMs
            val swayPhase = i * TAU / tentacleCount
            val sway = sin(animTimeMs / 2500f + swayPhase) * 4f * scale
            val sway2 = sin(animTimeMs / 1800f + swayPhase * 1.618f) * 2f * scale

            val tipX = x + sin(angle) * baseRadius * 1.5f + sway + sway2
            val tipY = y - tentacleHeight
            val ctrlX = x + sin(angle) * baseRadius * 0.8f + sway * 0.5f
            val ctrlY = y - tentacleHeight * 0.55f

            // Color: gradiente de base oscura a punta clara
            val tentacleColor = if (i % 3 == 0) AnemoneBase else AnemoneDark
            val tipColor = if (level >= 4) AnemoneBright else AnemoneTip

            val path = Path().apply {
                moveTo(x + sin(angle) * baseRadius * 0.4f, y - baseRadius * 0.2f)
                quadraticTo(ctrlX, ctrlY, tipX, tipY)
            }
            drawPath(path, tentacleColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

            // Punto brillante en la punta
            val tipRadius = (1.5f + (level - 1) * 0.3f) * scale
            drawCircle(
                color = tipColor,
                radius = tipRadius.coerceAtLeast(1f),
                center = Offset(tipX, tipY)
            )
        }
    }
}
