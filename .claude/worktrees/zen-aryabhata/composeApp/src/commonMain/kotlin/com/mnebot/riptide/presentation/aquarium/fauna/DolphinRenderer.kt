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
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val DolGrey   = Color(0xFF3A5A6A)  // medium blue-grey body
private val DolGrey2  = Color(0xFF4E6E80)  // lighter back
private val DolBelly  = Color(0xFFBBCED8)  // pale belly
private val DolBeak   = Color(0xFF2A4A5A)  // beak
private val DolWhite  = Color(0xFFF0F4F8)  // white mouth/smile
private val DolEye    = Color(0xFF111122)
private val DolFin    = Color(0xFF2A4A58)  // fins

object DolphinRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen  = (14f + level * 0.8f) * s
        val bodyH    = (5f  + level * 0.3f) * s
        val tailExt  = (6f  + level * 0.4f) * s
        val beakLen  = (5f  + level * 0.3f) * s

        // Dolphin arcs vertically as it swims (high verticalCoupling=0.80)
        // The vertical position is already handled by the aquarium, so just animate body
        val bodyArc  = sin(t * 1.3f * PI.toFloat()) * bodyH * 0.18f
        val tailSway = sin(t * 1.3f * PI.toFloat() + 0.6f) * 2.5f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FLUKES (horizontal, like all cetaceans) ───────────────────
            val tailX = x + bodyLen * 0.48f
            // Upper fluke
            val upperFluke = Path().apply {
                moveTo(tailX, y + bodyArc * 0.5f)
                cubicTo(tailX + tailExt * 0.4f, y - bodyH * 0.2f + tailSway,
                    tailX + tailExt * 0.8f, y - bodyH * 0.8f + tailSway,
                    tailX + tailExt, y - bodyH * 1.0f + tailSway)
                cubicTo(tailX + tailExt * 0.8f, y - bodyH * 0.5f + tailSway,
                    tailX + tailExt * 0.5f, y - bodyH * 0.1f + tailSway * 0.5f,
                    tailX, y + bodyArc * 0.5f)
                close()
            }
            drawPath(upperFluke, DolFin)

            // Lower fluke
            val lowerFluke = Path().apply {
                moveTo(tailX, y + bodyArc * 0.5f)
                cubicTo(tailX + tailExt * 0.5f, y + bodyH * 0.15f + tailSway * 0.5f,
                    tailX + tailExt * 0.8f, y + bodyH * 0.5f + tailSway,
                    tailX + tailExt, y + bodyH * 1.0f + tailSway)
                cubicTo(tailX + tailExt * 0.8f, y + bodyH * 0.8f + tailSway,
                    tailX + tailExt * 0.4f, y + bodyH * 0.2f + tailSway,
                    tailX, y + bodyArc * 0.5f)
                close()
            }
            drawPath(lowerFluke, DolFin)

            // ── DORSAL FIN (curved, mid-back) ──────────────────────────────────
            val dorsalSway = tailSway * 0.2f
            val dorsal = Path().apply {
                moveTo(x + bodyLen * 0.05f, y - bodyH * 0.88f + bodyArc * 0.3f)
                cubicTo(x + bodyLen * 0.10f + dorsalSway, y - bodyH * 1.80f + bodyArc * 0.3f,
                    x + bodyLen * 0.20f + dorsalSway, y - bodyH * 1.75f + bodyArc * 0.3f,
                    x + bodyLen * 0.28f, y - bodyH * 0.88f + bodyArc * 0.3f)
                cubicTo(x + bodyLen * 0.20f, y - bodyH * 0.84f + bodyArc * 0.3f,
                    x + bodyLen * 0.12f, y - bodyH * 0.85f + bodyArc * 0.3f,
                    x + bodyLen * 0.05f, y - bodyH * 0.88f + bodyArc * 0.3f)
                close()
            }
            drawPath(dorsal, DolFin)

            // ── PECTORAL FINS (paddle-shaped) ──────────────────────────────────
            val pectSway = sin(t * 2.6f * PI.toFloat()) * 1.2f * s
            val pect = Path().apply {
                moveTo(x - bodyLen * 0.15f, y + bodyH * 0.55f + bodyArc * 0.4f)
                cubicTo(x - bodyLen * 0.05f + pectSway, y + bodyH * 1.40f + bodyArc * 0.2f,
                    x + bodyLen * 0.08f + pectSway, y + bodyH * 1.50f + bodyArc * 0.2f,
                    x + bodyLen * 0.20f, y + bodyH * 0.90f + bodyArc * 0.3f)
                cubicTo(x + bodyLen * 0.08f, y + bodyH * 0.75f + bodyArc * 0.3f,
                    x - bodyLen * 0.02f, y + bodyH * 0.65f + bodyArc * 0.35f,
                    x - bodyLen * 0.15f, y + bodyH * 0.55f + bodyArc * 0.4f)
                close()
            }
            drawPath(pect, DolGrey)

            // ── MAIN BODY ──────────────────────────────────────────────────────
            val body = Path().apply {
                moveTo(x - bodyLen * 0.5f - beakLen, y + bodyArc * 0.5f)  // beak tip
                cubicTo(x - bodyLen * 0.5f - beakLen * 0.5f, y - bodyH * 0.35f + bodyArc * 0.5f,
                    x - bodyLen * 0.5f + bodyH * 0.3f, y - bodyH * 0.8f + bodyArc * 0.35f,
                    x - bodyLen * 0.2f, y - bodyH * 0.92f + bodyArc * 0.25f)
                cubicTo(x + bodyLen * 0.1f, y - bodyH + bodyArc * 0.15f,
                    x + bodyLen * 0.3f, y - bodyH * 0.9f + bodyArc * 0.1f,
                    x + bodyLen * 0.48f, y - bodyH * 0.35f + bodyArc * 0.15f)
                lineTo(x + bodyLen * 0.48f, y + bodyH * 0.35f + bodyArc * 0.15f)
                cubicTo(x + bodyLen * 0.3f, y + bodyH * 0.9f + bodyArc * 0.1f,
                    x + bodyLen * 0.1f, y + bodyH + bodyArc * 0.15f,
                    x - bodyLen * 0.2f, y + bodyH * 0.92f + bodyArc * 0.25f)
                cubicTo(x - bodyLen * 0.5f + bodyH * 0.3f, y + bodyH * 0.8f + bodyArc * 0.35f,
                    x - bodyLen * 0.5f - beakLen * 0.5f, y + bodyH * 0.25f + bodyArc * 0.5f,
                    x - bodyLen * 0.5f - beakLen, y + bodyArc * 0.5f)
                close()
            }
            drawPath(body, DolGrey2)

            // Belly
            val belly = Path().apply {
                moveTo(x - bodyLen * 0.5f - beakLen * 0.8f, y + bodyArc * 0.5f + bodyH * 0.08f)
                cubicTo(x - bodyLen * 0.35f, y + bodyH * 0.82f + bodyArc * 0.3f,
                    x + bodyLen * 0.15f, y + bodyH * 0.92f + bodyArc * 0.15f,
                    x + bodyLen * 0.45f, y + bodyH * 0.32f + bodyArc * 0.15f)
                cubicTo(x + bodyLen * 0.15f, y + bodyH * 0.62f + bodyArc * 0.15f,
                    x - bodyLen * 0.2f, y + bodyH * 0.72f + bodyArc * 0.25f,
                    x - bodyLen * 0.5f - beakLen * 0.8f, y + bodyArc * 0.5f + bodyH * 0.08f)
                close()
            }
            drawPath(belly, DolBelly.copy(alpha = 0.55f))

            // ── LEVEL 3+: Lateral flank stripe (bottlenose melon-yellow marking) ──
            if (level >= 3) {
                val stripe = Path().apply {
                    moveTo(x - bodyLen * 0.40f, y - bodyH * 0.28f + bodyArc * 0.3f)
                    cubicTo(
                        x - bodyLen * 0.10f, y - bodyH * 0.52f + bodyArc * 0.2f,
                        x + bodyLen * 0.15f, y - bodyH * 0.48f + bodyArc * 0.15f,
                        x + bodyLen * 0.45f, y - bodyH * 0.22f + bodyArc * 0.12f
                    )
                    cubicTo(
                        x + bodyLen * 0.15f, y - bodyH * 0.36f + bodyArc * 0.15f,
                        x - bodyLen * 0.10f, y - bodyH * 0.38f + bodyArc * 0.2f,
                        x - bodyLen * 0.40f, y - bodyH * 0.16f + bodyArc * 0.3f
                    )
                    close()
                }
                drawPath(stripe, Color(0xFFD4A840).copy(alpha = 0.28f))
            }

            // ── LEVEL 5+: Iridescent dorsal sheen ────────────────────────────────
            if (level >= 5) {
                val sheenPulse = (sin(t * 1.6f * PI.toFloat()) * 0.5f + 0.5f) * 0.15f + 0.12f
                drawLine(
                    DolBelly.copy(alpha = sheenPulse),
                    Offset(x - bodyLen * 0.28f, y - bodyH * 0.88f + bodyArc * 0.25f),
                    Offset(x + bodyLen * 0.28f, y - bodyH * 0.85f + bodyArc * 0.12f),
                    strokeWidth = (3.0f * s).coerceAtLeast(1.2f), cap = StrokeCap.Round
                )
            }

            // Smile / jaw line
            val smile = Path().apply {
                moveTo(x - bodyLen * 0.5f - beakLen, y + bodyArc * 0.5f + bodyH * 0.10f)
                cubicTo(
                    x - bodyLen * 0.5f - beakLen * 0.7f, y + bodyArc * 0.5f + bodyH * 0.22f,
                    x - bodyLen * 0.5f - beakLen * 0.3f, y + bodyArc * 0.5f + bodyH * 0.25f,
                    x - bodyLen * 0.5f + bodyH * 0.2f, y + bodyArc * 0.5f + bodyH * 0.20f
                )
            }
            drawPath(smile, DolWhite.copy(alpha = 0.70f),
                style = Stroke(width = (0.9f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round))

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.3f + level * 0.08f) * s
            val eyeX = x - bodyLen * 0.38f
            val eyeY = y - bodyH * 0.38f + bodyArc * 0.3f
            // Eye patch (distinctive darker eye patch)
            drawCircle(DolGrey.copy(alpha = 0.60f), eyeR * 1.8f, Offset(eyeX, eyeY))
            drawCircle(DolBelly, eyeR * 1.1f, Offset(eyeX, eyeY))
            drawCircle(DolEye, eyeR * 0.60f, Offset(eyeX + eyeR * 0.08f, eyeY))
            drawCircle(Color.White, eyeR * 0.20f, Offset(eyeX - eyeR * 0.22f, eyeY - eyeR * 0.22f))
        }
    }
}
