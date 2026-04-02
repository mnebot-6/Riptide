package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.mnebot.riptide.presentation.aquarium.PathPool
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val StarOrange  = Color(0xFFD94020)  // deep orange-red
private val StarOrange2 = Color(0xFFE86040)  // lighter arm highlight
private val StarCenter  = Color(0xFFC03010)  // darker central disc
private val StarDot     = Color(0xFFF09060)  // tube foot dots
private val StarOutline = Color(0xFF8A2010)  // outline

object StarfishRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        // Fixed floor creature — y is base (floor), draw upward & centred around floor
        val s = size / 28f
        val t = animTimeMs / 1000f

        val outerR = (9f + level * 0.6f) * s
        val innerR = outerR * 0.40f
        val armCount = 5
        val centerY = y - outerR * 0.55f   // place star slightly above floor

        // Slow breathing pulse
        val pulse = 1f + sin(t * 0.8f * PI.toFloat()) * 0.04f

        // ── STAR BODY (5-pointed) ──────────────────────────────────────────────
        val starPath = paths.obtain()
        for (i in 0 until armCount * 2) {
            val angle = i.toFloat() * PI.toFloat() / armCount.toFloat() - PI.toFloat() / 2f
            val r = if (i % 2 == 0) outerR * pulse else innerR
            val px = x + cos(angle) * r
            val py = centerY + sin(angle) * r
            if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
        }
        starPath.close()
        drawPath(starPath, StarOrange)

        // Arm highlight gradient (lighter on leading edge)
        for (i in 0 until armCount) {
            val angle = i.toFloat() * 2f * PI.toFloat() / armCount.toFloat() - PI.toFloat() / 2f
            val tipX = x + cos(angle) * outerR * pulse
            val tipY = centerY + sin(angle) * outerR * pulse
            val midX = x + cos(angle) * outerR * 0.5f
            val midY = centerY + sin(angle) * outerR * 0.5f
            drawLine(
                StarOrange2.copy(alpha = 0.45f),
                Offset(midX, midY), Offset(tipX, tipY),
                strokeWidth = (outerR * 0.28f).coerceAtLeast(1.0f),
                cap = StrokeCap.Round
            )
        }

        // Central disc
        drawCircle(StarCenter, innerR * 0.85f, Offset(x, centerY))

        // Outline
        drawPath(starPath, StarOutline.copy(alpha = 0.40f),
            style = Stroke(width = (0.8f * s).coerceAtLeast(0.3f)))

        // ── LEVEL 2+: Tube feet dots along each arm ───────────────────────────
        if (level >= 2) {
            val dotCount = 4 + level
            for (i in 0 until armCount) {
                val angle = i.toFloat() * 2f * PI.toFloat() / armCount.toFloat() - PI.toFloat() / 2f
                for (d in 1..dotCount) {
                    val dr = innerR + (outerR - innerR) * d.toFloat() / (dotCount + 1f)
                    val dpx = x + cos(angle) * dr
                    val dpy = centerY + sin(angle) * dr
                    drawCircle(StarDot.copy(alpha = 0.55f), (0.5f * s).coerceAtLeast(0.3f), Offset(dpx, dpy))
                }
            }
        }

        // ── LEVEL 4+: Textured surface dots (papulae / spines) ────────────────
        if (level >= 4) {
            val spinePositions = listOf(
                Pair(0.30f, -0.30f), Pair(-0.30f, -0.30f),
                Pair(0.30f,  0.30f), Pair(-0.30f,  0.30f),
                Pair(0.0f,   0.45f), Pair(0.0f,  -0.45f),
            )
            for ((sx, sy) in spinePositions) {
                val sAnim = sin(t * 1.5f * PI.toFloat() + sx * 3f) * 0.3f * s
                drawCircle(
                    StarCenter.copy(alpha = 0.50f),
                    (0.7f * s).coerceAtLeast(0.4f),
                    Offset(x + sx * outerR, centerY + sy * outerR + sAnim)
                )
            }
        }
    }
}
