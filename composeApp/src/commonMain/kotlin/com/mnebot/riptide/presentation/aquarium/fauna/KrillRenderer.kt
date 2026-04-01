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

// ── Palette ──────────────────────────────────────────────────────────────────
private val BodyPink       = Color(0xFFE8A8A0)  // translucent pale pink
private val BodyPale       = Color(0xFFF0C8C0)  // lighter pink highlight
private val BodyDeepPink   = Color(0xFFCC7878)  // deeper pink shading
private val SegmentLine    = Color(0xFFAA7070)  // segment dividers
private val ShellTrans     = Color(0xFFD8A098)  // semi-transparent shell
private val EyeBlack       = Color(0xFF1A1018)  // dark eyes
private val AntennaPink    = Color(0xFFBB8888)  // antennae
private val LegPink        = Color(0xFFCC9090)  // legs
private val ShadowDark     = Color(0xFF3A1818)  // shadow
private val BioGlow        = Color(0xFF60D0FF)  // bioluminescence (level 3+)
private val SwarmDot       = Color(0xFFD09898)  // swarm dots (level 5+)

object KrillRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        // Krill are tiny: use conservative scaling
        val bodyLen = (6f + level * 0.3f) * s
        val bodyH   = (1.8f + level * 0.1f) * s
        val segCount = 7

        // Quick tail flick + body arch
        val tailFlick = sin(t * 6.0f * PI.toFloat()) * 1.2f * s
        val bodyArch  = sin(t * 3.0f * PI.toFloat()) * 0.3f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── ANTENNAE (two long, one short pair) ─────────────────────────
            val antBaseX = x - bodyLen * 0.48f
            val antBaseY = y - bodyH * 0.3f

            // Long antennae
            for (side in intArrayOf(-1, 1)) {
                val antSway = sin(t * 5.5f * PI.toFloat() + side * 0.5f) * 1.5f * s
                val antPath = Path().apply {
                    moveTo(antBaseX, antBaseY + side * bodyH * 0.15f)
                    quadraticTo(
                        antBaseX - bodyLen * 0.35f + antSway * 0.3f,
                        antBaseY + side * bodyH * 0.6f,
                        antBaseX - bodyLen * 0.55f + antSway,
                        antBaseY + side * bodyH * 0.2f + antSway * 0.3f
                    )
                }
                drawPath(antPath, AntennaPink.copy(alpha = 0.60f),
                    style = Stroke(width = (0.3f * s).coerceAtLeast(0.15f), cap = StrokeCap.Round))
            }

            // Short antennules
            for (side in intArrayOf(-1, 1)) {
                val antSway2 = sin(t * 7.0f * PI.toFloat() + side * 0.8f) * 0.8f * s
                drawLine(
                    AntennaPink.copy(alpha = 0.45f),
                    Offset(antBaseX, antBaseY + side * bodyH * 0.08f),
                    Offset(antBaseX - bodyLen * 0.20f + antSway2, antBaseY + side * bodyH * 0.35f),
                    strokeWidth = (0.2f * s).coerceAtLeast(0.1f),
                    cap = StrokeCap.Round
                )
            }

            // ── PLEOPODS / LEGS (beating rapidly) ───────────────────────────
            val legCount = 5
            for (i in 0 until legCount) {
                val legFrac = i.toFloat() / legCount
                val legX = x - bodyLen * 0.35f + legFrac * bodyLen * 0.6f
                val legPhase = t * 10.0f * PI.toFloat() + i * 1.2f
                val legSwing = sin(legPhase) * bodyH * 0.6f

                drawLine(
                    LegPink.copy(alpha = 0.50f),
                    Offset(legX, y + bodyH * 0.35f),
                    Offset(legX + legSwing * 0.3f, y + bodyH * 0.35f + bodyH * 0.8f + legSwing * 0.2f),
                    strokeWidth = (0.25f * s).coerceAtLeast(0.12f),
                    cap = StrokeCap.Round
                )
            }

            // ── TAIL FAN (uropods at rear) ──────────────────────────────────
            val tailX = x + bodyLen * 0.45f
            val tailFan = Path().apply {
                moveTo(tailX - bodyLen * 0.05f, y)
                cubicTo(
                    tailX + bodyLen * 0.08f, y - bodyH * 0.5f + tailFlick * 0.3f,
                    tailX + bodyLen * 0.15f, y - bodyH * 0.8f + tailFlick,
                    tailX + bodyLen * 0.12f, y - bodyH * 0.3f + tailFlick * 0.5f
                )
                cubicTo(
                    tailX + bodyLen * 0.18f, y + tailFlick * 0.2f,
                    tailX + bodyLen * 0.15f, y + bodyH * 0.8f + tailFlick,
                    tailX + bodyLen * 0.08f, y + bodyH * 0.5f + tailFlick * 0.3f
                )
                close()
            }
            drawPath(tailFan, ShellTrans.copy(alpha = 0.40f))
            drawPath(tailFan, SegmentLine.copy(alpha = 0.20f),
                style = Stroke(width = (0.3f * s).coerceAtLeast(0.15f)))

            // ── MAIN BODY (segmented, translucent) ──────────────────────────
            // Body shadow
            val bodyShadow = Path().apply {
                moveTo(x - bodyLen * 0.48f, y + bodyArch * 0.1f)
                cubicTo(
                    x - bodyLen * 0.3f, y - bodyH * 1.05f + bodyArch,
                    x + bodyLen * 0.2f, y - bodyH * 1.0f + bodyArch * 0.5f,
                    x + bodyLen * 0.48f, y - bodyH * 0.15f + tailFlick * 0.1f
                )
                cubicTo(
                    x + bodyLen * 0.2f, y + bodyH * 1.0f - bodyArch * 0.5f,
                    x - bodyLen * 0.3f, y + bodyH * 1.05f - bodyArch,
                    x - bodyLen * 0.48f, y + bodyArch * 0.1f
                )
                close()
            }
            drawPath(bodyShadow, ShadowDark.copy(alpha = 0.10f))

            // Main body
            val bodyPath = Path().apply {
                moveTo(x - bodyLen * 0.46f, y + bodyArch * 0.1f)
                cubicTo(
                    x - bodyLen * 0.28f, y - bodyH + bodyArch,
                    x + bodyLen * 0.18f, y - bodyH * 0.95f + bodyArch * 0.5f,
                    x + bodyLen * 0.46f, y - bodyH * 0.12f + tailFlick * 0.05f
                )
                cubicTo(
                    x + bodyLen * 0.18f, y + bodyH * 0.95f - bodyArch * 0.5f,
                    x - bodyLen * 0.28f, y + bodyH - bodyArch,
                    x - bodyLen * 0.46f, y + bodyArch * 0.1f
                )
                close()
            }
            drawPath(bodyPath, BodyPink.copy(alpha = 0.65f))

            // Upper highlight
            val upperHL = Path().apply {
                moveTo(x - bodyLen * 0.40f, y - bodyH * 0.30f + bodyArch * 0.5f)
                cubicTo(
                    x - bodyLen * 0.20f, y - bodyH * 0.80f + bodyArch,
                    x + bodyLen * 0.10f, y - bodyH * 0.75f + bodyArch * 0.5f,
                    x + bodyLen * 0.38f, y - bodyH * 0.10f
                )
                cubicTo(
                    x + bodyLen * 0.10f, y - bodyH * 0.50f + bodyArch * 0.3f,
                    x - bodyLen * 0.15f, y - bodyH * 0.55f + bodyArch * 0.5f,
                    x - bodyLen * 0.40f, y - bodyH * 0.30f + bodyArch * 0.5f
                )
                close()
            }
            drawPath(upperHL, BodyPale.copy(alpha = 0.35f))

            // Visible segments (darker lines)
            for (i in 1 until segCount) {
                val segFrac = i.toFloat() / segCount
                val segX = x - bodyLen * 0.40f + segFrac * bodyLen * 0.80f
                val waveY = bodyArch * (1f - segFrac) + tailFlick * 0.02f * segFrac
                drawLine(
                    SegmentLine.copy(alpha = 0.25f),
                    Offset(segX, y - bodyH * 0.6f * (1f - (segFrac - 0.5f) * (segFrac - 0.5f) * 3f) + waveY),
                    Offset(segX, y + bodyH * 0.6f * (1f - (segFrac - 0.5f) * (segFrac - 0.5f) * 3f) - waveY),
                    strokeWidth = (0.2f * s).coerceAtLeast(0.1f)
                )
            }

            // Internal organs visible (gut line)
            drawLine(
                BodyDeepPink.copy(alpha = 0.25f),
                Offset(x - bodyLen * 0.30f, y + bodyArch * 0.2f),
                Offset(x + bodyLen * 0.30f, y + tailFlick * 0.02f),
                strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                cap = StrokeCap.Round
            )

            // ── EYE (compound, on stalk) ────────────────────────────────────
            val eyeR = (0.8f + level * 0.05f) * s
            val eyeX = x - bodyLen * 0.42f
            val eyeY = y - bodyH * 0.55f

            drawCircle(EyeBlack, eyeR, Offset(eyeX, eyeY))
            drawCircle(Color.White, eyeR * 0.25f, Offset(eyeX - eyeR * 0.2f, eyeY - eyeR * 0.2f))

            // ── LEVEL 3+: Slight bioluminescence ────────────────────────────
            if (level >= 3) {
                val bioAlpha = (sin(t * 2.0f * PI.toFloat()) * 0.5f + 0.5f) * 0.15f
                drawPath(bodyPath, BioGlow.copy(alpha = bioAlpha))
                // Photophore spots along underside
                for (i in 0 until 4) {
                    val pxFrac = 0.15f + i * 0.2f
                    val px = x - bodyLen * 0.35f + pxFrac * bodyLen * 0.75f
                    val py = y + bodyH * 0.5f
                    val pAlpha = (sin(t * 2.0f * PI.toFloat() + i * 0.7f) * 0.5f + 0.5f) * 0.30f
                    drawCircle(BioGlow.copy(alpha = pAlpha), s * 0.35f, Offset(px, py))
                }
            }

            // ── LEVEL 5+: Swarm of 2-3 tiny dots following ──────────────────
            if (level >= 5) {
                for (i in 0 until 3) {
                    val followDelay = (i + 1) * 0.4f
                    val followX = x + bodyLen * 0.55f + (i + 1) * bodyLen * 0.15f +
                                  sin(t * 3.0f * PI.toFloat() - followDelay) * s * 0.8f
                    val followY = y + sin(t * 2.5f * PI.toFloat() - followDelay * 1.3f) * bodyH * 0.8f
                    val dotSize = s * (0.5f - i * 0.1f)
                    drawCircle(SwarmDot.copy(alpha = 0.45f), dotSize, Offset(followX, followY))
                    drawCircle(BodyPink.copy(alpha = 0.25f), dotSize * 0.5f, Offset(followX, followY))
                    // Tiny eye dot
                    drawCircle(EyeBlack.copy(alpha = 0.40f), dotSize * 0.3f,
                        Offset(followX - dotSize * 0.3f, followY - dotSize * 0.2f))
                }
            }
        }
    }
}
