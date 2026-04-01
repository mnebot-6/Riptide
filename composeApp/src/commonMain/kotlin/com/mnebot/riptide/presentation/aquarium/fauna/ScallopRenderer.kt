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
import kotlin.math.cos
import kotlin.math.abs

// ── Palette ─────────────────────────────────────────────────────────────────
private val ShellWhite     = Color(0xFFF5F0E8)  // creamy white shell
private val ShellCream     = Color(0xFFEDE4D4)  // cream base
private val RibOrangeBrown = Color(0xFFC49060)  // orange-brown ribs
private val RibDark        = Color(0xFFA07048)  // darker rib shadow
private val RibLight       = Color(0xFFD8B080)  // lighter rib highlight
private val HingeGold      = Color(0xFFB89860)  // hinge/ear gold
private val HingeDark      = Color(0xFF8A7040)  // hinge shadow
private val InteriorWhite  = Color(0xFFFFF8F0)  // interior white
private val EyeBlue        = Color(0xFF2080D0)  // tiny scallop eye
private val EyeGlow        = Color(0xFF60B0FF)  // eye glow
private val PearlPink      = Color(0xFFF0D0E0)  // pearl pink
private val PearlWhite     = Color(0xFFFFF5F8)  // pearl highlight
private val ShadowBrown    = Color(0xFF6B5040)  // warm shadow

object ScallopRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val shellR = (10f + level * 0.5f) * s

        // ── Clapping animation: open/close cycle ─────────────────────────────
        val clapPeriod = 3500L  // 3.5s full cycle
        val clapPhase = (animTimeMs % clapPeriod).toFloat() / clapPeriod.toFloat()
        // 0..0.6 = open, 0.6..0.7 = closing, 0.7..0.8 = closed, 0.8..1.0 = opening
        val openAngle = when {
            clapPhase < 0.6f -> 0.28f  // open
            clapPhase < 0.7f -> 0.28f * (1f - (clapPhase - 0.6f) / 0.1f)  // closing
            clapPhase < 0.8f -> 0.0f   // closed
            else -> 0.28f * ((clapPhase - 0.8f) / 0.2f)  // opening
        }

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── GROUND SHADOW ────────────────────────────────────────────────
            drawOval(
                ShadowBrown.copy(alpha = 0.20f),
                topLeft = Offset(x - shellR * 0.8f, y + shellR * 0.05f),
                size = androidx.compose.ui.geometry.Size(shellR * 1.6f, shellR * 0.15f)
            )

            // ── BOTTOM HALF (slightly behind, rotated open) ──────────────────
            withTransform({
                rotate(degrees = openAngle * 180f / PI.toFloat(), pivot = Offset(x, y))
            }) {
                val bottomShell = Path().apply {
                    moveTo(x - shellR, y)
                    cubicTo(
                        x - shellR * 0.95f, y + shellR * 0.55f,
                        x - shellR * 0.5f, y + shellR * 1.0f,
                        x, y + shellR * 1.05f
                    )
                    cubicTo(
                        x + shellR * 0.5f, y + shellR * 1.0f,
                        x + shellR * 0.95f, y + shellR * 0.55f,
                        x + shellR, y
                    )
                    close()
                }
                drawPath(bottomShell, ShellCream)

                // Ribs on bottom
                val ribCount = 12
                for (i in 1 until ribCount) {
                    val angle = PI.toFloat() * i.toFloat() / ribCount
                    val ribEndX = x + cos(angle) * shellR * 0.95f
                    val ribEndY = y + sin(angle) * shellR * 0.95f
                    drawLine(
                        RibDark.copy(alpha = 0.25f),
                        Offset(x, y),
                        Offset(ribEndX, ribEndY),
                        strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                        cap = StrokeCap.Round
                    )
                }

                drawPath(
                    bottomShell, ShadowBrown.copy(alpha = 0.20f),
                    style = Stroke(width = (0.5f * s).coerceAtLeast(0.3f))
                )
            }

            // ── INTERIOR (visible when open) ─────────────────────────────────
            if (openAngle > 0.05f) {
                val interiorH = shellR * openAngle * 2.5f
                val interior = Path().apply {
                    moveTo(x - shellR * 0.8f, y)
                    cubicTo(
                        x - shellR * 0.5f, y - interiorH,
                        x + shellR * 0.5f, y - interiorH,
                        x + shellR * 0.8f, y
                    )
                    close()
                }
                drawPath(interior, InteriorWhite.copy(alpha = 0.60f))

                // ── LEVEL 5+: Pearl visible inside when open ─────────────────
                if (level >= 5 && openAngle > 0.15f) {
                    val pearlPulse = (sin(t * 1.2f * PI.toFloat()) * 0.5f + 0.5f)
                    val pearlR = 2.0f * s
                    val pearlY = y - interiorH * 0.3f

                    drawCircle(PearlPink.copy(alpha = 0.5f + pearlPulse * 0.2f), pearlR * 1.8f, Offset(x, pearlY))
                    drawCircle(PearlWhite.copy(alpha = 0.7f + pearlPulse * 0.15f), pearlR, Offset(x, pearlY))
                    drawCircle(Color.White, pearlR * 0.4f, Offset(x - pearlR * 0.3f, pearlY - pearlR * 0.3f))
                }
            }

            // ── TOP HALF (fan-shaped, main visible shell) ────────────────────
            withTransform({
                rotate(degrees = -openAngle * 180f / PI.toFloat(), pivot = Offset(x, y))
            }) {
                // Shell shadow
                val topShadow = Path().apply {
                    moveTo(x - shellR * 1.02f, y + 0.5f * s)
                    cubicTo(
                        x - shellR * 0.97f, y - shellR * 0.53f,
                        x - shellR * 0.52f, y - shellR * 1.02f,
                        x, y - shellR * 1.07f
                    )
                    cubicTo(
                        x + shellR * 0.52f, y - shellR * 1.02f,
                        x + shellR * 0.97f, y - shellR * 0.53f,
                        x + shellR * 1.02f, y + 0.5f * s
                    )
                    close()
                }
                drawPath(topShadow, ShadowBrown.copy(alpha = 0.15f))

                // Main top shell
                val topShell = Path().apply {
                    moveTo(x - shellR, y)
                    cubicTo(
                        x - shellR * 0.95f, y - shellR * 0.55f,
                        x - shellR * 0.5f, y - shellR * 1.0f,
                        x, y - shellR * 1.05f
                    )
                    cubicTo(
                        x + shellR * 0.5f, y - shellR * 1.0f,
                        x + shellR * 0.95f, y - shellR * 0.55f,
                        x + shellR, y
                    )
                    close()
                }
                drawPath(topShell, ShellWhite)

                // ── RIBS (radiating from hinge) ──────────────────────────────
                val topRibCount = 14
                for (i in 1 until topRibCount) {
                    val frac = i.toFloat() / topRibCount
                    val angle = PI.toFloat() * frac
                    val ribEndX = x + cos(angle) * shellR * 0.97f
                    val ribEndY = y - sin(angle) * shellR * 0.97f

                    // Main rib
                    drawLine(
                        RibOrangeBrown.copy(alpha = 0.40f + frac * 0.15f),
                        Offset(x, y),
                        Offset(ribEndX, ribEndY),
                        strokeWidth = (1.2f * s).coerceAtLeast(0.5f),
                        cap = StrokeCap.Round
                    )
                    // Rib highlight (offset slightly)
                    drawLine(
                        RibLight.copy(alpha = 0.20f),
                        Offset(x + s * 0.3f, y),
                        Offset(ribEndX + s * 0.3f, ribEndY),
                        strokeWidth = (0.5f * s).coerceAtLeast(0.25f),
                        cap = StrokeCap.Round
                    )
                }

                // Upper highlight
                val topHL = Path().apply {
                    moveTo(x - shellR * 0.5f, y - shellR * 0.4f)
                    cubicTo(
                        x - shellR * 0.3f, y - shellR * 0.85f,
                        x + shellR * 0.2f, y - shellR * 0.82f,
                        x + shellR * 0.4f, y - shellR * 0.35f
                    )
                    cubicTo(
                        x + shellR * 0.1f, y - shellR * 0.55f,
                        x - shellR * 0.2f, y - shellR * 0.58f,
                        x - shellR * 0.5f, y - shellR * 0.4f
                    )
                    close()
                }
                drawPath(topHL, Color.White.copy(alpha = 0.18f))

                // Shell outline
                drawPath(
                    topShell, RibDark.copy(alpha = 0.30f),
                    style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f))
                )

                // ── HINGE EARS (small wing-like flaps at hinge) ──────────────
                val earL = Path().apply {
                    moveTo(x - shellR * 0.05f, y)
                    lineTo(x - shellR * 0.35f, y - shellR * 0.15f)
                    lineTo(x - shellR * 0.30f, y + shellR * 0.03f)
                    close()
                }
                drawPath(earL, HingeGold)
                drawPath(earL, HingeDark.copy(alpha = 0.25f),
                    style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))

                val earR = Path().apply {
                    moveTo(x + shellR * 0.05f, y)
                    lineTo(x + shellR * 0.35f, y - shellR * 0.15f)
                    lineTo(x + shellR * 0.30f, y + shellR * 0.03f)
                    close()
                }
                drawPath(earR, HingeGold)
                drawPath(earR, HingeDark.copy(alpha = 0.25f),
                    style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))

                // ── LEVEL 3+: Tiny blue eyes along edge when open ────────────
                if (level >= 3 && openAngle > 0.1f) {
                    val eyeCount = 8
                    val eyePulse = (sin(t * 2.0f * PI.toFloat()) * 0.5f + 0.5f)
                    for (i in 1..eyeCount) {
                        val frac = i.toFloat() / (eyeCount + 1)
                        val angle = PI.toFloat() * frac
                        val eyeX = x + cos(angle) * shellR * 0.92f
                        val eyeY = y - sin(angle) * shellR * 0.92f
                        val eyeR = 0.6f * s

                        drawCircle(EyeGlow.copy(alpha = 0.25f + eyePulse * 0.15f), eyeR * 2.0f, Offset(eyeX, eyeY))
                        drawCircle(EyeBlue.copy(alpha = 0.7f + eyePulse * 0.15f), eyeR, Offset(eyeX, eyeY))
                        drawCircle(Color.White.copy(alpha = 0.6f), eyeR * 0.4f,
                            Offset(eyeX - eyeR * 0.2f, eyeY - eyeR * 0.2f))
                    }
                }
            }
        }
    }
}
