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

// ── Palette (from Recraft reference SVG) ─────────────────────────────────────
private val BodyCoral      = Color(0xFFF7835A)  // coral-orange body
private val BodyDeepCoral  = Color(0xFFDB6A47)  // deeper coral
private val RingBlue       = Color(0xFF020BD0)  // vivid electric blue rings
private val RingDarkBlue   = Color(0xFF03087E)  // dark blue ring shadow
private val ShadowTeal     = Color(0xFF086781)  // dark teal shadow
private val ShadowNavy     = Color(0xFF102746)  // dark navy
private val ShadowBlack    = Color(0xFF19181B)  // near-black
private val AccentSage     = Color(0xFF81B7A0)  // sage green accent
private val AccentDkSage   = Color(0xFF70A08F)  // darker sage
private val AccentTealGrn  = Color(0xFF3E817A)  // teal-green
private val ShadowDkTeal   = Color(0xFF064264)  // dark blue-teal

// Ring positions: (x_offset_fraction_of_bodyR, y_offset_fraction, radius_fraction)
private val ringPositions = listOf(
    Triple( 0.20f,  -0.50f, 0.22f),
    Triple(-0.30f,  -0.55f, 0.20f),
    Triple( 0.55f,  -0.15f, 0.19f),
    Triple(-0.60f,  -0.10f, 0.18f),
    Triple( 0.30f,   0.45f, 0.20f),
    Triple(-0.25f,   0.50f, 0.21f),
    Triple( 0.0f,   -0.75f, 0.17f),
    Triple( 0.0f,    0.70f, 0.17f),
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

            // ── ARMS (8, with shadow + suckers) ──────────────────────────────
            val armAngles = floatArrayOf(
                -2.4f, -1.8f, -1.2f, -0.7f, 0.7f, 1.2f, 1.8f, 2.4f
            )
            for ((idx, baseAngle) in armAngles.withIndex()) {
                val armSway = sin(t * 1.8f * PI.toFloat() + idx * 0.6f) * 0.15f
                val angle   = baseAngle + armSway
                val midX    = x + cos(angle) * armLen * 0.5f
                val midY    = y + sin(angle) * armLen * 0.5f + bodyR * 0.6f
                val tipX    = x + cos(angle + armSway * 0.5f) * armLen
                val tipY    = y + sin(angle + armSway * 0.5f) * armLen + bodyR * 0.8f

                val armBaseX = x + cos(angle) * bodyR * 0.7f
                val armBaseY = y + sin(angle) * bodyR * 0.7f + bodyR * 0.3f

                val arm = Path().apply {
                    moveTo(armBaseX, armBaseY)
                    cubicTo(midX * 0.8f + x * 0.2f, midY * 0.8f + y * 0.2f,
                        midX, midY, tipX, tipY)
                }

                // Arm shadow
                drawPath(arm, ShadowTeal.copy(alpha = 0.25f),
                    style = Stroke(width = (3.0f * s * (1f - idx * 0.03f)).coerceAtLeast(1.2f),
                        cap = StrokeCap.Round))
                // Arm main
                drawPath(arm, BodyDeepCoral.copy(alpha = 0.85f),
                    style = Stroke(width = (1.8f * s * (1f - idx * 0.03f)).coerceAtLeast(0.7f),
                        cap = StrokeCap.Round))
                // Arm highlight
                drawPath(arm, BodyCoral.copy(alpha = 0.4f),
                    style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f),
                        cap = StrokeCap.Round))

                // Suckers (sage green)
                val suckerCount = 3
                for (sk in 1..suckerCount) {
                    val skFrac = sk.toFloat() / (suckerCount + 1)
                    val skX = x + cos(angle) * armLen * skFrac
                    val skY = y + sin(angle) * armLen * skFrac + bodyR * 0.4f * skFrac
                    drawCircle(AccentSage.copy(alpha = 0.45f), 0.65f * s, Offset(skX, skY))
                    drawCircle(AccentDkSage.copy(alpha = 0.30f), 0.35f * s, Offset(skX, skY))
                }
            }

            // ── MANTLE SHADOW ────────────────────────────────────────────────
            drawCircle(ShadowNavy.copy(alpha = 0.20f), bodyR * 1.06f, Offset(x + bodyR * 0.03f, y + bodyR * 0.04f))

            // ── MANTLE (body, coral-orange) ──────────────────────────────────
            drawCircle(BodyCoral, bodyR, Offset(x, y))

            // Body shading (darker lower half)
            val mantleLower = Path().apply {
                moveTo(x - bodyR, y)
                cubicTo(x - bodyR, y + bodyR * 0.5f, x - bodyR * 0.7f, y + bodyR, x, y + bodyR)
                cubicTo(x + bodyR * 0.7f, y + bodyR, x + bodyR, y + bodyR * 0.5f, x + bodyR, y)
                close()
            }
            drawPath(mantleLower, BodyDeepCoral.copy(alpha = 0.35f))

            // Highlight (upper mantle)
            drawCircle(BodyCoral.copy(alpha = 0.3f), bodyR * 0.55f,
                Offset(x - bodyR * 0.15f, y - bodyR * 0.25f))

            // Teal accent shimmer
            drawCircle(AccentTealGrn.copy(alpha = 0.12f), bodyR * 0.7f,
                Offset(x + bodyR * 0.1f, y - bodyR * 0.1f))

            // ── BLUE RINGS (pulsing) ─────────────────────────────────────────
            for ((idx, ring) in ringPositions.withIndex()) {
                val (xFrac, yFrac, rFrac) = ring
                val pulse = sin(t * 2f * PI.toFloat() + idx * 0.5f)
                val ringAlpha = (0.55f + pulse * 0.30f).coerceIn(0.25f, 0.90f)
                val ringScale = 1f + pulse * 0.12f
                val ringR = rFrac * bodyR * ringScale
                val ringX = x + xFrac * bodyR
                val ringY = y + yFrac * bodyR

                // Ring glow
                drawCircle(RingBlue.copy(alpha = ringAlpha * 0.3f), ringR * 1.4f,
                    Offset(ringX, ringY))
                // Outer ring (vivid blue)
                drawCircle(RingBlue.copy(alpha = ringAlpha), ringR,
                    Offset(ringX, ringY),
                    style = Stroke(width = (1.2f * s).coerceAtLeast(0.5f)))
                // Inner dark fill
                drawCircle(RingDarkBlue.copy(alpha = ringAlpha * 0.5f), ringR * 0.55f,
                    Offset(ringX, ringY))
                // Inner bright center
                drawCircle(RingBlue.copy(alpha = ringAlpha * 0.6f), ringR * 0.3f,
                    Offset(ringX, ringY))
            }

            // ── EYES (detailed) ──────────────────────────────────────────────
            val eyeR = (1.6f + level * 0.1f) * s
            val eyeX = x - bodyR * 0.28f
            val eyeY = y - bodyR * 0.30f

            // Eye socket shadow
            drawCircle(ShadowNavy.copy(alpha = 0.35f), eyeR * 1.3f, Offset(eyeX, eyeY))
            // Sclera
            drawCircle(Color(0xFFFEFEFE), eyeR, Offset(eyeX, eyeY))
            // Iris (sage-teal)
            drawCircle(AccentTealGrn, eyeR * 0.70f, Offset(eyeX + eyeR * 0.04f, eyeY))
            // Pupil
            drawCircle(ShadowBlack, eyeR * 0.40f, Offset(eyeX + eyeR * 0.06f, eyeY + eyeR * 0.02f))
            // Shine
            drawCircle(Color.White, eyeR * 0.20f, Offset(eyeX - eyeR * 0.18f, eyeY - eyeR * 0.18f))
            drawCircle(Color.White.copy(alpha = 0.5f), eyeR * 0.10f,
                Offset(eyeX + eyeR * 0.12f, eyeY + eyeR * 0.10f))
        }
    }
}
