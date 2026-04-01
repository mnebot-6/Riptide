package com.mnebot.riptide.presentation.aquarium.flora

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val TubeOrange     = Color(0xFFE87830)   // main tube body
private val TubeDeep       = Color(0xFFC05820)   // shadow side
private val TubeLight      = Color(0xFFF09848)   // lit side highlight
private val TubePurple     = Color(0xFF9858A8)   // secondary tube color
private val TubePurpleDark = Color(0xFF6E3880)   // purple shadow
private val TubeRim        = Color(0xFFFFC880)   // tube opening rim
private val TubeInner      = Color(0xFF683010)   // dark tube interior
private val ParticleColor  = Color(0xFFFFE8B0)   // particles (level 3+)
private val GlowCyan       = Color(0xFF80E0FF)   // bioluminescent glow (level 5+)

object TubeSpongeRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        // Tube specifications: (offsetX, height factor, width factor, isPurple)
        val tubes = listOf(
            TubeSpec(-3.5f, 1.0f, 1.0f, false),
            TubeSpec(1.0f, 0.75f, 0.85f, true),
            TubeSpec(4.5f, 0.88f, 0.78f, false),
            TubeSpec(-1.0f, 0.55f, 0.70f, true)
        )

        val tubeCount = when {
            level <= 2 -> 3
            level <= 4 -> 4
            else -> 4
        }

        for (i in 0 until tubeCount) {
            val spec = tubes[i]
            drawTube(x, y, s, t, level, spec, i)
        }

        // Level 3+: particles floating out of tube openings
        if (level >= 3) {
            for (i in 0 until tubeCount) {
                val spec = tubes[i]
                val tubeH = (18f + level * 2f) * s * spec.heightFactor
                val tubeTopY = y - tubeH
                val tubeCx = x + spec.offsetX * s
                val particleCount = if (level >= 5) 4 else 2
                for (p in 0 until particleCount) {
                    val phase = i * 2.3f + p * 1.7f
                    val pLife = ((t * 0.6f + phase) % 2.0f) / 2.0f // 0..1 cycle
                    val px = tubeCx + sin(t * 1.2f + phase) * 2f * s
                    val py = tubeTopY - pLife * 10f * s
                    val pAlpha = (1f - pLife) * 0.6f
                    drawCircle(
                        color = ParticleColor.copy(alpha = pAlpha),
                        radius = (0.6f + pLife * 0.4f) * s,
                        center = Offset(px, py)
                    )
                }
            }
        }

        // Level 5+: bioluminescent glow around tubes
        if (level >= 5) {
            for (i in 0 until tubeCount) {
                val spec = tubes[i]
                val tubeH = (18f + level * 2f) * s * spec.heightFactor
                val tubeW = (4f + level * 0.3f) * s * spec.widthFactor
                val tubeCx = x + spec.offsetX * s
                val tubeTopY = y - tubeH
                val glowPulse = 0.12f + sin(t * 2.0f + i * 1.5f) * 0.06f
                drawCircle(
                    color = GlowCyan.copy(alpha = glowPulse),
                    radius = tubeW * 2.5f,
                    center = Offset(tubeCx, tubeTopY + tubeH * 0.4f)
                )
            }
        }
    }

    private data class TubeSpec(
        val offsetX: Float,
        val heightFactor: Float,
        val widthFactor: Float,
        val isPurple: Boolean
    )

    private fun DrawScope.drawTube(
        x: Float, y: Float, s: Float, t: Float,
        level: Int, spec: TubeSpec, index: Int
    ) {
        val tubeH = (18f + level * 2f) * s * spec.heightFactor
        val tubeW = (4f + level * 0.3f) * s * spec.widthFactor
        val tubeCx = x + spec.offsetX * s

        // Gentle sway at the top
        val sway = sin(t * 0.8f * PI.toFloat() + index * 1.4f) * 1.5f * s

        val baseBody = if (spec.isPurple) TubePurple else TubeOrange
        val baseShadow = if (spec.isPurple) TubePurpleDark else TubeDeep

        // Tube body (slightly tapered — wider at top)
        val taperBottom = tubeW * 0.75f
        val taperTop = tubeW
        val topY = y - tubeH

        // Shadow side
        val shadowPath = Path().apply {
            moveTo(tubeCx - taperBottom, y)
            lineTo(tubeCx - taperTop + sway, topY)
            lineTo(tubeCx + sway, topY)
            lineTo(tubeCx, y)
            close()
        }
        drawPath(shadowPath, baseShadow)

        // Lit side
        val litPath = Path().apply {
            moveTo(tubeCx, y)
            lineTo(tubeCx + sway, topY)
            lineTo(tubeCx + taperTop + sway, topY)
            lineTo(tubeCx + taperBottom, y)
            close()
        }
        drawPath(litPath, baseBody)

        // Highlight stripe on lit side
        val highlightPath = Path().apply {
            moveTo(tubeCx + taperBottom * 0.3f, y)
            lineTo(tubeCx + taperTop * 0.3f + sway, topY)
            lineTo(tubeCx + taperTop * 0.65f + sway, topY)
            lineTo(tubeCx + taperBottom * 0.65f, y)
            close()
        }
        drawPath(highlightPath, TubeLight.copy(alpha = 0.35f))

        // Porous texture — small dots along the tube surface
        val poreCount = (3 + level).coerceAtMost(7)
        for (p in 1..poreCount) {
            val frac = p.toFloat() / (poreCount + 1)
            val poreY = y - tubeH * frac
            val poreSway = sway * frac
            val halfW = taperBottom + (taperTop - taperBottom) * frac
            // Left side pore
            drawCircle(
                color = baseShadow.copy(alpha = 0.3f),
                radius = (0.7f * s).coerceAtLeast(0.4f),
                center = Offset(tubeCx - halfW * 0.4f + poreSway, poreY)
            )
            // Right side pore
            drawCircle(
                color = baseShadow.copy(alpha = 0.2f),
                radius = (0.5f * s).coerceAtLeast(0.3f),
                center = Offset(tubeCx + halfW * 0.5f + poreSway, poreY)
            )
        }

        // Opening at top — elliptical rim
        val rimRx = taperTop
        val rimRy = taperTop * 0.35f
        val rimCx = tubeCx + sway
        val rimCy = topY

        // Dark interior
        drawOval(
            color = TubeInner,
            topLeft = Offset(rimCx - rimRx * 0.7f, rimCy - rimRy * 0.7f),
            size = Size(rimRx * 1.4f, rimRy * 1.4f)
        )

        // Rim ring
        val rimPath = Path()
        val rimSteps = 20
        for (step in 0..rimSteps) {
            val angle = step.toFloat() / rimSteps * 2f * PI.toFloat()
            val rx = rimCx + cos(angle) * rimRx
            val ry = rimCy + sin(angle) * rimRy
            if (step == 0) rimPath.moveTo(rx, ry) else rimPath.lineTo(rx, ry)
        }
        rimPath.close()
        drawPath(
            rimPath,
            TubeRim,
            style = Stroke(width = (1.2f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round)
        )
    }
}
