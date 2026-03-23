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
private val HermShellTan   = Color(0xFF9B7840)  // shell tan/brown
private val HermShellDark  = Color(0xFF6A4E20)  // shell dark spiral
private val HermShellLight = Color(0xFFCCA868)  // shell highlight
private val HermOrange     = Color(0xFFD05030)  // claw & legs
private val HermOrange2    = Color(0xFFE87050)  // lighter claw tip
private val HermBlack      = Color(0xFF1A1A1A)  // outlines
private val HermWhite      = Color(0xFFF0E8D8)  // eye & belly

object HermitCrabRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val shellR = (7f + level * 0.5f) * s
        val legLen = (4f + level * 0.3f) * s

        val legSway = sin(t * 3.8f * PI.toFloat())

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // Shell carried on back — offset to the right/rear
            val shellX = x + shellR * 0.35f
            val shellY = y - shellR * 0.15f

            // ── SHELL — asymmetric spiral (main visual mass) ───────────────────
            // Outer shell oval
            val shellOuter = Path().apply {
                moveTo(shellX - shellR * 1.1f, shellY)
                cubicTo(shellX - shellR * 1.1f, shellY - shellR * 0.85f,
                    shellX + shellR * 0.3f,    shellY - shellR * 1.0f,
                    shellX + shellR * 0.9f,    shellY - shellR * 0.3f)
                cubicTo(shellX + shellR * 1.1f, shellY + shellR * 0.3f,
                    shellX + shellR * 0.6f,    shellY + shellR * 0.9f,
                    shellX - shellR * 0.2f,    shellY + shellR * 1.0f)
                cubicTo(shellX - shellR * 0.9f, shellY + shellR * 0.85f,
                    shellX - shellR * 1.2f,    shellY + shellR * 0.5f,
                    shellX - shellR * 1.1f,    shellY)
                close()
            }
            drawPath(shellOuter, HermShellTan)

            // Shell spiral bands
            for (band in 1..3) {
                val bandR = shellR * (0.75f - band * 0.18f)
                val spiralOffset = band * shellR * 0.12f
                drawCircle(
                    HermShellDark.copy(alpha = 0.35f),
                    bandR,
                    Offset(shellX + spiralOffset, shellY - spiralOffset * 0.5f),
                    style = Stroke(width = (1.0f * s).coerceAtLeast(0.4f))
                )
            }

            // Shell highlight
            val shellHighlight = Path().apply {
                moveTo(shellX - shellR * 0.7f, shellY - shellR * 0.5f)
                cubicTo(shellX - shellR * 0.3f, shellY - shellR * 0.85f,
                    shellX + shellR * 0.2f, shellY - shellR * 0.80f,
                    shellX + shellR * 0.55f, shellY - shellR * 0.40f)
                cubicTo(shellX + shellR * 0.2f, shellY - shellR * 0.55f,
                    shellX - shellR * 0.1f, shellY - shellR * 0.60f,
                    shellX - shellR * 0.7f, shellY - shellR * 0.5f)
                close()
            }
            drawPath(shellHighlight, HermShellLight.copy(alpha = 0.40f))

            // Shell outline
            drawPath(shellOuter, HermShellDark.copy(alpha = 0.35f),
                style = Stroke(width = (0.8f * s).coerceAtLeast(0.3f)))

            // Shell opening (where crab peeks out)
            val openingX = x - shellR * 0.6f
            val openingY = y + shellR * 0.1f
            drawCircle(HermShellDark.copy(alpha = 0.55f), shellR * 0.38f, Offset(openingX, openingY))

            // ── CRAB BODY (hidden in shell, only front visible) ────────────────
            val bodyR = shellR * 0.36f
            drawCircle(HermOrange2, bodyR, Offset(openingX + bodyR * 0.2f, openingY - bodyR * 0.1f))

            // ── WALKING LEGS (4 visible) ───────────────────────────────────────
            val legBaseX = openingX + bodyR * 0.5f
            val legBaseY = openingY + bodyR * 0.5f
            for (i in 0..3) {
                val lx = legBaseX + i * legLen * 0.25f
                val phase = i * 0.5f
                val legAnim = sin(t * 3.8f * PI.toFloat() + phase) * 1.5f * s
                // Upper segment
                drawLine(HermOrange,
                    Offset(lx, legBaseY),
                    Offset(lx + legAnim * 0.6f, legBaseY + legLen * 0.6f),
                    strokeWidth = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round)
                // Lower segment
                drawLine(HermOrange,
                    Offset(lx + legAnim * 0.6f, legBaseY + legLen * 0.6f),
                    Offset(lx + legAnim * 0.3f, legBaseY + legLen * 1.2f),
                    strokeWidth = (0.9f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round)
            }

            // ── LARGE CLAW — dominant claw, extends forward ────────────────────
            val clawBaseX = openingX - bodyR * 0.4f
            val clawBaseY = openingY - bodyR * 0.1f
            val clawLen = (5f + level * 0.4f) * s
            // Arm
            drawLine(HermOrange,
                Offset(clawBaseX, clawBaseY),
                Offset(clawBaseX - clawLen * 0.8f, clawBaseY - bodyR * 0.2f),
                strokeWidth = (2.5f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round)
            // Upper finger
            drawLine(HermOrange2,
                Offset(clawBaseX - clawLen * 0.8f, clawBaseY - bodyR * 0.2f),
                Offset(clawBaseX - clawLen * 1.2f, clawBaseY - bodyR * 0.55f),
                strokeWidth = (2.0f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round)
            // Lower finger
            drawLine(HermOrange,
                Offset(clawBaseX - clawLen * 0.8f, clawBaseY - bodyR * 0.2f),
                Offset(clawBaseX - clawLen * 1.15f, clawBaseY + bodyR * 0.15f),
                strokeWidth = (1.8f * s).coerceAtLeast(0.7f), cap = StrokeCap.Round)

            // ── ANTENNAE ───────────────────────────────────────────────────────
            val antSway = sin(t * 2.0f * PI.toFloat()) * 2.0f * s
            val antLen = (6f + level * 0.4f) * s
            drawLine(HermOrange.copy(alpha = 0.80f),
                Offset(clawBaseX, clawBaseY - bodyR * 0.5f),
                Offset(clawBaseX - antLen + antSway, clawBaseY - bodyR * 2.0f),
                strokeWidth = (0.6f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
            drawLine(HermOrange.copy(alpha = 0.80f),
                Offset(clawBaseX + bodyR * 0.3f, clawBaseY - bodyR * 0.5f),
                Offset(clawBaseX + bodyR * 0.3f - antLen * 0.7f - antSway, clawBaseY - bodyR * 1.5f),
                strokeWidth = (0.6f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)

            // ── EYES ON STALKS ─────────────────────────────────────────────────
            val eyeStalkLen = (2.0f + level * 0.1f) * s
            val eyeR = (1.1f + level * 0.07f) * s
            val eyeOffsets = listOf(Pair(-bodyR * 0.2f, -bodyR * 0.7f), Pair(bodyR * 0.3f, -bodyR * 0.75f))
            for ((exOff, eyOff) in eyeOffsets) {
                val eyeBaseX = openingX + exOff
                val eyeBaseY = openingY + eyOff
                drawLine(HermBlack,
                    Offset(eyeBaseX, eyeBaseY + eyeStalkLen * 0.5f),
                    Offset(eyeBaseX, eyeBaseY),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
                drawCircle(HermWhite, eyeR, Offset(eyeBaseX, eyeBaseY))
                drawCircle(HermBlack, eyeR * 0.58f, Offset(eyeBaseX + eyeR * 0.1f, eyeBaseY))
            }
        }
    }
}
