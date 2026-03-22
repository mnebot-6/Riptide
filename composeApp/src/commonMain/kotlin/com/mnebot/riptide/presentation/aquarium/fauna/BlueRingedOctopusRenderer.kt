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

// ── Palette ───────────────────────────────────────────────────────────────────
private val BROBody = Color(0xFFBB9020)  // yellow-ochre
private val BROArm  = Color(0xFFAA8015)  // arms
private val BRORing = Color(0xFF00B8FF)  // electric blue ring
private val BROCore = Color(0xFFAAEEFF)  // ring core (lighter)
private val BROEye  = Color(0xFFDD9900)  // eye

// Ring positions: (x_offset_fraction_of_bodyR, y_offset_fraction, radius_fraction)
private val ringPositions = listOf(
    Triple( 0.20f,  -0.50f, 0.22f),  // on mantle top-right
    Triple(-0.30f,  -0.55f, 0.20f),  // on mantle top-left
    Triple( 0.55f,  -0.15f, 0.19f),  // right side
    Triple(-0.60f,  -0.10f, 0.18f),  // left side
    Triple( 0.30f,   0.45f, 0.20f),  // lower right
    Triple(-0.25f,   0.50f, 0.21f),  // lower left
    Triple( 0.0f,   -0.75f, 0.17f),  // top
    Triple( 0.0f,    0.70f, 0.17f),  // bottom mantle edge
    // Arm rings (positions beyond mantle)
    Triple( 0.90f,  -0.30f, 0.16f),
    Triple(-0.95f,  -0.25f, 0.15f),
    Triple( 1.05f,   0.25f, 0.16f),
    Triple(-1.10f,   0.30f, 0.15f),
    Triple( 0.70f,   0.75f, 0.14f),
    Triple(-0.75f,   0.70f, 0.14f),
    Triple( 0.30f,   1.10f, 0.15f),
    Triple(-0.30f,   1.15f, 0.14f),
)

object BlueRingedOctopusRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyR  = (7f + level * 0.5f) * s
        val armLen = (8f + level * 0.6f) * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ════════════════════════════════════════════════════════════════
            // ARMS — 8 arms, shorter octopus style, with suckers
            // ════════════════════════════════════════════════════════════════
            val armAngles = floatArrayOf(
                -2.4f, -1.8f, -1.2f, -0.7f, 0.7f, 1.2f, 1.8f, 2.4f
            )
            for ((idx, baseAngle) in armAngles.withIndex()) {
                val armSway  = sin(t * 1.8f * PI.toFloat() + idx * 0.6f) * 0.15f
                val angle    = baseAngle + armSway
                val midX     = x + kotlin.math.cos(angle) * armLen * 0.5f
                val midY     = y + kotlin.math.sin(angle) * armLen * 0.5f + bodyR * 0.6f
                val tipX     = x + kotlin.math.cos(angle + armSway * 0.5f) * armLen
                val tipY     = y + kotlin.math.sin(angle + armSway * 0.5f) * armLen + bodyR * 0.8f

                val arm = Path().apply {
                    moveTo(x + kotlin.math.cos(angle) * bodyR * 0.7f,
                        y + kotlin.math.sin(angle) * bodyR * 0.7f + bodyR * 0.3f)
                    cubicTo(midX * 0.8f + x * 0.2f, midY * 0.8f + y * 0.2f,
                        midX, midY, tipX, tipY)
                }
                drawPath(arm, BROArm, style = Stroke(
                    width = (1.8f * s * (1f - idx.toFloat() * 0.03f)).coerceAtLeast(0.7f),
                    cap = StrokeCap.Round))

                // 3-4 suckers along each arm
                val suckerCount = 3
                for (sk in 1..suckerCount) {
                    val skFrac = sk.toFloat() / (suckerCount + 1)
                    val skX = x + kotlin.math.cos(angle) * armLen * skFrac
                    val skY = y + kotlin.math.sin(angle) * armLen * skFrac + bodyR * 0.4f * skFrac
                    drawCircle(BROBody.copy(alpha = 0.50f), 0.55f * s, Offset(skX, skY))
                }
            }

            // ════════════════════════════════════════════════════════════════
            // MANTLE (body)
            // ════════════════════════════════════════════════════════════════
            drawCircle(BROBody, bodyR, Offset(x, y))

            // Subtle body texture
            drawCircle(BROBody.copy(alpha = 0.25f), bodyR * 0.65f, Offset(x - bodyR * 0.15f, y - bodyR * 0.15f))

            // ════════════════════════════════════════════════════════════════
            // BLUE RINGS — pulsing iridescent rings
            // ════════════════════════════════════════════════════════════════
            for ((idx, ring) in ringPositions.withIndex()) {
                val (xFrac, yFrac, rFrac) = ring
                val pulse = sin(t * 2f * PI.toFloat() + idx * 0.5f)
                val ringAlpha = (0.55f + pulse * 0.30f).coerceIn(0.25f, 0.90f)
                val ringScale = 1f + pulse * 0.12f
                val ringR = rFrac * bodyR * ringScale
                val ringX = x + xFrac * bodyR
                val ringY = y + yFrac * bodyR

                // Outer ring (electric blue)
                drawCircle(BRORing.copy(alpha = ringAlpha), ringR,
                    Offset(ringX, ringY),
                    style = Stroke(width = (1.0f * s).coerceAtLeast(0.5f)))
                // Inner core (lighter)
                drawCircle(BROCore.copy(alpha = ringAlpha * 0.7f), ringR * 0.5f,
                    Offset(ringX, ringY))
            }

            // ════════════════════════════════════════════════════════════════
            // EYES
            // ════════════════════════════════════════════════════════════════
            val eyeR  = (1.6f + level * 0.1f) * s
            val eyeX  = x - bodyR * 0.28f
            val eyeY  = y - bodyR * 0.30f
            drawCircle(Color.White.copy(alpha = 0.85f), eyeR, Offset(eyeX, eyeY))
            drawCircle(BROEye, eyeR * 0.72f, Offset(eyeX, eyeY))
            drawCircle(Color(0xFF000000), eyeR * 0.40f, Offset(eyeX + eyeR * 0.08f, eyeY))
        }
    }
}
