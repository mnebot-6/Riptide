package com.mnebot.riptide.presentation.aquarium.flora

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.mnebot.riptide.presentation.aquarium.PathPool
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ── Palette ───────────────────────────────────────────────────────────────────
private val FanBranch = Color(0xFFD04010)  // main branches
private val FanMesh   = Color(0xFFE05020)  // connecting mesh
private val FanBase   = Color(0xFFB03000)  // stalk base
private val FanPolyp  = Color(0xFFFFEECC)  // polyp tips (level 5+)

// Precomputed branch tree: each branch is (startFrac, endFrac, depth)
// expressed as (startX, startY, endX, endY) in normalized units [-1..1, 0..1]
// We'll compute them at render time using a recursive-style expansion

private data class Branch(
    val x0: Float, val y0: Float,
    val x1: Float, val y1: Float,
    val depth: Int
)

private fun buildFanTree(maxDepth: Int): List<Branch> {
    val branches = mutableListOf<Branch>()

    fun addBranch(x0: Float, y0: Float, x1: Float, y1: Float, depth: Int) {
        branches.add(Branch(x0, y0, x1, y1, depth))
        if (depth >= maxDepth) return

        val dx = x1 - x0
        val dy = y1 - y0
        val len = sqrt(dx * dx + dy * dy) * 0.65f
        val angle = atan2(dy, dx)

        val leftAngle  = angle - 0.45f
        val rightAngle = angle + 0.45f

        addBranch(x1, y1, x1 + cos(leftAngle) * len,  y1 + sin(leftAngle) * len,  depth + 1)
        addBranch(x1, y1, x1 + cos(rightAngle) * len, y1 + sin(rightAngle) * len, depth + 1)
    }

    // Stalk from base up
    addBranch(0f, 0f, 0f, -0.25f, 0)
    return branches
}

object FanCoralRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        // y = base (floor). Draw upward.
        val s = size / 28f
        val t = animTimeMs / 1000f

        val fanH  = (26f + level * 2f) * s   // total height of the fan
        val fanW  = (20f + level * 1.5f) * s  // half-width of fan
        // Global sway: gentle tilt of whole fan
        val sway  = sin(t * 0.7f * PI.toFloat()) * 2f * s

        val maxDepth = when {
            level <= 2 -> 4
            level <= 5 -> 5
            else -> 6
        }

        val tree = buildFanTree(maxDepth)

        // Draw mesh connections between branches (level 3+)
        if (level >= 3) {
            val leafBranches = tree.filter { it.depth == maxDepth }
            // Connect nearby leaf endpoints
            for (i in leafBranches.indices) {
                for (j in i + 1 until leafBranches.size) {
                    val b1 = leafBranches[i]
                    val b2 = leafBranches[j]
                    val dist = sqrt((b1.x1 - b2.x1).let { it * it } + (b1.y1 - b2.y1).let { it * it })
                    if (dist < 0.28f) {
                        val mx1 = x + b1.x1 * fanW + sway * (-b1.y1)
                        val my1 = y + b1.y1 * fanH
                        val mx2 = x + b2.x1 * fanW + sway * (-b2.y1)
                        val my2 = y + b2.y1 * fanH
                        drawLine(
                            FanMesh.copy(alpha = 0.35f),
                            Offset(mx1, my1),
                            Offset(mx2, my2),
                            strokeWidth = (0.5f * s).coerceAtLeast(0.3f),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // Draw branches from deepest to shallowest (painter's algorithm)
        for (branch in tree.sortedByDescending { it.depth }) {
            val strokeW = when (branch.depth) {
                0 -> (4.0f * s).coerceAtLeast(2.0f)
                1 -> (2.5f * s).coerceAtLeast(1.2f)
                2 -> (1.8f * s).coerceAtLeast(0.9f)
                3 -> (1.2f * s).coerceAtLeast(0.6f)
                4 -> (0.8f * s).coerceAtLeast(0.4f)
                else -> (0.6f * s).coerceAtLeast(0.3f)
            }

            // Apply sway based on height (higher = more sway)
            val swayFactor0 = -branch.y0
            val swayFactor1 = -branch.y1

            val bx0 = x + branch.x0 * fanW + sway * swayFactor0
            val by0 = y + branch.y0 * fanH
            val bx1 = x + branch.x1 * fanW + sway * swayFactor1
            val by1 = y + branch.y1 * fanH

            val color = if (branch.depth == 0) FanBase else FanBranch

            drawLine(
                color,
                Offset(bx0, by0),
                Offset(bx1, by1),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        // Level 5+: polyp circles at branch tips
        if (level >= 5) {
            val tipBranches = tree.filter { it.depth == maxDepth }
            for (b in tipBranches) {
                val swayF = -b.y1
                val tx = x + b.x1 * fanW + sway * swayF
                val ty = y + b.y1 * fanH
                drawCircle(FanPolyp, (0.9f * s).coerceAtLeast(0.5f), Offset(tx, ty))
            }
        }

        // Base stalk reinforcement
        drawLine(
            FanBase,
            Offset(x, y),
            Offset(x + sway * 0.1f, y - fanH * 0.12f),
            strokeWidth = (5f * s).coerceAtLeast(2.5f),
            cap = StrokeCap.Round
        )
    }
}
