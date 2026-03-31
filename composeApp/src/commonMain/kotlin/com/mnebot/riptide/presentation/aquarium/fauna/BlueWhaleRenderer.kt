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

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen  = (22f + level * 1.2f) * s
        val bodyH    = (8f  + level * 0.5f) * s
        val tailExt  = (9f  + level * 0.6f) * s

        val tailSway = sin(t * 0.9f * PI.toFloat()) * 2.0f * s
        val bodyArc  = sin(t * 0.9f * PI.toFloat()) * bodyH * 0.08f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FLUKES (massive, layered) ───────────────────────────────
            val tailX = x + bodyLen * 0.47f
            val flukeSpan = tailExt * 0.85f

            // Fluke shadows
            val flukeShadow = Path().apply {
                moveTo(tailX - bodyLen * 0.01f, y + bodyArc * 0.3f)
                lineTo(tailX + flukeSpan * 1.02f, y - bodyH * 1.35f + tailSway)
                lineTo(tailX + flukeSpan * 0.5f, y + tailSway * 0.3f)
                lineTo(tailX + flukeSpan * 1.02f, y + bodyH * 1.35f + tailSway)
                close()
            }
            drawPath(flukeShadow, ShadowDeepNavy.copy(alpha = 0.15f))

            val upperFluke = Path().apply {
                moveTo(tailX, y + bodyArc * 0.3f)
                cubicTo(tailX + flukeSpan * 0.4f, y - bodyH * 0.3f + tailSway,
                    tailX + flukeSpan * 0.8f, y - bodyH * 1.1f + tailSway,
                    tailX + flukeSpan, y - bodyH * 1.3f + tailSway)
                cubicTo(tailX + flukeSpan * 0.8f, y - bodyH * 0.8f + tailSway,
                    tailX + flukeSpan * 0.45f, y - bodyH * 0.2f + tailSway * 0.6f,
                    tailX, y + bodyArc * 0.3f)
                close()
            }
            drawPath(upperFluke, ShadowDarkBlue)
            // Upper fluke highlight
            val upperFlukeHL = Path().apply {
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

            val lowerFluke = Path().apply {
                moveTo(tailX, y + bodyArc * 0.3f)
                cubicTo(tailX + flukeSpan * 0.45f, y + bodyH * 0.2f + tailSway * 0.6f,
                    tailX + flukeSpan * 0.8f, y + bodyH * 0.8f + tailSway,
                    tailX + flukeSpan, y + bodyH * 1.3f + tailSway)
                cubicTo(tailX + flukeSpan * 0.8f, y + bodyH * 1.1f + tailSway,
                    tailX + flukeSpan * 0.4f, y + bodyH * 0.3f + tailSway,
                    tailX, y + bodyArc * 0.3f)
                close()
            }
            drawPath(lowerFluke, ShadowDarkBlue)

            // ── TINY DORSAL FIN ──────────────────────────────────────────────
            val dorsalX = x + bodyLen * 0.30f
            val dorsalShadow = Path().apply {
                moveTo(dorsalX - bodyLen * 0.045f, y - bodyH * 0.83f + bodyArc * 0.1f)
                cubicTo(dorsalX - bodyLen * 0.015f, y - bodyH * 1.33f + bodyArc * 0.1f,
                    dorsalX + bodyLen * 0.035f, y - bodyH * 1.28f + bodyArc * 0.1f,
                    dorsalX + bodyLen * 0.055f, y - bodyH * 0.83f + bodyArc * 0.1f)
                close()
            }
            drawPath(dorsalShadow, ShadowDeepNavy.copy(alpha = 0.18f))

            val dorsal = Path().apply {
                moveTo(dorsalX - bodyLen * 0.04f, y - bodyH * 0.85f + bodyArc * 0.1f)
                cubicTo(dorsalX - bodyLen * 0.01f, y - bodyH * 1.30f + bodyArc * 0.1f,
                    dorsalX + bodyLen * 0.03f, y - bodyH * 1.25f + bodyArc * 0.1f,
                    dorsalX + bodyLen * 0.05f, y - bodyH * 0.85f + bodyArc * 0.1f)
                close()
            }
            drawPath(dorsal, ShadowDarkBlue)

            // ── PECTORAL FLIPPERS ────────────────────────────────────────────
            val pectShadow = Path().apply {
                moveTo(x - bodyLen * 0.14f, y + bodyH * 0.74f + bodyArc * 0.5f)
                cubicTo(x - bodyLen * 0.04f, y + bodyH * 1.58f + bodyArc * 0.35f,
                    x + bodyLen * 0.13f, y + bodyH * 1.65f + bodyArc * 0.25f,
                    x + bodyLen * 0.24f, y + bodyH * 0.98f + bodyArc * 0.35f)
                cubicTo(x + bodyLen * 0.13f, y + bodyH * 0.85f + bodyArc * 0.4f,
                    x + bodyLen * 0.03f, y + bodyH * 0.78f + bodyArc * 0.45f,
                    x - bodyLen * 0.14f, y + bodyH * 0.74f + bodyArc * 0.5f)
                close()
            }
            drawPath(pectShadow, ShadowDeepNavy.copy(alpha = 0.18f))

            val pect = Path().apply {
                moveTo(x - bodyLen * 0.15f, y + bodyH * 0.72f + bodyArc * 0.5f)
                cubicTo(x - bodyLen * 0.05f, y + bodyH * 1.55f + bodyArc * 0.35f,
                    x + bodyLen * 0.12f, y + bodyH * 1.62f + bodyArc * 0.25f,
                    x + bodyLen * 0.22f, y + bodyH * 0.95f + bodyArc * 0.35f)
                cubicTo(x + bodyLen * 0.12f, y + bodyH * 0.82f + bodyArc * 0.4f,
                    x + bodyLen * 0.02f, y + bodyH * 0.76f + bodyArc * 0.45f,
                    x - bodyLen * 0.15f, y + bodyH * 0.72f + bodyArc * 0.5f)
                close()
            }
            drawPath(pect, BodyLightBlue)
            // Flipper highlight
            val pectHL = Path().apply {
                moveTo(x - bodyLen * 0.12f, y + bodyH * 0.75f + bodyArc * 0.5f)
                cubicTo(x - bodyLen * 0.03f, y + bodyH * 1.25f + bodyArc * 0.35f,
                    x + bodyLen * 0.06f, y + bodyH * 1.30f + bodyArc * 0.30f,
                    x + bodyLen * 0.12f, y + bodyH * 0.90f + bodyArc * 0.35f)
                cubicTo(x + bodyLen * 0.05f, y + bodyH * 0.88f + bodyArc * 0.4f,
                    x - bodyLen * 0.02f, y + bodyH * 0.80f + bodyArc * 0.45f,
                    x - bodyLen * 0.12f, y + bodyH * 0.75f + bodyArc * 0.5f)
                close()
            }
            drawPath(pectHL, BellyPaleBlue.copy(alpha = 0.35f))

            // ── MAIN BODY (enormous, layered) ────────────────────────────────
            val bodyShadow = Path().apply {
                moveTo(x - bodyLen * 0.52f, y + bodyArc)
                cubicTo(x - bodyLen * 0.47f, y - bodyH * 0.80f + bodyArc * 0.85f,
                    x - bodyLen * 0.07f, y - bodyH * 1.02f + bodyArc * 0.5f,
                    x + bodyLen * 0.22f, y - bodyH * 0.94f + bodyArc * 0.2f)
                cubicTo(x + bodyLen * 0.37f, y - bodyH * 0.84f + bodyArc * 0.15f,
                    x + bodyLen * 0.47f, y - bodyH * 0.44f + bodyArc * 0.1f,
                    x + bodyLen * 0.49f, y - bodyH * 0.14f + bodyArc * 0.05f)
                lineTo(x + bodyLen * 0.49f, y + bodyH * 0.14f + bodyArc * 0.05f)
                cubicTo(x + bodyLen * 0.47f, y + bodyH * 0.44f + bodyArc * 0.1f,
                    x + bodyLen * 0.37f, y + bodyH * 0.84f + bodyArc * 0.15f,
                    x + bodyLen * 0.22f, y + bodyH * 0.94f + bodyArc * 0.2f)
                cubicTo(x - bodyLen * 0.07f, y + bodyH * 1.02f + bodyArc * 0.5f,
                    x - bodyLen * 0.47f, y + bodyH * 0.80f + bodyArc * 0.85f,
                    x - bodyLen * 0.52f, y + bodyArc)
                close()
            }
            drawPath(bodyShadow, ShadowDarkBlue.copy(alpha = 0.20f))

            val body = Path().apply {
                moveTo(x - bodyLen * 0.5f, y + bodyArc)
                cubicTo(x - bodyLen * 0.45f, y - bodyH * 0.78f + bodyArc * 0.85f,
                    x - bodyLen * 0.05f, y - bodyH + bodyArc * 0.5f,
                    x + bodyLen * 0.2f, y - bodyH * 0.92f + bodyArc * 0.2f)
                cubicTo(x + bodyLen * 0.35f, y - bodyH * 0.82f + bodyArc * 0.15f,
                    x + bodyLen * 0.45f, y - bodyH * 0.42f + bodyArc * 0.1f,
                    x + bodyLen * 0.47f, y - bodyH * 0.12f + bodyArc * 0.05f)
                lineTo(x + bodyLen * 0.47f, y + bodyH * 0.12f + bodyArc * 0.05f)
                cubicTo(x + bodyLen * 0.45f, y + bodyH * 0.42f + bodyArc * 0.1f,
                    x + bodyLen * 0.35f, y + bodyH * 0.82f + bodyArc * 0.15f,
                    x + bodyLen * 0.2f, y + bodyH * 0.92f + bodyArc * 0.2f)
                cubicTo(x - bodyLen * 0.05f, y + bodyH + bodyArc * 0.5f,
                    x - bodyLen * 0.45f, y + bodyH * 0.78f + bodyArc * 0.85f,
                    x - bodyLen * 0.5f, y + bodyArc)
                close()
            }
            drawPath(body, BodyLightBlue)

            // Upper body bright blue accent
            val upperAccent = Path().apply {
                moveTo(x - bodyLen * 0.42f, y - bodyH * 0.30f + bodyArc * 0.8f)
                cubicTo(x - bodyLen * 0.20f, y - bodyH * 0.90f + bodyArc * 0.5f,
                    x + bodyLen * 0.15f, y - bodyH * 0.88f + bodyArc * 0.3f,
                    x + bodyLen * 0.38f, y - bodyH * 0.50f + bodyArc * 0.15f)
                cubicTo(x + bodyLen * 0.15f, y - bodyH * 0.62f + bodyArc * 0.3f,
                    x - bodyLen * 0.10f, y - bodyH * 0.65f + bodyArc * 0.5f,
                    x - bodyLen * 0.42f, y - bodyH * 0.30f + bodyArc * 0.8f)
                close()
            }
            drawPath(upperAccent, BodyBrightBlue.copy(alpha = 0.25f))

            // Pale teal highlight near head
            val headHL = Path().apply {
                moveTo(x - bodyLen * 0.45f, y - bodyH * 0.20f + bodyArc * 0.85f)
                cubicTo(x - bodyLen * 0.38f, y - bodyH * 0.60f + bodyArc * 0.75f,
                    x - bodyLen * 0.20f, y - bodyH * 0.65f + bodyArc * 0.6f,
                    x - bodyLen * 0.10f, y - bodyH * 0.40f + bodyArc * 0.5f)
                cubicTo(x - bodyLen * 0.18f, y - bodyH * 0.45f + bodyArc * 0.6f,
                    x - bodyLen * 0.32f, y - bodyH * 0.42f + bodyArc * 0.75f,
                    x - bodyLen * 0.45f, y - bodyH * 0.20f + bodyArc * 0.85f)
                close()
            }
            drawPath(headHL, BodyPaleTeal.copy(alpha = 0.30f))

            // ── BELLY (pale, layered) ────────────────────────────────────────
            val belly = Path().apply {
                moveTo(x - bodyLen * 0.48f, y + bodyArc + bodyH * 0.05f)
                cubicTo(x - bodyLen * 0.35f, y + bodyH * 0.88f + bodyArc * 0.7f,
                    x + bodyLen * 0.2f, y + bodyH * 0.90f + bodyArc * 0.2f,
                    x + bodyLen * 0.44f, y + bodyH * 0.10f + bodyArc * 0.05f)
                cubicTo(x + bodyLen * 0.2f, y + bodyH * 0.60f + bodyArc * 0.2f,
                    x - bodyLen * 0.10f, y + bodyH * 0.65f + bodyArc * 0.5f,
                    x - bodyLen * 0.48f, y + bodyArc + bodyH * 0.05f)
                close()
            }
            drawPath(belly, BellyWhite.copy(alpha = 0.50f))

            // Belly inner bright
            val bellyInner = Path().apply {
                moveTo(x - bodyLen * 0.40f, y + bodyArc + bodyH * 0.10f)
                cubicTo(x - bodyLen * 0.25f, y + bodyH * 0.72f + bodyArc * 0.6f,
                    x + bodyLen * 0.15f, y + bodyH * 0.70f + bodyArc * 0.2f,
                    x + bodyLen * 0.38f, y + bodyH * 0.12f + bodyArc * 0.05f)
                cubicTo(x + bodyLen * 0.15f, y + bodyH * 0.48f + bodyArc * 0.2f,
                    x - bodyLen * 0.08f, y + bodyH * 0.50f + bodyArc * 0.4f,
                    x - bodyLen * 0.40f, y + bodyArc + bodyH * 0.10f)
                close()
            }
            drawPath(bellyInner, BellyPaleGray.copy(alpha = 0.35f))

            // ── VENTRAL PLEATS ───────────────────────────────────────────────
            val pleatCount = 6 + level / 2
            for (p in 0 until pleatCount) {
                val pf = (p.toFloat() + 0.5f) / pleatCount.toFloat()
                val px = x - bodyLen * 0.42f + bodyLen * 0.5f * pf
                drawLine(
                    ShadowMedBlue.copy(alpha = 0.22f),
                    Offset(px, y + bodyH * 0.12f + bodyArc * (1f - pf * 0.5f)),
                    Offset(px + bodyLen * 0.08f, y + bodyH * 0.85f + bodyArc * (1f - pf * 0.3f)),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.3f)
                )
            }

            // ── CORAL ACCENT (mouth/jaw area) ────────────────────────────────
            val mouthAccent = Path().apply {
                moveTo(x - bodyLen * 0.50f, y + bodyArc - bodyH * 0.05f)
                cubicTo(x - bodyLen * 0.48f, y + bodyArc + bodyH * 0.25f,
                    x - bodyLen * 0.42f, y + bodyArc + bodyH * 0.30f,
                    x - bodyLen * 0.35f, y + bodyArc + bodyH * 0.20f)
                cubicTo(x - bodyLen * 0.40f, y + bodyArc + bodyH * 0.15f,
                    x - bodyLen * 0.46f, y + bodyArc + bodyH * 0.08f,
                    x - bodyLen * 0.50f, y + bodyArc - bodyH * 0.05f)
                close()
            }
            drawPath(mouthAccent, AccentCoral.copy(alpha = 0.35f))

            // ── BLOW HOLE ────────────────────────────────────────────────────
            val blowX = x - bodyLen * 0.38f
            val blowY = y - bodyH * 0.82f + bodyArc * 0.85f
            drawLine(ShadowDarkBlue.copy(alpha = 0.55f),
                Offset(blowX - bodyLen * 0.02f, blowY),
                Offset(blowX + bodyLen * 0.01f, blowY),
                strokeWidth = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round)

            // ── EYE (detailed) ───────────────────────────────────────────────
            val eyeR = (1.2f + level * 0.07f) * s
            val eyeX = x - bodyLen * 0.38f
            val eyeY = y - bodyH * 0.55f + bodyArc * 0.85f

            // Eye socket
            drawCircle(ShadowDeepNavy.copy(alpha = 0.3f), eyeR * 1.3f, Offset(eyeX, eyeY))
            // Sclera
            drawCircle(BellyWhite, eyeR * 1.05f, Offset(eyeX, eyeY))
            // Iris (bright blue)
            drawCircle(BodyMedBlue, eyeR * 0.65f, Offset(eyeX + eyeR * 0.05f, eyeY))
            // Pupil
            drawCircle(ShadowDeepNavy, eyeR * 0.35f, Offset(eyeX + eyeR * 0.07f, eyeY + eyeR * 0.02f))
            // Shine
            drawCircle(Color.White, eyeR * 0.18f, Offset(eyeX - eyeR * 0.15f, eyeY - eyeR * 0.15f))
        }
    }
}
