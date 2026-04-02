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

// ── Palette ─────────────────────────────────────────────────────────────────
private val BackDarkPurple  = Color(0xFF1A1840)  // dark blue-purple back
private val BackMidPurple   = Color(0xFF2A2860)  // mid purple
private val BackBlue        = Color(0xFF303878)  // blue accent
private val SideSilver      = Color(0xFFB8C0D0)  // silver sides
private val SideBright      = Color(0xFFD0D8E8)  // bright silver
private val BellySilver     = Color(0xFFE8E8F0)  // lighter belly
private val BellyWhite      = Color(0xFFF5F5F8)  // white belly
private val BillGray        = Color(0xFF808898)  // bill color
private val BillDark        = Color(0xFF484858)  // dark bill
private val BillLight       = Color(0xFFA0A8B8)  // bill highlight
private val DorsalPurple    = Color(0xFF282050)  // dorsal fin
private val DorsalBlue      = Color(0xFF384088)  // dorsal highlight
private val FinDark         = Color(0xFF1A1838)  // dark fin
private val ShadowNavy      = Color(0xFF0A0828)  // dark shadow
private val ElectricBlue    = Color(0xFF40A0FF)  // electric blue glow (level 5+)
private val ElectricCyan    = Color(0xFF60D0FF)  // cyan highlight

object SwordfishRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyLen = (14f + level * 0.8f) * s
        val bodyH = (5.5f + level * 0.4f) * s
        val billLen = (8f + level * 0.5f) * s

        // Powerful tail oscillation
        val tailFreq = 3.5f
        val tailSway = sin(t * tailFreq * PI.toFloat()) * 1.2f * s
        val bodyBob = sin(t * 1.8f * PI.toFloat()) * s * 0.04f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = bodyBob)
            }) {

            // ── CRESCENT TAIL FIN ────────────────────────────────────────────
            val tailX = x + bodyLen * 0.45f

            val tailShadow = paths.obtain().apply {
                moveTo(tailX - bodyLen * 0.02f, y)
                lineTo(tailX + bodyLen * 0.18f, y - bodyH * 1.1f + tailSway)
                lineTo(tailX + bodyLen * 0.08f, y + tailSway * 0.3f)
                lineTo(tailX + bodyLen * 0.18f, y + bodyH * 1.1f + tailSway)
                close()
            }
            drawPath(tailShadow, ShadowNavy.copy(alpha = 0.15f))

            // Upper tail lobe
            val tailUp = paths.obtain().apply {
                moveTo(tailX, y - bodyH * 0.12f)
                cubicTo(
                    tailX + bodyLen * 0.06f, y - bodyH * 0.4f + tailSway * 0.4f,
                    tailX + bodyLen * 0.12f, y - bodyH * 0.85f + tailSway * 0.8f,
                    tailX + bodyLen * 0.16f, y - bodyH * 1.05f + tailSway
                )
                cubicTo(
                    tailX + bodyLen * 0.10f, y - bodyH * 0.5f + tailSway * 0.5f,
                    tailX + bodyLen * 0.05f, y - bodyH * 0.08f + tailSway * 0.15f,
                    tailX, y
                )
                close()
            }
            drawPath(tailUp, FinDark)

            // Lower tail lobe
            val tailDown = paths.obtain().apply {
                moveTo(tailX, y)
                cubicTo(
                    tailX + bodyLen * 0.05f, y + bodyH * 0.08f + tailSway * 0.15f,
                    tailX + bodyLen * 0.10f, y + bodyH * 0.5f + tailSway * 0.5f,
                    tailX + bodyLen * 0.16f, y + bodyH * 1.05f + tailSway
                )
                cubicTo(
                    tailX + bodyLen * 0.12f, y + bodyH * 0.85f + tailSway * 0.8f,
                    tailX + bodyLen * 0.06f, y + bodyH * 0.4f + tailSway * 0.4f,
                    tailX, y + bodyH * 0.12f
                )
                close()
            }
            drawPath(tailDown, FinDark)

            // ── TALL DORSAL FIN (signature) ──────────────────────────────────
            val dorsalSway = sin(t * 2.5f * PI.toFloat()) * s * 0.3f

            val dorsalShadow = paths.obtain().apply {
                moveTo(x - bodyLen * 0.25f, y - bodyH * 0.88f)
                cubicTo(
                    x - bodyLen * 0.18f + dorsalSway, y - bodyH * 1.95f,
                    x - bodyLen * 0.02f + dorsalSway, y - bodyH * 1.90f,
                    x + bodyLen * 0.15f, y - bodyH * 0.85f
                )
                close()
            }
            drawPath(dorsalShadow, ShadowNavy.copy(alpha = 0.15f))

            val dorsal = paths.obtain().apply {
                moveTo(x - bodyLen * 0.23f, y - bodyH * 0.90f)
                cubicTo(
                    x - bodyLen * 0.16f + dorsalSway, y - bodyH * 1.90f,
                    x - bodyLen * 0.0f + dorsalSway, y - bodyH * 1.85f,
                    x + bodyLen * 0.13f, y - bodyH * 0.87f
                )
                cubicTo(
                    x + bodyLen * 0.02f, y - bodyH * 0.82f,
                    x - bodyLen * 0.10f, y - bodyH * 0.84f,
                    x - bodyLen * 0.23f, y - bodyH * 0.90f
                )
                close()
            }
            drawPath(dorsal, DorsalPurple)

            // Dorsal highlight
            val dorsalHL = paths.obtain().apply {
                moveTo(x - bodyLen * 0.18f, y - bodyH * 0.92f)
                cubicTo(
                    x - bodyLen * 0.12f + dorsalSway * 0.6f, y - bodyH * 1.55f,
                    x - bodyLen * 0.02f + dorsalSway * 0.6f, y - bodyH * 1.52f,
                    x + bodyLen * 0.08f, y - bodyH * 0.89f
                )
                close()
            }
            drawPath(dorsalHL, DorsalBlue.copy(alpha = 0.45f))

            // ── ANAL FIN ─────────────────────────────────────────────────────
            val analFin = paths.obtain().apply {
                moveTo(x + bodyLen * 0.12f, y + bodyH * 0.72f)
                cubicTo(
                    x + bodyLen * 0.18f, y + bodyH * 1.10f,
                    x + bodyLen * 0.25f, y + bodyH * 1.05f,
                    x + bodyLen * 0.30f, y + bodyH * 0.68f
                )
                cubicTo(
                    x + bodyLen * 0.25f, y + bodyH * 0.60f,
                    x + bodyLen * 0.18f, y + bodyH * 0.62f,
                    x + bodyLen * 0.12f, y + bodyH * 0.72f
                )
                close()
            }
            drawPath(analFin, FinDark)

            // ── PECTORAL FIN ─────────────────────────────────────────────────
            val pectSway = sin(t * 1.8f * PI.toFloat()) * s * 0.5f
            val pect = paths.obtain().apply {
                moveTo(x - bodyLen * 0.22f, y + bodyH * 0.08f)
                cubicTo(
                    x - bodyLen * 0.12f + pectSway, y + bodyH * 0.55f,
                    x - bodyLen * 0.02f + pectSway, y + bodyH * 0.60f,
                    x + bodyLen * 0.08f, y + bodyH * 0.32f
                )
                cubicTo(
                    x - bodyLen * 0.02f, y + bodyH * 0.20f,
                    x - bodyLen * 0.12f, y + bodyH * 0.12f,
                    x - bodyLen * 0.22f, y + bodyH * 0.08f
                )
                close()
            }
            drawPath(pect, FinDark.copy(alpha = 0.60f))

            // ── MAIN BODY (elongated, robust) ────────────────────────────────
            val bodyFrontX = x - bodyLen * 0.48f

            val bodyShadow = paths.obtain().apply {
                moveTo(bodyFrontX - s * 0.3f, y)
                cubicTo(
                    bodyFrontX + bodyLen * 0.08f, y - bodyH * 1.02f,
                    x + bodyLen * 0.12f, y - bodyH * 0.98f,
                    x + bodyLen * 0.47f, y
                )
                cubicTo(
                    x + bodyLen * 0.12f, y + bodyH * 0.98f,
                    bodyFrontX + bodyLen * 0.08f, y + bodyH * 1.02f,
                    bodyFrontX - s * 0.3f, y
                )
                close()
            }
            drawPath(bodyShadow, ShadowNavy.copy(alpha = 0.15f))

            val body = paths.obtain().apply {
                moveTo(bodyFrontX, y)
                cubicTo(
                    bodyFrontX + bodyLen * 0.08f, y - bodyH,
                    x + bodyLen * 0.10f, y - bodyH * 0.95f,
                    x + bodyLen * 0.45f, y
                )
                cubicTo(
                    x + bodyLen * 0.10f, y + bodyH * 0.95f,
                    bodyFrontX + bodyLen * 0.08f, y + bodyH,
                    bodyFrontX, y
                )
                close()
            }

            // Dark purple-blue back
            drawPath(body, BackDarkPurple)

            // Mid-body blue band
            val midBand = paths.obtain().apply {
                moveTo(bodyFrontX + bodyLen * 0.03f, y - bodyH * 0.30f)
                cubicTo(
                    bodyFrontX + bodyLen * 0.10f, y - bodyH * 0.75f,
                    x + bodyLen * 0.08f, y - bodyH * 0.72f,
                    x + bodyLen * 0.42f, y - bodyH * 0.10f
                )
                lineTo(x + bodyLen * 0.42f, y + bodyH * 0.10f)
                cubicTo(
                    x + bodyLen * 0.08f, y + bodyH * 0.72f,
                    bodyFrontX + bodyLen * 0.10f, y + bodyH * 0.75f,
                    bodyFrontX + bodyLen * 0.03f, y + bodyH * 0.30f
                )
                close()
            }
            drawPath(midBand, BackMidPurple.copy(alpha = 0.60f))

            // Silver sides
            val silverSide = paths.obtain().apply {
                moveTo(bodyFrontX + bodyLen * 0.05f, y - bodyH * 0.12f)
                cubicTo(
                    bodyFrontX + bodyLen * 0.12f, y - bodyH * 0.48f,
                    x + bodyLen * 0.06f, y - bodyH * 0.45f,
                    x + bodyLen * 0.40f, y - bodyH * 0.06f
                )
                lineTo(x + bodyLen * 0.40f, y + bodyH * 0.06f)
                cubicTo(
                    x + bodyLen * 0.06f, y + bodyH * 0.45f,
                    bodyFrontX + bodyLen * 0.12f, y + bodyH * 0.48f,
                    bodyFrontX + bodyLen * 0.05f, y + bodyH * 0.12f
                )
                close()
            }
            drawPath(silverSide, SideSilver.copy(alpha = 0.50f))

            // White belly
            val bellyPath = paths.obtain().apply {
                moveTo(bodyFrontX + bodyLen * 0.06f, y + bodyH * 0.05f)
                cubicTo(
                    bodyFrontX + bodyLen * 0.10f, y + bodyH * 0.78f,
                    x + bodyLen * 0.06f, y + bodyH * 0.75f,
                    x + bodyLen * 0.38f, y + bodyH * 0.04f
                )
                cubicTo(
                    x + bodyLen * 0.06f, y + bodyH * 0.50f,
                    bodyFrontX + bodyLen * 0.10f, y + bodyH * 0.52f,
                    bodyFrontX + bodyLen * 0.06f, y + bodyH * 0.05f
                )
                close()
            }
            drawPath(bellyPath, BellySilver.copy(alpha = 0.50f))
            drawPath(bellyPath, BellyWhite.copy(alpha = 0.20f))

            // Upper highlight
            val upperHL = paths.obtain().apply {
                moveTo(bodyFrontX + bodyLen * 0.06f, y - bodyH * 0.40f)
                cubicTo(
                    bodyFrontX + bodyLen * 0.12f, y - bodyH * 0.82f,
                    x + bodyLen * 0.05f, y - bodyH * 0.78f,
                    x + bodyLen * 0.32f, y - bodyH * 0.22f
                )
                cubicTo(
                    x + bodyLen * 0.05f, y - bodyH * 0.52f,
                    bodyFrontX + bodyLen * 0.12f, y - bodyH * 0.55f,
                    bodyFrontX + bodyLen * 0.06f, y - bodyH * 0.40f
                )
                close()
            }
            drawPath(upperHL, SideBright.copy(alpha = 0.22f))

            // Body outline
            drawPath(
                body, ShadowNavy.copy(alpha = 0.15f),
                style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f))
            )

            // ── BILL (long flat sword) ──────────────────────────────────────
            val billTip = bodyFrontX - billLen
            val billHalfW = bodyH * 0.08f

            // Bill shadow
            val billShadow = paths.obtain().apply {
                moveTo(bodyFrontX, y - billHalfW * 1.5f)
                lineTo(billTip - s * 0.3f, y)
                lineTo(bodyFrontX, y + billHalfW * 1.5f)
                close()
            }
            drawPath(billShadow, ShadowNavy.copy(alpha = 0.10f))

            // Main bill
            val bill = paths.obtain().apply {
                moveTo(bodyFrontX, y - billHalfW)
                cubicTo(
                    bodyFrontX - billLen * 0.3f, y - billHalfW * 0.8f,
                    bodyFrontX - billLen * 0.7f, y - billHalfW * 0.3f,
                    billTip, y
                )
                cubicTo(
                    bodyFrontX - billLen * 0.7f, y + billHalfW * 0.3f,
                    bodyFrontX - billLen * 0.3f, y + billHalfW * 0.8f,
                    bodyFrontX, y + billHalfW
                )
                close()
            }
            drawPath(bill, BillGray)

            // Bill upper highlight
            val billHL = paths.obtain().apply {
                moveTo(bodyFrontX - billLen * 0.05f, y - billHalfW * 0.5f)
                cubicTo(
                    bodyFrontX - billLen * 0.35f, y - billHalfW * 0.45f,
                    bodyFrontX - billLen * 0.65f, y - billHalfW * 0.15f,
                    billTip + billLen * 0.05f, y
                )
                cubicTo(
                    bodyFrontX - billLen * 0.65f, y - billHalfW * 0.05f,
                    bodyFrontX - billLen * 0.35f, y - billHalfW * 0.15f,
                    bodyFrontX - billLen * 0.05f, y - billHalfW * 0.15f
                )
                close()
            }
            drawPath(billHL, BillLight.copy(alpha = 0.45f))

            // Bill ridge line
            drawLine(
                BillDark.copy(alpha = 0.35f),
                Offset(bodyFrontX, y),
                Offset(billTip, y),
                strokeWidth = (0.5f * s).coerceAtLeast(0.25f),
                cap = StrokeCap.Round
            )

            // Bill outline
            drawPath(
                bill, BillDark.copy(alpha = 0.30f),
                style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f))
            )

            // ── LEVEL 3+: Sharper bill detail ───────────────────────────────
            if (level >= 3) {
                // Serrated edge texture
                val serrCount = 6
                for (i in 0 until serrCount) {
                    val frac = i.toFloat() / serrCount
                    val sx = bodyFrontX - billLen * (0.15f + frac * 0.7f)
                    val serrH = billHalfW * (0.5f - frac * 0.3f)
                    drawLine(
                        BillDark.copy(alpha = 0.20f),
                        Offset(sx, y - serrH),
                        Offset(sx, y + serrH),
                        strokeWidth = (0.3f * s).coerceAtLeast(0.15f)
                    )
                }
            }

            // ── EYE ──────────────────────────────────────────────────────────
            val eyeR = (1.5f + level * 0.08f) * s
            val eyeX = bodyFrontX + bodyLen * 0.06f
            val eyeY = y - bodyH * 0.28f

            drawCircle(ShadowNavy.copy(alpha = 0.30f), eyeR * 1.25f, Offset(eyeX, eyeY))
            drawCircle(Color(0xFFF5F5F0), eyeR, Offset(eyeX, eyeY))
            drawCircle(BackDarkPurple, eyeR * 0.60f, Offset(eyeX + eyeR * 0.06f, eyeY))
            drawCircle(ShadowNavy, eyeR * 0.35f, Offset(eyeX + eyeR * 0.08f, eyeY + eyeR * 0.02f))
            drawCircle(Color.White, eyeR * 0.18f, Offset(eyeX - eyeR * 0.18f, eyeY - eyeR * 0.18f))

            // ── GILL COVER ──────────────────────────────────────────────────
            val gill = paths.obtain().apply {
                moveTo(bodyFrontX + bodyLen * 0.05f, y - bodyH * 0.12f)
                cubicTo(
                    bodyFrontX + bodyLen * 0.10f, y - bodyH * 0.38f,
                    bodyFrontX + bodyLen * 0.12f, y + bodyH * 0.30f,
                    bodyFrontX + bodyLen * 0.06f, y + bodyH * 0.18f
                )
            }
            drawPath(
                gill, BackBlue.copy(alpha = 0.20f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round)
            )

            // ── LATERAL LINE ─────────────────────────────────────────────────
            drawLine(
                BackBlue.copy(alpha = 0.25f),
                Offset(bodyFrontX + bodyLen * 0.08f, y - bodyH * 0.05f),
                Offset(x + bodyLen * 0.38f, y - bodyH * 0.03f),
                strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                cap = StrokeCap.Round
            )

            // ── LEVEL 5+: Electric blue highlights on bill edge ─────────────
            if (level >= 5) {
                val elecPulse = (sin(t * 1.5f * PI.toFloat()) * 0.5f + 0.5f) * 0.35f

                // Glowing bill edge
                drawPath(
                    bill, ElectricBlue.copy(alpha = elecPulse),
                    style = Stroke(width = (1.2f * s).coerceAtLeast(0.5f))
                )

                // Electric glow at tip
                drawCircle(
                    ElectricCyan.copy(alpha = elecPulse * 0.6f),
                    2.0f * s,
                    Offset(billTip, y)
                )
                drawCircle(
                    ElectricBlue.copy(alpha = elecPulse * 0.3f),
                    4.0f * s,
                    Offset(billTip, y)
                )

                // Electric accents along bill
                for (i in 0 until 3) {
                    val sparkX = bodyFrontX - billLen * (0.3f + i * 0.2f)
                    val sparkAlpha = elecPulse * (1f - i * 0.2f)
                    drawCircle(
                        ElectricCyan.copy(alpha = sparkAlpha * 0.5f),
                        1.0f * s,
                        Offset(sparkX, y - billHalfW * 0.5f)
                    )
                }
            }

            } // bob transform
        }
    }
}
