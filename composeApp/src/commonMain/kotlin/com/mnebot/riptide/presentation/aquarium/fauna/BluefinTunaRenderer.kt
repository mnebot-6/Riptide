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
private val BackDarkBlue    = Color(0xFF1A2848)  // dark metallic blue back
private val BackMidBlue     = Color(0xFF2A4878)  // mid-tone blue
private val BackSteelBlue   = Color(0xFF3A68A0)  // steel blue highlight
private val SideSilver      = Color(0xFFC0C8D8)  // silver sides
private val SideBright      = Color(0xFFD8E0E8)  // bright silver
private val BellyWhite      = Color(0xFFF0F0F0)  // white belly
private val BellyPure       = Color(0xFFFAFAFA)  // pure white highlight
private val FinDarkBlue     = Color(0xFF182040)  // dark blue fins
private val FinYellow       = Color(0xFFE8C840)  // yellow finlets
private val FinYellowLight  = Color(0xFFF0D860)  // lighter yellow
private val ShadowNavy      = Color(0xFF0A1428)  // dark shadow
private val MetallicSheen   = Color(0xFF80A0D0)  // metallic sheen
private val SpeedBlue       = Color(0xFF4080D0)  // speed line effect

object BluefinTunaRenderer : CreatureRenderer {
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

