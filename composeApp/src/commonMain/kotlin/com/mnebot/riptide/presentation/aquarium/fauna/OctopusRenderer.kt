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

// ── Palette ───────────────────────────────────────────────────────────────────
private val OctBody  = Color(0xFF8B2020)  // deep red-brown mantle
private val OctBody2 = Color(0xFFAA3030)  // lighter mantle highlight
private val OctArm   = Color(0xFF7A1818)  // arm color
private val OctSucker= Color(0xFFEE8866)  // sucker pads
private val OctEye   = Color(0xFF111122)
private val OctPupil = Color(0xFFFFAA00)  // W-shaped pupil hint
private val OctChrom = Color(0xFFCC6622)  // chromatophore spots

object OctopusRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val t = animTimeMs / 1000f

        val mantleR  = (6f + level * 0.4f) * s
        val armLen   = (9f + level * 0.6f) * s
        val armCount = 8

        // BURST: contract all arms inward, then spread
        val burstPhase = (animTimeMs % 1800L).toFloat() / 1800f
        val armSpread = 0.7f + sin(burstPhase * 2f * PI.toFloat()).coerceAtLeast(0f) * 0.30f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── ARMS (8) ─────────────────────────────────────────────────────
            for (i in 0 until armCount) {
                // Arms spread from 180° (rear left) to ~315° (lower right) in front
                val baseAngle = PI.toFloat() + i.toFloat() / (armCount - 1f) * 1.5f * PI.toFloat()
                val armSway = sin(t * 2.8f * PI.toFloat() + i * 0.8f) * armLen * 0.35f * armSpread
                val aLen = armLen * (0.75f + 0.25f * sin(i.toFloat() * 0.7f + 0.5f))

                val armBaseX = x + cos(baseAngle) * mantleR * 0.7f
                val armBaseY = y + sin(baseAngle) * mantleR * 0.7f

                val arm = paths.obtain().apply {
                    moveTo(armBaseX, armBaseY)
                    cubicTo(
                        armBaseX + cos(baseAngle) * aLen * 0.4f + armSway * 0.3f,
                        armBaseY + sin(baseAngle) * aLen * 0.4f + armSpread * mantleR * 0.4f,
                        armBaseX + cos(baseAngle) * aLen * 0.75f + armSway * 0.7f,
                        armBaseY + sin(baseAngle) * aLen * 0.7f + armSpread * mantleR * 0.3f,
                        armBaseX + cos(baseAngle) * aLen + armSway,
                        armBaseY + sin(baseAngle) * aLen * 0.9f
                    )
                }
                // Arm width tapers from thick at base to thin at tip
                val armW = (2.8f * s * (1f - i * 0.03f)).coerceAtLeast(0.8f)
                drawPath(arm, OctArm, style = Stroke(width = armW * 1.8f, cap = StrokeCap.Round))
                drawPath(arm, OctArm, style = Stroke(width = armW, cap = StrokeCap.Round))

                // ── SUCKERS (level 2+) ────────────────────────────────────────
                if (level >= 2) {
                    val suckerCount = 4 + level
                    for (sc in 1..suckerCount) {
                        val sf = sc.toFloat() / (suckerCount + 1f)
                        // Approximate point along the bezier: linear interpolation of control points
                        val sx = armBaseX + cos(baseAngle) * aLen * sf + armSway * sf * sf
                        val sy = armBaseY + sin(baseAngle) * aLen * sf * 0.9f + armSpread * mantleR * 0.35f * sf
                        val sR = (armW * 0.6f * (1f - sf * 0.5f)).coerceAtLeast(0.3f)
                        drawCircle(OctSucker.copy(alpha = 0.65f), sR, Offset(sx, sy))
                    }
                }
            }

            // ── MANTLE (rounded, slightly pointed at top) ─────────────────────
            val mantle = paths.obtain().apply {
                moveTo(x, y - mantleR * 1.35f)  // pointed top
                cubicTo(x + mantleR * 0.8f, y - mantleR * 1.30f,
                    x + mantleR * 1.1f, y - mantleR * 0.4f,
                    x + mantleR * 0.85f, y + mantleR * 0.3f)
                cubicTo(x + mantleR * 0.5f, y + mantleR * 0.7f,
                    x - mantleR * 0.5f, y + mantleR * 0.7f,
                    x - mantleR * 0.85f, y + mantleR * 0.3f)
                cubicTo(x - mantleR * 1.1f, y - mantleR * 0.4f,
                    x - mantleR * 0.8f, y - mantleR * 1.30f,
                    x, y - mantleR * 1.35f)
                close()
            }
            drawPath(mantle, OctBody)

            // Mantle highlight
            drawCircle(OctBody2.copy(alpha = 0.40f), mantleR * 0.55f, Offset(x - mantleR * 0.15f, y - mantleR * 0.55f))

            // ── CHROMATOPHORES (colour-change dots) ───────────────────────────
            val chromCount = 8 + level * 2
            val chromPulse = (sin(t * 1.8f * PI.toFloat()) * 0.5f + 0.5f) * 0.35f
            val chromPositions = listOf(
                Pair(0.0f, -0.7f), Pair(0.5f, -0.5f), Pair(-0.5f, -0.5f),
                Pair(0.65f, 0.0f), Pair(-0.65f, 0.0f), Pair(0.4f, 0.2f), Pair(-0.4f, 0.2f), Pair(0.0f, 0.1f)
            )
            for ((cx2, cy2) in chromPositions) {
                drawCircle(
                    OctChrom.copy(alpha = chromPulse + 0.15f),
                    (0.9f * s).coerceAtLeast(0.4f),
                    Offset(x + cx2 * mantleR, y + cy2 * mantleR)
                )
            }

            // Mantle outline
            drawPath(mantle, OctArm.copy(alpha = 0.30f),
                style = Stroke(width = (0.8f * s).coerceAtLeast(0.3f)))

            // ── EYES ──────────────────────────────────────────────────────────
            val eyeR = (1.6f + level * 0.10f) * s
            for (eyeXFrac in listOf(-0.48f, 0.48f)) {
                val eyeX = x + eyeXFrac * mantleR
                val eyeY = y - mantleR * 0.15f
                drawCircle(OctBody2, eyeR * 1.15f, Offset(eyeX, eyeY))
                drawCircle(OctEye, eyeR, Offset(eyeX, eyeY))
                drawCircle(OctPupil.copy(alpha = 0.40f), eyeR * 0.45f, Offset(eyeX, eyeY))
                drawCircle(Color.White, eyeR * 0.20f, Offset(eyeX - eyeR * 0.25f, eyeY - eyeR * 0.25f))
            }
        }
    }
}
