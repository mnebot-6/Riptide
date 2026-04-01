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
private val ShellBrown     = Color(0xFF5A4A30)  // dark brown carapace
private val ShellOlive     = Color(0xFF6B6038)  // olive-brown shell
private val ShellDark      = Color(0xFF3A3020)  // darkest shell shadow
private val ShellLight     = Color(0xFF8A7A58)  // lighter shell highlight
private val ShellTan       = Color(0xFF9A8A60)  // tan ridge highlight
private val BellyPale      = Color(0xFFB8A880)  // underside pale
private val TelsonBrown    = Color(0xFF4A3A22)  // tail spike brown
private val TelsonDark     = Color(0xFF2A2012)  // tail tip dark
private val LegOlive       = Color(0xFF6A5A38)  // leg color
private val EyeBlack       = Color(0xFF1A1210)  // compound eye
private val ShadowNavy     = Color(0xFF0A0A18)  // shadow
private val ShadowOlive    = Color(0xFF2A2A10)  // olive shadow
private val PatternTan     = Color(0xFF7A6A48)  // shell pattern (level 3+)
private val AmberGlow      = Color(0xFFFFB040)  // amber glow (level 5+)

object HorseshoeCrabRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val shellW  = (10f + level * 0.6f) * s   // prosoma width
        val shellH  = (8f + level * 0.5f) * s    // prosoma front-to-back length
        val abdW    = (7f + level * 0.4f) * s    // opisthosoma width
        val abdH    = (5f + level * 0.3f) * s    // opisthosoma length
        val telsonL = (8f + level * 0.5f) * s    // tail length

        // Slow plodding movement
        val legPhase = t * 2.5f * PI.toFloat()
        val bodyBob  = sin(t * 1.5f * PI.toFloat()) * s * 0.1f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = bodyBob)
            }) {

            // ── TELSON (long pointed tail) ──────────────────────────────────
            val telsonBaseX = x + shellH * 0.2f + abdH * 0.8f
            val telsonSway  = sin(t * 0.8f * PI.toFloat()) * s * 0.5f

            val telsonPath = Path().apply {
                moveTo(telsonBaseX, y - abdW * 0.08f)
                cubicTo(
                    telsonBaseX + telsonL * 0.3f, y - s * 0.5f + telsonSway * 0.2f,
                    telsonBaseX + telsonL * 0.7f, y + telsonSway * 0.5f,
                    telsonBaseX + telsonL, y + telsonSway
                )
                cubicTo(
                    telsonBaseX + telsonL * 0.7f, y + telsonSway * 0.5f + s * 0.3f,
                    telsonBaseX + telsonL * 0.3f, y + s * 0.5f + telsonSway * 0.2f,
                    telsonBaseX, y + abdW * 0.08f
                )
                close()
            }
            drawPath(telsonPath, TelsonBrown)
            // Telson ridge highlight
            drawLine(
                TelsonDark.copy(alpha = 0.50f),
                Offset(telsonBaseX, y),
                Offset(telsonBaseX + telsonL * 0.95f, y + telsonSway * 0.95f),
                strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                cap = StrokeCap.Round
            )
            // Telson darker tip
            drawCircle(TelsonDark, s * 0.5f,
                Offset(telsonBaseX + telsonL, y + telsonSway))

            // ── LEGS (subtle movement underneath) ───────────────────────────
            val legCount = 5
            for (i in 0 until legCount) {
                val legBaseX = x - shellH * 0.15f + i * shellH * 0.18f
                val legSwing = sin(legPhase + i * 0.8f) * s * 1.0f

                // Upper legs (between body and shell)
                for (side in intArrayOf(-1, 1)) {
                    val legTipX = legBaseX + legSwing
                    val legTipY = y + side * (shellW * 0.45f + s * 1.5f)
                    // Leg shadow
                    drawLine(
                        ShadowOlive.copy(alpha = 0.20f),
                        Offset(legBaseX, y + side * shellW * 0.2f),
                        Offset(legTipX + s * 0.2f, legTipY + s * 0.2f),
                        strokeWidth = (0.8f * s).coerceAtLeast(0.4f),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        LegOlive.copy(alpha = 0.55f),
                        Offset(legBaseX, y + side * shellW * 0.2f),
                        Offset(legTipX, legTipY),
                        strokeWidth = (0.5f * s).coerceAtLeast(0.25f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // ── OPISTHOSOMA (rear section, trapezoidal + marginal spines) ───
            val abdBaseX = x + shellH * 0.15f
            val abdPath = Path().apply {
                moveTo(abdBaseX, y - abdW * 0.5f)
                cubicTo(
                    abdBaseX + abdH * 0.3f, y - abdW * 0.48f,
                    abdBaseX + abdH * 0.7f, y - abdW * 0.3f,
                    abdBaseX + abdH, y - abdW * 0.1f
                )
                lineTo(abdBaseX + abdH, y + abdW * 0.1f)
                cubicTo(
                    abdBaseX + abdH * 0.7f, y + abdW * 0.3f,
                    abdBaseX + abdH * 0.3f, y + abdW * 0.48f,
                    abdBaseX, y + abdW * 0.5f
                )
                close()
            }
            drawPath(abdPath, ShellOlive)
            drawPath(abdPath, ShellDark.copy(alpha = 0.25f))

            // Marginal spines on opisthosoma
            for (side in intArrayOf(-1, 1)) {
                for (i in 0 until 3) {
                    val spineX = abdBaseX + abdH * 0.25f + i * abdH * 0.25f
                    val spineY = y + side * (abdW * 0.35f + i * abdW * 0.02f)
                    drawLine(
                        ShellDark.copy(alpha = 0.40f),
                        Offset(spineX, spineY),
                        Offset(spineX + s * 0.8f, spineY + side * s * 0.8f),
                        strokeWidth = (0.4f * s).coerceAtLeast(0.2f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Opisthosoma ridge
            drawLine(
                ShellTan.copy(alpha = 0.30f),
                Offset(abdBaseX + abdH * 0.1f, y),
                Offset(abdBaseX + abdH * 0.9f, y),
                strokeWidth = (0.7f * s).coerceAtLeast(0.3f),
                cap = StrokeCap.Round
            )

            // ── PROSOMA (front dome-shaped carapace) ────────────────────────
            val prosomaPath = Path().apply {
                moveTo(x - shellH * 0.5f, y)
                cubicTo(
                    x - shellH * 0.5f, y - shellW * 0.6f,
                    x - shellH * 0.1f, y - shellW * 0.58f,
                    x + shellH * 0.2f, y - shellW * 0.5f
                )
                lineTo(x + shellH * 0.2f, y + shellW * 0.5f)
                cubicTo(
                    x - shellH * 0.1f, y + shellW * 0.58f,
                    x - shellH * 0.5f, y + shellW * 0.6f,
                    x - shellH * 0.5f, y
                )
                close()
            }

            // Shell shadow
            drawPath(prosomaPath, ShadowOlive.copy(alpha = 0.20f))

            // Shell main
            drawPath(prosomaPath, ShellBrown)

            // Central dome highlight
            val domeHL = Path().apply {
                moveTo(x - shellH * 0.35f, y - shellW * 0.15f)
                cubicTo(
                    x - shellH * 0.30f, y - shellW * 0.40f,
                    x, y - shellW * 0.38f,
                    x + shellH * 0.10f, y - shellW * 0.20f
                )
                cubicTo(
                    x - shellH * 0.05f, y - shellW * 0.25f,
                    x - shellH * 0.20f, y - shellW * 0.22f,
                    x - shellH * 0.35f, y - shellW * 0.15f
                )
                close()
            }
            drawPath(domeHL, ShellLight.copy(alpha = 0.35f))

            // Lower dome highlight (symmetric)
            val domeLowHL = Path().apply {
                moveTo(x - shellH * 0.35f, y + shellW * 0.15f)
                cubicTo(
                    x - shellH * 0.30f, y + shellW * 0.40f,
                    x, y + shellW * 0.38f,
                    x + shellH * 0.10f, y + shellW * 0.20f
                )
                cubicTo(
                    x - shellH * 0.05f, y + shellW * 0.25f,
                    x - shellH * 0.20f, y + shellW * 0.22f,
                    x - shellH * 0.35f, y + shellW * 0.15f
                )
                close()
            }
            drawPath(domeLowHL, ShellLight.copy(alpha = 0.20f))

            // Median ridge line
            drawLine(
                ShellTan.copy(alpha = 0.35f),
                Offset(x - shellH * 0.40f, y),
                Offset(x + shellH * 0.18f, y),
                strokeWidth = (0.8f * s).coerceAtLeast(0.4f),
                cap = StrokeCap.Round
            )

            // Lateral ridges
            for (side in intArrayOf(-1, 1)) {
                val ridgePath = Path().apply {
                    moveTo(x - shellH * 0.30f, y + side * shellW * 0.15f)
                    cubicTo(
                        x - shellH * 0.15f, y + side * shellW * 0.30f,
                        x + shellH * 0.05f, y + side * shellW * 0.32f,
                        x + shellH * 0.18f, y + side * shellW * 0.28f
                    )
                }
                drawPath(ridgePath, ShellTan.copy(alpha = 0.25f),
                    style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round))
            }

            // Shell outline
            drawPath(prosomaPath, ShellDark.copy(alpha = 0.20f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f)))

            // ── COMPOUND EYES (lateral, on prosoma) ─────────────────────────
            val eyeR = (1.0f + level * 0.06f) * s
            for (side in intArrayOf(-1, 1)) {
                val eyeX = x - shellH * 0.15f
                val eyeY = y + side * shellW * 0.28f
                drawCircle(ShadowNavy.copy(alpha = 0.25f), eyeR * 1.15f,
                    Offset(eyeX + s * 0.1f, eyeY + side * s * 0.1f))
                drawCircle(EyeBlack, eyeR, Offset(eyeX, eyeY))
                drawCircle(ShellLight.copy(alpha = 0.25f), eyeR * 0.3f,
                    Offset(eyeX - eyeR * 0.2f, eyeY - side * eyeR * 0.2f))
            }

            // Median eyes (small, near front center)
            drawCircle(EyeBlack.copy(alpha = 0.50f), s * 0.4f,
                Offset(x - shellH * 0.35f, y - shellW * 0.05f))
            drawCircle(EyeBlack.copy(alpha = 0.50f), s * 0.4f,
                Offset(x - shellH * 0.35f, y + shellW * 0.05f))

            // ── LEVEL 3+: Faint shell pattern ───────────────────────────────
            if (level >= 3) {
                // Concentric growth rings on prosoma
                for (i in 1..3) {
                    val ringScale = 0.3f + i * 0.18f
                    val ringPath = Path().apply {
                        moveTo(x - shellH * 0.5f * ringScale, y)
                        cubicTo(
                            x - shellH * 0.5f * ringScale, y - shellW * 0.6f * ringScale,
                            x - shellH * 0.1f * ringScale, y - shellW * 0.58f * ringScale,
                            x + shellH * 0.2f * ringScale, y - shellW * 0.5f * ringScale
                        )
                    }
                    drawPath(ringPath, PatternTan.copy(alpha = 0.20f),
                        style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))
                }
            }

            // ── LEVEL 5+: Ancient amber glow ────────────────────────────────
            if (level >= 5) {
                val glowAlpha = (sin(t * 0.8f * PI.toFloat()) * 0.5f + 0.5f) * 0.15f
                drawPath(prosomaPath, AmberGlow.copy(alpha = glowAlpha))
                // Amber glow on edges
                drawPath(prosomaPath, AmberGlow.copy(alpha = glowAlpha * 0.7f),
                    style = Stroke(width = (2.5f * s).coerceAtLeast(1.0f)))
                // Telson glow tip
                val tipGlow = (sin(t * 1.2f * PI.toFloat()) * 0.5f + 0.5f) * 0.25f
                drawCircle(AmberGlow.copy(alpha = tipGlow), s * 2.0f,
                    Offset(telsonBaseX + telsonL * 0.8f, y + telsonSway * 0.8f))
            }

            } // bob transform
        }
    }
}
