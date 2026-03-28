package com.mnebot.riptide.presentation.aquarium.flora

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.cos
import kotlin.math.sin

private val CoralBase = Color(0xFFE8967A)
private val CoralDark = Color(0xFFCC7A62)
private val CoralLight = Color(0xFFF2B8A0)
private val CoralHighlight = Color(0xFFF7D4C0)

object BrainCoralRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        // y = base (suelo). Se dibuja hacia arriba.
        val scale = size / 28f
        val baseWidth = (18f + level * 4f) * scale
        val baseHeight = (14f + level * 3f) * scale
        val lobeCount = when {
            level <= 2 -> 1
            level <= 5 -> 2
            else -> 3
        }

        for (i in 0 until lobeCount) {
            val offsetX = when (lobeCount) {
                1 -> 0f
                2 -> (i - 0.5f) * baseWidth * 0.45f
                else -> (i - 1f) * baseWidth * 0.38f
            }
            val lobeW = if (lobeCount == 1) baseWidth else baseWidth * 0.65f
            val lobeH = baseHeight * (if (i == lobeCount / 2) 1f else 0.8f)
            val color = when (i % 3) {
                0 -> CoralBase
                1 -> CoralDark
                else -> CoralLight
            }
            drawCoralDome(x + offsetX, y, lobeW, lobeH, color, level, scale)
        }
    }

    private fun DrawScope.drawCoralDome(
        cx: Float, baseY: Float,
        w: Float, h: Float,
        color: Color, level: Int, scale: Float
    ) {
        // Domo principal
        val domePath = Path().apply {
            moveTo(cx - w / 2f, baseY)
            cubicTo(
                cx - w / 2f, baseY - h * 0.6f,
                cx - w * 0.2f, baseY - h,
                cx, baseY - h * 1.05f
            )
            cubicTo(
                cx + w * 0.2f, baseY - h,
                cx + w / 2f, baseY - h * 0.6f,
                cx + w / 2f, baseY
            )
            close()
        }
        drawPath(domePath, color)

        // Crestas (surcos del coral cerebro)
        val ridgeCount = (2 + level).coerceAtMost(7)
        val ridgeStroke = (1.2f * scale).coerceAtLeast(0.8f)
        for (r in 1..ridgeCount) {
            val frac = r.toFloat() / (ridgeCount + 1)
            val ry = baseY - h * frac * 0.85f
            val rw = w * 0.4f * (1f - frac * 0.5f)
            val wobble = sin(frac * 8f) * rw * 0.15f
            drawLine(
                color = CoralDark.copy(alpha = 0.5f),
                start = Offset(cx - rw + wobble, ry),
                end = Offset(cx + rw + wobble, ry),
                strokeWidth = ridgeStroke
            )
        }

        // Highlight superior
        if (level >= 3) {
            val hlSize = w * 0.15f
            drawCircle(
                color = CoralHighlight.copy(alpha = 0.4f),
                radius = hlSize,
                center = Offset(cx - w * 0.1f, baseY - h * 0.8f)
            )
        }
    }
}
