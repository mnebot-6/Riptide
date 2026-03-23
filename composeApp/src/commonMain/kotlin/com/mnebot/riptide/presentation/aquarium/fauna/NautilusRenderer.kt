package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val NautWhite  = Color(0xFFF5F0E8)  // shell white
private val NautBrown  = Color(0xFFB07840)  // orange-brown stripes
private val NautBrown2 = Color(0xFF6A4820)  // dark spiral
private val NautInner  = Color(0xFFDDCCA8)  // inner shell
private val NautTent   = Color(0xFFCCAA88)  // tentacles
private val NautEye    = Color(0xFF333322)  // eye

object NautilusRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        // Nautilus slowly rotates as it swims
        val rotAngle = sin(t * 0.5f * PI.toFloat()) * 0.12f  // gentle rock

        val shellR = (9f + level * 0.55f) * s
        val tentLen = (4f + level * 0.3f) * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
            rotate(rotAngle * 180f / PI.toFloat(), pivot = Offset(x, y))
        }) {

            // ── SHELL BASE (white outer) ────────────────────────────────────────
            drawCircle(NautWhite, shellR, Offset(x, y))

            // ── BROWN SPIRAL STRIPES (radiating from centre) ──────────────────
            val stripeCount = 7 + level
            for (i in 0 until stripeCount) {
                val startAngle = i.toFloat() / stripeCount * 2f * PI.toFloat()
                val endAngle   = startAngle + PI.toFloat() / stripeCount
                val stripe = Path().apply {
                    moveTo(x, y)
                    lineTo(x + cos(startAngle) * shellR, y + sin(startAngle) * shellR)
                    cubicTo(
                        x + cos(startAngle + 0.15f) * shellR * 0.9f, y + sin(startAngle + 0.15f) * shellR * 0.9f,
                        x + cos(endAngle - 0.15f) * shellR * 0.9f,   y + sin(endAngle - 0.15f) * shellR * 0.9f,
                        x + cos(endAngle) * shellR,                   y + sin(endAngle) * shellR
                    )
                    close()
                }
                if (i % 2 == 0) drawPath(stripe, NautBrown.copy(alpha = 0.55f))
            }

            // ── SPIRAL SEPTA (internal chambers) ──────────────────────────────
            for (ring in 1..4) {
                val ringR = shellR * (0.25f + ring * 0.18f)
                drawCircle(
                    NautBrown2.copy(alpha = 0.25f + ring * 0.04f),
                    ringR,
                    Offset(x, y),
                    style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f))
                )
            }

            // ── INNER UMBILICUS (dark centre dot) ────────────────────────────
            drawCircle(NautBrown2.copy(alpha = 0.65f), shellR * 0.12f, Offset(x, y))

            // ── SHELL OUTLINE ─────────────────────────────────────────────────
            drawCircle(
                NautBrown2.copy(alpha = 0.40f), shellR,
                style = Stroke(width = (1.0f * s).coerceAtLeast(0.5f))
            )

            // ── APERTURE (opening, to the left) ──────────────────────────────
            // The opening is on the left side when moving right
            val apertureX = x - shellR * 0.75f
            val apertureW = shellR * 0.45f
            val apertureH = shellR * 0.55f
            val aperture = Path().apply {
                moveTo(apertureX - apertureW * 0.2f, y - apertureH * 0.9f)
                cubicTo(apertureX - apertureW, y - apertureH * 0.5f,
                    apertureX - apertureW, y + apertureH * 0.5f,
                    apertureX - apertureW * 0.2f, y + apertureH * 0.9f)
                cubicTo(apertureX + apertureW * 0.5f, y + apertureH * 0.6f,
                    apertureX + apertureW * 0.5f, y - apertureH * 0.6f,
                    apertureX - apertureW * 0.2f, y - apertureH * 0.9f)
                close()
            }
            drawPath(aperture, NautInner)
            drawPath(aperture, NautBrown2.copy(alpha = 0.30f),
                style = Stroke(width = (0.7f * s).coerceAtLeast(0.3f)))

            // ── TENTACLES (level 1+, more at higher levels) ───────────────────
            val tentacleCount = (6 + level * 2).coerceAtMost(18)
            for (i in 0 until tentacleCount) {
                val tFrac = (i.toFloat() - tentacleCount / 2f) / tentacleCount.toFloat()
                val tentX = apertureX - apertureW * 0.6f
                val tentY = y + tFrac * apertureH * 1.4f
                val tentSway = sin(t * 2.5f * PI.toFloat() + i * 0.4f) * tentLen * 0.4f
                val tLen = tentLen * (0.7f + 0.3f * sin(i.toFloat()))
                drawLine(
                    NautTent.copy(alpha = 0.75f),
                    Offset(tentX, tentY),
                    Offset(tentX - tLen + tentSway, tentY + tentSway * 0.3f),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f),
                    cap = StrokeCap.Round
                )
            }

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.1f + level * 0.07f) * s
            val eyeX = apertureX - apertureW * 0.1f
            val eyeY = y - apertureH * 0.30f
            drawCircle(NautWhite, eyeR * 1.1f, Offset(eyeX, eyeY))
            drawCircle(NautEye, eyeR * 0.65f, Offset(eyeX, eyeY))
        }
    }
}
