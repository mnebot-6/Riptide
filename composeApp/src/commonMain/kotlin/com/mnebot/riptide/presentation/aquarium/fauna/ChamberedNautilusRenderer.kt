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
import kotlin.math.exp
import kotlin.math.sin

// ── Palette ─────────────────────────────────────────────────────────────────
private val ShellCream       = Color(0xFFF5E6C8)  // cream exterior
private val ShellTan         = Color(0xFFD4B896)  // warm tan
private val ShellBrown       = Color(0xFFA07850)  // medium brown
private val StripeBrown      = Color(0xFF7A4E2A)  // reddish-brown stripes
private val StripeDeep       = Color(0xFF5C3518)  // deep brown stripe shadow
private val ChamberLine      = Color(0xFFC4986A)  // chamber wall color
private val ChamberShadow    = Color(0xFF8B6B4A)  // darker chamber line
private val InnerPink        = Color(0xFFECC8B0)  // pinkish inner shell
private val TentacleOrange   = Color(0xFFE8A070)  // tentacle base
private val TentacleTip      = Color(0xFFF0C8A8)  // pale tentacle tip
private val ShadowDeep       = Color(0xFF3E2810)  // deep shadow
private val EyeBlack         = Color(0xFF1A1008)  // near-black
private val PearlWhite       = Color(0xFFFFF8F0)  // pearlescent highlight

object ChamberedNautilusRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val shellR = (10f + level * 0.6f) * s
        val bobY = sin(t * 1.4f * PI.toFloat()) * s * 0.6f
        val bodyTilt = sin(t * 1.4f * PI.toFloat() + 0.2f) * 3f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = bobY)
                rotate(degrees = bodyTilt, pivot = Offset(x, y))
            }) {

                // ── TENTACLES (emerging from shell opening, left side) ───────
                val tentacleCount = 8
                for (i in 0 until tentacleCount) {
                    val baseAngle = -PI.toFloat() * 0.55f + i * PI.toFloat() * 0.14f
                    val phase = t * 2.2f * PI.toFloat() + i * 0.5f
                    val tentLen = (4f + i * 0.3f) * s
                    val waveAmp = tentLen * 0.12f

                    val bx = x - shellR * 0.65f + cos(baseAngle) * shellR * 0.2f
                    val by = y + sin(baseAngle) * shellR * 0.45f

                    val cp1x = bx - tentLen * 0.4f + sin(phase) * waveAmp
                    val cp1y = by + sin(baseAngle) * tentLen * 0.3f + cos(phase) * waveAmp * 0.5f
                    val tipX = bx - tentLen * 0.8f + sin(phase + 0.5f) * waveAmp * 1.5f
                    val tipY = by + sin(baseAngle) * tentLen * 0.7f + cos(phase + 0.5f) * waveAmp

                    val tentacle = Path().apply {
                        moveTo(bx, by)
                        cubicTo(cp1x, cp1y, tipX - tentLen * 0.1f, tipY, tipX, tipY)
                    }

                    drawPath(tentacle, TentacleOrange.copy(alpha = 0.7f),
                        style = Stroke(width = (1.4f * s * (1f - i * 0.06f)).coerceAtLeast(0.5f),
                            cap = StrokeCap.Round))
                    drawPath(tentacle, TentacleTip.copy(alpha = 0.3f),
                        style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f),
                            cap = StrokeCap.Round))
                }

                // ── SHELL SHADOW ─────────────────────────────────────────────
                drawCircle(ShadowDeep.copy(alpha = 0.18f), shellR * 1.05f,
                    Offset(x + shellR * 0.03f, y + shellR * 0.04f))

                // ── MAIN SHELL (oval, cream base) ────────────────────────────
                val shellPath = Path().apply {
                    moveTo(x - shellR * 0.7f, y)
                    cubicTo(x - shellR * 0.7f, y - shellR * 0.95f,
                        x + shellR * 0.4f, y - shellR * 0.95f,
                        x + shellR * 0.65f, y - shellR * 0.3f)
                    cubicTo(x + shellR * 0.75f, y + shellR * 0.1f,
                        x + shellR * 0.65f, y + shellR * 0.7f,
                        x + shellR * 0.15f, y + shellR * 0.9f)
                    cubicTo(x - shellR * 0.3f, y + shellR * 0.95f,
                        x - shellR * 0.7f, y + shellR * 0.6f,
                        x - shellR * 0.7f, y)
                    close()
                }
                drawPath(shellPath, ShellCream)

                // Shell tan layer (lower half depth)
                val shellLower = Path().apply {
                    moveTo(x - shellR * 0.65f, y + shellR * 0.1f)
                    cubicTo(x - shellR * 0.55f, y + shellR * 0.85f,
                        x + shellR * 0.5f, y + shellR * 0.85f,
                        x + shellR * 0.6f, y + shellR * 0.1f)
                    cubicTo(x + shellR * 0.5f, y + shellR * 0.6f,
                        x - shellR * 0.3f, y + shellR * 0.65f,
                        x - shellR * 0.65f, y + shellR * 0.1f)
                    close()
                }
                drawPath(shellLower, ShellTan.copy(alpha = 0.45f))

                // ── REDDISH-BROWN RADIAL STRIPES ─────────────────────────────
                val stripeCount = 12
                for (i in 0 until stripeCount) {
                    val angle = -PI.toFloat() * 0.4f + i * PI.toFloat() * 0.12f
                    val innerR = shellR * 0.25f
                    val outerR = shellR * 0.85f
                    val sx = x + cos(angle) * innerR
                    val sy = y + sin(angle) * innerR
                    val ex = x + cos(angle) * outerR
                    val ey = y + sin(angle) * outerR
                    drawLine(
                        StripeBrown.copy(alpha = 0.50f),
                        Offset(sx, sy), Offset(ex, ey),
                        strokeWidth = (1.2f * s).coerceAtLeast(0.4f),
                        cap = StrokeCap.Round
                    )
                    // Stripe shadow
                    drawLine(
                        StripeDeep.copy(alpha = 0.15f),
                        Offset(sx + 0.3f * s, sy + 0.3f * s),
                        Offset(ex + 0.3f * s, ey + 0.3f * s),
                        strokeWidth = (0.8f * s).coerceAtLeast(0.3f),
                        cap = StrokeCap.Round
                    )
                }

                // ── LOGARITHMIC SPIRAL CHAMBER LINES ─────────────────────────
                val chamberCount = if (level >= 3) 7 else 5
                val growthRate = 0.18f
                for (c in 1..chamberCount) {
                    val cFrac = c.toFloat() / (chamberCount + 1).toFloat()
                    val r = shellR * 0.2f * exp(growthRate * c.toFloat())
                            .coerceAtMost(shellR * 0.78f)
                    // Draw chamber arc
                    val segments = 16
                    val chamberPath = Path().apply {
                        val startAngle = -PI.toFloat() * 0.6f + cFrac * 0.3f
                        val sweep = PI.toFloat() * 1.2f
                        val a0 = startAngle
                        moveTo(x + cos(a0) * r, y + sin(a0) * r)
                        for (seg in 1..segments) {
                            val a = startAngle + sweep * seg / segments
                            lineTo(x + cos(a) * r, y + sin(a) * r)
                        }
                    }
                    val lineColor = if (level >= 3) ChamberShadow else ChamberLine
                    drawPath(chamberPath, lineColor.copy(alpha = 0.55f),
                        style = Stroke(width = (0.9f * s).coerceAtLeast(0.3f),
                            cap = StrokeCap.Round))

                    // Level 3+: extra detail line parallel to chamber
                    if (level >= 3) {
                        val detailR = r * 0.92f
                        val detailPath = Path().apply {
                            val startA = -PI.toFloat() * 0.55f + cFrac * 0.3f
                            val sweep = PI.toFloat() * 1.1f
                            moveTo(x + cos(startA) * detailR, y + sin(startA) * detailR)
                            for (seg in 1..segments) {
                                val a = startA + sweep * seg / segments
                                lineTo(x + cos(a) * detailR, y + sin(a) * detailR)
                            }
                        }
                        drawPath(detailPath, ChamberLine.copy(alpha = 0.30f),
                            style = Stroke(width = (0.5f * s).coerceAtLeast(0.2f)))
                    }
                }

                // ── INNER SHELL (opening, pinkish) ───────────────────────────
                val opening = Path().apply {
                    moveTo(x - shellR * 0.7f, y - shellR * 0.25f)
                    cubicTo(x - shellR * 0.55f, y - shellR * 0.55f,
                        x - shellR * 0.25f, y - shellR * 0.45f,
                        x - shellR * 0.15f, y - shellR * 0.1f)
                    cubicTo(x - shellR * 0.2f, y + shellR * 0.2f,
                        x - shellR * 0.4f, y + shellR * 0.35f,
                        x - shellR * 0.7f, y + shellR * 0.25f)
                    close()
                }
                drawPath(opening, InnerPink.copy(alpha = 0.65f))
                drawPath(opening, ShadowDeep.copy(alpha = 0.15f),
                    style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))

                // Shell highlight (upper)
                drawCircle(PearlWhite.copy(alpha = 0.25f), shellR * 0.35f,
                    Offset(x + shellR * 0.1f, y - shellR * 0.35f))

                // Shell outline
                drawPath(shellPath, ShellBrown.copy(alpha = 0.25f),
                    style = Stroke(width = (0.7f * s).coerceAtLeast(0.3f)))

                // ── LEVEL 5+: PEARLESCENT SHELL SHEEN ────────────────────────
                if (level >= 5) {
                    val sheenPhase = sin(t * 1.5f * PI.toFloat()) * 0.15f
                    drawCircle(PearlWhite.copy(alpha = 0.20f + sheenPhase),
                        shellR * 0.55f, Offset(x + shellR * 0.05f, y - shellR * 0.15f))
                    drawCircle(Color(0xFFE8D0F0).copy(alpha = 0.12f + sheenPhase * 0.5f),
                        shellR * 0.40f, Offset(x - shellR * 0.15f, y + shellR * 0.10f))
                    // Iridescent edge
                    drawPath(shellPath, Color(0xFFD0E8F0).copy(alpha = 0.15f),
                        style = Stroke(width = (1.8f * s).coerceAtLeast(0.8f)))
                }

                // ── EYE ──────────────────────────────────────────────────────
                val eyeR = (1.3f + level * 0.08f) * s
                val eyeX = x - shellR * 0.55f
                val eyeY = y - shellR * 0.08f

                drawCircle(ShadowDeep.copy(alpha = 0.35f), eyeR * 1.2f, Offset(eyeX, eyeY))
                drawCircle(PearlWhite, eyeR, Offset(eyeX, eyeY))
                drawCircle(ShellBrown, eyeR * 0.55f, Offset(eyeX + eyeR * 0.05f, eyeY))
                drawCircle(EyeBlack, eyeR * 0.30f, Offset(eyeX + eyeR * 0.07f, eyeY + eyeR * 0.02f))
                drawCircle(Color.White, eyeR * 0.18f, Offset(eyeX - eyeR * 0.15f, eyeY - eyeR * 0.15f))

            } // bob + tilt transform
        }
    }
}
