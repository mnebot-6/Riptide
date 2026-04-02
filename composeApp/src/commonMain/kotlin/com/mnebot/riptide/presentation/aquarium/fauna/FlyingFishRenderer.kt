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
import kotlin.math.cos

// ── Palette ─────────────────────────────────────────────────────────────────
private val BodyBlue       = Color(0xFF3060A8)  // blue body
private val BodyLightBlue  = Color(0xFF5088C8)  // lighter blue
private val BodyDarkBlue   = Color(0xFF1A3870)  // dark blue back
private val SideSilver     = Color(0xFFC8D0E0)  // silver sides
private val SideBright     = Color(0xFFE0E8F0)  // bright silver
private val BellyWhite     = Color(0xFFF5F5F5)  // white belly
private val WingBase       = Color(0xFF4878B8)  // wing base blue
private val WingMembrane   = Color(0xFF88B0D8)  // wing membrane translucent
private val WingIridescent = Color(0xFF70D0E8)  // iridescent shimmer
private val WingPurple     = Color(0xFF8080D0)  // purple iridescence
private val TailDark       = Color(0xFF1A3060)  // dark tail
private val ShadowNavy     = Color(0xFF0A1830)  // dark shadow
private val DropletBlue    = Color(0xFF80C0F0)  // water droplet

object FlyingFishRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyLen = (11f + level * 0.6f) * s
        val bodyH = (3.5f + level * 0.25f) * s
        val wingSpan = (12f + level * 0.8f) * s

        // Wing flap (gentle oscillation -- gliding more than flapping)
        val wingFlap = sin(t * 2.2f * PI.toFloat()) * 0.12f
        val tailSway = sin(t * 3.5f * PI.toFloat()) * 0.8f * s
        val glideY = sin(t * 1.2f * PI.toFloat()) * s * 0.06f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = glideY)
            }) {

            // ── TAIL FIN (asymmetric -- lower lobe longer for thrust) ────────
            val tailX = x + bodyLen * 0.48f

            val tailUpper = paths.obtain().apply {
                moveTo(tailX, y - bodyH * 0.1f)
                cubicTo(
                    tailX + bodyLen * 0.12f, y - bodyH * 0.5f + tailSway * 0.5f,
                    tailX + bodyLen * 0.22f, y - bodyH * 0.9f + tailSway,
                    tailX + bodyLen * 0.25f, y - bodyH * 1.0f + tailSway
                )
                cubicTo(
                    tailX + bodyLen * 0.18f, y - bodyH * 0.4f + tailSway * 0.3f,
                    tailX + bodyLen * 0.10f, y + tailSway * 0.1f,
                    tailX, y
                )
                close()
            }
            drawPath(tailUpper, TailDark)

            // Lower lobe (longer -- characteristic of flying fish)
            val tailLower = paths.obtain().apply {
                moveTo(tailX, y)
                cubicTo(
                    tailX + bodyLen * 0.10f, y + tailSway * 0.1f,
                    tailX + bodyLen * 0.20f, y + bodyH * 0.6f + tailSway * 0.4f,
                    tailX + bodyLen * 0.30f, y + bodyH * 1.4f + tailSway
                )
                cubicTo(
                    tailX + bodyLen * 0.22f, y + bodyH * 1.1f + tailSway * 0.8f,
                    tailX + bodyLen * 0.12f, y + bodyH * 0.5f + tailSway * 0.4f,
                    tailX, y + bodyH * 0.1f
                )
                close()
            }
            drawPath(tailLower, TailDark)

            // ── PECTORAL WINGS (huge, spread wide) ──────────────────────────
            // Upper wing surface angle follows wingFlap
            val wingAngle = -0.3f + wingFlap

            // Wing shadow (cast below)
            val wingShadowPath = paths.obtain().apply {
                moveTo(x - bodyLen * 0.25f, y - bodyH * 0.2f)
                cubicTo(
                    x - bodyLen * 0.15f + sin(wingAngle) * wingSpan * 0.1f,
                    y - bodyH * 0.5f + cos(wingAngle) * wingSpan * 0.15f,
                    x - bodyLen * 0.0f + sin(wingAngle) * wingSpan * 0.4f,
                    y - bodyH * 0.3f - cos(wingAngle) * wingSpan * 0.5f,
                    x + bodyLen * 0.1f + sin(wingAngle) * wingSpan * 0.3f,
                    y - bodyH * 0.1f - cos(wingAngle) * wingSpan
                )
                cubicTo(
                    x - bodyLen * 0.05f,
                    y - bodyH * 0.1f - cos(wingAngle) * wingSpan * 0.6f,
                    x - bodyLen * 0.2f,
                    y - bodyH * 0.3f,
                    x - bodyLen * 0.25f, y - bodyH * 0.2f
                )
                close()
            }
            drawPath(wingShadowPath, ShadowNavy.copy(alpha = 0.12f))

            // Main wing (upper pectoral)
            val wing = paths.obtain().apply {
                moveTo(x - bodyLen * 0.25f, y - bodyH * 0.25f)
                cubicTo(
                    x - bodyLen * 0.1f,
                    y - bodyH * 0.6f - cos(wingAngle) * wingSpan * 0.2f,
                    x + bodyLen * 0.05f,
                    y - bodyH * 0.4f - cos(wingAngle) * wingSpan * 0.6f,
                    x + bodyLen * 0.15f,
                    y - bodyH * 0.15f - cos(wingAngle) * wingSpan
                )
                cubicTo(
                    x + bodyLen * 0.1f,
                    y - bodyH * 0.3f - cos(wingAngle) * wingSpan * 0.65f,
                    x - bodyLen * 0.02f,
                    y - bodyH * 0.5f - cos(wingAngle) * wingSpan * 0.3f,
                    x - bodyLen * 0.18f, y - bodyH * 0.15f
                )
                close()
            }
            drawPath(wing, WingBase.copy(alpha = 0.50f))
            drawPath(wing, WingMembrane.copy(alpha = 0.35f))

            // Wing veins (fin rays)
            val veinCount = 6
            for (v in 0 until veinCount) {
                val vFrac = v.toFloat() / (veinCount - 1)
                val vStartX = x - bodyLen * 0.22f + bodyLen * vFrac * 0.3f
                val vStartY = y - bodyH * 0.22f - bodyH * vFrac * 0.1f
                val vEndFrac = 0.3f + vFrac * 0.7f
                val vEndX = x - bodyLen * 0.1f + bodyLen * vFrac * 0.25f +
                            sin(wingAngle) * wingSpan * vEndFrac * 0.15f
                val vEndY = y - bodyH * 0.2f - cos(wingAngle) * wingSpan * vEndFrac

                drawLine(
                    WingBase.copy(alpha = 0.30f),
                    Offset(vStartX, vStartY),
                    Offset(vEndX, vEndY),
                    strokeWidth = (0.4f * s).coerceAtLeast(0.2f),
                    cap = StrokeCap.Round
                )
            }

            // ── LEVEL 3+: Wing membrane iridescence ─────────────────────────
            if (level >= 3) {
                val iridPulse = (sin(t * 0.8f * PI.toFloat()) * 0.5f + 0.5f) * 0.20f
                drawPath(wing, WingIridescent.copy(alpha = iridPulse))
                drawPath(wing, WingPurple.copy(alpha = iridPulse * 0.5f))
            }

            // Wing outline
            drawPath(
                wing, WingBase.copy(alpha = 0.35f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f))
            )

            // ── PELVIC FINS (secondary "wings", smaller) ────────────────────
            val pelvicSpan = wingSpan * 0.45f
            val pelvicFlap = wingFlap * 0.6f

            val pelvic = paths.obtain().apply {
                moveTo(x + bodyLen * 0.05f, y + bodyH * 0.15f)
                cubicTo(
                    x + bodyLen * 0.12f,
                    y + bodyH * 0.3f + cos(pelvicFlap) * pelvicSpan * 0.2f,
                    x + bodyLen * 0.18f,
                    y + bodyH * 0.2f + cos(pelvicFlap) * pelvicSpan * 0.5f,
                    x + bodyLen * 0.22f,
                    y + bodyH * 0.1f + cos(pelvicFlap) * pelvicSpan
                )
                cubicTo(
                    x + bodyLen * 0.15f,
                    y + bodyH * 0.15f + cos(pelvicFlap) * pelvicSpan * 0.5f,
                    x + bodyLen * 0.10f,
                    y + bodyH * 0.2f + cos(pelvicFlap) * pelvicSpan * 0.15f,
                    x + bodyLen * 0.05f, y + bodyH * 0.10f
                )
                close()
            }
            drawPath(pelvic, WingMembrane.copy(alpha = 0.35f))

            // ── MAIN BODY (streamlined) ──────────────────────────────────────
            val headX = x - bodyLen * 0.55f

            val bodyShadow = paths.obtain().apply {
                moveTo(headX - s * 0.3f, y)
                cubicTo(
                    headX + bodyLen * 0.15f, y - bodyH * 1.02f,
                    x + bodyLen * 0.15f, y - bodyH * 0.82f,
                    tailX, y
                )
                cubicTo(
                    x + bodyLen * 0.15f, y + bodyH * 0.82f,
                    headX + bodyLen * 0.15f, y + bodyH * 1.02f,
                    headX - s * 0.3f, y
                )
                close()
            }
            drawPath(bodyShadow, ShadowNavy.copy(alpha = 0.12f))

            val body = paths.obtain().apply {
                moveTo(headX, y)
                cubicTo(
                    headX + bodyLen * 0.15f, y - bodyH,
                    x + bodyLen * 0.12f, y - bodyH * 0.8f,
                    tailX, y
                )
                cubicTo(
                    x + bodyLen * 0.12f, y + bodyH * 0.8f,
                    headX + bodyLen * 0.15f, y + bodyH,
                    headX, y
                )
                close()
            }
            drawPath(body, BodyBlue)

            // Silver sides
            val silverSide = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.05f, y - bodyH * 0.15f)
                cubicTo(
                    headX + bodyLen * 0.18f, y - bodyH * 0.5f,
                    x + bodyLen * 0.10f, y - bodyH * 0.45f,
                    tailX - bodyLen * 0.05f, y - bodyH * 0.05f
                )
                lineTo(tailX - bodyLen * 0.05f, y + bodyH * 0.05f)
                cubicTo(
                    x + bodyLen * 0.10f, y + bodyH * 0.45f,
                    headX + bodyLen * 0.18f, y + bodyH * 0.5f,
                    headX + bodyLen * 0.05f, y + bodyH * 0.15f
                )
                close()
            }
            drawPath(silverSide, SideSilver.copy(alpha = 0.55f))

            // White belly
            val belly = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.08f, y + bodyH * 0.05f)
                cubicTo(
                    headX + bodyLen * 0.15f, y + bodyH * 0.75f,
                    x + bodyLen * 0.08f, y + bodyH * 0.65f,
                    tailX - bodyLen * 0.1f, y + bodyH * 0.03f
                )
                cubicTo(
                    x + bodyLen * 0.08f, y + bodyH * 0.45f,
                    headX + bodyLen * 0.15f, y + bodyH * 0.50f,
                    headX + bodyLen * 0.08f, y + bodyH * 0.05f
                )
                close()
            }
            drawPath(belly, BellyWhite.copy(alpha = 0.50f))

            // Upper highlight
            val upperHL = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.08f, y - bodyH * 0.35f)
                cubicTo(
                    headX + bodyLen * 0.18f, y - bodyH * 0.80f,
                    x + bodyLen * 0.05f, y - bodyH * 0.65f,
                    x + bodyLen * 0.25f, y - bodyH * 0.15f
                )
                cubicTo(
                    x + bodyLen * 0.05f, y - bodyH * 0.40f,
                    headX + bodyLen * 0.15f, y - bodyH * 0.55f,
                    headX + bodyLen * 0.08f, y - bodyH * 0.35f
                )
                close()
            }
            drawPath(upperHL, SideBright.copy(alpha = 0.25f))

            // Body outline
            drawPath(
                body, ShadowNavy.copy(alpha = 0.15f),
                style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f))
            )

            // ── EYE (large, characteristic of flying fish) ──────────────────
            val eyeR = (1.6f + level * 0.10f) * s
            val eyeX = headX + bodyLen * 0.10f
            val eyeY = y - bodyH * 0.25f

            drawCircle(ShadowNavy.copy(alpha = 0.30f), eyeR * 1.25f, Offset(eyeX, eyeY))
            drawCircle(Color(0xFFF8F8F5), eyeR, Offset(eyeX, eyeY))
            drawCircle(BodyDarkBlue, eyeR * 0.60f, Offset(eyeX + eyeR * 0.06f, eyeY))
            drawCircle(ShadowNavy, eyeR * 0.35f, Offset(eyeX + eyeR * 0.08f, eyeY + eyeR * 0.02f))
            drawCircle(Color.White, eyeR * 0.20f, Offset(eyeX - eyeR * 0.18f, eyeY - eyeR * 0.18f))
            drawCircle(Color.White.copy(alpha = 0.5f), eyeR * 0.10f,
                Offset(eyeX + eyeR * 0.12f, eyeY + eyeR * 0.10f))

            // ── LATERAL LINE ─────────────────────────────────────────────────
            drawLine(
                BodyLightBlue.copy(alpha = 0.30f),
                Offset(headX + bodyLen * 0.12f, y - bodyH * 0.05f),
                Offset(tailX - bodyLen * 0.05f, y - bodyH * 0.03f),
                strokeWidth = (0.5f * s).coerceAtLeast(0.25f),
                cap = StrokeCap.Round
            )

            // ── LEVEL 5+: Water droplets falling from wings ─────────────────
            if (level >= 5) {
                val dropCount = 5
                for (d in 0 until dropCount) {
                    val dropPhase = (t * 0.8f + d * 1.3f) % 3.0f
                    if (dropPhase < 2.0f) {
                        val dropFall = dropPhase / 2.0f  // 0..1
                        val dropAlpha = (1f - dropFall) * 0.40f
                        val dropX = x - bodyLen * 0.1f + d * bodyLen * 0.08f +
                                    sin(wingAngle) * wingSpan * (0.2f + d * 0.1f) * 0.1f
                        val dropStartY = y - bodyH * 0.3f - cos(wingAngle) * wingSpan * (0.3f + d * 0.12f)
                        val dropY = dropStartY + dropFall * bodyH * 2.5f
                        val dropR = (0.5f + (1f - dropFall) * 0.3f) * s

                        drawCircle(DropletBlue.copy(alpha = dropAlpha), dropR * 2.0f, Offset(dropX, dropY))
                        drawCircle(Color.White.copy(alpha = dropAlpha * 0.5f), dropR * 0.8f, Offset(dropX, dropY))
                    }
                }
            }

            } // glide transform
        }
    }
}
