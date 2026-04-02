package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.mnebot.riptide.presentation.aquarium.PathPool
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val BarnShell = Color(0xFFC5C5B5)  // outer shell
private val BarnPlate = Color(0xFF959585)  // plate texture
private val BarnInner = Color(0xFF555545)  // interior
private val BarnCirri = Color(0xFF887755)  // feeding appendages

// Barnacle cluster layout: (xOffset fraction of size, height fraction, width fraction)
private val barnacleLayout = listOf(
    Triple( 0.00f, 0.55f, 1.00f),  // center, tallest
    Triple(-0.30f, 0.40f, 0.80f),  // left
    Triple( 0.28f, 0.45f, 0.85f),  // right
    Triple(-0.55f, 0.28f, 0.65f),  // far left, small
    Triple( 0.52f, 0.30f, 0.68f),  // far right, small
    Triple(-0.18f, 0.22f, 0.55f),  // front small (overlap)
    Triple( 0.18f, 0.24f, 0.58f),  // front small right
)

object BarnacleRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        // y = base (floor). Draw upward.
        val s = size / 28f
        val t = animTimeMs / 1000f

        val clusterW = (16f + level * 1.5f) * s
        val baseH    = (14f + level * 1.0f) * s

        // Draw each barnacle in the cluster
        for ((xFrac, hFrac, wFrac) in barnacleLayout) {
            val bx = x + xFrac * clusterW
            val bh = baseH * hFrac
            val bw = clusterW * wFrac * 0.32f

            drawSingleBarnacle(bx, y, bw, bh, level, t, s)
        }
    }

    private fun DrawScope.drawSingleBarnacle(
        cx: Float, baseY: Float,
        halfW: Float, height: Float,
        level: Int, t: Float, s: Float
    ) {
        // Cone / volcano shape
        val topY = baseY - height
        val openingW = halfW * 0.55f

        val shell = paths.obtain().apply {
            moveTo(cx - halfW, baseY)
            cubicTo(
                cx - halfW * 0.8f, baseY - height * 0.3f,
                cx - openingW - 1f * s, topY + height * 0.1f,
                cx - openingW, topY
            )
            // Top opening (slightly inward-sloping walls)
            lineTo(cx + openingW, topY)
            cubicTo(
                cx + openingW + 1f * s, topY + height * 0.1f,
                cx + halfW * 0.8f, baseY - height * 0.3f,
                cx + halfW, baseY
            )
            close()
        }
        drawPath(shell, BarnShell)

        // Plate texture lines on the shell
        val plateCount = 3
        for (p in 1..plateCount) {
            val pFrac = p.toFloat() / (plateCount + 1)
            val py    = baseY - height * pFrac * 0.85f
            val pw    = halfW * (1f - pFrac * 0.35f) + openingW * pFrac * 0.4f
            drawLine(
                BarnPlate.copy(alpha = 0.45f),
                Offset(cx - pw, py),
                Offset(cx + pw, py),
                strokeWidth = (0.7f * s).coerceAtLeast(0.4f),
                cap = StrokeCap.Round
            )
        }

        // Interior (dark opening at top)
        val interior = paths.obtain().apply {
            moveTo(cx - openingW, topY)
            lineTo(cx + openingW, topY)
            lineTo(cx + openingW * 0.7f, topY + height * 0.12f)
            lineTo(cx - openingW * 0.7f, topY + height * 0.12f)
            close()
        }
        drawPath(interior, BarnInner)

        // Shell outline
        drawPath(shell, BarnPlate.copy(alpha = 0.40f),
            style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))

        // ────────────────────────────────────────────────────────────────
        // LEVEL 3+: Cirri (feeding appendages waving out of opening)
        // ────────────────────────────────────────────────────────────────
        if (level >= 3) {
            val cirriCount = 4
            for (c in 0 until cirriCount) {
                val cFrac   = (c.toFloat() / (cirriCount - 1)) - 0.5f
                val cBaseX  = cx + cFrac * openingW * 1.3f
                val cSway   = sin(t * 2f * PI.toFloat() + c * 0.8f) * halfW * 0.4f
                val cirriLen = height * (0.22f + c % 2 * 0.10f)
                val cirri = paths.obtain().apply {
                    moveTo(cBaseX, topY)
                    quadraticTo(
                        cBaseX + cSway * 0.5f, topY - cirriLen * 0.5f,
                        cBaseX + cSway, topY - cirriLen
                    )
                }
                drawPath(cirri, BarnCirri.copy(alpha = 0.70f),
                    style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round))
            }
        }
    }
}
