package com.mnebot.riptide.presentation.aquarium.flora

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.mnebot.riptide.presentation.aquarium.PathPool
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.cos
import kotlin.math.sin

private const val TAU = 6.28318530f

private val KelpDark = Color(0xFF1E6B3A)
private val KelpMid = Color(0xFF2E8B57)
private val KelpLight = Color(0xFF3CB371)
private val LeafColor = Color(0xFF45A065)
private val LeafLight = Color(0xFF60C080)

object KelpRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val scale = size / 28f
        val stalkCount = when {
            level <= 2 -> 1
            level <= 5 -> 2
            else -> 3
        }

        for (s in 0 until stalkCount) {
            val offsetX = when (stalkCount) {
                1 -> 0f
                2 -> (s - 0.5f) * 10f * scale
                else -> (s - 1f) * 9f * scale
            }
            val stalkHeight = (25f + level * 6f) * scale * (if (s == stalkCount / 2) 1f else 0.8f)
            drawStalk(x + offsetX, y, stalkHeight, level, animTimeMs, s, scale)
        }
    }

    private fun DrawScope.drawStalk(
        baseX: Float, baseY: Float,
        height: Float, level: Int,
        animTimeMs: Long, stalkIndex: Int, scale: Float
    ) {
        val segments = 8
        val segH = height / segments
        val strokeWidth = (3f * scale).coerceAtLeast(1.5f)
        val swayPhase = stalkIndex * 2.1f

        var prevX = baseX
        var prevY = baseY

        val leafCount = when {
            level <= 2 -> 3
            level <= 5 -> 5
            else -> 7
        }
        val leafInterval = segments / (leafCount + 1)

        for (i in 1..segments) {
            val progress = i.toFloat() / segments
            // Ondulación que crece hacia la punta
            val sway = sin(animTimeMs / 3000f + swayPhase + progress * 2f) * 5f * scale * progress
            val nextX = baseX + sway
            val nextY = baseY - segH * i

            // Color: más claro hacia arriba
            val color = if (progress < 0.5f) KelpDark else KelpMid

            drawLine(
                color = color,
                start = Offset(prevX, prevY),
                end = Offset(nextX, nextY),
                strokeWidth = strokeWidth * (1f - progress * 0.35f),
                cap = StrokeCap.Round
            )

            // Hojas alternas
            if (i > 1 && i % leafInterval.coerceAtLeast(1) == 0) {
                val leafSide = if ((i + stalkIndex) % 2 == 0) 1f else -1f
                val leafSway = sin(animTimeMs / 2200f + swayPhase + i * 0.8f) * 2f * scale
                drawLeaf(nextX, nextY, leafSide, progress, scale, level, leafSway)
            }

            prevX = nextX
            prevY = nextY
        }

        // Punta: hojita terminal
        val tipSway = sin(animTimeMs / 1800f + swayPhase) * 3f * scale
        drawCircle(
            color = KelpLight,
            radius = (2f * scale).coerceAtLeast(1f),
            center = Offset(prevX + tipSway, prevY)
        )
    }

    private fun DrawScope.drawLeaf(
        x: Float, y: Float,
        side: Float, progress: Float,
        scale: Float, level: Int, sway: Float
    ) {
        val leafLen = (8f + level * 1.5f) * scale * (0.6f + progress * 0.4f)
        val leafWidth = (3f + level * 0.5f) * scale
        val color = if (progress > 0.6f) LeafLight else LeafColor

        val tipX = x + (leafLen + sway) * side
        val tipY = y - leafLen * 0.3f

        val path = paths.obtain().apply {
            moveTo(x, y)
            quadraticTo(
                x + leafLen * 0.5f * side + sway * 0.5f,
                y - leafWidth,
                tipX, tipY
            )
            quadraticTo(
                x + leafLen * 0.5f * side + sway * 0.5f,
                y + leafWidth * 0.5f,
                x, y
            )
            close()
        }
        drawPath(path, color)
    }
}
