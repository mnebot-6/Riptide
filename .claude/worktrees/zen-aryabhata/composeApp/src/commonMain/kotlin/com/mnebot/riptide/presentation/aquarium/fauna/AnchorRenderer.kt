package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val AnchorIron  = Color(0xFF3A3A48)  // dark iron grey
private val AnchorIron2 = Color(0xFF5A5A68)  // lighter iron highlight
private val AnchorRust  = Color(0xFF8B4020)  // rust accent
private val AnchorChain = Color(0xFF5A5A60)  // chain links

object AnchorRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        // Fixed floor creature — y is base, draw upward
        val s = size / 28f
        val t = animTimeMs / 1000f

        val anchorH  = (18f + level * 0.8f) * s  // total height
        val crossW   = (10f + level * 0.5f) * s  // crossbar width
        val armW     = (8f  + level * 0.4f) * s  // arm span (half)
        val shaftW   = (1.8f + level * 0.08f) * s

        val topY  = y - anchorH         // top of anchor (ring)
        val crossY = topY + anchorH * 0.20f   // crossbar position
        val stockY = topY + anchorH * 0.22f   // bottom of ring area, top of shaft
        val armY  = y - anchorH * 0.08f       // where arms branch (near bottom)

        // ── RING (at top) ───────────────────────────────────────────────────────
        val ringR = (3.5f + level * 0.18f) * s
        drawCircle(
            AnchorIron, ringR,
            center = Offset(x, topY + ringR),
            style = Stroke(width = (shaftW * 0.9f).coerceAtLeast(0.8f))
        )

        // ── CROSSBAR (stock) ────────────────────────────────────────────────────
        val cross = Path().apply {
            moveTo(x - crossW, crossY - shaftW * 0.7f)
            lineTo(x + crossW, crossY - shaftW * 0.7f)
            lineTo(x + crossW, crossY + shaftW * 0.7f)
            lineTo(x - crossW, crossY + shaftW * 0.7f)
            close()
        }
        drawPath(cross, AnchorIron)
        // Crossbar caps (thicker ends)
        for (capX in listOf(x - crossW, x + crossW)) {
            drawCircle(AnchorIron, shaftW * 1.1f, Offset(capX, crossY))
        }
        // Centre shaft cap
        drawCircle(AnchorIron2, shaftW * 1.1f, Offset(x, crossY))

        // ── MAIN SHAFT ──────────────────────────────────────────────────────────
        drawLine(
            AnchorIron,
            Offset(x, stockY),
            Offset(x, armY),
            strokeWidth = (shaftW * 2f).coerceAtLeast(1.5f),
            cap = StrokeCap.Round
        )

        // ── ARMS (curved arms at bottom) ────────────────────────────────────────
        val arm = Path().apply {
            // Left arm
            moveTo(x, armY)
            cubicTo(x - armW * 0.5f, armY - anchorH * 0.05f,
                x - armW * 0.85f, armY + anchorH * 0.03f,
                x - armW, armY - anchorH * 0.02f)
        }
        drawPath(arm, AnchorIron, style = Stroke(width = (shaftW * 1.8f).coerceAtLeast(1.2f), cap = StrokeCap.Round))

        val armRight = Path().apply {
            moveTo(x, armY)
            cubicTo(x + armW * 0.5f, armY - anchorH * 0.05f,
                x + armW * 0.85f, armY + anchorH * 0.03f,
                x + armW, armY - anchorH * 0.02f)
        }
        drawPath(armRight, AnchorIron, style = Stroke(width = (shaftW * 1.8f).coerceAtLeast(1.2f), cap = StrokeCap.Round))

        // ── FLUKES (tips of arms — diamond-shaped) ──────────────────────────────
        for (side in listOf(-1f, 1f)) {
            val flukeTipX = x + side * armW
            val flukeTipY = armY - anchorH * 0.02f
            val flukeH = anchorH * 0.10f
            val flukeW = armW * 0.22f
            val fluke = Path().apply {
                moveTo(flukeTipX, flukeTipY - flukeH)
                lineTo(flukeTipX + side * flukeW, flukeTipY)
                lineTo(flukeTipX, flukeTipY + flukeH * 0.5f)
                lineTo(flukeTipX - side * flukeW, flukeTipY)
                close()
            }
            drawPath(fluke, AnchorIron)
            drawPath(fluke, AnchorIron2.copy(alpha = 0.40f),
                style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))
        }

        // Crown at bottom centre
        drawCircle(AnchorIron, shaftW * 1.3f, Offset(x, armY))

        // ── SHAFT HIGHLIGHT ─────────────────────────────────────────────────────
        drawLine(
            AnchorIron2.copy(alpha = 0.50f),
            Offset(x - shaftW * 0.4f, stockY + anchorH * 0.04f),
            Offset(x - shaftW * 0.4f, armY - anchorH * 0.02f),
            strokeWidth = (shaftW * 0.5f).coerceAtLeast(0.4f),
            cap = StrokeCap.Round
        )

        // ── LEVEL 2+: Chain hanging from ring ─────────────────────────────────
        if (level >= 2) {
            val chainLen  = (8f + level * 0.8f) * s
            val chainEndX = x + chainLen * 0.8f
            val chainEndY = topY + ringR * 2f + chainLen
            // Draw chain as alternating oval links
            val linkCount = (4 + level / 2).coerceAtMost(10)
            for (lk in 0 until linkCount) {
                val lf = lk.toFloat() / (linkCount - 1f)
                val lx = x + ringR + (chainEndX - x - ringR) * lf
                val ly = topY + ringR * 1.8f + (chainEndY - topY - ringR * 1.8f) * lf
                val lAngle = if (lk % 2 == 0) 0f else 90f
                drawCircle(
                    AnchorChain,
                    (1.2f * s).coerceAtLeast(0.5f),
                    Offset(lx, ly),
                    style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f))
                )
            }
        }

        // ── LEVEL 4+: Rust patches ─────────────────────────────────────────────
        if (level >= 4) {
            val rustPositions = listOf(
                Offset(x - shaftW * 0.5f, crossY + anchorH * 0.15f),
                Offset(x + armW * 0.55f, armY + anchorH * 0.01f),
                Offset(x - armW * 0.6f,  armY - anchorH * 0.03f),
            )
            for (rp in rustPositions) {
                drawCircle(AnchorRust.copy(alpha = 0.55f), (1.8f * s).coerceAtLeast(0.8f), rp)
            }
        }
    }
}
