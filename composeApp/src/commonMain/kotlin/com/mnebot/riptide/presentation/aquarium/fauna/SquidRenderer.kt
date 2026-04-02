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

// ── Palette ───────────────────────────────────────────────────────────────────
private val SqBody  = Color(0xFFCC4466)  // reddish-pink mantle
private val SqBody2 = Color(0xFFEE6688)  // lighter mantle
private val SqBelly = Color(0xFFFFCCDD)  // pale semi-transparent belly
private val SqFin   = Color(0xFFAA3355)  // lateral fin
private val SqArm   = Color(0xFFBB3355)  // arms
private val SqTent  = Color(0xFF993344)  // longer tentacles
private val SqChrom = Color(0xFFEE4477)  // chromatophore stripes
private val SqEye   = Color(0xFF112233)

object SquidRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val t = animTimeMs / 1000f

        // BURST movement: squid jets backward rapidly
        val burstPhase = (animTimeMs % 1400L).toFloat() / 1400f
        val jetTension = sin(burstPhase * 2f * PI.toFloat()).coerceAtLeast(0f)

        val mantleLen = (13f + level * 0.7f) * s
        val mantleH   = (4f  + level * 0.25f) * s
        val armLen    = (5f  + level * 0.35f) * s
        val tentLen   = (9f  + level * 0.6f) * s

        val tailSway = sin(t * 4.5f * PI.toFloat()) * 1.5f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── ARMS (8 short) — extend forward from head ──────────────────────
            val armSpread = 0.9f - jetTension * 0.3f  // close arms during jet
            for (i in 0 until 8) {
                val tFrac = (i.toFloat() - 3.5f) / 3.5f
                val armBase = x - mantleLen * 0.5f
                val armX = armBase - armLen * (0.6f + armSpread * 0.4f)
                val armYBase = y + tFrac * mantleH * armSpread
                drawLine(
                    SqArm.copy(alpha = 0.80f),
                    Offset(armBase, armYBase),
                    Offset(armX + tailSway * 0.3f * tFrac, armYBase),
                    strokeWidth = (1.2f * s * (1f - kotlin.math.abs(tFrac) * 0.3f)).coerceAtLeast(0.5f),
                    cap = StrokeCap.Round
                )
            }

            // ── TENTACLES (2 long, with clubs at tips) ─────────────────────────
            for (sign in listOf(-1f, 1f)) {
                val tentSway = sin(t * 3.5f * PI.toFloat() + sign * 0.5f) * tentLen * 0.3f
                val tentBase = x - mantleLen * 0.5f
                val tent = paths.obtain().apply {
                    moveTo(tentBase, y + sign * mantleH * 0.2f)
                    cubicTo(
                        tentBase - tentLen * 0.3f, y + sign * mantleH * 0.15f + tentSway * 0.3f,
                        tentBase - tentLen * 0.7f, y + sign * mantleH * 0.25f + tentSway * 0.7f,
                        tentBase - tentLen + tentSway, y + sign * mantleH * 0.3f
                    )
                }
                // Arm shaft
                drawPath(tent, SqTent.copy(alpha = 0.75f),
                    style = Stroke(width = (1.0f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round))
                // Club at tip (wider oval)
                val clubX = tentBase - tentLen + tentSway
                val clubY = y + sign * mantleH * 0.3f
                val clubR = (1.5f + level * 0.1f) * s
                drawCircle(SqTent.copy(alpha = 0.85f), clubR, Offset(clubX, clubY))
            }

            // ── LATERAL FINS (diamond shape, near tail) ────────────────────────
            val finX = x + mantleLen * 0.25f
            val finW = mantleLen * 0.30f
            val finH = mantleH * 1.4f
            val finL = paths.obtain().apply {
                moveTo(finX - finW * 0.3f, y)
                lineTo(finX + finW * 0.5f, y - finH)
                lineTo(finX + finW, y)
                lineTo(finX + finW * 0.5f, y + finH)
                close()
            }
            drawPath(finL, SqFin.copy(alpha = 0.75f))
            drawPath(finL, SqBody.copy(alpha = 0.25f),
                style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))

            // ── MANTLE (torpedo, pointed at tail) ─────────────────────────────
            val mantlePath = paths.obtain().apply {
                moveTo(x - mantleLen * 0.5f, y - mantleH * 0.05f)  // head end
                cubicTo(x - mantleLen * 0.3f, y - mantleH,
                    x + mantleLen * 0.2f, y - mantleH * 0.85f,
                    x + mantleLen * 0.5f, y - mantleH * 0.15f + tailSway * 0.15f)
                lineTo(x + mantleLen * 0.5f, y + mantleH * 0.15f + tailSway * 0.15f)
                cubicTo(x + mantleLen * 0.2f, y + mantleH * 0.85f,
                    x - mantleLen * 0.3f, y + mantleH,
                    x - mantleLen * 0.5f, y + mantleH * 0.05f)
                close()
            }
            drawPath(mantlePath, SqBody)

            // Belly highlight
            val belly = paths.obtain().apply {
                moveTo(x - mantleLen * 0.45f, y - mantleH * 0.02f)
                cubicTo(x - mantleLen * 0.2f, y - mantleH * 0.85f,
                    x + mantleLen * 0.15f, y - mantleH * 0.72f,
                    x + mantleLen * 0.45f, y - mantleH * 0.12f + tailSway * 0.1f)
                cubicTo(x + mantleLen * 0.15f, y - mantleH * 0.42f,
                    x - mantleLen * 0.1f, y - mantleH * 0.48f,
                    x - mantleLen * 0.45f, y - mantleH * 0.02f)
                close()
            }
            drawPath(belly, SqBelly.copy(alpha = 0.30f))

            // ── LEVEL 3+: Photophores (bioluminescent ventral dots) ──────────────
            if (level >= 3) {
                val glow = (sin(t * 2.2f * PI.toFloat()) * 0.5f + 0.5f) * 0.40f + 0.20f
                val photoPositions = listOf(-0.30f, -0.12f, 0.08f, 0.26f)
                for (px in photoPositions) {
                    drawCircle(Color(0xFFAAEEFF).copy(alpha = glow),
                        (1.0f * s).coerceAtLeast(0.4f),
                        Offset(x + px * mantleLen, y + mantleH * 0.72f))
                }
            }

            // ── LEVEL 5+: Luminescent edge on lateral fins ────────────────────────
            if (level >= 5) {
                val finGlow = (sin(t * 1.5f * PI.toFloat()) * 0.5f + 0.5f) * 0.30f + 0.18f
                val finX = x + mantleLen * 0.25f
                drawLine(Color(0xFF88CCFF).copy(alpha = finGlow),
                    Offset(finX + mantleLen * 0.30f, y),
                    Offset(finX, y - mantleH * 1.40f),
                    strokeWidth = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round)
                drawLine(Color(0xFF88CCFF).copy(alpha = finGlow),
                    Offset(finX + mantleLen * 0.30f, y),
                    Offset(finX, y + mantleH * 1.40f),
                    strokeWidth = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round)
            }

            // ── CHROMATOPHORE STRIPES ─────────────────────────────────────────
            val stripeCount = 5 + level
            val chromAnim = (sin(t * 2.5f * PI.toFloat()) * 0.5f + 0.5f) * 0.30f
            for (i in 0 until stripeCount) {
                val sf = (i.toFloat() - stripeCount / 2f) / stripeCount.toFloat()
                val stripeX = x + sf * mantleLen * 0.7f
                drawLine(
                    SqChrom.copy(alpha = chromAnim + 0.10f),
                    Offset(stripeX, y - mantleH * 0.7f),
                    Offset(stripeX + mantleLen * 0.05f, y + mantleH * 0.7f),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.3f)
                )
            }

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.4f + level * 0.08f) * s
            val eyeX = x - mantleLen * 0.40f
            val eyeY = y - mantleH * 0.28f
            drawCircle(SqBelly, eyeR * 1.1f, Offset(eyeX, eyeY))
            drawCircle(SqEye, eyeR * 0.70f, Offset(eyeX, eyeY))
            drawCircle(Color.White, eyeR * 0.22f, Offset(eyeX - eyeR * 0.22f, eyeY - eyeR * 0.22f))
        }
    }
}
