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
import kotlin.math.cos
import kotlin.math.sin

// ── Palette ─────────────────────────────────────────────────────────────────
private val BodyBlueGray    = Color(0xFF7090A8)  // blue-gray main body
private val BodyDarkBlue    = Color(0xFF4A6880)  // darker blue-gray
private val BodyPaleBlue    = Color(0xFF90B0C8)  // pale blue highlight
private val BodyLightGray   = Color(0xFFB0C8D8)  // light gray belly
private val SpotDark        = Color(0xFF506878)  // mottled dark spots
private val SpotLight       = Color(0xFF88A8B8)  // mottled light spots
private val TuskIvory       = Color(0xFFF0E8D0)  // tusk base color
private val TuskHighlight   = Color(0xFFFFF8E8)  // tusk highlight
private val TuskShadow      = Color(0xFFD0C0A0)  // tusk shadow
private val FlukeDark       = Color(0xFF3A5468)  // fluke dark
private val ShadowDeep      = Color(0xFF203040)  // deep shadow
private val EyeDark         = Color(0xFF182830)  // near-black
private val IceCrystal      = Color(0xFFC0E8FF)  // ice crystal sparkle
private val IceWhite        = Color(0xFFE8F8FF)  // bright ice highlight

object NarwhalRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyLen = (16f + level * 1.0f) * s
        val bodyH = (6f + level * 0.4f) * s
        val tuskLen = (10f + level * 0.8f) * s

        // Gentle whale undulation
        val freq = 1.0f * PI.toFloat()
        val frontArc = sin(t * freq) * bodyH * 0.02f
        val midArc = sin(t * freq + 0.5f) * bodyH * 0.04f
        val rearArc = sin(t * freq + 1.0f) * bodyH * 0.07f
        val tailSway = sin(t * freq + 1.0f) * 1.5f * s
        val flipperSweep = sin(t * freq + 0.8f) * 0.3f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FLUKES ──────────────────────────────────────────────
            val tailX = x + bodyLen * 0.45f
            val flukeSpan = bodyH * 0.8f

            val upperFluke = paths.obtain().apply {
                moveTo(tailX, y + rearArc * 0.3f)
                cubicTo(tailX + flukeSpan * 0.4f, y - bodyH * 0.3f + tailSway,
                    tailX + flukeSpan * 0.7f, y - bodyH * 0.9f + tailSway,
                    tailX + flukeSpan, y - bodyH * 1.0f + tailSway)
                cubicTo(tailX + flukeSpan * 0.7f, y - bodyH * 0.6f + tailSway,
                    tailX + flukeSpan * 0.35f, y - bodyH * 0.15f + tailSway * 0.5f,
                    tailX, y + rearArc * 0.3f)
                close()
            }
            drawPath(upperFluke, FlukeDark)

            val lowerFluke = paths.obtain().apply {
                moveTo(tailX, y + rearArc * 0.3f)
                cubicTo(tailX + flukeSpan * 0.35f, y + bodyH * 0.15f + tailSway * 0.5f,
                    tailX + flukeSpan * 0.7f, y + bodyH * 0.6f + tailSway,
                    tailX + flukeSpan, y + bodyH * 1.0f + tailSway)
                cubicTo(tailX + flukeSpan * 0.7f, y + bodyH * 0.9f + tailSway,
                    tailX + flukeSpan * 0.4f, y + bodyH * 0.3f + tailSway,
                    tailX, y + rearArc * 0.3f)
                close()
            }
            drawPath(lowerFluke, FlukeDark)

            // Fluke highlights
            drawPath(upperFluke, BodyDarkBlue.copy(alpha = 0.25f))
            drawPath(lowerFluke, BodyDarkBlue.copy(alpha = 0.20f))

            // ── PECTORAL FLIPPERS ────────────────────────────────────────
            val pect = paths.obtain().apply {
                moveTo(x - bodyLen * 0.10f, y + bodyH * 0.65f + flipperSweep * 0.5f)
                cubicTo(x - bodyLen * 0.02f, y + bodyH * 1.35f + flipperSweep * 0.3f,
                    x + bodyLen * 0.10f, y + bodyH * 1.40f + flipperSweep * 0.2f,
                    x + bodyLen * 0.18f, y + bodyH * 0.85f + flipperSweep * 0.3f)
                cubicTo(x + bodyLen * 0.10f, y + bodyH * 0.75f + flipperSweep * 0.4f,
                    x + bodyLen * 0.01f, y + bodyH * 0.68f + flipperSweep * 0.45f,
                    x - bodyLen * 0.10f, y + bodyH * 0.65f + flipperSweep * 0.5f)
                close()
            }
            drawPath(pect, BodyDarkBlue)
            drawPath(pect, BodyPaleBlue.copy(alpha = 0.20f))

            // ── MAIN BODY (smooth whale shape, S-wave) ───────────────────
            val bodyShadow = paths.obtain().apply {
                moveTo(x - bodyLen * 0.48f, y + frontArc)
                cubicTo(x - bodyLen * 0.40f, y - bodyH * 0.82f + frontArc * 0.8f,
                    x + bodyLen * 0.05f, y - bodyH * 0.98f + midArc * 0.4f,
                    x + bodyLen * 0.35f, y - bodyH * 0.60f + rearArc * 0.2f)
                cubicTo(x + bodyLen * 0.42f, y - bodyH * 0.30f + rearArc * 0.1f,
                    x + bodyLen * 0.47f, y + rearArc * 0.05f,
                    x + bodyLen * 0.47f, y + rearArc * 0.05f)
                lineTo(x + bodyLen * 0.47f, y + bodyH * 0.10f + rearArc * 0.05f)
                cubicTo(x + bodyLen * 0.42f, y + bodyH * 0.30f + rearArc * 0.1f,
                    x + bodyLen * 0.35f, y + bodyH * 0.60f + rearArc * 0.2f,
                    x + bodyLen * 0.05f, y + bodyH * 0.98f + midArc * 0.4f)
                cubicTo(x - bodyLen * 0.40f, y + bodyH * 0.82f + frontArc * 0.8f,
                    x - bodyLen * 0.48f, y + frontArc,
                    x - bodyLen * 0.48f, y + frontArc)
                close()
            }
            drawPath(bodyShadow, ShadowDeep.copy(alpha = 0.15f))

            val body = paths.obtain().apply {
                moveTo(x - bodyLen * 0.46f, y + frontArc)
                cubicTo(x - bodyLen * 0.38f, y - bodyH * 0.80f + frontArc * 0.8f,
                    x + bodyLen * 0.03f, y - bodyH * 0.95f + midArc * 0.4f,
                    x + bodyLen * 0.33f, y - bodyH * 0.58f + rearArc * 0.2f)
                cubicTo(x + bodyLen * 0.40f, y - bodyH * 0.28f + rearArc * 0.1f,
                    x + bodyLen * 0.45f, y + rearArc * 0.05f,
                    x + bodyLen * 0.45f, y + rearArc * 0.05f)
                lineTo(x + bodyLen * 0.45f, y + bodyH * 0.08f + rearArc * 0.05f)
                cubicTo(x + bodyLen * 0.40f, y + bodyH * 0.28f + rearArc * 0.1f,
                    x + bodyLen * 0.33f, y + bodyH * 0.58f + rearArc * 0.2f,
                    x + bodyLen * 0.03f, y + bodyH * 0.95f + midArc * 0.4f)
                cubicTo(x - bodyLen * 0.38f, y + bodyH * 0.80f + frontArc * 0.8f,
                    x - bodyLen * 0.46f, y + frontArc,
                    x - bodyLen * 0.46f, y + frontArc)
                close()
            }
            drawPath(body, BodyBlueGray)

            // Upper body highlight
            val upperHL = paths.obtain().apply {
                moveTo(x - bodyLen * 0.38f, y - bodyH * 0.25f + frontArc * 0.8f)
                cubicTo(x - bodyLen * 0.20f, y - bodyH * 0.70f + midArc * 0.5f,
                    x + bodyLen * 0.10f, y - bodyH * 0.72f + midArc * 0.3f,
                    x + bodyLen * 0.30f, y - bodyH * 0.35f + rearArc * 0.2f)
                cubicTo(x + bodyLen * 0.10f, y - bodyH * 0.50f + midArc * 0.3f,
                    x - bodyLen * 0.12f, y - bodyH * 0.52f + midArc * 0.5f,
                    x - bodyLen * 0.38f, y - bodyH * 0.25f + frontArc * 0.8f)
                close()
            }
            drawPath(upperHL, BodyPaleBlue.copy(alpha = 0.30f))

            // Belly (lighter)
            val belly = paths.obtain().apply {
                moveTo(x - bodyLen * 0.40f, y + frontArc + bodyH * 0.05f)
                cubicTo(x - bodyLen * 0.25f, y + bodyH * 0.75f + midArc * 0.6f,
                    x + bodyLen * 0.15f, y + bodyH * 0.72f + midArc * 0.2f,
                    x + bodyLen * 0.38f, y + bodyH * 0.08f + rearArc * 0.05f)
                cubicTo(x + bodyLen * 0.15f, y + bodyH * 0.50f + midArc * 0.2f,
                    x - bodyLen * 0.10f, y + bodyH * 0.52f + midArc * 0.4f,
                    x - bodyLen * 0.40f, y + frontArc + bodyH * 0.05f)
                close()
            }
            drawPath(belly, BodyLightGray.copy(alpha = 0.40f))

            // ── MOTTLED SPOTS (characteristic narwhal pattern) ───────────
            val spots = listOf(
                Triple(0.10f, -0.35f, 0.06f), Triple(-0.05f, -0.45f, 0.05f),
                Triple(0.25f, -0.25f, 0.07f), Triple(-0.15f, -0.30f, 0.05f),
                Triple(0.35f, -0.10f, 0.04f), Triple(0.05f, -0.55f, 0.04f),
                Triple(-0.25f, -0.20f, 0.06f), Triple(0.18f, 0.15f, 0.05f),
                Triple(-0.10f, 0.25f, 0.04f), Triple(0.30f, 0.08f, 0.05f),
                Triple(-0.30f, 0.15f, 0.04f), Triple(0.20f, -0.50f, 0.03f),
                Triple(0.00f, 0.40f, 0.04f), Triple(-0.20f, 0.35f, 0.03f),
            )
            for ((idx, spot) in spots.withIndex()) {
                val (sx, sy, sr) = spot
                val spotX = x + bodyLen * sx
                val spotY = y + bodyH * sy
                val spotColor = if (idx % 2 == 0) SpotDark else SpotLight
                drawCircle(spotColor.copy(alpha = 0.30f), bodyLen * sr, Offset(spotX, spotY))
            }

            // Body outline
            drawPath(body, BodyDarkBlue.copy(alpha = 0.10f),
                style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))

            // ── TUSK (spiral horn extending from head) ───────────────────
            val tuskBaseX = x - bodyLen * 0.46f
            val tuskBaseY = y + frontArc - bodyH * 0.05f
            val tuskTipX = tuskBaseX - tuskLen
            val tuskTipY = tuskBaseY

            // Tusk shadow
            drawLine(TuskShadow.copy(alpha = 0.30f),
                Offset(tuskBaseX, tuskBaseY + s * 0.5f),
                Offset(tuskTipX, tuskTipY + s * 0.3f),
                strokeWidth = (2.0f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round)

            // Main tusk
            drawLine(TuskIvory,
                Offset(tuskBaseX, tuskBaseY),
                Offset(tuskTipX, tuskTipY),
                strokeWidth = (1.8f * s).coerceAtLeast(0.7f), cap = StrokeCap.Round)

            // Tusk highlight
            drawLine(TuskHighlight.copy(alpha = 0.50f),
                Offset(tuskBaseX, tuskBaseY - s * 0.2f),
                Offset(tuskTipX, tuskTipY - s * 0.15f),
                strokeWidth = (0.6f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)

            // Tusk taper (thinner at tip) - redraw tip segment thinner
            drawLine(TuskIvory.copy(alpha = 0.80f),
                Offset(tuskTipX + tuskLen * 0.3f, tuskTipY),
                Offset(tuskTipX, tuskTipY),
                strokeWidth = (1.0f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round)

            // ── LEVEL 3+: SPIRAL DETAIL ON TUSK ─────────────────────────
            if (level >= 3) {
                val spiralCount = 12
                for (sp in 0 until spiralCount) {
                    val frac = sp.toFloat() / spiralCount
                    val spX = tuskBaseX - tuskLen * frac
                    val spAngle = frac * PI.toFloat() * 6f + t * 0.5f
                    val spOff = sin(spAngle) * s * 0.5f * (1f - frac * 0.5f)
                    drawCircle(TuskShadow.copy(alpha = 0.25f),
                        s * 0.3f * (1f - frac * 0.4f),
                        Offset(spX, tuskBaseY + spOff))
                }
            }

            // ── LEVEL 5+: ARCTIC ICE CRYSTAL SPARKLE AROUND TUSK ────────
            if (level >= 5) {
                for (c in 0 until 10) {
                    val cPhase = t * 1.8f + c * 1.2f
                    val cFrac = (c.toFloat() + sin(cPhase) * 0.2f) / 10f
                    val cx = tuskBaseX - tuskLen * cFrac
                    val cy = tuskBaseY + sin(cPhase * 2f) * bodyH * 0.3f
                    val sparkleAlpha = (sin(cPhase) * 0.5f + 0.5f) * 0.4f
                    val sparkleR = s * (0.4f + sin(cPhase * 0.7f) * 0.2f)

                    // Crystal halo
                    drawCircle(IceCrystal.copy(alpha = sparkleAlpha * 0.4f),
                        sparkleR * 2.5f, Offset(cx, cy))
                    // Crystal center
                    drawCircle(IceWhite.copy(alpha = sparkleAlpha),
                        sparkleR, Offset(cx, cy))

                    // Cross sparkle lines
                    val lineLen = sparkleR * 1.5f
                    drawLine(IceWhite.copy(alpha = sparkleAlpha * 0.6f),
                        Offset(cx - lineLen, cy), Offset(cx + lineLen, cy),
                        strokeWidth = (0.3f * s).coerceAtLeast(0.15f))
                    drawLine(IceWhite.copy(alpha = sparkleAlpha * 0.6f),
                        Offset(cx, cy - lineLen), Offset(cx, cy + lineLen),
                        strokeWidth = (0.3f * s).coerceAtLeast(0.15f))
                }
            }

            // ── ROUNDED HEAD (melon) ─────────────────────────────────────
            val melon = paths.obtain().apply {
                moveTo(x - bodyLen * 0.46f, y + frontArc - bodyH * 0.15f)
                cubicTo(x - bodyLen * 0.50f, y + frontArc - bodyH * 0.40f,
                    x - bodyLen * 0.48f, y + frontArc - bodyH * 0.55f,
                    x - bodyLen * 0.38f, y + frontArc - bodyH * 0.60f)
                cubicTo(x - bodyLen * 0.30f, y + frontArc - bodyH * 0.55f,
                    x - bodyLen * 0.28f, y + frontArc - bodyH * 0.30f,
                    x - bodyLen * 0.30f, y + frontArc - bodyH * 0.15f)
                close()
            }
            drawPath(melon, BodyBlueGray.copy(alpha = 0.60f))
            drawPath(melon, BodyPaleBlue.copy(alpha = 0.15f))

            // ── EYE ──────────────────────────────────────────────────────
            val eyeR = (1.1f + level * 0.06f) * s
            val eyeX = x - bodyLen * 0.38f
            val eyeY = y - bodyH * 0.38f + frontArc * 0.8f

            drawCircle(ShadowDeep.copy(alpha = 0.30f), eyeR * 1.25f, Offset(eyeX, eyeY))
            drawCircle(Color(0xFFE0E8F0), eyeR, Offset(eyeX, eyeY))
            drawCircle(BodyDarkBlue, eyeR * 0.60f, Offset(eyeX + eyeR * 0.05f, eyeY))
            drawCircle(EyeDark, eyeR * 0.35f, Offset(eyeX + eyeR * 0.07f, eyeY + eyeR * 0.02f))
            drawCircle(Color.White, eyeR * 0.18f,
                Offset(eyeX - eyeR * 0.15f, eyeY - eyeR * 0.15f))

            // ── MOUTH LINE ───────────────────────────────────────────────
            val mouthPath = paths.obtain().apply {
                moveTo(x - bodyLen * 0.46f, y + frontArc + bodyH * 0.02f)
                cubicTo(x - bodyLen * 0.43f, y + frontArc + bodyH * 0.08f,
                    x - bodyLen * 0.40f, y + frontArc + bodyH * 0.10f,
                    x - bodyLen * 0.36f, y + frontArc + bodyH * 0.06f)
            }
            drawPath(mouthPath, BodyDarkBlue.copy(alpha = 0.35f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round))
        }
    }
}
