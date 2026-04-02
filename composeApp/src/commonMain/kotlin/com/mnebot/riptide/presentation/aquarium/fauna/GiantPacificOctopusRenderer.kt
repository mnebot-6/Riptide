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
private val BodyRust        = Color(0xFFC05030)  // main reddish-brown
private val BodyDarkRust    = Color(0xFF8B3420)  // darker rust shadow
private val BodyOrange      = Color(0xFFD87048)  // warm orange highlight
private val BodyPaleOrange  = Color(0xFFE89878)  // pale highlight
private val SpotBrown       = Color(0xFF6E2818)  // texture spots
private val SpotLight       = Color(0xFFD89068)  // lighter spots
private val SuckerCream     = Color(0xFFF0D8C0)  // sucker color
private val SuckerDark      = Color(0xFFA86848)  // sucker ring
private val ShadowDeep      = Color(0xFF401810)  // deep shadow
private val InkBlack        = Color(0xFF180808)  // ink cloud
private val EyeOrange       = Color(0xFFE0A060)  // iris color
private val MantleHighlight = Color(0xFFF0B888)  // mantle top highlight

object GiantPacificOctopusRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val mantleR = (9f + level * 0.7f) * s
        val armLen = (14f + level * 1.0f) * s

        // Mantle breathing
        val breathe = sin(t * 1.0f * PI.toFloat()) * 0.04f
        val mantleRDyn = mantleR * (1f + breathe)

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── LEVEL 5+: INK CLOUD TRAIL ────────────────────────────────
            if (level >= 5) {
                val inkPeriod = 10000L
                val inkPhase = (animTimeMs % inkPeriod).toFloat() / 1000f
                if (inkPhase < 2.5f) {
                    val progress = inkPhase / 2.5f
                    val alpha = (1f - progress) * 0.35f
                    for (p in 0 until 8) {
                        val drift = sin(p * 1.3f + t * 0.5f) * armLen * 0.12f
                        val px = x + armLen * 0.3f + drift + progress * armLen * 0.5f
                        val py = y + mantleR * 0.4f + cos(p * 0.9f) * armLen * 0.08f +
                                progress * armLen * 0.2f
                        val pr = (2.5f + p * 0.3f) * s * (1f - progress * 0.4f)
                        drawCircle(InkBlack.copy(alpha = alpha), pr.coerceAtLeast(0.5f),
                            Offset(px, py))
                    }
                }
            }

            // ── ARMS (8, spread out with independent undulation) ─────────
            val armAngles = floatArrayOf(
                -2.6f, -2.0f, -1.4f, -0.8f, 0.8f, 1.4f, 2.0f, 2.6f
            )
            for ((idx, baseAngle) in armAngles.withIndex()) {
                val phase = t * 1.5f * PI.toFloat() + idx * 0.7f
                // Multi-joint wave: base -> mid -> tip
                val wave1 = sin(phase) * armLen * 0.06f
                val wave2 = sin(phase + 0.6f) * armLen * 0.14f
                val wave3 = sin(phase + 1.2f) * armLen * 0.25f

                val perpX = -sin(baseAngle)
                val perpY = cos(baseAngle)

                val bx = x + cos(baseAngle) * mantleRDyn * 0.6f
                val by = y + sin(baseAngle) * mantleRDyn * 0.6f + mantleR * 0.4f

                val cp1x = x + cos(baseAngle) * armLen * 0.3f + perpX * wave1
                val cp1y = y + sin(baseAngle) * armLen * 0.3f + perpY * wave1 + mantleR * 0.5f
                val cp2x = x + cos(baseAngle) * armLen * 0.6f + perpX * wave2
                val cp2y = y + sin(baseAngle) * armLen * 0.6f + perpY * wave2 + mantleR * 0.6f
                val tipX = x + cos(baseAngle) * armLen + perpX * wave3
                val tipY = y + sin(baseAngle) * armLen + perpY * wave3 + mantleR * 0.7f

                val arm = paths.obtain().apply {
                    moveTo(bx, by)
                    cubicTo(cp1x, cp1y, cp2x, cp2y, tipX, tipY)
                }

                // Arm thickness decreases from base to tip
                val baseWidth = (3.5f * s * (1f - idx * 0.02f)).coerceAtLeast(1.5f)
                // Shadow
                drawPath(arm, ShadowDeep.copy(alpha = 0.20f),
                    style = Stroke(width = baseWidth * 1.3f, cap = StrokeCap.Round))
                // Main arm
                drawPath(arm, BodyRust.copy(alpha = 0.85f),
                    style = Stroke(width = baseWidth, cap = StrokeCap.Round))
                // Highlight
                drawPath(arm, BodyOrange.copy(alpha = 0.35f),
                    style = Stroke(width = baseWidth * 0.45f, cap = StrokeCap.Round))

                // Suckers along arm
                val suckerCount = 4
                for (sk in 1..suckerCount) {
                    val frac = sk.toFloat() / (suckerCount + 1)
                    val skPhase = phase + frac * 1.2f
                    val skX = bx * (1f - frac) + tipX * frac +
                            perpX * sin(skPhase) * armLen * frac * 0.10f
                    val skY = by * (1f - frac) + tipY * frac +
                            perpY * sin(skPhase) * armLen * frac * 0.10f
                    val suckerR = (1.0f - frac * 0.5f) * s
                    drawCircle(SuckerCream.copy(alpha = 0.55f), suckerR, Offset(skX, skY))
                    drawCircle(SuckerDark.copy(alpha = 0.30f), suckerR * 0.55f, Offset(skX, skY))
                }
            }

            // ── MANTLE SHADOW ────────────────────────────────────────────
            drawCircle(ShadowDeep.copy(alpha = 0.18f), mantleRDyn * 1.08f,
                Offset(x + mantleR * 0.03f, y + mantleR * 0.04f))

            // ── MANTLE (large dome) ──────────────────────────────────────
            val mantlePath = paths.obtain().apply {
                moveTo(x - mantleRDyn * 0.9f, y + mantleR * 0.2f)
                cubicTo(x - mantleRDyn * 0.9f, y - mantleRDyn * 0.9f,
                    x + mantleRDyn * 0.9f, y - mantleRDyn * 0.9f,
                    x + mantleRDyn * 0.9f, y + mantleR * 0.2f)
                cubicTo(x + mantleRDyn * 0.7f, y + mantleR * 0.5f,
                    x - mantleRDyn * 0.7f, y + mantleR * 0.5f,
                    x - mantleRDyn * 0.9f, y + mantleR * 0.2f)
                close()
            }
            drawPath(mantlePath, BodyRust)

            // Lower mantle shading
            val mantleLower = paths.obtain().apply {
                moveTo(x - mantleRDyn * 0.85f, y)
                cubicTo(x - mantleRDyn * 0.6f, y + mantleR * 0.45f,
                    x + mantleRDyn * 0.6f, y + mantleR * 0.45f,
                    x + mantleRDyn * 0.85f, y)
                cubicTo(x + mantleRDyn * 0.6f, y + mantleR * 0.3f,
                    x - mantleRDyn * 0.6f, y + mantleR * 0.3f,
                    x - mantleRDyn * 0.85f, y)
                close()
            }
            drawPath(mantleLower, BodyDarkRust.copy(alpha = 0.40f))

            // Upper highlight
            drawCircle(MantleHighlight.copy(alpha = 0.25f), mantleRDyn * 0.45f,
                Offset(x - mantleR * 0.1f, y - mantleR * 0.35f))

            // ── TEXTURE SPOTS ────────────────────────────────────────────
            val spotPositions = listOf(
                Pair(0.25f, -0.40f), Pair(-0.30f, -0.35f),
                Pair(0.45f, -0.10f), Pair(-0.50f, -0.05f),
                Pair(0.15f,  0.15f), Pair(-0.20f,  0.20f),
                Pair(0.55f, -0.30f), Pair(-0.60f, -0.25f),
                Pair(0.35f,  0.05f), Pair(-0.40f,  0.10f),
                Pair(0.10f, -0.55f), Pair(-0.15f, -0.50f),
            )
            for ((sx, sy) in spotPositions) {
                val spotX = x + sx * mantleRDyn
                val spotY = y + sy * mantleR
                drawCircle(SpotBrown.copy(alpha = 0.22f), mantleR * 0.08f,
                    Offset(spotX, spotY))
            }

            // ── LEVEL 3+: CHROMATOPHORE COLOR-CHANGING SPOTS ─────────────
            if (level >= 3) {
                for ((idx, pos) in spotPositions.take(8).withIndex()) {
                    val pulse = sin(t * 2.5f * PI.toFloat() + idx * 0.8f)
                    val chromAlpha = (0.15f + pulse * 0.15f).coerceIn(0.05f, 0.30f)
                    val spotX = x + pos.first * mantleRDyn
                    val spotY = y + pos.second * mantleR
                    drawCircle(SpotLight.copy(alpha = chromAlpha), mantleR * 0.10f,
                        Offset(spotX, spotY))
                    drawCircle(BodyPaleOrange.copy(alpha = chromAlpha * 0.6f),
                        mantleR * 0.06f, Offset(spotX, spotY))
                }
            }

            // ── WEB BETWEEN ARMS (inter-arm membrane) ────────────────────
            val webPath = paths.obtain().apply {
                val wbx1 = x - mantleRDyn * 0.5f
                val wby  = y + mantleR * 0.55f
                val wbx2 = x + mantleRDyn * 0.5f
                moveTo(wbx1, wby)
                cubicTo(x - mantleRDyn * 0.2f, wby + mantleR * 0.3f,
                    x + mantleRDyn * 0.2f, wby + mantleR * 0.3f,
                    wbx2, wby)
                cubicTo(x + mantleRDyn * 0.15f, wby + mantleR * 0.15f,
                    x - mantleRDyn * 0.15f, wby + mantleR * 0.15f,
                    wbx1, wby)
                close()
            }
            drawPath(webPath, BodyDarkRust.copy(alpha = 0.25f))

            // ── EYES (large, expressive) ─────────────────────────────────
            val eyeR = (2.0f + level * 0.12f) * s
            val eyeSpacing = mantleRDyn * 0.30f

            for (side in listOf(-1f, 1f)) {
                val eyeX = x + side * eyeSpacing
                val eyeY = y - mantleR * 0.10f

                // Eye socket
                drawCircle(ShadowDeep.copy(alpha = 0.35f), eyeR * 1.35f, Offset(eyeX, eyeY))
                // Sclera
                drawCircle(Color(0xFFF8F0E8), eyeR, Offset(eyeX, eyeY))
                // Iris
                drawCircle(EyeOrange, eyeR * 0.65f, Offset(eyeX + eyeR * 0.05f * side, eyeY))
                // Pupil (horizontal bar, octopus-style)
                val pupilPath = paths.obtain().apply {
                    val px = eyeX + eyeR * 0.06f * side
                    val py = eyeY
                    moveTo(px - eyeR * 0.30f, py)
                    cubicTo(px - eyeR * 0.30f, py - eyeR * 0.08f,
                        px + eyeR * 0.30f, py - eyeR * 0.08f,
                        px + eyeR * 0.30f, py)
                    cubicTo(px + eyeR * 0.30f, py + eyeR * 0.08f,
                        px - eyeR * 0.30f, py + eyeR * 0.08f,
                        px - eyeR * 0.30f, py)
                    close()
                }
                drawPath(pupilPath, Color(0xFF1A0A05))
                // Shine
                drawCircle(Color.White, eyeR * 0.20f,
                    Offset(eyeX - eyeR * 0.18f, eyeY - eyeR * 0.18f))
                drawCircle(Color.White.copy(alpha = 0.5f), eyeR * 0.10f,
                    Offset(eyeX + eyeR * 0.12f, eyeY + eyeR * 0.10f))
            }
        }
    }
}
