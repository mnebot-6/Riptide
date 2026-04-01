package com.mnebot.riptide.presentation.aquarium.flora

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val CoralRed       = Color(0xFFD03020)   // main branch red
private val CoralOrange    = Color(0xFFE86030)   // lighter branch orange
private val CoralYellow    = Color(0xFFF09040)   // branch highlight
private val CoralDeep      = Color(0xFF981810)   // deep shadow red
private val CoralBase      = Color(0xFF781008)   // base stalk
private val TipWhite       = Color(0xFFFFE8D0)   // hot tips
private val GlowRed        = Color(0xFFFF4020)   // pulse glow
private val AuraRed        = Color(0xFFFF2010)   // warning aura (level 5+)

object FireCoralRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val baseH = (20f + level * 2.5f) * s
        val baseW = (5f + level * 0.4f) * s

        // Subtle pulsing glow animation
        val pulsePhase = sin(t * 1.5f * PI.toFloat())
        val pulseAlpha = 0.08f + pulsePhase * 0.04f
        val brightPulse = if (level >= 3) 0.14f + pulsePhase * 0.08f else pulseAlpha

        // Level 5+: warning aura — faint red glow around entire coral
        if (level >= 5) {
            val auraRadius = baseH * 0.65f
            val auraPulse = 0.08f + sin(t * 1.0f * PI.toFloat()) * 0.05f
            drawCircle(
                color = AuraRed.copy(alpha = auraPulse),
                radius = auraRadius,
                center = Offset(x, y - baseH * 0.45f)
            )
        }

        // Base stalk — thick trunk from ground
        val stalkW = baseW * 0.6f
        val stalkH = baseH * 0.25f
        val stalkPath = Path().apply {
            moveTo(x - stalkW, y)
            cubicTo(
                x - stalkW * 0.9f, y - stalkH * 0.5f,
                x - stalkW * 0.5f, y - stalkH,
                x, y - stalkH
            )
            cubicTo(
                x + stalkW * 0.5f, y - stalkH,
                x + stalkW * 0.9f, y - stalkH * 0.5f,
                x + stalkW, y
            )
            close()
        }
        drawPath(stalkPath, CoralBase)

        // Main branches — tree-like structure
        val branchCount = when {
            level <= 2 -> 3
            level <= 4 -> 4
            else -> 5
        }

        val branchSpecs = listOf(
            BranchSpec(0f, 1.0f, 0f),           // center tall
            BranchSpec(-0.55f, 0.78f, -0.18f),  // left
            BranchSpec(0.50f, 0.72f, 0.15f),    // right
            BranchSpec(-0.30f, 0.60f, -0.08f),  // inner left
            BranchSpec(0.25f, 0.65f, 0.10f)     // inner right
        )

        for (i in 0 until branchCount) {
            val spec = branchSpecs[i]
            drawBranch(x, y, s, t, level, baseH, baseW, spec, i, brightPulse)
        }

        // Pulsing glow overlay on branches
        for (i in 0 until branchCount) {
            val spec = branchSpecs[i]
            val branchTop = y - stalkH - baseH * spec.heightFactor * 0.85f
            val branchCx = x + spec.xOffset * baseW * 2f
            drawCircle(
                color = GlowRed.copy(alpha = brightPulse),
                radius = baseW * 1.5f * spec.heightFactor,
                center = Offset(branchCx, branchTop + baseH * 0.2f)
            )
        }
    }

    private data class BranchSpec(
        val xOffset: Float,    // horizontal position factor
        val heightFactor: Float,
        val lean: Float        // lean direction
    )

    private fun DrawScope.drawBranch(
        x: Float, y: Float, s: Float, t: Float, level: Int,
        baseH: Float, baseW: Float, spec: BranchSpec, index: Int,
        pulseAlpha: Float
    ) {
        val stalkTop = y - baseH * 0.25f
        val branchH = baseH * spec.heightFactor * 0.85f
        val branchCx = x + spec.xOffset * baseW * 2f

        // Gentle sway
        val sway = sin(t * 0.6f * PI.toFloat() + index * 1.2f) * 1.5f * s

        // Main branch line — curved upward
        val segments = 6
        val segH = branchH / segments
        val strokeBase = (2.5f * s).coerceAtLeast(1.2f)

        var prevX = x + spec.xOffset * baseW * 0.8f
        var prevY = stalkTop

        for (seg in 1..segments) {
            val progress = seg.toFloat() / segments
            val segSway = sway * progress + spec.lean * baseW * progress * 2f
            val nextX = branchCx + segSway
            val nextY = stalkTop - segH * seg

            val color = when {
                progress < 0.3f -> CoralDeep
                progress < 0.6f -> CoralRed
                progress < 0.85f -> CoralOrange
                else -> CoralYellow
            }

            val strokeW = strokeBase * (1f - progress * 0.5f)
            drawLine(
                color = color,
                start = Offset(prevX, prevY),
                end = Offset(nextX, nextY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Sub-branches at intervals
            if (seg % 2 == 0 && seg < segments) {
                val subLen = branchH * 0.18f * (1f - progress * 0.3f)
                val subDir = if (seg % 4 == 0) -1f else 1f
                val subEndX = nextX + subDir * subLen + sway * 0.3f
                val subEndY = nextY - subLen * 0.6f
                drawLine(
                    color = CoralOrange,
                    start = Offset(nextX, nextY),
                    end = Offset(subEndX, subEndY),
                    strokeWidth = strokeW * 0.6f,
                    cap = StrokeCap.Round
                )

                // Hot tip on sub-branch
                drawCircle(
                    color = TipWhite.copy(alpha = 0.6f + pulseAlpha),
                    radius = (0.8f * s).coerceAtLeast(0.4f),
                    center = Offset(subEndX, subEndY)
                )
            }

            prevX = nextX
            prevY = nextY
        }

        // Hot tip on main branch
        drawCircle(
            color = TipWhite.copy(alpha = 0.7f + pulseAlpha),
            radius = (1.0f * s).coerceAtLeast(0.5f),
            center = Offset(prevX, prevY)
        )

        // Side nubs (bumpy texture along main branch)
        val nubCount = 2 + level / 2
        for (n in 1..nubCount) {
            val frac = n.toFloat() / (nubCount + 1)
            val nubY = stalkTop - branchH * frac
            val nubSway = sway * frac + spec.lean * baseW * frac * 2f
            val nubX = branchCx + nubSway
            val side = if (n % 2 == 0) 1f else -1f
            drawCircle(
                color = CoralRed.copy(alpha = 0.5f),
                radius = (1.2f * s * (1f - frac * 0.3f)).coerceAtLeast(0.5f),
                center = Offset(nubX + side * baseW * 0.5f, nubY)
            )
        }
    }
}
