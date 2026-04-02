package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.mnebot.riptide.presentation.aquarium.PathPool
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin

// ── Palette ──────────────────────────────────────────────────────────────────
private val BodyYellow     = Color(0xFFFDD84E)  // bright lemon yellow
private val BodyGold       = Color(0xFFEDC030)  // darker golden yellow
private val BodyPaleYellow = Color(0xFFFEEF8A)  // pale highlight yellow
private val StripBlack     = Color(0xFF1A1A20)  // stripe near-black
private val StripDarkGray  = Color(0xFF3A3A44)  // stripe dark gray
private val BandWhite      = Color(0xFFF8F6EE)  // white band between stripes
private val BandCream      = Color(0xFFEDE8D8)  // cream-white shading
private val EyeStripBlack  = Color(0xFF0A0A12)  // eye bar black
private val FinYellow      = Color(0xFFFBC830)  // fin warm yellow
private val FinOrange      = Color(0xFFF5A020)  // fin orange accent
private val ShadowNavy     = Color(0xFF08082A)  // dark shadow
private val ShadowBlue     = Color(0xFF0A3060)  // blue shadow

object ButterflyfishRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyR   = (8f + level * 0.5f) * s   // disc radius
        val bodyRX  = bodyR * 1.05f              // slightly wider than tall
        val bodyRY  = bodyR * 0.95f
        val tailExt = (3.5f + level * 0.3f) * s

        // Gentle hover + subtle tail sway
        val hoverY   = sin(t * 2.2f * PI.toFloat()) * s * 0.06f
        val tailSway = sin(t * 3.0f * PI.toFloat()) * 0.6f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = hoverY)
            }) {

            // ── TAIL FIN ────────────────────────────────────────────────────
            val tailX = x + bodyRX * 0.85f
            val tailShadow = paths.obtain().apply {
                moveTo(tailX - bodyRX * 0.05f, y - bodyRY * 0.15f)
                cubicTo(
                    tailX + tailExt * 0.5f, y - tailExt * 0.65f + tailSway,
                    tailX + tailExt * 0.8f, y - tailExt * 0.4f + tailSway,
                    tailX + tailExt, y + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.8f, y + tailExt * 0.4f + tailSway,
                    tailX + tailExt * 0.5f, y + tailExt * 0.65f + tailSway,
                    tailX - bodyRX * 0.05f, y + bodyRY * 0.15f
                )
                close()
            }
            drawPath(tailShadow, ShadowBlue.copy(alpha = 0.18f))

            val tail = paths.obtain().apply {
                moveTo(tailX, y - bodyRY * 0.12f)
                cubicTo(
                    tailX + tailExt * 0.45f, y - tailExt * 0.6f + tailSway,
                    tailX + tailExt * 0.75f, y - tailExt * 0.35f + tailSway,
                    tailX + tailExt * 0.95f, y + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.75f, y + tailExt * 0.35f + tailSway,
                    tailX + tailExt * 0.45f, y + tailExt * 0.6f + tailSway,
                    tailX, y + bodyRY * 0.12f
                )
                close()
            }
            drawPath(tail, FinYellow)
            drawPath(tail, FinOrange.copy(alpha = 0.3f))

            // ── DORSAL FIN (thin, spiny) ────────────────────────────────────
            val dorsalSway = sin(t * 2.5f * PI.toFloat()) * 0.3f * s
            val dorsal = paths.obtain().apply {
                moveTo(x - bodyRX * 0.3f, y - bodyRY * 0.92f)
                cubicTo(
                    x - bodyRX * 0.05f + dorsalSway, y - bodyRY * 1.55f,
                    x + bodyRX * 0.2f + dorsalSway, y - bodyRY * 1.50f,
                    x + bodyRX * 0.5f, y - bodyRY * 0.88f
                )
                cubicTo(
                    x + bodyRX * 0.25f, y - bodyRY * 0.84f,
                    x - bodyRX * 0.05f, y - bodyRY * 0.86f,
                    x - bodyRX * 0.3f, y - bodyRY * 0.92f
                )
                close()
            }
            drawPath(dorsal, BodyGold.copy(alpha = 0.85f))
            // Dorsal spines
            for (i in 0 until 5) {
                val spineX = x - bodyRX * 0.2f + i * bodyRX * 0.16f
                val spineTop = y - bodyRY * 1.0f - (bodyRY * 0.35f - i * bodyRY * 0.04f)
                drawLine(
                    StripBlack.copy(alpha = 0.35f),
                    Offset(spineX, y - bodyRY * 0.90f),
                    Offset(spineX + dorsalSway * 0.3f, spineTop),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.25f),
                    cap = StrokeCap.Round
                )
            }

            // ── PELVIC FIN (thin, yellow) ───────────────────────────────────
            val pelvic = paths.obtain().apply {
                moveTo(x - bodyRX * 0.25f, y + bodyRY * 0.88f)
                cubicTo(
                    x - bodyRX * 0.10f, y + bodyRY * 1.35f,
                    x + bodyRX * 0.15f, y + bodyRY * 1.30f,
                    x + bodyRX * 0.25f, y + bodyRY * 0.85f
                )
                close()
            }
            drawPath(pelvic, FinYellow.copy(alpha = 0.7f))

            // ── MAIN BODY (disc shape) ──────────────────────────────────────
            val bodyShadow = paths.obtain().apply {
                moveTo(x - bodyRX * 1.03f, y)
                cubicTo(x - bodyRX * 1.03f, y - bodyRY * 1.03f, x + bodyRX * 1.03f, y - bodyRY * 1.03f, x + bodyRX * 1.03f, y)
                cubicTo(x + bodyRX * 1.03f, y + bodyRY * 1.03f, x - bodyRX * 1.03f, y + bodyRY * 1.03f, x - bodyRX * 1.03f, y)
                close()
            }
            drawPath(bodyShadow, ShadowBlue.copy(alpha = 0.15f))

            val body = paths.obtain().apply {
                moveTo(x - bodyRX, y)
                cubicTo(x - bodyRX, y - bodyRY, x + bodyRX, y - bodyRY, x + bodyRX, y)
                cubicTo(x + bodyRX, y + bodyRY, x - bodyRX, y + bodyRY, x - bodyRX, y)
                close()
            }
            drawPath(body, BodyYellow)

            // Upper highlight
            val highlight = paths.obtain().apply {
                moveTo(x - bodyRX * 0.6f, y - bodyRY * 0.35f)
                cubicTo(
                    x - bodyRX * 0.4f, y - bodyRY * 0.82f,
                    x + bodyRX * 0.3f, y - bodyRY * 0.78f,
                    x + bodyRX * 0.5f, y - bodyRY * 0.25f
                )
                cubicTo(
                    x + bodyRX * 0.2f, y - bodyRY * 0.50f,
                    x - bodyRX * 0.2f, y - bodyRY * 0.52f,
                    x - bodyRX * 0.6f, y - bodyRY * 0.35f
                )
                close()
            }
            drawPath(highlight, BodyPaleYellow.copy(alpha = 0.40f))

            // ── VERTICAL STRIPES (black + white alternating) ────────────────
            data class StripeSpec(val cx: Float, val topY: Float, val botY: Float, val hw: Float)
            val stripes = listOf(
                StripeSpec(x - bodyRX * 0.30f, y - bodyRY * 0.85f, y + bodyRY * 0.82f, bodyR * 0.06f),
                StripeSpec(x - bodyRX * 0.05f, y - bodyRY * 0.92f, y + bodyRY * 0.90f, bodyR * 0.07f),
                StripeSpec(x + bodyRX * 0.18f, y - bodyRY * 0.88f, y + bodyRY * 0.85f, bodyR * 0.06f),
                StripeSpec(x + bodyRX * 0.40f, y - bodyRY * 0.75f, y + bodyRY * 0.72f, bodyR * 0.05f),
                StripeSpec(x + bodyRX * 0.58f, y - bodyRY * 0.60f, y + bodyRY * 0.55f, bodyR * 0.05f),
            )

            for ((idx, stripe) in stripes.withIndex()) {
                // Alternating black and white
                val isBlack = idx % 2 == 0
                val stripeColor = if (isBlack) StripBlack.copy(alpha = 0.70f)
                                  else BandWhite.copy(alpha = 0.80f)
                val stripePath = paths.obtain().apply {
                    moveTo(stripe.cx - stripe.hw, stripe.topY)
                    cubicTo(
                        stripe.cx - stripe.hw + stripe.hw * 0.4f, stripe.topY + (stripe.botY - stripe.topY) * 0.3f,
                        stripe.cx - stripe.hw - stripe.hw * 0.2f, stripe.topY + (stripe.botY - stripe.topY) * 0.7f,
                        stripe.cx - stripe.hw, stripe.botY
                    )
                    lineTo(stripe.cx + stripe.hw, stripe.botY)
                    cubicTo(
                        stripe.cx + stripe.hw + stripe.hw * 0.2f, stripe.topY + (stripe.botY - stripe.topY) * 0.7f,
                        stripe.cx + stripe.hw - stripe.hw * 0.4f, stripe.topY + (stripe.botY - stripe.topY) * 0.3f,
                        stripe.cx + stripe.hw, stripe.topY
                    )
                    close()
                }
                drawPath(stripePath, stripeColor)
            }

            // ── EYE BAR (black diagonal stripe over eye) ────────────────────
            val eyeBarPath = paths.obtain().apply {
                moveTo(x - bodyRX * 0.72f, y - bodyRY * 0.75f)
                cubicTo(
                    x - bodyRX * 0.62f, y - bodyRY * 0.90f,
                    x - bodyRX * 0.52f, y - bodyRY * 0.88f,
                    x - bodyRX * 0.48f, y - bodyRY * 0.70f
                )
                lineTo(x - bodyRX * 0.48f, y + bodyRY * 0.55f)
                cubicTo(
                    x - bodyRX * 0.52f, y + bodyRY * 0.72f,
                    x - bodyRX * 0.62f, y + bodyRY * 0.75f,
                    x - bodyRX * 0.72f, y + bodyRY * 0.60f
                )
                close()
            }
            drawPath(eyeBarPath, EyeStripBlack.copy(alpha = 0.80f))

            // Level 3+: eye stripe wider and more prominent
            if (level >= 3) {
                val eyeBarWide = paths.obtain().apply {
                    moveTo(x - bodyRX * 0.75f, y - bodyRY * 0.78f)
                    cubicTo(
                        x - bodyRX * 0.66f, y - bodyRY * 0.95f,
                        x - bodyRX * 0.56f, y - bodyRY * 0.92f,
                        x - bodyRX * 0.45f, y - bodyRY * 0.72f
                    )
                    lineTo(x - bodyRX * 0.45f, y + bodyRY * 0.58f)
                    cubicTo(
                        x - bodyRX * 0.56f, y + bodyRY * 0.78f,
                        x - bodyRX * 0.66f, y + bodyRY * 0.80f,
                        x - bodyRX * 0.75f, y + bodyRY * 0.63f
                    )
                    close()
                }
                drawPath(eyeBarWide, EyeStripBlack.copy(alpha = 0.25f))
            }

            // Body outline
            drawPath(body, StripDarkGray.copy(alpha = 0.20f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f)))

            // ── SNOUT (pointed nose, characteristic) ────────────────────────
            val snout = paths.obtain().apply {
                moveTo(x - bodyRX * 0.98f, y - bodyRY * 0.10f)
                cubicTo(
                    x - bodyRX * 1.25f, y - bodyRY * 0.05f,
                    x - bodyRX * 1.25f, y + bodyRY * 0.05f,
                    x - bodyRX * 0.98f, y + bodyRY * 0.10f
                )
                close()
            }
            drawPath(snout, BodyGold)
            drawPath(snout, StripBlack.copy(alpha = 0.15f),
                style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))

            // ── EYE ─────────────────────────────────────────────────────────
            val eyeR = (1.4f + level * 0.08f) * s
            val eyeX = x - bodyRX * 0.62f
            val eyeY = y - bodyRY * 0.15f

            drawCircle(ShadowNavy.copy(alpha = 0.35f), eyeR * 1.25f, Offset(eyeX, eyeY))
            drawCircle(Color(0xFFF0EDE0), eyeR, Offset(eyeX, eyeY))
            drawCircle(Color(0xFF2A2A3A), eyeR * 0.55f, Offset(eyeX + eyeR * 0.06f, eyeY))
            drawCircle(ShadowNavy, eyeR * 0.35f, Offset(eyeX + eyeR * 0.08f, eyeY + eyeR * 0.02f))
            drawCircle(Color.White, eyeR * 0.20f, Offset(eyeX - eyeR * 0.18f, eyeY - eyeR * 0.18f))
            drawCircle(Color.White.copy(alpha = 0.5f), eyeR * 0.10f, Offset(eyeX + eyeR * 0.14f, eyeY + eyeR * 0.10f))

            // ── LEVEL 5+: Iridescent shimmer on body ────────────────────────
            if (level >= 5) {
                val shimmerAlpha = (sin(t * 1.8f * PI.toFloat()) * 0.5f + 0.5f) * 0.18f
                val shimmer = paths.obtain().apply {
                    moveTo(x - bodyRX * 0.5f, y - bodyRY * 0.6f)
                    cubicTo(
                        x - bodyRX * 0.2f, y - bodyRY * 0.85f,
                        x + bodyRX * 0.3f, y - bodyRY * 0.80f,
                        x + bodyRX * 0.6f, y - bodyRY * 0.4f
                    )
                    cubicTo(
                        x + bodyRX * 0.3f, y - bodyRY * 0.55f,
                        x - bodyRX * 0.1f, y - bodyRY * 0.58f,
                        x - bodyRX * 0.5f, y - bodyRY * 0.6f
                    )
                    close()
                }
                drawPath(shimmer, Color(0xFF60D0FF).copy(alpha = shimmerAlpha))
                drawPath(shimmer, Color(0xFFB0FFE0).copy(alpha = shimmerAlpha * 0.6f))
                // Body glow outline
                drawPath(body, BodyPaleYellow.copy(alpha = 0.12f),
                    style = Stroke(width = (2.0f * s).coerceAtLeast(0.8f)))
            }

            } // hover transform
        }
    }
}
