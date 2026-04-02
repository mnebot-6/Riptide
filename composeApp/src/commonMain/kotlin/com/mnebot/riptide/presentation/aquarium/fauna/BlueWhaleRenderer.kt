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

// ── Palette (from Recraft reference SVG) ─────────────────────────────────────
private val BodyLightBlue  = Color(0xFF75B4BC)  // main body light blue-gray
private val BodyPaleTeal   = Color(0xFFAAD5DB)  // pale teal highlight
private val BodyBrightBlue = Color(0xFF4190D2)  // bright blue accent
private val BodyMedBlue    = Color(0xFF0876C2)  // medium bright blue
private val BellyWhite     = Color(0xFFE7E8E9)  // near-white belly
private val BellyPaleGray  = Color(0xFFE3ECED)  // pale gray-blue belly
private val BellyPaleBlue  = Color(0xFF8DBDE1)  // pale blue accent
private val ShadowDarkBlue = Color(0xFF06457B)  // dark blue shadow
private val ShadowMedBlue  = Color(0xFF065FA2)  // medium blue depth
private val ShadowDeepNavy = Color(0xFF052343)  // very dark navy
private val AccentCoral    = Color(0xFFF8825D)  // coral/salmon accent
private val AccentDustyRose = Color(0xFFCD7D73)  // dusty rose

object BlueWhaleRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen  = (22f + level * 1.2f) * s
        val bodyH    = (8f  + level * 0.5f) * s
        val tailExt  = (9f  + level * 0.6f) * s

        // ── Full-body S-wave: phase propagates head → tail ────────────────────
        val freq     = 0.9f * PI.toFloat()
        // Front (head): barely moves
        val frontArc = sin(t * freq)         * bodyH * 0.01f
        // Mid-body: moderate flex
        val midArc   = sin(t * freq + 0.5f)  * bodyH * 0.04f
        // Rear (caudal peduncle): full amplitude
        val rearArc  = sin(t * freq + 1.0f)  * bodyH * 0.08f

        val tailSway     = sin(t * freq + 1.0f)  * 2.0f * s
        val flipperSweep = sin(t * freq + 1.2f)  * 0.3f * s  // pectoral flipper
        val flukeAsym    = sin(t * freq + 1.0f)  * 0.12f     // lobe asymmetry hint

        // Upper fluke slightly taller on upstroke, lower on downstroke
        val upperFlukeH = bodyH * 1.3f * (1f + flukeAsym * 0.08f)
        val lowerFlukeH = bodyH * 1.3f * (1f - flukeAsym * 0.08f)

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FLUKES (massive, asymmetric for 3D hint) ────────────────
            val tailX = x + bodyLen * 0.47f
            val flukeSpan = tailExt * 0.85f

            val flukeShadow = paths.obtain().apply {
                moveTo(tailX - bodyLen * 0.01f, y + rearArc * 0.3f)
                lineTo(tailX + flukeSpan * 1.02f, y - upperFlukeH + tailSway)
                lineTo(tailX + flukeSpan * 0.5f, y + tailSway * 0.3f)
                lineTo(tailX + flukeSpan * 1.02f, y + lowerFlukeH + tailSway)
                close()
            }
            drawPath(flukeShadow, ShadowDeepNavy.copy(alpha = 0.15f))

            val upperFluke = paths.obtain().apply {
                moveTo(tailX, y + rearArc * 0.3f)
                cubicTo(tailX + flukeSpan * 0.4f, y - bodyH * 0.3f + tailSway,
                    tailX + flukeSpan * 0.8f, y - bodyH * 1.1f + tailSway,
                    tailX + flukeSpan, y - upperFlukeH + tailSway)
                cubicTo(tailX + flukeSpan * 0.8f, y - bodyH * 0.8f + tailSway,
                    tailX + flukeSpan * 0.45f, y - bodyH * 0.2f + tailSway * 0.6f,
                    tailX, y + rearArc * 0.3f)
                close()
            }
            drawPath(upperFluke, ShadowDarkBlue)

            val upperFlukeHL = paths.obtain().apply {
                moveTo(tailX + flukeSpan * 0.15f, y - bodyH * 0.10f + tailSway * 0.3f)
                cubicTo(tailX + flukeSpan * 0.4f, y - bodyH * 0.5f + tailSway * 0.6f,
                    tailX + flukeSpan * 0.65f, y - bodyH * 0.85f + tailSway,
                    tailX + flukeSpan * 0.8f, y - bodyH * 1.0f + tailSway)
                cubicTo(tailX + flukeSpan * 0.6f, y - bodyH * 0.65f + tailSway * 0.8f,
                    tailX + flukeSpan * 0.35f, y - bodyH * 0.20f + tailSway * 0.4f,
                    tailX + flukeSpan * 0.15f, y - bodyH * 0.02f + tailSway * 0.2f)
                close()
            }
            drawPath(upperFlukeHL, ShadowMedBlue.copy(alpha = 0.5f))

            val lowerFluke = paths.obtain().apply {
                moveTo(tailX, y + rearArc * 0.3f)
                cubicTo(tailX + flukeSpan * 0.45f, y + bodyH * 0.2f + tailSway * 0.6f,
                    tailX + flukeSpan * 0.8f, y + bodyH * 0.8f + tailSway,
                    tailX + flukeSpan, y + lowerFlukeH + tailSway)
                cubicTo(tailX + flukeSpan * 0.8f, y + bodyH * 1.1f + tailSway,
                    tailX + flukeSpan * 0.4f, y + bodyH * 0.3f + tailSway,
                    tailX, y + rearArc * 0.3f)
                close()
            }
            drawPath(lowerFluke, ShadowDarkBlue)

            // ── TINY DORSAL FIN ──────────────────────────────────────────────
            val dorsalX = x + bodyLen * 0.30f
            val dorsalShadow = paths.obtain().apply {
                moveTo(dorsalX - bodyLen * 0.045f, y - bodyH * 0.83f + midArc * 0.1f)
                cubicTo(dorsalX - bodyLen * 0.015f, y - bodyH * 1.33f + midArc * 0.1f,
                    dorsalX + bodyLen * 0.035f, y - bodyH * 1.28f + midArc * 0.1f,
                    dorsalX + bodyLen * 0.055f, y - bodyH * 0.83f + midArc * 0.1f)
                close()
            }
            drawPath(dorsalShadow, ShadowDeepNavy.copy(alpha = 0.18f))

            val dorsal = paths.obtain().apply {
                moveTo(dorsalX - bodyLen * 0.04f, y - bodyH * 0.85f + midArc * 0.1f)
                cubicTo(dorsalX - bodyLen * 0.01f, y - bodyH * 1.30f + midArc * 0.1f,
                    dorsalX + bodyLen * 0.03f, y - bodyH * 1.25f + midArc * 0.1f,
                    dorsalX + bodyLen * 0.05f, y - bodyH * 0.85f + midArc * 0.1f)
                close()
            }
            drawPath(dorsal, ShadowDarkBlue)

            // ── PECTORAL FLIPPERS (sweep with flipperSweep) ──────────────────
            val pectShadow = paths.obtain().apply {
                moveTo(x - bodyLen * 0.14f, y + bodyH * 0.74f + flipperSweep * 0.5f)
                cubicTo(x - bodyLen * 0.04f, y + bodyH * 1.58f + flipperSweep * 0.35f,
                    x + bodyLen * 0.13f, y + bodyH * 1.65f + flipperSweep * 0.25f,
                    x + bodyLen * 0.24f, y + bodyH * 0.98f + flipperSweep * 0.35f)
                cubicTo(x + bodyLen * 0.13f, y + bodyH * 0.85f + flipperSweep * 0.4f,
                    x + bodyLen * 0.03f, y + bodyH * 0.78f + flipperSweep * 0.45f,
                    x - bodyLen * 0.14f, y + bodyH * 0.74f + flipperSweep * 0.5f)
                close()
            }
            drawPath(pectShadow, ShadowDeepNavy.copy(alpha = 0.18f))

            val pect = paths.obtain().apply {
                moveTo(x - bodyLen * 0.15f, y + bodyH * 0.72f + flipperSweep * 0.5f)
                cubicTo(x - bodyLen * 0.05f, y + bodyH * 1.55f + flipperSweep * 0.35f,
                    x + bodyLen * 0.12f, y + bodyH * 1.62f + flipperSweep * 0.25f,
                    x + bodyLen * 0.22f, y + bodyH * 0.95f + flipperSweep * 0.35f)
                cubicTo(x + bodyLen * 0.12f, y + bodyH * 0.82f + flipperSweep * 0.4f,
                    x + bodyLen * 0.02f, y + bodyH * 0.76f + flipperSweep * 0.45f,
                    x - bodyLen * 0.15f, y + bodyH * 0.72f + flipperSweep * 0.5f)
                close()
            }
            drawPath(pect, BodyLightBlue)

            val pectHL = paths.obtain().apply {
                moveTo(x - bodyLen * 0.12f, y + bodyH * 0.75f + flipperSweep * 0.5f)
                cubicTo(x - bodyLen * 0.03f, y + bodyH * 1.25f + flipperSweep * 0.35f,
                    x + bodyLen * 0.06f, y + bodyH * 1.30f + flipperSweep * 0.30f,
                    x + bodyLen * 0.12f, y + bodyH * 0.90f + flipperSweep * 0.35f)
                cubicTo(x + bodyLen * 0.05f, y + bodyH * 0.88f + flipperSweep * 0.4f,
                    x - bodyLen * 0.02f, y + bodyH * 0.80f + flipperSweep * 0.45f,
                    x - bodyLen * 0.12f, y + bodyH * 0.75f + flipperSweep * 0.5f)
                close()
            }
            drawPath(pectHL, BellyPaleBlue.copy(alpha = 0.35f))

            // ── MAIN BODY (S-wave: frontArc / midArc / rearArc by segment) ───
            val bodyShadow = paths.obtain().apply {
                moveTo(x - bodyLen * 0.52f, y + frontArc)
                cubicTo(x - bodyLen * 0.47f, y - bodyH * 0.80f + frontArc * 0.85f,
                    x - bodyLen * 0.07f, y - bodyH * 1.02f + midArc * 0.5f,
                    x + bodyLen * 0.22f, y - bodyH * 0.94f + midArc * 0.2f)
                cubicTo(x + bodyLen * 0.37f, y - bodyH * 0.84f + rearArc * 0.15f,
                    x + bodyLen * 0.47f, y - bodyH * 0.44f + rearArc * 0.1f,
                    x + bodyLen * 0.49f, y - bodyH * 0.14f + rearArc * 0.05f)
                lineTo(x + bodyLen * 0.49f, y + bodyH * 0.14f + rearArc * 0.05f)
                cubicTo(x + bodyLen * 0.47f, y + bodyH * 0.44f + rearArc * 0.1f,
                    x + bodyLen * 0.37f, y + bodyH * 0.84f + rearArc * 0.15f,
                    x + bodyLen * 0.22f, y + bodyH * 0.94f + midArc * 0.2f)
                cubicTo(x - bodyLen * 0.07f, y + bodyH * 1.02f + midArc * 0.5f,
                    x - bodyLen * 0.47f, y + bodyH * 0.80f + frontArc * 0.85f,
                    x - bodyLen * 0.52f, y + frontArc)
                close()
            }
            drawPath(bodyShadow, ShadowDarkBlue.copy(alpha = 0.20f))

            val body = paths.obtain().apply {
                moveTo(x - bodyLen * 0.5f, y + frontArc)
                cubicTo(x - bodyLen * 0.45f, y - bodyH * 0.78f + frontArc * 0.85f,
                    x - bodyLen * 0.05f, y - bodyH + midArc * 0.5f,
                    x + bodyLen * 0.2f, y - bodyH * 0.92f + midArc * 0.2f)
                cubicTo(x + bodyLen * 0.35f, y - bodyH * 0.82f + rearArc * 0.15f,
                    x + bodyLen * 0.45f, y - bodyH * 0.42f + rearArc * 0.1f,
                    x + bodyLen * 0.47f, y - bodyH * 0.12f + rearArc * 0.05f)
                lineTo(x + bodyLen * 0.47f, y + bodyH * 0.12f + rearArc * 0.05f)
                cubicTo(x + bodyLen * 0.45f, y + bodyH * 0.42f + rearArc * 0.1f,
                    x + bodyLen * 0.35f, y + bodyH * 0.82f + rearArc * 0.15f,
                    x + bodyLen * 0.2f, y + bodyH * 0.92f + midArc * 0.2f)
                cubicTo(x - bodyLen * 0.05f, y + bodyH + midArc * 0.5f,
                    x - bodyLen * 0.45f, y + bodyH * 0.78f + frontArc * 0.85f,
                    x - bodyLen * 0.5f, y + frontArc)
                close()
            }
            drawPath(body, BodyLightBlue)

            // Body outline definition (subtle, from SVG style)
            drawPath(body, ShadowDarkBlue.copy(alpha = 0.10f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.2f)))

            // Mid-body shadow band (depth layering)
            val midShadow = paths.obtain().apply {
                moveTo(x - bodyLen * 0.10f, y - bodyH * 0.30f + midArc * 0.4f)
                cubicTo(x + bodyLen * 0.05f, y - bodyH * 0.82f + midArc * 0.3f,
                    x + bodyLen * 0.30f, y - bodyH * 0.78f + rearArc * 0.2f,
                    x + bodyLen * 0.45f, y - bodyH * 0.38f + rearArc * 0.1f)
                cubicTo(x + bodyLen * 0.30f, y - bodyH * 0.55f + rearArc * 0.15f,
                    x + bodyLen * 0.05f, y - bodyH * 0.60f + midArc * 0.3f,
                    x - bodyLen * 0.10f, y - bodyH * 0.30f + midArc * 0.4f)
                close()
            }
            drawPath(midShadow, ShadowMedBlue.copy(alpha = 0.12f))

            // Upper body bright blue accent
            val upperAccent = paths.obtain().apply {
                moveTo(x - bodyLen * 0.42f, y - bodyH * 0.30f + frontArc * 0.8f)
                cubicTo(x - bodyLen * 0.20f, y - bodyH * 0.90f + midArc * 0.5f,
                    x + bodyLen * 0.15f, y - bodyH * 0.88f + midArc * 0.3f,
                    x + bodyLen * 0.38f, y - bodyH * 0.50f + rearArc * 0.15f)
                cubicTo(x + bodyLen * 0.15f, y - bodyH * 0.62f + midArc * 0.3f,
                    x - bodyLen * 0.10f, y - bodyH * 0.65f + midArc * 0.5f,
                    x - bodyLen * 0.42f, y - bodyH * 0.30f + frontArc * 0.8f)
                close()
            }
            drawPath(upperAccent, BodyBrightBlue.copy(alpha = 0.25f))

            // Pale teal highlight near head
            val headHL = paths.obtain().apply {
                moveTo(x - bodyLen * 0.45f, y - bodyH * 0.20f + frontArc * 0.85f)
                cubicTo(x - bodyLen * 0.38f, y - bodyH * 0.60f + frontArc * 0.75f,
                    x - bodyLen * 0.20f, y - bodyH * 0.65f + midArc * 0.6f,
                    x - bodyLen * 0.10f, y - bodyH * 0.40f + midArc * 0.5f)
                cubicTo(x - bodyLen * 0.18f, y - bodyH * 0.45f + midArc * 0.6f,
                    x - bodyLen * 0.32f, y - bodyH * 0.42f + frontArc * 0.75f,
                    x - bodyLen * 0.45f, y - bodyH * 0.20f + frontArc * 0.85f)
                close()
            }
            drawPath(headHL, BodyPaleTeal.copy(alpha = 0.30f))

            // ── ROSTRUM DEFINITION (U-shaped head, darker outline) ───────────
            val rostrum = paths.obtain().apply {
                moveTo(x - bodyLen * 0.50f, y + frontArc - bodyH * 0.05f)
                cubicTo(x - bodyLen * 0.49f, y + frontArc + bodyH * 0.45f,
                    x - bodyLen * 0.44f, y + frontArc + bodyH * 0.52f,
                    x - bodyLen * 0.38f, y + frontArc + bodyH * 0.35f)
                cubicTo(x - bodyLen * 0.42f, y + frontArc + bodyH * 0.20f,
                    x - bodyLen * 0.47f, y + frontArc + bodyH * 0.10f,
                    x - bodyLen * 0.50f, y + frontArc - bodyH * 0.05f)
                close()
            }
            drawPath(rostrum, ShadowDarkBlue.copy(alpha = 0.12f))

            // ── BELLY (pale, layered) ────────────────────────────────────────
            val belly = paths.obtain().apply {
                moveTo(x - bodyLen * 0.48f, y + frontArc + bodyH * 0.05f)
                cubicTo(x - bodyLen * 0.35f, y + bodyH * 0.88f + midArc * 0.7f,
                    x + bodyLen * 0.2f, y + bodyH * 0.90f + midArc * 0.2f,
                    x + bodyLen * 0.44f, y + bodyH * 0.10f + rearArc * 0.05f)
                cubicTo(x + bodyLen * 0.2f, y + bodyH * 0.60f + midArc * 0.2f,
                    x - bodyLen * 0.10f, y + bodyH * 0.65f + midArc * 0.5f,
                    x - bodyLen * 0.48f, y + frontArc + bodyH * 0.05f)
                close()
            }
            drawPath(belly, BellyWhite.copy(alpha = 0.50f))

            val bellyInner = paths.obtain().apply {
                moveTo(x - bodyLen * 0.40f, y + frontArc + bodyH * 0.10f)
                cubicTo(x - bodyLen * 0.25f, y + bodyH * 0.72f + midArc * 0.6f,
                    x + bodyLen * 0.15f, y + bodyH * 0.70f + midArc * 0.2f,
                    x + bodyLen * 0.38f, y + bodyH * 0.12f + rearArc * 0.05f)
                cubicTo(x + bodyLen * 0.15f, y + bodyH * 0.48f + midArc * 0.2f,
                    x - bodyLen * 0.08f, y + bodyH * 0.50f + midArc * 0.4f,
                    x - bodyLen * 0.40f, y + frontArc + bodyH * 0.10f)
                close()
            }
            drawPath(bellyInner, BellyPaleGray.copy(alpha = 0.35f))

            // ── VENTRAL PLEATS ───────────────────────────────────────────────
            val pleatCount = 6 + level / 2
            for (p in 0 until pleatCount) {
                val pf = (p.toFloat() + 0.5f) / pleatCount.toFloat()
                val px = x - bodyLen * 0.42f + bodyLen * 0.5f * pf
                // Shadow side
                drawLine(
                    ShadowMedBlue.copy(alpha = 0.28f),
                    Offset(px, y + bodyH * 0.12f + midArc * (1f - pf * 0.5f)),
                    Offset(px + bodyLen * 0.08f, y + bodyH * 0.85f + midArc * (1f - pf * 0.3f)),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.3f)
                )
                // Highlight side (pale blue, offset by 0.5s)
                drawLine(
                    BellyPaleBlue.copy(alpha = 0.18f),
                    Offset(px + 0.5f * s, y + bodyH * 0.12f + midArc * (1f - pf * 0.5f)),
                    Offset(px + bodyLen * 0.08f + 0.5f * s, y + bodyH * 0.85f + midArc * (1f - pf * 0.3f)),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.2f)
                )
            }

            // ── CORAL ACCENT (mouth/jaw area) ────────────────────────────────
            val mouthAccent = paths.obtain().apply {
                moveTo(x - bodyLen * 0.50f, y + frontArc - bodyH * 0.05f)
                cubicTo(x - bodyLen * 0.48f, y + frontArc + bodyH * 0.25f,
                    x - bodyLen * 0.42f, y + frontArc + bodyH * 0.30f,
                    x - bodyLen * 0.35f, y + frontArc + bodyH * 0.20f)
                cubicTo(x - bodyLen * 0.40f, y + frontArc + bodyH * 0.15f,
                    x - bodyLen * 0.46f, y + frontArc + bodyH * 0.08f,
                    x - bodyLen * 0.50f, y + frontArc - bodyH * 0.05f)
                close()
            }
            drawPath(mouthAccent, AccentCoral.copy(alpha = 0.35f))

            // ── BLOW HOLE ────────────────────────────────────────────────────
            val blowX = x - bodyLen * 0.38f
            val blowY = y - bodyH * 0.82f + frontArc * 0.85f
            drawLine(ShadowDarkBlue.copy(alpha = 0.55f),
                Offset(blowX - bodyLen * 0.02f, blowY),
                Offset(blowX + bodyLen * 0.01f, blowY),
                strokeWidth = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round)

            // ── PERIODIC BLOWHOLE SPRAY (level 3+, every 8s) ─────────────────
            if (level >= 3) {
                val sprayPeriod = 8000L
                val sprayPhase = (animTimeMs % sprayPeriod).toFloat() / 1000f
                if (sprayPhase < 1.2f) {
                    val sprayProgress = sprayPhase / 1.2f
                    val sprayAlpha = (1f - sprayProgress).coerceIn(0f, 1f)
                    for (p in 0 until 6) {
                        val drift = sin(p.toFloat() * 1.1f + p * 0.18f) * bodyLen * 0.025f
                        val riseSpeed = (0.8f + p * 0.12f) * sprayProgress
                        val pX = blowX + drift
                        val pY = blowY - riseSpeed * bodyH * 1.8f
                        val pR = (1.0f - sprayProgress * 0.6f) * s * 1.2f
                        drawCircle(
                            BellyPaleBlue.copy(alpha = sprayAlpha * 0.65f),
                            pR.coerceAtLeast(0.3f),
                            Offset(pX, pY)
                        )
                    }
                }
            }

            // ── EYE (detailed) ───────────────────────────────────────────────
            val eyeR = (1.2f + level * 0.07f) * s
            val eyeX = x - bodyLen * 0.38f
            val eyeY = y - bodyH * 0.55f + frontArc * 0.85f

            drawCircle(ShadowDeepNavy.copy(alpha = 0.3f), eyeR * 1.3f, Offset(eyeX, eyeY))
            drawCircle(BellyWhite, eyeR * 1.05f, Offset(eyeX, eyeY))
            drawCircle(BodyMedBlue, eyeR * 0.65f, Offset(eyeX + eyeR * 0.05f, eyeY))
            drawCircle(ShadowDeepNavy, eyeR * 0.35f, Offset(eyeX + eyeR * 0.07f, eyeY + eyeR * 0.02f))
            drawCircle(Color.White, eyeR * 0.18f, Offset(eyeX - eyeR * 0.15f, eyeY - eyeR * 0.15f))
        }
    }
}
