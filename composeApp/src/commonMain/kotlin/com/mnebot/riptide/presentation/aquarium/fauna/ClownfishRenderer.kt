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
private val ClownOrange  = Color(0xFFE8611A)  // deep orange body
private val ClownOrange2 = Color(0xFFFF8C3A)  // lighter orange highlight
private val ClownWhite   = Color(0xFFF5F5F5)  // white stripes
private val ClownBlack   = Color(0xFF1A1A1A)  // black outlines & head
private val ClownFin     = Color(0xFFB84010)  // darker fin orange
private val ClownEye     = Color(0xFF111122)  // dark pupil

object ClownfishRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen = (9f + level * 0.6f) * s
        val bodyH   = (6f + level * 0.4f) * s
        val tailExt = (4.5f + level * 0.35f) * s

        val tailSway = sin(t * 3.8f * PI.toFloat()) * 1.8f * s
        val bodyWave = sin(t * 3.8f * PI.toFloat() + 0.5f) * 0.4f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FIN ──────────────────────────────────────────────────────
            val tailX = x + bodyLen * 0.5f
            val tailH  = bodyH * 1.1f

            val tail = Path().apply {
                moveTo(tailX, y - bodyH * 0.25f)
                cubicTo(
                    tailX + tailExt * 0.4f, y - tailH * 0.5f + tailSway * 0.7f,
                    tailX + tailExt * 0.8f, y - tailH + tailSway,
                    tailX + tailExt, y - tailH * 1.1f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.7f, y + tailSway * 0.4f,
                    tailX + tailExt * 0.4f, y + tailH * 0.4f + tailSway * 0.4f,
                    tailX + tailExt, y + tailH * 1.1f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.8f, y + tailH + tailSway,
                    tailX + tailExt * 0.4f, y + tailH * 0.5f + tailSway * 0.7f,
                    tailX, y + bodyH * 0.25f
                )
                close()
            }
            drawPath(tail, ClownOrange)
            // Black edge stripe on tail
            drawPath(tail, ClownBlack.copy(alpha = 0.5f), style = Stroke(width = (1.2f * s).coerceAtLeast(0.5f)))

            // ── DORSAL FIN ────────────────────────────────────────────────────
            val dorsalSway = sin(t * 3.0f * PI.toFloat()) * 0.4f * s
            val dorsal = Path().apply {
                moveTo(x - bodyLen * 0.5f, y - bodyH * 0.88f)
                cubicTo(
                    x - bodyLen * 0.1f + dorsalSway, y - bodyH * 1.55f,
                    x + bodyLen * 0.2f + dorsalSway, y - bodyH * 1.50f,
                    x + bodyLen * 0.4f, y - bodyH * 0.88f
                )
                cubicTo(
                    x + bodyLen * 0.2f, y - bodyH * 0.82f,
                    x, y - bodyH * 0.84f,
                    x - bodyLen * 0.5f, y - bodyH * 0.88f
                )
                close()
            }
            drawPath(dorsal, ClownBlack)
            // Orange base strip on dorsal
            val dorsalBase = Path().apply {
                moveTo(x - bodyLen * 0.5f, y - bodyH * 0.88f)
                cubicTo(
                    x - bodyLen * 0.2f, y - bodyH * 1.05f,
                    x + bodyLen * 0.1f, y - bodyH * 1.02f,
                    x + bodyLen * 0.4f, y - bodyH * 0.88f
                )
                cubicTo(x + bodyLen * 0.1f, y - bodyH * 0.82f,
                    x - bodyLen * 0.2f, y - bodyH * 0.82f,
                    x - bodyLen * 0.5f, y - bodyH * 0.88f)
                close()
            }
            drawPath(dorsalBase, ClownFin.copy(alpha = 0.9f))

            // ── ANAL FIN ──────────────────────────────────────────────────────
            val anal = Path().apply {
                moveTo(x + bodyLen * 0.05f, y + bodyH * 0.88f)
                cubicTo(
                    x + bodyLen * 0.2f, y + bodyH * 1.3f,
                    x + bodyLen * 0.38f, y + bodyH * 1.28f,
                    x + bodyLen * 0.45f, y + bodyH * 0.88f
                )
                close()
            }
            drawPath(anal, ClownBlack)

            // ── MAIN BODY ─────────────────────────────────────────────────────
            val body = Path().apply {
                moveTo(x - bodyLen, y + bodyWave * 0.2f)
                cubicTo(
                    x - bodyLen * 0.75f, y - bodyH,
                    x + bodyLen * 0.05f, y - bodyH * 0.98f,
                    x + bodyLen * 0.5f, y - bodyH * 0.3f + bodyWave * 0.1f
                )
                lineTo(x + bodyLen * 0.5f, y + bodyH * 0.3f + bodyWave * 0.1f)
                cubicTo(
                    x + bodyLen * 0.05f, y + bodyH * 0.98f,
                    x - bodyLen * 0.75f, y + bodyH,
                    x - bodyLen, y + bodyWave * 0.2f
                )
                close()
            }
            drawPath(body, ClownOrange)

            // Body highlight
            val highlight = Path().apply {
                moveTo(x - bodyLen * 0.7f, y - bodyH * 0.5f)
                cubicTo(
                    x - bodyLen * 0.5f, y - bodyH * 0.85f,
                    x + bodyLen * 0.1f, y - bodyH * 0.80f,
                    x + bodyLen * 0.35f, y - bodyH * 0.35f
                )
                cubicTo(
                    x + bodyLen * 0.1f, y - bodyH * 0.55f,
                    x - bodyLen * 0.3f, y - bodyH * 0.6f,
                    x - bodyLen * 0.7f, y - bodyH * 0.5f
                )
                close()
            }
            drawPath(highlight, ClownOrange2.copy(alpha = 0.45f))

            // ── WHITE STRIPES (3) WITH BLACK EDGES ────────────────────────────
            // Stripe 1: behind head
            val stripeW = bodyH * 0.55f
            data class StripePos(val cx: Float, val top: Float, val bot: Float, val hw: Float)
            val stripes = listOf(
                StripePos(x - bodyLen * 0.55f, -bodyH * 0.90f, bodyH * 0.88f, stripeW * 0.45f),
                StripePos(x - bodyLen * 0.05f, -bodyH * 0.98f, bodyH * 0.96f, stripeW * 0.50f),
                StripePos(x + bodyLen * 0.38f, -bodyH * 0.55f, bodyH * 0.52f, stripeW * 0.40f),
            )
            for (stripe in stripes) {
                // Black edge
                drawLine(
                    ClownBlack,
                    Offset(stripe.cx - stripe.hw - 1.0f * s, y + stripe.top),
                    Offset(stripe.cx - stripe.hw - 1.0f * s, y + stripe.bot),
                    strokeWidth = (1.2f * s).coerceAtLeast(0.5f)
                )
                drawLine(
                    ClownBlack,
                    Offset(stripe.cx + stripe.hw + 1.0f * s, y + stripe.top),
                    Offset(stripe.cx + stripe.hw + 1.0f * s, y + stripe.bot),
                    strokeWidth = (1.2f * s).coerceAtLeast(0.5f)
                )
                // White center
                drawLine(
                    ClownWhite,
                    Offset(stripe.cx, y + stripe.top),
                    Offset(stripe.cx, y + stripe.bot),
                    strokeWidth = (stripe.hw * 2f).coerceAtLeast(1.0f)
                )
            }

            // ── BLACK HEAD/FACE PATCH ─────────────────────────────────────────
            val head = Path().apply {
                moveTo(x - bodyLen, y + bodyWave * 0.2f)
                cubicTo(
                    x - bodyLen * 0.75f, y - bodyH * 0.9f,
                    x - bodyLen * 0.62f, y - bodyH * 0.88f,
                    x - bodyLen * 0.52f, y - bodyH * 0.3f
                )
                lineTo(x - bodyLen * 0.52f, y + bodyH * 0.3f)
                cubicTo(
                    x - bodyLen * 0.62f, y + bodyH * 0.88f,
                    x - bodyLen * 0.75f, y + bodyH * 0.9f,
                    x - bodyLen, y + bodyWave * 0.2f
                )
                close()
            }
            drawPath(head, ClownBlack.copy(alpha = 0.80f))

            // ── PECTORAL FIN ──────────────────────────────────────────────────
            val pectSway = sin(t * 4.5f * PI.toFloat()) * 1.2f * s
            val pect = Path().apply {
                moveTo(x - bodyLen * 0.3f, y + bodyH * 0.05f)
                cubicTo(
                    x - bodyLen * 0.1f + pectSway, y + bodyH * 0.7f,
                    x + bodyLen * 0.05f + pectSway, y + bodyH * 0.75f,
                    x + bodyLen * 0.18f, y + bodyH * 0.4f
                )
                cubicTo(
                    x, y + bodyH * 0.25f,
                    x - bodyLen * 0.15f, y + bodyH * 0.15f,
                    x - bodyLen * 0.3f, y + bodyH * 0.05f
                )
                close()
            }
            drawPath(pect, ClownOrange2.copy(alpha = 0.80f))

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.4f + level * 0.08f) * s
            val eyeX = x - bodyLen * 0.72f
            val eyeY = y - bodyH * 0.30f
            drawCircle(ClownWhite, eyeR, Offset(eyeX, eyeY))
            drawCircle(ClownEye, eyeR * 0.60f, Offset(eyeX + eyeR * 0.08f, eyeY))
            // Eye shine
            drawCircle(ClownWhite, eyeR * 0.22f, Offset(eyeX - eyeR * 0.25f, eyeY - eyeR * 0.25f))

            // ── LEVEL 3+: Lateral line (orange-gold) ──────────────────────────
            if (level >= 3) {
                drawLine(
                    ClownOrange2.copy(alpha = 0.55f),
                    Offset(x - bodyLen * 0.45f, y - bodyH * 0.10f),
                    Offset(x + bodyLen * 0.45f, y - bodyH * 0.12f),
                    strokeWidth = (0.9f * s).coerceAtLeast(0.5f),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
