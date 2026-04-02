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
private val BodySage       = Color(0xFF9FC8BC)  // sage green body
private val BodyLightSage  = Color(0xFFB4D1C5)  // lighter sage
private val BodyTealGray   = Color(0xFF72B4B8)  // teal-gray mid-tone
private val BellyCream     = Color(0xFFF4F3E5)  // cream belly
private val BellyWhite     = Color(0xFFF9F7F1)  // near-white highlight
private val AccentCoral    = Color(0xFFFA835B)  // coral/salmon accent
private val AccentPeach    = Color(0xFFFCA272)  // light peach
private val AccentDeepRed  = Color(0xFFD95C4E)  // deeper red-coral
private val AccentDarkRed  = Color(0xFFB94D49)  // dark red shadow
private val HighlightTeal  = Color(0xFF15A3A8)  // teal highlight
private val ShadowNavy     = Color(0xFF050530)  // near-black
private val ShadowDeepBlue = Color(0xFF042974)  // dark blue-purple
private val ShadowDarkTeal = Color(0xFF035578)  // dark teal
private val DarkGray       = Color(0xFF323D44)  // dark gray accent
private val MediumTeal     = Color(0xFF057E8E)  // medium teal

object BarracudaRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen  = (18f + level * 1.2f) * s
        val bodyH    = (4.5f + level * 0.35f) * s

        // ── Barracuda two-state locomotion (5s cycle: 3.5s glide + 1.5s burst) ──
        val cycle      = (animTimeMs % 5000L).toFloat() / 1000f
        val burstBlend = if (cycle > 3.5f) ((cycle - 3.5f) / 0.15f).coerceIn(0f, 1f) else 0f
        val tailFreq   = 0.8f + burstBlend * 5.2f
        val tailAmp    = (0.4f + burstBlend * 1.6f) * s
        val tailSway   = sin(t * tailFreq * PI.toFloat()) * tailAmp
        val jawOpen    = sin(t * 0.7f * PI.toFloat()) * 0.015f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FIN (forked, layered) ───────────────────────────────────
            val tailX = x + bodyLen * 0.45f
            val forkLen = 4.0f * s

            // Tail shadow
            val tailShadow = paths.obtain().apply {
                moveTo(tailX - bodyLen * 0.02f, y)
                lineTo(tailX + forkLen * 1.05f, y - forkLen * 1.25f + tailSway)
                lineTo(tailX + forkLen * 0.5f, y + tailSway * 0.3f)
                lineTo(tailX + forkLen * 1.05f, y + forkLen * 1.25f + tailSway)
                close()
            }
            drawPath(tailShadow, ShadowDeepBlue.copy(alpha = 0.20f))

            // Upper fork
            val tailTopFork = paths.obtain().apply {
                moveTo(tailX, y - bodyH * 0.2f)
                cubicTo(
                    tailX + forkLen * 0.3f, y - bodyH * 0.3f + tailSway * 0.3f,
                    tailX + forkLen * 0.7f, y - forkLen * 0.9f + tailSway,
                    tailX + forkLen, y - forkLen * 1.2f + tailSway
                )
                cubicTo(
                    tailX + forkLen * 0.7f, y + tailSway * 0.3f,
                    tailX + forkLen * 0.4f, y + bodyH * 0.1f + tailSway * 0.2f,
                    tailX, y
                )
                close()
            }
            drawPath(tailTopFork, ShadowDarkTeal)

            // Upper fork highlight
            val tailTopHL = paths.obtain().apply {
                moveTo(tailX + forkLen * 0.1f, y - bodyH * 0.18f)
                cubicTo(
                    tailX + forkLen * 0.35f, y - bodyH * 0.25f + tailSway * 0.3f,
                    tailX + forkLen * 0.6f, y - forkLen * 0.7f + tailSway,
                    tailX + forkLen * 0.85f, y - forkLen * 0.95f + tailSway
                )
                cubicTo(
                    tailX + forkLen * 0.6f, y + tailSway * 0.2f,
                    tailX + forkLen * 0.35f, y + bodyH * 0.05f + tailSway * 0.15f,
                    tailX + forkLen * 0.1f, y - bodyH * 0.05f
                )
                close()
            }
            drawPath(tailTopHL, BodyTealGray.copy(alpha = 0.5f))

            // Lower fork
            val tailBotFork = paths.obtain().apply {
                moveTo(tailX, y)
                cubicTo(
                    tailX + forkLen * 0.4f, y + bodyH * 0.1f + tailSway * 0.2f,
                    tailX + forkLen * 0.7f, y + tailSway * 0.3f,
                    tailX + forkLen, y + forkLen * 1.2f + tailSway
                )
                cubicTo(
                    tailX + forkLen * 0.7f, y + forkLen * 0.9f + tailSway,
                    tailX + forkLen * 0.3f, y + bodyH * 0.3f + tailSway * 0.3f,
                    tailX, y + bodyH * 0.2f
                )
                close()
            }
            drawPath(tailBotFork, ShadowDarkTeal)

            // ── DORSAL FINS ──────────────────────────────────────────────────
            val dorsalSway = sin(t * 3f * PI.toFloat()) * 0.4f * s * (1f - burstBlend * 0.8f)

            // First dorsal shadow
            val dorsal1Shadow = paths.obtain().apply {
                moveTo(x - bodyLen * 0.12f, y - bodyH * 0.83f)
                cubicTo(
                    x - bodyLen * 0.02f + dorsalSway, y - bodyH * 1.75f,
                    x + bodyLen * 0.10f + dorsalSway, y - bodyH * 1.70f,
                    x + bodyLen * 0.16f, y - bodyH * 0.83f
                )
                close()
            }
            drawPath(dorsal1Shadow, ShadowDeepBlue.copy(alpha = 0.18f))

            // First dorsal
            val dorsal1 = paths.obtain().apply {
                moveTo(x - bodyLen * 0.1f, y - bodyH * 0.85f)
                cubicTo(
                    x - bodyLen * 0.0f + dorsalSway, y - bodyH * 1.7f,
                    x + bodyLen * 0.08f + dorsalSway, y - bodyH * 1.65f,
                    x + bodyLen * 0.14f, y - bodyH * 0.85f
                )
                cubicTo(
                    x + bodyLen * 0.08f, y - bodyH * 0.75f,
                    x + bodyLen * 0.0f, y - bodyH * 0.78f,
                    x - bodyLen * 0.1f, y - bodyH * 0.85f
                )
                close()
            }
            drawPath(dorsal1, AccentDeepRed)
            // Dorsal highlight
            val dorsal1HL = paths.obtain().apply {
                moveTo(x - bodyLen * 0.06f, y - bodyH * 0.87f)
                cubicTo(
                    x + dorsalSway * 0.6f, y - bodyH * 1.40f,
                    x + bodyLen * 0.05f + dorsalSway * 0.6f, y - bodyH * 1.38f,
                    x + bodyLen * 0.10f, y - bodyH * 0.87f
                )
                close()
            }
            drawPath(dorsal1HL, AccentCoral.copy(alpha = 0.6f))

            // Second dorsal
            val dorsal2 = paths.obtain().apply {
                moveTo(x + bodyLen * 0.22f, y - bodyH * 0.78f)
                cubicTo(
                    x + bodyLen * 0.27f + dorsalSway * 0.5f, y - bodyH * 1.2f,
                    x + bodyLen * 0.34f + dorsalSway * 0.5f, y - bodyH * 1.15f,
                    x + bodyLen * 0.38f, y - bodyH * 0.75f
                )
                cubicTo(
                    x + bodyLen * 0.34f, y - bodyH * 0.65f,
                    x + bodyLen * 0.27f, y - bodyH * 0.68f,
                    x + bodyLen * 0.22f, y - bodyH * 0.78f
                )
                close()
            }
            drawPath(dorsal2, AccentDeepRed)
            // Dorsal 2 highlight
            val dorsal2HL = paths.obtain().apply {
                moveTo(x + bodyLen * 0.24f, y - bodyH * 0.75f)
                cubicTo(
                    x + bodyLen * 0.28f + dorsalSway * 0.3f, y - bodyH * 1.0f,
                    x + bodyLen * 0.32f + dorsalSway * 0.3f, y - bodyH * 0.98f,
                    x + bodyLen * 0.36f, y - bodyH * 0.73f
                )
                close()
            }
            drawPath(dorsal2HL, AccentCoral.copy(alpha = 0.45f))

            // Anal fin
            val anal = paths.obtain().apply {
                moveTo(x + bodyLen * 0.22f, y + bodyH * 0.78f)
                cubicTo(
                    x + bodyLen * 0.27f, y + bodyH * 1.2f,
                    x + bodyLen * 0.34f, y + bodyH * 1.15f,
                    x + bodyLen * 0.38f, y + bodyH * 0.75f
                )
                cubicTo(
                    x + bodyLen * 0.34f, y + bodyH * 0.65f,
                    x + bodyLen * 0.27f, y + bodyH * 0.68f,
                    x + bodyLen * 0.22f, y + bodyH * 0.78f
                )
                close()
            }
            drawPath(anal, AccentDeepRed)

            // ── MAIN BODY (torpedo, layered) ─────────────────────────────────
            val headX = x - bodyLen

            // Body shadow
            val bodyShadow = paths.obtain().apply {
                moveTo(headX - 0.5f * s, y)
                cubicTo(
                    headX + bodyLen * 0.05f, y - bodyH * 1.05f,
                    x - bodyLen * 0.28f, y - bodyH * 1.05f,
                    x + bodyLen * 0.52f, y - bodyH * 0.28f
                )
                lineTo(tailX + 0.5f * s, y)
                lineTo(x + bodyLen * 0.52f, y + bodyH * 0.28f)
                cubicTo(
                    x - bodyLen * 0.28f, y + bodyH * 1.05f,
                    headX + bodyLen * 0.05f, y + bodyH * 1.05f,
                    headX - 0.5f * s, y
                )
                close()
            }
            drawPath(bodyShadow, ShadowDeepBlue.copy(alpha = 0.18f))

            // Main body
            val body = paths.obtain().apply {
                moveTo(headX, y)
                cubicTo(
                    headX + bodyLen * 0.05f, y - bodyH * 0.45f,
                    x - bodyLen * 0.3f, y - bodyH,
                    x + bodyLen * 0.5f, y - bodyH * 0.25f
                )
                lineTo(tailX, y)
                lineTo(x + bodyLen * 0.5f, y + bodyH * 0.25f)
                cubicTo(
                    x - bodyLen * 0.3f, y + bodyH,
                    headX + bodyLen * 0.05f, y + bodyH * 0.45f,
                    headX, y
                )
                close()
            }
            drawPath(body, BodySage)

            // Upper body highlight
            val upperHL = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.03f, y - bodyH * 0.15f)
                cubicTo(
                    headX + bodyLen * 0.08f, y - bodyH * 0.40f,
                    x - bodyLen * 0.25f, y - bodyH * 0.90f,
                    x + bodyLen * 0.45f, y - bodyH * 0.22f
                )
                cubicTo(
                    x + bodyLen * 0.2f, y - bodyH * 0.55f,
                    x - bodyLen * 0.15f, y - bodyH * 0.60f,
                    headX + bodyLen * 0.03f, y - bodyH * 0.15f
                )
                close()
            }
            drawPath(upperHL, BodyLightSage.copy(alpha = 0.5f))

            // Belly counter-shading (cream)
            val belly = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.05f, y + bodyH * 0.05f)
                cubicTo(
                    x - bodyLen * 0.2f, y + bodyH * 0.85f,
                    x + bodyLen * 0.3f, y + bodyH * 0.8f,
                    tailX, y + bodyH * 0.2f
                )
                cubicTo(
                    x + bodyLen * 0.3f, y + bodyH * 0.55f,
                    x - bodyLen * 0.2f, y + bodyH * 0.6f,
                    headX + bodyLen * 0.05f, y + bodyH * 0.05f
                )
                close()
            }
            drawPath(belly, BellyCream.copy(alpha = 0.50f))

            // Belly bright center
            val bellyCenter = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.08f, y + bodyH * 0.10f)
                cubicTo(
                    x - bodyLen * 0.15f, y + bodyH * 0.65f,
                    x + bodyLen * 0.25f, y + bodyH * 0.60f,
                    tailX - bodyLen * 0.05f, y + bodyH * 0.15f
                )
                cubicTo(
                    x + bodyLen * 0.2f, y + bodyH * 0.40f,
                    x - bodyLen * 0.1f, y + bodyH * 0.42f,
                    headX + bodyLen * 0.08f, y + bodyH * 0.10f
                )
                close()
            }
            drawPath(bellyCenter, BellyWhite.copy(alpha = 0.30f))

            // ── UPPER JAW RIDGE ──────────────────────────────────────────────
            val upperJaw = paths.obtain().apply {
                moveTo(headX - 1.5f * s, y - bodyH * 0.08f)
                cubicTo(
                    headX + bodyLen * 0.02f, y - bodyH * 0.22f,
                    headX + bodyLen * 0.06f, y - bodyH * 0.24f,
                    headX + bodyLen * 0.14f, y - bodyH * 0.18f
                )
                cubicTo(
                    headX + bodyLen * 0.08f, y - bodyH * 0.14f,
                    headX + bodyLen * 0.03f, y - bodyH * 0.10f,
                    headX - 1.5f * s, y - bodyH * 0.08f
                )
                close()
            }
            drawPath(upperJaw, DarkGray.copy(alpha = 0.45f))

            // ── LOWER JAW ────────────────────────────────────────────────────
            val lowerJaw = paths.obtain().apply {
                moveTo(headX - 1.5f * s, y + bodyH * 0.08f + jawOpen)
                cubicTo(
                    headX + bodyLen * 0.02f, y + bodyH * 0.35f + jawOpen,
                    headX + bodyLen * 0.06f, y + bodyH * 0.38f + jawOpen,
                    headX + bodyLen * 0.14f, y + bodyH * 0.3f + jawOpen
                )
                cubicTo(
                    headX + bodyLen * 0.08f, y + bodyH * 0.3f + jawOpen,
                    headX + bodyLen * 0.03f, y + bodyH * 0.18f + jawOpen,
                    headX - 1.5f * s, y + bodyH * 0.08f + jawOpen
                )
                close()
            }
            drawPath(lowerJaw, DarkGray.copy(alpha = 0.7f))

            // Teeth (sharper, coral-tinted) — lower teeth follow jawOpen
            val toothCount = 5
            for (i in 0 until toothCount) {
                val tx = headX + bodyLen * 0.015f + i * bodyLen * 0.022f
                drawLine(
                    BellyWhite.copy(alpha = 0.75f),
                    Offset(tx, y + bodyH * 0.22f + jawOpen),
                    Offset(tx, y + bodyH * 0.08f + jawOpen),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.4f),
                    cap = StrokeCap.Round
                )
            }
            // Upper teeth
            for (i in 0 until 3) {
                val tx = headX + bodyLen * 0.02f + i * bodyLen * 0.025f
                drawLine(
                    BellyWhite.copy(alpha = 0.6f),
                    Offset(tx, y - bodyH * 0.08f),
                    Offset(tx, y + bodyH * 0.05f),
                    strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                    cap = StrokeCap.Round
                )
            }

            // ── LATERAL LINE ─────────────────────────────────────────────────
            drawLine(
                ShadowDarkTeal.copy(alpha = 0.45f),
                Offset(headX + bodyLen * 0.10f, y - bodyH * 0.06f),
                Offset(tailX - forkLen * 0.3f, y - bodyH * 0.08f),
                strokeWidth = (0.7f * s).coerceAtLeast(0.4f),
                cap = StrokeCap.Round
            )

            // ── PECTORAL FIN (small, coral-tinted) ───────────────────────────
            val pectAmp  = (1f - burstBlend) * 0.5f * s
            val pectSway = sin(t * 3.5f * PI.toFloat()) * pectAmp
            val pect = paths.obtain().apply {
                moveTo(x - bodyLen * 0.55f, y + bodyH * 0.15f)
                cubicTo(
                    x - bodyLen * 0.48f + pectSway, y + bodyH * 0.65f,
                    x - bodyLen * 0.40f + pectSway, y + bodyH * 0.68f,
                    x - bodyLen * 0.35f, y + bodyH * 0.35f
                )
                cubicTo(
                    x - bodyLen * 0.42f, y + bodyH * 0.25f,
                    x - bodyLen * 0.50f, y + bodyH * 0.18f,
                    x - bodyLen * 0.55f, y + bodyH * 0.15f
                )
                close()
            }
            drawPath(pect, AccentCoral.copy(alpha = 0.55f))

            // ── PELVIC FINS (ventral pair, small) ────────────────────────────
            val pelv = paths.obtain().apply {
                moveTo(x - bodyLen * 0.30f, y + bodyH * 0.35f)
                cubicTo(
                    x - bodyLen * 0.25f + pectSway * 0.3f, y + bodyH * 0.72f,
                    x - bodyLen * 0.18f + pectSway * 0.3f, y + bodyH * 0.70f,
                    x - bodyLen * 0.15f, y + bodyH * 0.40f
                )
                cubicTo(
                    x - bodyLen * 0.20f, y + bodyH * 0.30f,
                    x - bodyLen * 0.27f, y + bodyH * 0.28f,
                    x - bodyLen * 0.30f, y + bodyH * 0.35f
                )
                close()
            }
            drawPath(pelv, AccentPeach.copy(alpha = 0.50f))

            // ── LEVEL 3+: Flank blotches (darker teal chevrons) ─────────────
            if (level >= 3) {
                for (b in 0 until 5) {
                    val bx = x - bodyLen * 0.3f + b * bodyLen * 0.16f
                    drawLine(
                        ShadowDarkTeal.copy(alpha = 0.25f),
                        Offset(bx, y - bodyH * 0.55f),
                        Offset(bx + 0.8f * s, y + bodyH * 0.45f),
                        strokeWidth = (2.5f * s).coerceAtLeast(1.0f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // ── LEVEL 5+: Iridescent sheen ──────────────────────────────────
            if (level >= 5) {
                drawLine(
                    HighlightTeal.copy(alpha = 0.40f),
                    Offset(headX + bodyLen * 0.08f, y - bodyH * 0.35f),
                    Offset(tailX, y - bodyH * 0.30f),
                    strokeWidth = (1.2f * s).coerceAtLeast(0.6f),
                    cap = StrokeCap.Round
                )
                // Coral accent line along belly
                drawLine(
                    AccentPeach.copy(alpha = 0.30f),
                    Offset(headX + bodyLen * 0.12f, y + bodyH * 0.30f),
                    Offset(tailX - forkLen * 0.5f, y + bodyH * 0.25f),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.4f),
                    cap = StrokeCap.Round
                )
            }

            // ── EYE (detailed) ───────────────────────────────────────────────
            val eyeR = (1.6f + level * 0.1f) * s
            val eyeX = headX + bodyLen * 0.10f
            val eyeY = y - bodyH * 0.28f

            // Eye socket shadow
            drawCircle(ShadowNavy.copy(alpha = 0.3f), eyeR * 1.25f, Offset(eyeX, eyeY))
            // Sclera
            drawCircle(BellyWhite, eyeR, Offset(eyeX, eyeY))
            // Iris (teal)
            drawCircle(MediumTeal, eyeR * 0.60f, Offset(eyeX + eyeR * 0.08f, eyeY))
            // Pupil
            drawCircle(ShadowNavy, eyeR * 0.35f, Offset(eyeX + eyeR * 0.10f, eyeY + eyeR * 0.02f))
            // Shine
            drawCircle(Color.White, eyeR * 0.20f, Offset(eyeX - eyeR * 0.18f, eyeY - eyeR * 0.18f))
            // Secondary shine
            drawCircle(Color.White.copy(alpha = 0.5f), eyeR * 0.10f,
                Offset(eyeX + eyeR * 0.15f, eyeY + eyeR * 0.12f))

            // ── GILL COVER SHADOW ────────────────────────────────────────────
            val gillCover = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.08f, y - bodyH * 0.10f)
                cubicTo(
                    headX + bodyLen * 0.18f, y - bodyH * 0.45f,
                    headX + bodyLen * 0.22f, y + bodyH * 0.35f,
                    headX + bodyLen * 0.10f, y + bodyH * 0.15f
                )
                close()
            }
            drawPath(gillCover, ShadowDarkTeal.copy(alpha = 0.15f))
        }
    }
}
