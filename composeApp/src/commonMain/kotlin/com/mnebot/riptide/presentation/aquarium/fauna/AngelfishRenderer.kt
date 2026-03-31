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

// ── Palette (from Recraft reference SVG) ─────────────────────────────────────
private val BodyTeal       = Color(0xFF6DBBC2)  // teal-blue body
private val BodyLightTeal  = Color(0xFF7FBFC6)  // lighter teal
private val BodyMedTeal    = Color(0xFF4DA7B0)  // medium teal
private val BodyDarkTeal   = Color(0xFF0694A6)  // dark teal accent
private val StripeCream    = Color(0xFFE8E6DB)  // cream/ivory stripes
private val StripeWarm     = Color(0xFFEAE2CF)  // warm cream
private val OliveKhaki     = Color(0xFFADA970)  // olive body mid-tone
private val OliveGold      = Color(0xFFCDC052)  // olive-gold
private val AccentCoral    = Color(0xFFD4654E)  // coral red
private val AccentOrange   = Color(0xFFF67C52)  // orange-coral
private val AccentSalmon   = Color(0xFFF78C69)  // salmon-peach
private val GoldenYellow   = Color(0xFFFACA66)  // golden yellow
private val LightGold      = Color(0xFFFED58B)  // light gold highlights
private val ShadowBlue     = Color(0xFF024B7E)  // dark blue shadow
private val ShadowDkTeal   = Color(0xFF367881)  // dark teal-green
private val DeepNavy       = Color(0xFF111253)  // near-black

object AngelfishRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen = (8f + level * 0.5f) * s
        val bodyH   = (9f + level * 0.6f) * s
        val tailExt = (4f + level * 0.4f) * s

        val tailSway = sin(t * 3.2f * PI.toFloat()) * 1.6f * s
        val finSway  = sin(t * 2.5f * PI.toFloat()) * 0.7f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FIN (golden, layered) ───────────────────────────────────
            val tailX = x + bodyLen * 0.5f

            val tailShadow = Path().apply {
                moveTo(tailX - bodyLen * 0.02f, y - bodyH * 0.13f)
                cubicTo(
                    tailX + tailExt * 0.52f, y - bodyH * 0.62f + tailSway,
                    tailX + tailExt * 0.92f, y - bodyH * 1.02f + tailSway,
                    tailX + tailExt * 1.02f, y - bodyH * 1.12f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.72f, y + tailSway * 0.3f,
                    tailX + tailExt * 0.42f, y + bodyH * 0.32f + tailSway * 0.3f,
                    tailX + tailExt * 1.02f, y + bodyH * 1.12f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.92f, y + bodyH * 1.02f + tailSway,
                    tailX + tailExt * 0.52f, y + bodyH * 0.62f + tailSway,
                    tailX - bodyLen * 0.02f, y + bodyH * 0.13f
                )
                close()
            }
            drawPath(tailShadow, ShadowBlue.copy(alpha = 0.18f))

            val tail = Path().apply {
                moveTo(tailX, y - bodyH * 0.15f)
                cubicTo(
                    tailX + tailExt * 0.5f, y - bodyH * 0.6f + tailSway,
                    tailX + tailExt * 0.9f, y - bodyH * 1.0f + tailSway,
                    tailX + tailExt, y - bodyH * 1.1f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.7f, y + tailSway * 0.3f,
                    tailX + tailExt * 0.4f, y + bodyH * 0.3f + tailSway * 0.3f,
                    tailX + tailExt, y + bodyH * 1.1f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.9f, y + bodyH * 1.0f + tailSway,
                    tailX + tailExt * 0.5f, y + bodyH * 0.6f + tailSway,
                    tailX, y + bodyH * 0.15f
                )
                close()
            }
            drawPath(tail, GoldenYellow)
            // Tail highlight
            val tailHL = Path().apply {
                moveTo(tailX + tailExt * 0.15f, y - bodyH * 0.10f)
                cubicTo(
                    tailX + tailExt * 0.45f, y - bodyH * 0.45f + tailSway * 0.8f,
                    tailX + tailExt * 0.7f, y - bodyH * 0.75f + tailSway,
                    tailX + tailExt * 0.85f, y - bodyH * 0.88f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.65f, y - bodyH * 0.50f + tailSway * 0.6f,
                    tailX + tailExt * 0.4f, y - bodyH * 0.20f + tailSway * 0.3f,
                    tailX + tailExt * 0.15f, y
                )
                close()
            }
            drawPath(tailHL, LightGold.copy(alpha = 0.5f))
            drawPath(tail, AccentCoral.copy(alpha = 0.25f), style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))

            // ── DORSAL FIN (layered) ─────────────────────────────────────────
            val dorsalShadow = Path().apply {
                moveTo(x - bodyLen * 0.62f, y - bodyH * 0.90f)
                cubicTo(
                    x - bodyLen * 0.22f + finSway, y - bodyH * 1.88f,
                    x + bodyLen * 0.17f + finSway, y - bodyH * 1.83f,
                    x + bodyLen * 0.50f, y - bodyH * 0.90f
                )
                close()
            }
            drawPath(dorsalShadow, ShadowBlue.copy(alpha = 0.18f))

            val dorsal = Path().apply {
                moveTo(x - bodyLen * 0.6f, y - bodyH * 0.92f)
                cubicTo(
                    x - bodyLen * 0.2f + finSway, y - bodyH * 1.85f,
                    x + bodyLen * 0.15f + finSway, y - bodyH * 1.80f,
                    x + bodyLen * 0.48f, y - bodyH * 0.92f
                )
                cubicTo(
                    x + bodyLen * 0.2f, y - bodyH * 0.86f,
                    x - bodyLen * 0.1f, y - bodyH * 0.88f,
                    x - bodyLen * 0.6f, y - bodyH * 0.92f
                )
                close()
            }
            drawPath(dorsal, ShadowDkTeal)
            // Dorsal highlight ridge
            val dorsalHL = Path().apply {
                moveTo(x - bodyLen * 0.45f, y - bodyH * 0.93f)
                cubicTo(
                    x - bodyLen * 0.15f + finSway * 0.7f, y - bodyH * 1.50f,
                    x + bodyLen * 0.10f + finSway * 0.7f, y - bodyH * 1.48f,
                    x + bodyLen * 0.38f, y - bodyH * 0.93f
                )
                close()
            }
            drawPath(dorsalHL, BodyDarkTeal.copy(alpha = 0.6f))
            // Yellow leading edge
            drawLine(
                GoldenYellow.copy(alpha = 0.65f),
                Offset(x - bodyLen * 0.6f, y - bodyH * 0.92f),
                Offset(x - bodyLen * 0.1f + finSway, y - bodyH * 1.82f),
                strokeWidth = (1.0f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round
            )

            // ── ANAL FIN (layered) ───────────────────────────────────────────
            val anal = Path().apply {
                moveTo(x - bodyLen * 0.4f, y + bodyH * 0.92f)
                cubicTo(
                    x - bodyLen * 0.1f - finSway, y + bodyH * 1.75f,
                    x + bodyLen * 0.2f - finSway, y + bodyH * 1.70f,
                    x + bodyLen * 0.5f, y + bodyH * 0.92f
                )
                cubicTo(
                    x + bodyLen * 0.2f, y + bodyH * 0.86f,
                    x, y + bodyH * 0.88f,
                    x - bodyLen * 0.4f, y + bodyH * 0.92f
                )
                close()
            }
            drawPath(anal, ShadowDkTeal)
            drawLine(
                GoldenYellow.copy(alpha = 0.65f),
                Offset(x - bodyLen * 0.4f, y + bodyH * 0.92f),
                Offset(x + bodyLen * 0.1f - finSway, y + bodyH * 1.72f),
                strokeWidth = (1.0f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round
            )

            // ── PECTORAL FIN ─────────────────────────────────────────────────
            val pectSway = sin(t * 4.0f * PI.toFloat()) * 1.5f * s
            val pect = Path().apply {
                moveTo(x - bodyLen * 0.4f, y - bodyH * 0.05f)
                cubicTo(
                    x - bodyLen * 0.15f + pectSway, y + bodyH * 0.6f,
                    x + bodyLen * 0.05f + pectSway, y + bodyH * 0.7f,
                    x + bodyLen * 0.2f, y + bodyH * 0.3f
                )
                cubicTo(x, y + bodyH * 0.15f, x - bodyLen * 0.2f, y, x - bodyLen * 0.4f, y - bodyH * 0.05f)
                close()
            }
            drawPath(pect, BodyLightTeal.copy(alpha = 0.6f))
            drawPath(pect, AccentSalmon.copy(alpha = 0.20f))

            // ── MAIN BODY (multi-layered) ────────────────────────────────────
            val bodyShadow = Path().apply {
                moveTo(x - bodyLen * 1.02f, y)
                cubicTo(
                    x - bodyLen * 0.72f, y - bodyH * 1.02f,
                    x + bodyLen * 0.07f, y - bodyH * 1.0f,
                    x + bodyLen * 0.52f, y - bodyH * 0.27f
                )
                lineTo(x + bodyLen * 0.52f, y + bodyH * 0.27f)
                cubicTo(
                    x + bodyLen * 0.07f, y + bodyH * 1.0f,
                    x - bodyLen * 0.72f, y + bodyH * 1.02f,
                    x - bodyLen * 1.02f, y
                )
                close()
            }
            drawPath(bodyShadow, ShadowBlue.copy(alpha = 0.18f))

            val body = Path().apply {
                moveTo(x - bodyLen, y)
                cubicTo(
                    x - bodyLen * 0.7f, y - bodyH,
                    x + bodyLen * 0.05f, y - bodyH * 0.98f,
                    x + bodyLen * 0.5f, y - bodyH * 0.25f
                )
                lineTo(x + bodyLen * 0.5f, y + bodyH * 0.25f)
                cubicTo(
                    x + bodyLen * 0.05f, y + bodyH * 0.98f,
                    x - bodyLen * 0.7f, y + bodyH,
                    x - bodyLen, y
                )
                close()
            }
            drawPath(body, BodyTeal)

            // Olive mid-tone overlay (body center)
            val oliveZone = Path().apply {
                moveTo(x - bodyLen * 0.75f, y - bodyH * 0.15f)
                cubicTo(
                    x - bodyLen * 0.55f, y - bodyH * 0.75f,
                    x + bodyLen * 0.10f, y - bodyH * 0.72f,
                    x + bodyLen * 0.45f, y - bodyH * 0.20f
                )
                lineTo(x + bodyLen * 0.45f, y + bodyH * 0.20f)
                cubicTo(
                    x + bodyLen * 0.10f, y + bodyH * 0.72f,
                    x - bodyLen * 0.55f, y + bodyH * 0.75f,
                    x - bodyLen * 0.75f, y + bodyH * 0.15f
                )
                close()
            }
            drawPath(oliveZone, OliveKhaki.copy(alpha = 0.30f))

            // Upper highlight
            val highlight = Path().apply {
                moveTo(x - bodyLen * 0.65f, y - bodyH * 0.45f)
                cubicTo(
                    x - bodyLen * 0.45f, y - bodyH * 0.82f,
                    x + bodyLen * 0.10f, y - bodyH * 0.78f,
                    x + bodyLen * 0.38f, y - bodyH * 0.30f
                )
                cubicTo(
                    x + bodyLen * 0.10f, y - bodyH * 0.52f,
                    x - bodyLen * 0.25f, y - bodyH * 0.55f,
                    x - bodyLen * 0.65f, y - bodyH * 0.45f
                )
                close()
            }
            drawPath(highlight, BodyLightTeal.copy(alpha = 0.35f))

            // Belly warmth
            val bellyWarm = Path().apply {
                moveTo(x - bodyLen * 0.60f, y + bodyH * 0.10f)
                cubicTo(
                    x - bodyLen * 0.40f, y + bodyH * 0.85f,
                    x + bodyLen * 0.15f, y + bodyH * 0.82f,
                    x + bodyLen * 0.45f, y + bodyH * 0.18f
                )
                cubicTo(
                    x + bodyLen * 0.15f, y + bodyH * 0.58f,
                    x - bodyLen * 0.20f, y + bodyH * 0.60f,
                    x - bodyLen * 0.60f, y + bodyH * 0.10f
                )
                close()
            }
            drawPath(bellyWarm, AccentSalmon.copy(alpha = 0.18f))

            // ── CREAM STRIPES (curved bands) ─────────────────────────────────
            data class StripeSpec(val cx: Float, val topY: Float, val botY: Float, val hw: Float)
            val stripes = listOf(
                StripeSpec(x - bodyLen * 0.52f, y - bodyH * 0.85f, y + bodyH * 0.82f, bodyH * 0.12f),
                StripeSpec(x - bodyLen * 0.02f, y - bodyH * 0.95f, y + bodyH * 0.92f, bodyH * 0.13f),
            )
            for (stripe in stripes) {
                // Cream base
                val sp = Path().apply {
                    moveTo(stripe.cx - stripe.hw, stripe.topY)
                    cubicTo(
                        stripe.cx - stripe.hw + stripe.hw * 0.3f, stripe.topY + (stripe.botY - stripe.topY) * 0.3f,
                        stripe.cx - stripe.hw - stripe.hw * 0.2f, stripe.topY + (stripe.botY - stripe.topY) * 0.7f,
                        stripe.cx - stripe.hw, stripe.botY
                    )
                    lineTo(stripe.cx + stripe.hw, stripe.botY)
                    cubicTo(
                        stripe.cx + stripe.hw + stripe.hw * 0.2f, stripe.topY + (stripe.botY - stripe.topY) * 0.7f,
                        stripe.cx + stripe.hw - stripe.hw * 0.3f, stripe.topY + (stripe.botY - stripe.topY) * 0.3f,
                        stripe.cx + stripe.hw, stripe.topY
                    )
                    close()
                }
                drawPath(sp, StripeCream.copy(alpha = 0.75f))
                // Warm inner
                val spInner = Path().apply {
                    val inset = stripe.hw * 0.35f
                    moveTo(stripe.cx - stripe.hw + inset, stripe.topY + (stripe.botY - stripe.topY) * 0.08f)
                    lineTo(stripe.cx + stripe.hw - inset, stripe.topY + (stripe.botY - stripe.topY) * 0.08f)
                    lineTo(stripe.cx + stripe.hw - inset, stripe.botY - (stripe.botY - stripe.topY) * 0.08f)
                    lineTo(stripe.cx - stripe.hw + inset, stripe.botY - (stripe.botY - stripe.topY) * 0.08f)
                    close()
                }
                drawPath(spInner, StripeWarm.copy(alpha = 0.40f))
                // Dark edges
                drawLine(DeepNavy.copy(alpha = 0.25f),
                    Offset(stripe.cx - stripe.hw - 0.3f * s, stripe.topY),
                    Offset(stripe.cx - stripe.hw - 0.3f * s, stripe.botY),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.25f))
                drawLine(DeepNavy.copy(alpha = 0.25f),
                    Offset(stripe.cx + stripe.hw + 0.3f * s, stripe.topY),
                    Offset(stripe.cx + stripe.hw + 0.3f * s, stripe.botY),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.25f))
            }

            // ── CORAL ACCENT BAND (horizontal, mid-body) ────────────────────
            val coralBand = Path().apply {
                moveTo(x - bodyLen * 0.80f, y + bodyH * 0.02f)
                cubicTo(
                    x - bodyLen * 0.40f, y - bodyH * 0.20f,
                    x + bodyLen * 0.15f, y - bodyH * 0.18f,
                    x + bodyLen * 0.48f, y - bodyH * 0.05f
                )
                cubicTo(
                    x + bodyLen * 0.15f, y - bodyH * 0.08f,
                    x - bodyLen * 0.40f, y - bodyH * 0.10f,
                    x - bodyLen * 0.80f, y + bodyH * 0.12f
                )
                close()
            }
            drawPath(coralBand, AccentCoral.copy(alpha = 0.35f))

            // Golden body outline
            drawPath(body, GoldenYellow.copy(alpha = 0.30f),
                style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f)))

            // ── EYE (detailed) ───────────────────────────────────────────────
            val eyeR = (1.5f + level * 0.08f) * s
            val eyeX = x - bodyLen * 0.70f
            val eyeY = y - bodyH * 0.28f

            // Eye ring (golden)
            drawCircle(OliveGold.copy(alpha = 0.5f), eyeR * 1.35f, Offset(eyeX, eyeY))
            drawCircle(GoldenYellow, eyeR * 1.15f, Offset(eyeX, eyeY))
            // Iris (olive)
            drawCircle(OliveKhaki, eyeR * 0.70f, Offset(eyeX + eyeR * 0.06f, eyeY))
            // Pupil
            drawCircle(DeepNavy, eyeR * 0.40f, Offset(eyeX + eyeR * 0.08f, eyeY + eyeR * 0.02f))
            // Shine
            drawCircle(Color.White, eyeR * 0.20f, Offset(eyeX - eyeR * 0.20f, eyeY - eyeR * 0.18f))
            drawCircle(Color.White.copy(alpha = 0.5f), eyeR * 0.10f, Offset(eyeX + eyeR * 0.15f, eyeY + eyeR * 0.12f))

            // ── LEVEL 4+: Trailing thread fins ───────────────────────────────
            if (level >= 4) {
                val threadLen = (6f + level * 0.5f) * s
                drawLine(
                    LightGold.copy(alpha = 0.55f),
                    Offset(x + bodyLen * 0.1f + finSway, y - bodyH * 1.78f),
                    Offset(x + bodyLen * 0.3f + finSway + threadLen, y - bodyH * 1.78f - threadLen * 0.5f),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round
                )
                drawLine(
                    LightGold.copy(alpha = 0.55f),
                    Offset(x + bodyLen * 0.1f - finSway, y + bodyH * 1.68f),
                    Offset(x + bodyLen * 0.3f - finSway + threadLen, y + bodyH * 1.68f + threadLen * 0.5f),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round
                )
            }
        }
    }
}