        // Fast tail oscillation (tuna are speed machines)
        val tailFreq = 5.0f + level * 0.3f
        val tailSway = sin(t * tailFreq * PI.toFloat()) * 1.5f * s
        val bodyBob = sin(t * 2.5f * PI.toFloat()) * 0.05f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = bodyBob)
            }) {

            // ── CRESCENT TAIL FIN ────────────────────────────────────────────
            val tailX = x + bodyLen * 0.45f

            val tailShadow = paths.obtain().apply {
                moveTo(tailX, y)
                cubicTo(
                    tailX + bodyLen * 0.10f, y - bodyH * 0.3f + tailSway * 0.3f,
                    tailX + bodyLen * 0.18f, y - bodyH * 1.0f + tailSway * 0.7f,
                    tailX + bodyLen * 0.22f, y - bodyH * 1.3f + tailSway
                )
                cubicTo(
                    tailX + bodyLen * 0.15f, y - bodyH * 0.5f + tailSway * 0.4f,
                    tailX + bodyLen * 0.15f, y + bodyH * 0.5f + tailSway * 0.4f,
                    tailX + bodyLen * 0.22f, y + bodyH * 1.3f + tailSway
                )
                cubicTo(
                    tailX + bodyLen * 0.18f, y + bodyH * 1.0f + tailSway * 0.7f,
                    tailX + bodyLen * 0.10f, y + bodyH * 0.3f + tailSway * 0.3f,
                    tailX, y
                )
                close()
            }
            drawPath(tailShadow, ShadowNavy.copy(alpha = 0.20f))

            // Upper crescent
            val tailUpper = paths.obtain().apply {
                moveTo(tailX, y - bodyH * 0.15f)
                cubicTo(
                    tailX + bodyLen * 0.08f, y - bodyH * 0.4f + tailSway * 0.4f,
                    tailX + bodyLen * 0.16f, y - bodyH * 0.95f + tailSway * 0.8f,
                    tailX + bodyLen * 0.20f, y - bodyH * 1.25f + tailSway
                )
                cubicTo(
                    tailX + bodyLen * 0.13f, y - bodyH * 0.6f + tailSway * 0.5f,
                    tailX + bodyLen * 0.06f, y - bodyH * 0.1f + tailSway * 0.2f,
                    tailX, y
                )
                close()
            }
            drawPath(tailUpper, FinDarkBlue)

            // Lower crescent
            val tailLower = paths.obtain().apply {
                moveTo(tailX, y)
                cubicTo(
                    tailX + bodyLen * 0.06f, y + bodyH * 0.1f + tailSway * 0.2f,
                    tailX + bodyLen * 0.13f, y + bodyH * 0.6f + tailSway * 0.5f,
                    tailX + bodyLen * 0.20f, y + bodyH * 1.25f + tailSway
                )
                cubicTo(
                    tailX + bodyLen * 0.16f, y + bodyH * 0.95f + tailSway * 0.8f,
                    tailX + bodyLen * 0.08f, y + bodyH * 0.4f + tailSway * 0.4f,
                    tailX, y + bodyH * 0.15f
                )
                close()
            }
            drawPath(tailLower, FinDarkBlue)

            // ── YELLOW FINLETS (series behind dorsal/anal) ──────────────────
            val finletCount = 5
            for (i in 0 until finletCount) {
                val fx = tailX - bodyLen * 0.05f - i * bodyLen * 0.04f
                val finletSway = sin(t * tailFreq * PI.toFloat() + i * 0.3f) * s * 0.3f
                // Dorsal finlets
                drawLine(
                    FinYellow.copy(alpha = 0.70f),
                    Offset(fx, y - bodyH * (0.45f - i * 0.04f)),
                    Offset(fx + s * 0.5f + finletSway, y - bodyH * (0.60f - i * 0.03f)),
                    strokeWidth = (1.0f * s).coerceAtLeast(0.5f),
                    cap = StrokeCap.Round
                )
                // Ventral finlets
                drawLine(
                    FinYellow.copy(alpha = 0.60f),
                    Offset(fx, y + bodyH * (0.40f - i * 0.03f)),
                    Offset(fx + s * 0.5f + finletSway, y + bodyH * (0.55f - i * 0.02f)),
                    strokeWidth = (0.9f * s).coerceAtLeast(0.4f),
                    cap = StrokeCap.Round
                )
            }

            // ── FIRST DORSAL FIN ─────────────────────────────────────────────
            val dorsal1 = paths.obtain().apply {
                moveTo(x - bodyLen * 0.15f, y - bodyH * 0.88f)
                cubicTo(
                    x - bodyLen * 0.05f, y - bodyH * 1.45f,
                    x + bodyLen * 0.08f, y - bodyH * 1.40f,
                    x + bodyLen * 0.14f, y - bodyH * 0.85f
                )
                cubicTo(
                    x + bodyLen * 0.06f, y - bodyH * 0.80f,
                    x - bodyLen * 0.02f, y - bodyH * 0.82f,
                    x - bodyLen * 0.15f, y - bodyH * 0.88f
                )
                close()
            }
            drawPath(dorsal1, FinDarkBlue)
            drawPath(dorsal1, BackMidBlue.copy(alpha = 0.4f))

            // ── SECOND DORSAL FIN (smaller) ──────────────────────────────────
            val dorsal2 = paths.obtain().apply {
                moveTo(x + bodyLen * 0.15f, y - bodyH * 0.75f)
                cubicTo(
                    x + bodyLen * 0.20f, y - bodyH * 1.05f,
                    x + bodyLen * 0.27f, y - bodyH * 1.00f,
                    x + bodyLen * 0.30f, y - bodyH * 0.72f
                )
                close()
            }
            drawPath(dorsal2, FinDarkBlue)

            // ── ANAL FIN ─────────────────────────────────────────────────────
            val anal = paths.obtain().apply {
                moveTo(x + bodyLen * 0.12f, y + bodyH * 0.72f)
                cubicTo(
                    x + bodyLen * 0.18f, y + bodyH * 1.05f,
                    x + bodyLen * 0.25f, y + bodyH * 1.00f,
                    x + bodyLen * 0.30f, y + bodyH * 0.68f
                )
                close()
            }
            drawPath(anal, FinDarkBlue)

            // ── PECTORAL FIN ─────────────────────────────────────────────────
            val pectSway = sin(t * 2.0f * PI.toFloat()) * s * 0.4f
            val pect = paths.obtain().apply {
                moveTo(x - bodyLen * 0.35f, y + bodyH * 0.1f)
                cubicTo(
                    x - bodyLen * 0.25f + pectSway, y + bodyH * 0.55f,
                    x - bodyLen * 0.15f + pectSway, y + bodyH * 0.60f,
                    x - bodyLen * 0.08f, y + bodyH * 0.35f
                )
                cubicTo(
                    x - bodyLen * 0.18f, y + bodyH * 0.22f,
                    x - bodyLen * 0.28f, y + bodyH * 0.15f,
                    x - bodyLen * 0.35f, y + bodyH * 0.1f
                )
                close()
            }
            drawPath(pect, FinDarkBlue.copy(alpha = 0.65f))

            // ── MAIN BODY (torpedo shape) ────────────────────────────────────
            val headX = x - bodyLen * 0.55f

            val bodyShadow = paths.obtain().apply {
                moveTo(headX - s * 0.5f, y)
                cubicTo(
                    headX + bodyLen * 0.1f, y - bodyH * 1.02f,
                    x + bodyLen * 0.1f, y - bodyH * 1.02f,
                    x + bodyLen * 0.47f, y
                )
                cubicTo(
                    x + bodyLen * 0.1f, y + bodyH * 1.02f,
                    headX + bodyLen * 0.1f, y + bodyH * 1.02f,
                    headX - s * 0.5f, y
                )
                close()
            }
            drawPath(bodyShadow, ShadowNavy.copy(alpha = 0.15f))

            val body = paths.obtain().apply {
                moveTo(headX, y)
                cubicTo(
                    headX + bodyLen * 0.1f, y - bodyH,
                    x + bodyLen * 0.08f, y - bodyH * 0.98f,
                    x + bodyLen * 0.45f, y
                )
                cubicTo(
                    x + bodyLen * 0.08f, y + bodyH * 0.98f,
                    headX + bodyLen * 0.1f, y + bodyH,
                    headX, y
                )
                close()
            }

            // Dark blue back (upper half)
            drawPath(body, BackDarkBlue)

            // Silver side band
            val silverBand = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.03f, y - bodyH * 0.15f)
                cubicTo(
                    headX + bodyLen * 0.15f, y - bodyH * 0.45f,
                    x + bodyLen * 0.10f, y - bodyH * 0.42f,
                    x + bodyLen * 0.42f, y - bodyH * 0.08f
                )
                lineTo(x + bodyLen * 0.42f, y + bodyH * 0.08f)
                cubicTo(
                    x + bodyLen * 0.10f, y + bodyH * 0.42f,
                    headX + bodyLen * 0.15f, y + bodyH * 0.45f,
                    headX + bodyLen * 0.03f, y + bodyH * 0.15f
                )
                close()
            }
            drawPath(silverBand, SideSilver)

            // White belly
            val bellyPath = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.05f, y + bodyH * 0.08f)
                cubicTo(
                    headX + bodyLen * 0.12f, y + bodyH * 0.85f,
                    x + bodyLen * 0.08f, y + bodyH * 0.82f,
                    x + bodyLen * 0.40f, y + bodyH * 0.05f
                )
                cubicTo(
                    x + bodyLen * 0.08f, y + bodyH * 0.55f,
                    headX + bodyLen * 0.12f, y + bodyH * 0.58f,
                    headX + bodyLen * 0.05f, y + bodyH * 0.08f
                )
                close()
            }
            drawPath(bellyPath, BellyWhite.copy(alpha = 0.70f))
            drawPath(bellyPath, BellyPure.copy(alpha = 0.25f))

            // Bright silver upper accent
            val silverHL = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.08f, y - bodyH * 0.35f)
                cubicTo(
                    headX + bodyLen * 0.18f, y - bodyH * 0.75f,
                    x + bodyLen * 0.05f, y - bodyH * 0.72f,
                    x + bodyLen * 0.35f, y - bodyH * 0.20f
                )
                cubicTo(
                    x + bodyLen * 0.05f, y - bodyH * 0.48f,
                    headX + bodyLen * 0.15f, y - bodyH * 0.52f,
                    headX + bodyLen * 0.08f, y - bodyH * 0.35f
                )
                close()
            }
            drawPath(silverHL, SideBright.copy(alpha = 0.30f))

            // ── LEVEL 3+: Metallic sheen ────────────────────────────────────
            if (level >= 3) {
                val sheen = (sin(t * 1.0f * PI.toFloat()) * 0.5f + 0.5f) * 0.20f
                drawPath(silverBand, MetallicSheen.copy(alpha = sheen))

                // More defined lateral line
                drawLine(
                    BackSteelBlue.copy(alpha = 0.40f),
                    Offset(headX + bodyLen * 0.10f, y - bodyH * 0.05f),
                    Offset(x + bodyLen * 0.38f, y - bodyH * 0.03f),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.4f),
                    cap = StrokeCap.Round
                )
            }

            // Body outline
            drawPath(
                body, ShadowNavy.copy(alpha = 0.18f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f))
            )

            // ── EYE ──────────────────────────────────────────────────────────
            val eyeR = (1.5f + level * 0.08f) * s
            val eyeX = headX + bodyLen * 0.08f
            val eyeY = y - bodyH * 0.22f

            drawCircle(ShadowNavy.copy(alpha = 0.30f), eyeR * 1.25f, Offset(eyeX, eyeY))
            drawCircle(Color(0xFFF5F5F0), eyeR, Offset(eyeX, eyeY))
            drawCircle(BackDarkBlue, eyeR * 0.60f, Offset(eyeX + eyeR * 0.06f, eyeY))
            drawCircle(ShadowNavy, eyeR * 0.35f, Offset(eyeX + eyeR * 0.08f, eyeY + eyeR * 0.02f))
            drawCircle(Color.White, eyeR * 0.18f, Offset(eyeX - eyeR * 0.18f, eyeY - eyeR * 0.18f))

            // ── GILL COVER ──────────────────────────────────────────────────
            val gill = paths.obtain().apply {
                moveTo(headX + bodyLen * 0.06f, y - bodyH * 0.15f)
                cubicTo(
                    headX + bodyLen * 0.12f, y - bodyH * 0.42f,
                    headX + bodyLen * 0.14f, y + bodyH * 0.32f,
                    headX + bodyLen * 0.08f, y + bodyH * 0.20f
                )
            }
            drawPath(
                gill, BackMidBlue.copy(alpha = 0.25f),
                style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
            )

            // ── LEVEL 5+: Speed line effect (faint streaks behind) ──────────
            if (level >= 5) {
                val speedAlpha = (sin(t * 3.0f * PI.toFloat()) * 0.5f + 0.5f) * 0.18f
                val streakCount = 4
                for (i in 0 until streakCount) {
                    val sy = y - bodyH * 0.4f + i * bodyH * 0.25f
                    val streakLen = bodyLen * (0.3f + sin(t * 2f + i.toFloat()) * 0.1f)

                    drawLine(
                        SpeedBlue.copy(alpha = speedAlpha * (1f - i * 0.15f)),
                        Offset(x + bodyLen * 0.48f, sy),
                        Offset(x + bodyLen * 0.48f + streakLen, sy),
                        strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                        cap = StrokeCap.Round
                    )
                }
            }

            } // bob transform
        }
    }
}
