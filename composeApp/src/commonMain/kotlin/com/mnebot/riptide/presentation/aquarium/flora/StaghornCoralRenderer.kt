package com.mnebot.riptide.presentation.aquarium.flora

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.mnebot.riptide.presentation.aquarium.PathPool
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val CoralCream     = Color(0xFFE8D8C0)   // main branch cream
private val CoralTan       = Color(0xFFD0B898)   // mid-branch tan
private val CoralBrown     = Color(0xFFA08060)   // base brown
private val CoralDark      = Color(0xFF785838)   // deep shadow
private val CoralHighlight = Color(0xFFF5EDE0)   // lit highlight
private val PolypColor     = Color(0xFFFFE0C0)   // polyp dots
private val PolypBright    = Color(0xFFFFEED8)   // polyp highlight (level 3+)
private val IridescentA    = Color(0xFFB0D8F0)   // iridescent blue (level 5+)
private val IridescentB    = Color(0xFFD0B0E8)   // iridescent purple (level 5+)

object StaghornCoralRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val totalH = (22f + level * 2.5f) * s

        // Base mound
        val baseW = (6f + level * 0.5f) * s
        val baseH = totalH * 0.12f
        val basePath = paths.obtain().apply {
            moveTo(x - baseW, y)
            cubicTo(
                x - baseW * 0.8f, y - baseH,
                x + baseW * 0.8f, y - baseH,
                x + baseW, y
            )
            close()
        }
        drawPath(basePath, CoralDark)

        // Main trunk
        val trunkTop = y - totalH * 0.3f
        val trunkW = (2.5f * s).coerceAtLeast(1.2f)
        drawLine(
            color = CoralBrown,
            start = Offset(x, y - baseH * 0.5f),
            end = Offset(x, trunkTop),
            strokeWidth = trunkW,
            cap = StrokeCap.Round
        )

        // Level 5+: iridescent shimmer overlay on branches
        val iridescent = level >= 5
        val iridPhase = t * 0.8f

        // Primary antler branches — recursive-style
        val branchDepth = when {
            level <= 2 -> 2
            level <= 4 -> 3
            else -> 4
        }

        // Two main branches (left and right from trunk top)
        drawAntler(x, trunkTop, s, t, level, totalH * 0.7f, -0.35f, branchDepth, 0, iridescent, iridPhase)
        drawAntler(x, trunkTop, s, t, level, totalH * 0.7f, 0.35f, branchDepth, 1, iridescent, iridPhase)
        // Center continuation
        drawAntler(x, trunkTop, s, t, level, totalH * 0.75f, 0f, branchDepth, 2, iridescent, iridPhase)
    }

    private fun DrawScope.drawAntler(
        bx: Float, by: Float, s: Float, t: Float, level: Int,
        length: Float, lean: Float, depth: Int, seed: Int,
        iridescent: Boolean, iridPhase: Float
    ) {
        if (depth <= 0) return

        val segLen = length * 0.35f
        val sway = sin(t * 0.5f * PI.toFloat() + seed * 1.7f) * 0.8f * s * (4 - depth)

        // End point of this segment
        val endX = bx + lean * segLen * 2.2f + sway
        val endY = by - segLen

        // Stroke width decreases with depth
        val strokeW = when (depth) {
            4 -> (2.2f * s).coerceAtLeast(1.0f)
            3 -> (1.6f * s).coerceAtLeast(0.8f)
            2 -> (1.1f * s).coerceAtLeast(0.6f)
            else -> (0.7f * s).coerceAtLeast(0.4f)
        }

        // Color by depth — lighter toward tips
        val branchColor = when (depth) {
            4 -> CoralBrown
            3 -> CoralTan
            2 -> CoralCream
            else -> CoralHighlight
        }

        // Draw branch segment
        drawLine(
            color = branchColor,
            start = Offset(bx, by),
            end = Offset(endX, endY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Iridescent overlay (level 5+)
        if (iridescent) {
            val shimmer = sin(iridPhase + seed * 0.8f + depth * 0.5f)
            val iridColor = if (shimmer > 0f) IridescentA else IridescentB
            val iridAlpha = 0.15f + sin(iridPhase * 2f + seed) * 0.08f
            drawLine(
                color = iridColor.copy(alpha = iridAlpha.coerceIn(0f, 0.3f)),
                start = Offset(bx, by),
                end = Offset(endX, endY),
                strokeWidth = strokeW * 1.5f,
                cap = StrokeCap.Round
            )
        }

        // Polyp dots at branch tip and along segment
        val showMorePolyps = level >= 3
        val polypRadius = (0.6f * s).coerceAtLeast(0.3f)

        // Tip polyp — always present
        val polypPulse = sin(t * 2f * PI.toFloat() + seed * 2.3f + depth) * 0.3f
        drawCircle(
            color = if (showMorePolyps) PolypBright else PolypColor,
            radius = polypRadius * (1f + polypPulse * 0.2f),
            center = Offset(endX, endY)
        )

        // Mid-segment polyps (level 3+)
        if (showMorePolyps) {
            val midX = (bx + endX) * 0.5f
            val midY = (by + endY) * 0.5f
            drawCircle(
                color = PolypColor.copy(alpha = 0.7f),
                radius = polypRadius * 0.7f,
                center = Offset(midX, midY)
            )
        }

        // Recurse: fork into two sub-branches
        if (depth > 1) {
            val forkLeanL = lean - 0.3f
            val forkLeanR = lean + 0.3f
            val subLen = length * 0.6f
            drawAntler(endX, endY, s, t, level, subLen, forkLeanL, depth - 1, seed * 3 + 1, iridescent, iridPhase)
            drawAntler(endX, endY, s, t, level, subLen, forkLeanR, depth - 1, seed * 3 + 2, iridescent, iridPhase)
        }
    }
}
