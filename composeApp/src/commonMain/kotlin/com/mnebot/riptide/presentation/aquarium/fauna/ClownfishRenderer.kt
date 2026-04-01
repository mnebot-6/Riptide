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
private val BodyOrange     = Color(0xFFFC924A)  // main body orange
private val BodyCoral      = Color(0xFFFC5F49)  // coral-salmon accent
private val BodyDeep       = Color(0xFFE24A44)  // red-orange depth
private val BodyDarkRed    = Color(0xFFB93938)  // darkest red shadows
private val HighlightGold  = Color(0xFFFEB75D)  // warm highlight
private val StripeTeal     = Color(0xFF42C8AC)  // stripe base (teal)
private val StripeMint     = Color(0xFF85E2AC)  // stripe mid (mint)
private val StripeLight    = Color(0xFFBCEFC2)  // stripe highlight (pale green)
private val DarkNavy       = Color(0xFF040718)  // near-black accents
private val ShadowBlue     = Color(0xFF093F64)  // dark blue shadows
private val DeepNavy       = Color(0xFF072746)  // deepest shadow
private val AccentTeal     = Color(0xFF14A1A0)  // teal accents
private val EyeNavy        = Color(0xFF097A91)  // eye iris teal

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

        // ── Clownfish waddle: pectoral-propelled + vertical bob + body tilt ─────
        val bobY     = sin(t * 3.8f * PI.toFloat()) * s * 0.08f
        val bodyTilt = sin(t * 3.8f * PI.toFloat() + 0.3f) * 5f  // degrees
        val tailSway = sin(t * 3.8f * PI.toFloat()) * 0.8f * s   // was 1.8s → pectoral-propelled
        val bodyWave = sin(t * 3.8f * PI.toFloat() + 0.5f) * 0.4f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = bobY)
                rotate(degrees = bodyTilt, pivot = Offset(x, y))
            }) {

            // ── TAIL FIN (layered) ───────────────────────────────────────────
            val tailX = x + bodyLen * 0.5f
            val tailH = bodyH * 1.1f

            // Shadow layer
            val tailShadow = Path().apply {
                moveTo(tailX - bodyLen * 0.05f, y - bodyH * 0.20f)
                cubicTo(
                    tailX + tailExt * 0.45f, y - tailH * 0.55f + tailSway * 0.7f,
                    tailX + tailExt * 0.85f, y - tailH * 1.05f + tailSway,
                    tailX + tailExt * 1.05f, y - tailH * 1.15f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.75f, y + tailSway * 0.4f,
                    tailX + tailExt * 0.45f, y + tailH * 0.45f + tailSway * 0.4f,
                    tailX + tailExt * 1.05f, y + tailH * 1.15f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.85f, y + tailH * 1.05f + tailSway,
                    tailX + tailExt * 0.45f, y + tailH * 0.55f + tailSway * 0.7f,
                    tailX - bodyLen * 0.05f, y + bodyH * 0.20f
                )
                close()
            }
            drawPath(tailShadow, ShadowBlue.copy(alpha = 0.45f))

            // Main tail
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
            drawPath(tail, BodyCoral)

            // Tail highlight (upper lobe)
            val tailHighlight = Path().apply {
                moveTo(tailX + tailExt * 0.15f, y - bodyH * 0.20f)
                cubicTo(
                    tailX + tailExt * 0.4f, y - tailH * 0.45f + tailSway * 0.6f,
                    tailX + tailExt * 0.65f, y - tailH * 0.75f + tailSway * 0.8f,
                    tailX + tailExt * 0.85f, y - tailH * 0.85f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.6f, y - tailH * 0.60f + tailSway * 0.7f,
                    tailX + tailExt * 0.35f, y - tailH * 0.30f + tailSway * 0.4f,
                    tailX + tailExt * 0.15f, y - bodyH * 0.10f
                )
                close()
            }
            drawPath(tailHighlight, BodyOrange.copy(alpha = 0.7f))

            // Tail fin edge
            drawPath(tail, DeepNavy.copy(alpha = 0.35f), style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f)))

            // ── DORSAL FIN (layered) ─────────────────────────────────────────
            val dorsalSway = sin(t * 3.0f * PI.toFloat()) * 0.4f * s

            // Dorsal shadow
            val dorsalShadow = Path().apply {
                moveTo(x - bodyLen * 0.52f, y - bodyH * 0.86f)
                cubicTo(
                    x - bodyLen * 0.12f + dorsalSway, y - bodyH * 1.58f,
                    x + bodyLen * 0.22f + dorsalSway, y - bodyH * 1.53f,
                    x + bodyLen * 0.42f, y - bodyH * 0.86f
                )
                cubicTo(
                    x + bodyLen * 0.22f, y - bodyH * 0.80f,
                    x, y - bodyH * 0.82f,
                    x - bodyLen * 0.52f, y - bodyH * 0.86f
                )
                close()
            }
            drawPath(dorsalShadow, ShadowBlue.copy(alpha = 0.5f))

            // Dorsal main
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
            drawPath(dorsal, BodyDeep)

            // Dorsal highlight ridge
            val dorsalRidge = Path().apply {
                moveTo(x - bodyLen * 0.35f, y - bodyH * 0.90f)
                cubicTo(
                    x - bodyLen * 0.05f + dorsalSway * 0.7f, y - bodyH * 1.30f,
                    x + bodyLen * 0.15f + dorsalSway * 0.7f, y - bodyH * 1.28f,
                    x + bodyLen * 0.32f, y - bodyH * 0.90f
                )
                cubicTo(
                    x + bodyLen * 0.12f, y - bodyH * 1.05f,
                    x - bodyLen * 0.1f, y - bodyH * 1.08f,
                    x - bodyLen * 0.35f, y - bodyH * 0.90f
                )
                close()
            }
            drawPath(dorsalRidge, BodyCoral.copy(alpha = 0.7f))

            // ── ANAL FIN (layered) ───────────────────────────────────────────
            val analShadow = Path().apply {
                moveTo(x + bodyLen * 0.03f, y + bodyH * 0.86f)
                cubicTo(
                    x + bodyLen * 0.18f, y + bodyH * 1.35f,
                    x + bodyLen * 0.40f, y + bodyH * 1.32f,
                    x + bodyLen * 0.47f, y + bodyH * 0.86f
                )
                close()
            }
            drawPath(analShadow, ShadowBlue.copy(alpha = 0.4f))

            val anal = Path().apply {
                moveTo(x + bodyLen * 0.05f, y + bodyH * 0.88f)
                cubicTo(
                    x + bodyLen * 0.2f, y + bodyH * 1.3f,
                    x + bodyLen * 0.38f, y + bodyH * 1.28f,
                    x + bodyLen * 0.45f, y + bodyH * 0.88f
                )
                close()
            }
            drawPath(anal, BodyDeep)

            // ── MAIN BODY (multi-layered) ────────────────────────────────────

            // Body shadow (slightly larger, darker)
            val bodyShadow = Path().apply {
                moveTo(x - bodyLen * 1.02f, y + bodyWave * 0.2f)
                cubicTo(
                    x - bodyLen * 0.77f, y - bodyH * 1.02f,
                    x + bodyLen * 0.07f, y - bodyH * 1.0f,
                    x + bodyLen * 0.52f, y - bodyH * 0.32f + bodyWave * 0.1f
                )
                lineTo(x + bodyLen * 0.52f, y + bodyH * 0.32f + bodyWave * 0.1f)
                cubicTo(
                    x + bodyLen * 0.07f, y + bodyH * 1.0f,
                    x - bodyLen * 0.77f, y + bodyH * 1.02f,
                    x - bodyLen * 1.02f, y + bodyWave * 0.2f
                )
                close()
            }
            drawPath(bodyShadow, ShadowBlue.copy(alpha = 0.3f))

            // Main body
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
            drawPath(body, BodyOrange)

            // Belly shadow (lower body, subtle depth)
            val belly = Path().apply {
                moveTo(x - bodyLen * 0.85f, y + bodyH * 0.1f)
                cubicTo(
                    x - bodyLen * 0.6f, y + bodyH * 0.95f,
                    x + bodyLen * 0.1f, y + bodyH * 0.93f,
                    x + bodyLen * 0.48f, y + bodyH * 0.25f
                )
                cubicTo(
                    x + bodyLen * 0.1f, y + bodyH * 0.75f,
                    x - bodyLen * 0.4f, y + bodyH * 0.8f,
                    x - bodyLen * 0.85f, y + bodyH * 0.1f
                )
                close()
            }
            drawPath(belly, BodyDeep.copy(alpha = 0.35f))

            // Upper highlight (warm glow on top)
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
            drawPath(highlight, HighlightGold.copy(alpha = 0.30f))

            // Small specular highlight near head
            val specular = Path().apply {
                moveTo(x - bodyLen * 0.65f, y - bodyH * 0.55f)
                cubicTo(
                    x - bodyLen * 0.55f, y - bodyH * 0.75f,
                    x - bodyLen * 0.35f, y - bodyH * 0.72f,
                    x - bodyLen * 0.28f, y - bodyH * 0.48f
                )
                cubicTo(
                    x - bodyLen * 0.38f, y - bodyH * 0.58f,
                    x - bodyLen * 0.52f, y - bodyH * 0.60f,
                    x - bodyLen * 0.65f, y - bodyH * 0.55f
                )
                close()
            }
            drawPath(specular, HighlightGold.copy(alpha = 0.20f))

            // ── TEAL-GREEN STRIPES (3) ───────────────────────────────────────
            // Based on reference: stripes have teal/mint/pale green gradient
            data class StripeSpec(
                val cx: Float, val topY: Float, val botY: Float,
                val halfW: Float, val curve: Float
            )

            val stripes = listOf(
                StripeSpec(
                    x - bodyLen * 0.55f,
                    y - bodyH * 0.88f, y + bodyH * 0.85f,
                    bodyH * 0.22f, bodyH * 0.08f
                ),
                StripeSpec(
                    x - bodyLen * 0.05f,
                    y - bodyH * 0.96f, y + bodyH * 0.93f,
                    bodyH * 0.24f, bodyH * 0.10f
                ),
                StripeSpec(
                    x + bodyLen * 0.38f,
                    y - bodyH * 0.52f, y + bodyH * 0.50f,
                    bodyH * 0.18f, bodyH * 0.06f
                )
            )

            for (stripe in stripes) {
                // Teal base (widest layer)
                val pathBase = Path().apply {
                    moveTo(stripe.cx - stripe.halfW, stripe.topY)
                    cubicTo(
                        stripe.cx - stripe.halfW + stripe.curve, stripe.topY + (stripe.botY - stripe.topY) * 0.3f,
                        stripe.cx - stripe.halfW - stripe.curve * 0.5f, stripe.topY + (stripe.botY - stripe.topY) * 0.7f,
                        stripe.cx - stripe.halfW, stripe.botY
                    )
                    lineTo(stripe.cx + stripe.halfW, stripe.botY)
                    cubicTo(
                        stripe.cx + stripe.halfW + stripe.curve * 0.5f, stripe.topY + (stripe.botY - stripe.topY) * 0.7f,
                        stripe.cx + stripe.halfW - stripe.curve, stripe.topY + (stripe.botY - stripe.topY) * 0.3f,
                        stripe.cx + stripe.halfW, stripe.topY
                    )
                    close()
                }
                drawPath(pathBase, StripeTeal)

                // Mint middle layer (slightly narrower)
                val inset = stripe.halfW * 0.25f
                val pathMid = Path().apply {
                    moveTo(stripe.cx - stripe.halfW + inset, stripe.topY + (stripe.botY - stripe.topY) * 0.05f)
                    cubicTo(
                        stripe.cx - stripe.halfW + inset + stripe.curve * 0.7f, stripe.topY + (stripe.botY - stripe.topY) * 0.35f,
                        stripe.cx - stripe.halfW + inset - stripe.curve * 0.3f, stripe.topY + (stripe.botY - stripe.topY) * 0.65f,
                        stripe.cx - stripe.halfW + inset, stripe.botY - (stripe.botY - stripe.topY) * 0.05f
                    )
                    lineTo(stripe.cx + stripe.halfW - inset, stripe.botY - (stripe.botY - stripe.topY) * 0.05f)
                    cubicTo(
                        stripe.cx + stripe.halfW - inset + stripe.curve * 0.3f, stripe.topY + (stripe.botY - stripe.topY) * 0.65f,
                        stripe.cx + stripe.halfW - inset - stripe.curve * 0.7f, stripe.topY + (stripe.botY - stripe.topY) * 0.35f,
                        stripe.cx + stripe.halfW - inset, stripe.topY + (stripe.botY - stripe.topY) * 0.05f
                    )
                    close()
                }
                drawPath(pathMid, StripeMint.copy(alpha = 0.8f))

                // Pale highlight center
                val centerInset = stripe.halfW * 0.5f
                val pathCenter = Path().apply {
                    moveTo(stripe.cx - stripe.halfW + centerInset, stripe.topY + (stripe.botY - stripe.topY) * 0.15f)
                    lineTo(stripe.cx + stripe.halfW - centerInset, stripe.topY + (stripe.botY - stripe.topY) * 0.15f)
                    lineTo(stripe.cx + stripe.halfW - centerInset, stripe.botY - (stripe.botY - stripe.topY) * 0.15f)
                    lineTo(stripe.cx - stripe.halfW + centerInset, stripe.botY - (stripe.botY - stripe.topY) * 0.15f)
                    close()
                }
                drawPath(pathCenter, StripeLight.copy(alpha = 0.5f))

                // Dark edge lines
                drawLine(
                    DeepNavy.copy(alpha = 0.4f),
                    Offset(stripe.cx - stripe.halfW - 0.5f * s, stripe.topY),
                    Offset(stripe.cx - stripe.halfW - 0.5f * s, stripe.botY),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f)
                )
                drawLine(
                    DeepNavy.copy(alpha = 0.4f),
                    Offset(stripe.cx + stripe.halfW + 0.5f * s, stripe.topY),
                    Offset(stripe.cx + stripe.halfW + 0.5f * s, stripe.botY),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f)
                )
            }

            // ── BODY OUTLINE (thin dark navy, from SVG reference) ────────────
            drawPath(body, DeepNavy.copy(alpha = 0.22f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.2f)))

            // ── HEAD PATCH (dark, blended) ───────────────────────────────────
            val head = Path().apply {
                moveTo(x - bodyLen, y + bodyWave * 0.2f)
                cubicTo(
                    x - bodyLen * 0.75f, y - bodyH * 0.9f,
                    x - bodyLen * 0.64f, y - bodyH * 0.88f,
                    x - bodyLen * 0.55f, y - bodyH * 0.25f
                )
                lineTo(x - bodyLen * 0.55f, y + bodyH * 0.25f)
                cubicTo(
                    x - bodyLen * 0.64f, y + bodyH * 0.88f,
                    x - bodyLen * 0.75f, y + bodyH * 0.9f,
                    x - bodyLen, y + bodyWave * 0.2f
                )
                close()
            }
            drawPath(head, DarkNavy.copy(alpha = 0.70f))

            // Head gradient overlay (softer transition)
            val headFade = Path().apply {
                moveTo(x - bodyLen * 0.60f, y - bodyH * 0.6f)
                cubicTo(
                    x - bodyLen * 0.58f, y - bodyH * 0.80f,
                    x - bodyLen * 0.50f, y - bodyH * 0.78f,
                    x - bodyLen * 0.48f, y - bodyH * 0.35f
                )
                lineTo(x - bodyLen * 0.48f, y + bodyH * 0.35f)
                cubicTo(
                    x - bodyLen * 0.50f, y + bodyH * 0.78f,
                    x - bodyLen * 0.58f, y + bodyH * 0.80f,
                    x - bodyLen * 0.60f, y + bodyH * 0.6f
                )
                close()
            }
            drawPath(headFade, DarkNavy.copy(alpha = 0.35f))

            // ── GILL PLATE DETAIL ────────────────────────────────────────────
            val gillPlate = Path().apply {
                moveTo(x - bodyLen * 0.54f, y - bodyH * 0.28f)
                cubicTo(
                    x - bodyLen * 0.50f, y - bodyH * 0.55f,
                    x - bodyLen * 0.46f, y - bodyH * 0.58f,
                    x - bodyLen * 0.44f, y - bodyH * 0.28f
                )
                cubicTo(
                    x - bodyLen * 0.46f, y - bodyH * 0.12f,
                    x - bodyLen * 0.50f, y - bodyH * 0.10f,
                    x - bodyLen * 0.54f, y - bodyH * 0.28f
                )
                close()
            }
            drawPath(gillPlate, DarkNavy.copy(alpha = 0.20f))
            // Gill plate edge line
            val gillEdge = Path().apply {
                moveTo(x - bodyLen * 0.54f, y - bodyH * 0.42f)
                cubicTo(
                    x - bodyLen * 0.50f, y - bodyH * 0.65f,
                    x - bodyLen * 0.46f, y + bodyH * 0.55f,
                    x - bodyLen * 0.54f, y + bodyH * 0.38f
                )
            }
            drawPath(gillEdge, DarkNavy.copy(alpha = 0.30f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round))

            // ── PECTORAL FIN (layered) ───────────────────────────────────────
            val pectSway = sin(t * 4.5f * PI.toFloat()) * 2.5f * s   // was 1.2s → primary propulsor

            // Shadow
            val pectShadow = Path().apply {
                moveTo(x - bodyLen * 0.28f, y + bodyH * 0.08f)
                cubicTo(
                    x - bodyLen * 0.08f + pectSway, y + bodyH * 0.75f,
                    x + bodyLen * 0.08f + pectSway, y + bodyH * 0.80f,
                    x + bodyLen * 0.22f, y + bodyH * 0.45f
                )
                cubicTo(
                    x + bodyLen * 0.03f, y + bodyH * 0.30f,
                    x - bodyLen * 0.13f, y + bodyH * 0.20f,
                    x - bodyLen * 0.28f, y + bodyH * 0.08f
                )
                close()
            }
            drawPath(pectShadow, ShadowBlue.copy(alpha = 0.3f))

            // Main pectoral fin
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
            drawPath(pect, BodyCoral.copy(alpha = 0.80f))

            // Fin highlight
            val pectHL = Path().apply {
                moveTo(x - bodyLen * 0.25f, y + bodyH * 0.10f)
                cubicTo(
                    x - bodyLen * 0.12f + pectSway * 0.6f, y + bodyH * 0.45f,
                    x - bodyLen * 0.02f + pectSway * 0.6f, y + bodyH * 0.48f,
                    x + bodyLen * 0.08f, y + bodyH * 0.30f
                )
                cubicTo(
                    x - bodyLen * 0.02f, y + bodyH * 0.22f,
                    x - bodyLen * 0.12f, y + bodyH * 0.15f,
                    x - bodyLen * 0.25f, y + bodyH * 0.10f
                )
                close()
            }
            drawPath(pectHL, HighlightGold.copy(alpha = 0.25f))

            // ── EYE (detailed) ───────────────────────────────────────────────
            val eyeR = (1.5f + level * 0.08f) * s
            val eyeX = x - bodyLen * 0.72f
            val eyeY = y - bodyH * 0.28f

            // Eye socket shadow
            drawCircle(ShadowBlue.copy(alpha = 0.4f), eyeR * 1.3f, Offset(eyeX, eyeY))
            // Sclera (white)
            drawCircle(Color(0xFFF0F0F0), eyeR, Offset(eyeX, eyeY))
            // Iris (teal, from reference)
            drawCircle(EyeNavy, eyeR * 0.65f, Offset(eyeX + eyeR * 0.06f, eyeY + eyeR * 0.02f))
            // Pupil (dark center)
            drawCircle(DarkNavy, eyeR * 0.38f, Offset(eyeX + eyeR * 0.08f, eyeY + eyeR * 0.04f))
            // Primary shine
            drawCircle(Color.White, eyeR * 0.22f, Offset(eyeX - eyeR * 0.22f, eyeY - eyeR * 0.22f))
            // Secondary shine (smaller, offset)
            drawCircle(Color.White.copy(alpha = 0.6f), eyeR * 0.12f, Offset(eyeX + eyeR * 0.18f, eyeY + eyeR * 0.15f))

            // ── MOUTH LINE ───────────────────────────────────────────────────
            val mouthPath = Path().apply {
                moveTo(x - bodyLen * 0.95f, y + bodyH * 0.05f)
                cubicTo(
                    x - bodyLen * 0.90f, y + bodyH * 0.15f,
                    x - bodyLen * 0.85f, y + bodyH * 0.18f,
                    x - bodyLen * 0.78f, y + bodyH * 0.12f
                )
            }
            drawPath(
                mouthPath, DarkNavy.copy(alpha = 0.5f),
                style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
            )

            // ── LEVEL 3+: Lateral line + subtle gill detail ──────────────────
            if (level >= 3) {
                // Lateral line (teal accent)
                drawLine(
                    AccentTeal.copy(alpha = 0.45f),
                    Offset(x - bodyLen * 0.45f, y - bodyH * 0.08f),
                    Offset(x + bodyLen * 0.42f, y - bodyH * 0.10f),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.4f),
                    cap = StrokeCap.Round
                )
                // Gill slit
                val gillPath = Path().apply {
                    moveTo(x - bodyLen * 0.50f, y - bodyH * 0.35f)
                    cubicTo(
                        x - bodyLen * 0.48f, y - bodyH * 0.10f,
                        x - bodyLen * 0.47f, y + bodyH * 0.10f,
                        x - bodyLen * 0.50f, y + bodyH * 0.30f
                    )
                }
                drawPath(
                    gillPath, BodyDarkRed.copy(alpha = 0.4f),
                    style = Stroke(width = (0.5f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
                )
            }

            // ── LEVEL 5+: Bioluminescent edge glow ──────────────────────────
            if (level >= 5) {
                drawPath(body, AccentTeal.copy(alpha = 0.15f),
                    style = Stroke(width = (2.0f * s).coerceAtLeast(0.8f)))
                // Small light dot near eye
                drawCircle(
                    StripeLight.copy(alpha = 0.4f),
                    eyeR * 0.5f,
                    Offset(eyeX + eyeR * 1.8f, eyeY - eyeR * 0.3f)
                )
            }

            } // waddle + bob transform
        }
    }
}
