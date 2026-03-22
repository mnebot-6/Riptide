package com.mnebot.riptide.presentation.aquarium.flora

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val PosGreen  = Color(0xFF3A8020)  // leaf green
private val PosLight  = Color(0xFF5AA030)  // lighter tips
private val PosDark   = Color(0xFF2A6010)  // darker base areas
private val PosMatte  = Color(0xFF4A3010)  // dead fibers (level 5+)
private val PosYellow = Color(0xFF90B020)  // yellow-green tips

object PosidoniaRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        // y = base (floor). Draw upward.
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bladeCount = when {
            level <= 2 -> 7
            level <= 5 -> 10
            else -> 13
        }

        // Level 5+: dead fiber "matte" at base
        if (level >= 5) {
            val matteR = (5f + level * 0.5f) * s
            drawCircle(PosMatte.copy(alpha = 0.55f), matteR, Offset(x, y - matteR * 0.4f))
            drawCircle(PosMatte.copy(alpha = 0.30f), matteR * 0.65f, Offset(x + matteR * 0.3f, y - matteR * 0.2f))
        }

        for (i in 0 until bladeCount) {
            val frac  = (i.toFloat() / (bladeCount - 1)) - 0.5f  // -0.5 to 0.5
            val baseX = x + frac * (8f + level * 1.0f) * s
            drawBlade(baseX, y, level, t, i, s)
        }
    }

    private fun DrawScope.drawBlade(
        baseX: Float, baseY: Float,
        level: Int, t: Float,
        bladeIndex: Int, s: Float
    ) {
        val bladeHeight = (22f + level * 5f) * s * (0.75f + (bladeIndex % 3) * 0.12f)
        val bladeW0     = (2.5f + level * 0.2f) * s  // width at base
        val bladeW1     = (0.8f + level * 0.08f) * s  // width at tip
        val phase       = bladeIndex * 0.7f

        // Per-segment sway: amplitude grows toward tip
        val segments = 10
        val segH = bladeHeight / segments

        var prevX = baseX
        var prevY = baseY

        val pathLeft  = Path()
        val pathRight = Path()
        pathLeft.moveTo(prevX, prevY)
        pathRight.moveTo(prevX, prevY)

        val xPositions = Array(segments + 1) { 0f }
        val yPositions = Array(segments + 1) { 0f }
        xPositions[0] = baseX
        yPositions[0] = baseY

        // Compute spine positions
        for (seg in 1..segments) {
            val progress = seg.toFloat() / segments
            val sway     = sin(t * 1.2f * PI.toFloat() + phase + progress * 2.5f) * 6f * s * progress
            xPositions[seg] = baseX + sway
            yPositions[seg] = baseY - segH * seg
        }

        // Build left and right edges
        for (seg in 1..segments) {
            val progress = seg.toFloat() / segments
            val halfW    = bladeW0 * (1f - progress) + bladeW1 * progress

            val cx = xPositions[seg]
            val cy = yPositions[seg]
            pathLeft.lineTo(cx - halfW, cy)
            pathRight.lineTo(cx + halfW, cy)
        }

        // Tip: rounded
        val tipX = xPositions[segments]
        val tipY = yPositions[segments]

        // Combine into one blade path
        val bladePath = Path().apply {
            moveTo(baseX - bladeW0, baseY)
            // Left edge going up
            for (seg in 1..segments) {
                val progress = seg.toFloat() / segments
                val halfW    = bladeW0 * (1f - progress) + bladeW1 * progress
                lineTo(xPositions[seg] - halfW, yPositions[seg])
            }
            // Rounded tip
            cubicTo(tipX - bladeW1, tipY - bladeW1 * 1.5f,
                tipX + bladeW1, tipY - bladeW1 * 1.5f,
                tipX + bladeW1, tipY)
            // Right edge going down
            for (seg in segments downTo 1) {
                val progress = seg.toFloat() / segments
                val halfW    = bladeW0 * (1f - progress) + bladeW1 * progress
                lineTo(xPositions[seg] + halfW, yPositions[seg])
            }
            lineTo(baseX + bladeW0, baseY)
            close()
        }

        // Color: greener in middle, darker at base, lighter/yellower at tip
        val bladeColor = if (bladeIndex % 3 == 0) PosDark else PosGreen
        drawPath(bladePath, bladeColor)

        // Tip highlight
        drawCircle(PosYellow.copy(alpha = 0.50f), bladeW1 * 1.2f, Offset(tipX, tipY))

        // Level 3+: fibrous dead material at base (browner)
        if (level >= 3) {
            drawLine(
                PosMatte.copy(alpha = 0.35f),
                Offset(baseX, baseY),
                Offset(baseX, baseY - bladeHeight * 0.08f),
                strokeWidth = bladeW0 * 1.5f,
                cap = StrokeCap.Round
            )
        }

        // Center rib line
        drawLine(
            PosLight.copy(alpha = 0.40f),
            Offset(xPositions[1], yPositions[1]),
            Offset(tipX, tipY - bladeW1),
            strokeWidth = (0.5f * s).coerceAtLeast(0.3f),
            cap = StrokeCap.Round
        )
    }
}
