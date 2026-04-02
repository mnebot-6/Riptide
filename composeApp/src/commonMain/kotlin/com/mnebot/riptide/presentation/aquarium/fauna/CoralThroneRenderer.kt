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
private val CoralPink      = Color(0xFFE87098)   // main coral pink
private val CoralPinkDark  = Color(0xFFB84870)   // pink shadow
private val CoralOrange    = Color(0xFFE88040)   // orange coral sections
private val CoralOrangeDk  = Color(0xFFC06028)   // orange shadow
private val CoralPurple    = Color(0xFFA860C0)   // purple coral accents
private val CoralPurpleDk  = Color(0xFF783890)   // purple shadow
private val SeatBase       = Color(0xFF98607A)   // seat cushion color
private val SeatDark       = Color(0xFF684058)   // seat shadow
private val StarfishOrange = Color(0xFFF0A030)   // starfish (level 3+)
private val StarfishLight  = Color(0xFFFFCC60)   // starfish highlight
private val CrownGold      = Color(0xFFD0B040)   // seaweed crown (level 5+)
private val CrownGoldLight = Color(0xFFE8D060)   // crown highlight

object CoralThroneRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val throneW = (12f + level * 0.8f) * s  // half-width
        val throneH = (22f + level * 2f) * s     // total height
        val seatH = throneH * 0.35f
        val backH = throneH * 0.65f

        // Gentle coral pulse
        val pulse = sin(t * 0.8f * PI.toFloat()) * 0.5f * s

        // ── Seat base (wide, flat) ──────────────────────────────────────────
        val seatTop = y - seatH
        val seatPath = paths.obtain().apply {
            moveTo(x - throneW, y)
            cubicTo(
                x - throneW * 0.9f, seatTop + seatH * 0.3f,
                x - throneW * 0.4f, seatTop,
                x, seatTop
            )
            cubicTo(
                x + throneW * 0.4f, seatTop,
                x + throneW * 0.9f, seatTop + seatH * 0.3f,
                x + throneW, y
            )
            close()
        }
        drawPath(seatPath, SeatDark)

        // Seat top surface
        val seatSurf = paths.obtain().apply {
            moveTo(x - throneW * 0.85f, seatTop + seatH * 0.15f)
            cubicTo(
                x - throneW * 0.5f, seatTop - seatH * 0.05f,
                x + throneW * 0.5f, seatTop - seatH * 0.05f,
                x + throneW * 0.85f, seatTop + seatH * 0.15f
            )
            cubicTo(
                x + throneW * 0.5f, seatTop + seatH * 0.1f,
                x - throneW * 0.5f, seatTop + seatH * 0.1f,
                x - throneW * 0.85f, seatTop + seatH * 0.15f
            )
            close()
        }
        drawPath(seatSurf, SeatBase)

        // ── Front legs (coral formations) ───────────────────────────────────
        drawCoralLeg(x - throneW * 0.8f, y, s, seatH * 0.9f, CoralOrange, CoralOrangeDk, pulse)
        drawCoralLeg(x + throneW * 0.8f, y, s, seatH * 0.9f, CoralPurple, CoralPurpleDk, pulse)

        // ── Backrest (tall coral formation) ─────────────────────────────────
        val backTop = y - seatH - backH
        val backW = throneW * 0.85f

        // Main backrest shape
        val backPath = paths.obtain().apply {
            moveTo(x - backW, seatTop)
            cubicTo(
                x - backW * 1.1f, seatTop - backH * 0.3f,
                x - backW * 0.7f, backTop + backH * 0.1f,
                x - backW * 0.3f, backTop + pulse
            )
            // Crown-like top contour
            cubicTo(
                x - backW * 0.1f, backTop - backH * 0.06f + pulse,
                x + backW * 0.1f, backTop - backH * 0.06f + pulse,
                x + backW * 0.3f, backTop + pulse
            )
            cubicTo(
                x + backW * 0.7f, backTop + backH * 0.1f,
                x + backW * 1.1f, seatTop - backH * 0.3f,
                x + backW, seatTop
            )
            close()
        }
        drawPath(backPath, CoralPink)

        // Backrest shadow
        val backShadow = paths.obtain().apply {
            moveTo(x - backW * 0.2f, seatTop)
            cubicTo(
                x + backW * 0.3f, seatTop - backH * 0.2f,
                x + backW * 0.8f, seatTop - backH * 0.35f,
                x + backW * 0.9f, seatTop
            )
            lineTo(x + backW, seatTop)
            cubicTo(
                x + backW * 1.05f, seatTop - backH * 0.3f,
                x + backW * 0.65f, backTop + backH * 0.1f,
                x + backW * 0.3f, backTop + pulse
            )
            cubicTo(
                x + backW * 0.1f, backTop + backH * 0.05f,
                x, seatTop - backH * 0.4f,
                x - backW * 0.2f, seatTop
            )
            close()
        }
        drawPath(backShadow, CoralPinkDark.copy(alpha = 0.4f))

        // Coral texture bumps on backrest
        val bumpCount = 4 + level / 2
        for (i in 0 until bumpCount) {
            val frac = (i + 1).toFloat() / (bumpCount + 1)
            val bumpY = seatTop - backH * frac
            val bumpX = x + sin(i * 2.1f) * backW * 0.4f
            val bumpR = (1.5f + sin(i * 1.3f) * 0.5f) * s
            val bumpColor = when (i % 3) {
                0 -> CoralPink
                1 -> CoralOrange
                else -> CoralPurple
            }
            drawCircle(
                color = bumpColor.copy(alpha = 0.5f),
                radius = bumpR,
                center = Offset(bumpX + pulse * 0.3f, bumpY)
            )
        }

        // ── Armrests (coral growing outward) ────────────────────────────────
        drawArmrest(x - throneW, seatTop, s, t, -1f, level, CoralOrange, CoralOrangeDk)
        drawArmrest(x + throneW, seatTop, s, t, 1f, level, CoralPurple, CoralPurpleDk)

        // ── Level 3+: tiny starfish on left armrest ─────────────────────────
        if (level >= 3) {
            val sfX = x - throneW * 1.15f
            val sfY = seatTop - 2f * s
            drawStarfish(sfX, sfY, s * 1.8f, t)
        }

        // ── Level 5+: golden seaweed crown on top ───────────────────────────
        if (level >= 5) {
            drawSeaweedCrown(x, backTop + pulse - 1f * s, s, t, backW * 0.6f)
        }
    }

    private fun DrawScope.drawCoralLeg(
        cx: Float, baseY: Float, s: Float, height: Float,
        mainColor: Color, shadowColor: Color, pulse: Float
    ) {
        val legW = 2.5f * s
        val legPath = paths.obtain().apply {
            moveTo(cx - legW, baseY)
            cubicTo(
                cx - legW * 1.2f, baseY - height * 0.4f,
                cx - legW * 0.8f, baseY - height * 0.8f,
                cx, baseY - height + pulse * 0.2f
            )
            cubicTo(
                cx + legW * 0.8f, baseY - height * 0.8f,
                cx + legW * 1.2f, baseY - height * 0.4f,
                cx + legW, baseY
            )
            close()
        }
        drawPath(legPath, mainColor)
        // Shadow
        val shadowP = paths.obtain().apply {
            moveTo(cx, baseY)
            cubicTo(
                cx + legW * 0.5f, baseY - height * 0.3f,
                cx + legW * 0.8f, baseY - height * 0.6f,
                cx + legW * 0.3f, baseY - height + pulse * 0.2f
            )
            lineTo(cx, baseY - height + pulse * 0.2f)
            cubicTo(
                cx + legW * 0.3f, baseY - height * 0.6f,
                cx + legW * 0.2f, baseY - height * 0.3f,
                cx, baseY
            )
            close()
        }
        drawPath(shadowP, shadowColor.copy(alpha = 0.35f))
    }

    private fun DrawScope.drawArmrest(
        ax: Float, ay: Float, s: Float, t: Float,
        dir: Float, level: Int, mainColor: Color, shadowColor: Color
    ) {
        val armLen = (5f + level * 0.5f) * s
        val armH = 3f * s
        val sway = sin(t * 0.6f * PI.toFloat()) * 0.5f * s

        val tipX = ax + dir * armLen
        val tipY = ay - armH + sway

        val armPath = paths.obtain().apply {
            moveTo(ax, ay)
            cubicTo(
                ax + dir * armLen * 0.3f, ay - armH * 1.5f,
                ax + dir * armLen * 0.7f, tipY - armH * 0.3f,
                tipX, tipY
            )
            cubicTo(
                ax + dir * armLen * 0.7f, tipY + armH * 0.5f,
                ax + dir * armLen * 0.3f, ay + armH * 0.3f,
                ax, ay
            )
            close()
        }
        drawPath(armPath, mainColor)
        drawPath(armPath, shadowColor.copy(alpha = 0.2f),
            style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f)))

        // Coral nub at tip
        drawCircle(
            color = mainColor,
            radius = (1.5f * s).coerceAtLeast(0.7f),
            center = Offset(tipX, tipY)
        )
    }

    private fun DrawScope.drawStarfish(cx: Float, cy: Float, sz: Float, t: Float) {
        val arms = 5
        val innerR = sz * 0.3f
        val outerR = sz
        val wobble = sin(t * 0.4f * PI.toFloat()) * 0.1f

        val starPath = paths.obtain()
        for (i in 0 until arms * 2) {
            val angle = i.toFloat() / (arms * 2) * 2f * PI.toFloat() - PI.toFloat() / 2f + wobble
            val r = if (i % 2 == 0) outerR else innerR
            val px = cx + cos(angle) * r
            val py = cy + sin(angle) * r
            if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
        }
        starPath.close()
        drawPath(starPath, StarfishOrange)
        // Center highlight
        drawCircle(StarfishLight.copy(alpha = 0.5f), innerR * 0.6f, Offset(cx, cy))
    }

    private fun DrawScope.drawSeaweedCrown(
        cx: Float, cy: Float, s: Float, t: Float, halfW: Float
    ) {
        val peakCount = 5
        val crownH = 5f * s

        for (i in 0 until peakCount) {
            val frac = (i.toFloat() / (peakCount - 1)) - 0.5f
            val px = cx + frac * halfW * 2f
            val peakH = crownH * (0.7f + sin(i * 1.5f) * 0.3f)
            val sway = sin(t * 1.0f + i * 0.8f) * s * 0.8f

            drawLine(
                color = CrownGold,
                start = Offset(px, cy),
                end = Offset(px + sway, cy - peakH),
                strokeWidth = (1.5f * s).coerceAtLeast(0.7f),
                cap = StrokeCap.Round
            )
            // Gold tip orb
            drawCircle(
                color = CrownGoldLight,
                radius = (0.8f * s).coerceAtLeast(0.4f),
                center = Offset(px + sway, cy - peakH)
            )
        }
    }
}
