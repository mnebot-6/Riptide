package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
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
private val UrchinBody  = Color(0xFF3A1040)  // dark purple
private val UrchinBand  = Color(0xFF4A2050)  // slightly lighter bands
private val UrchinSpine = Color(0xFF2A0830)  // spines
private val UrchinTip   = Color(0xFF6A3070)  // spine tips
private val UrchinMouth = Color(0xFF220820)  // mouth

object SeaUrchinRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        // y = base (floor). Draw upward from here.
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyR  = (7f + level * 0.6f) * s
        // Center of the urchin body sits on the floor
        val cx = x
        val cy = y - bodyR

        // ════════════════════════════════════════════════════════════════
        // SPINES — radiating outward, ~30 spines
        // ════════════════════════════════════════════════════════════════
        val spineCount = 28 + level * 2
        for (i in 0 until spineCount) {
            val baseAngle = i.toFloat() / spineCount * 2f * PI.toFloat()
            // Very subtle animation: ±2°
            val sway = sin(t * 0.8f + i * 0.3f) * (PI.toFloat() / 90f)
            val angle = baseAngle + sway

            // Spine length depends on angle (longest at equator/sides, shortest at top/bottom)
            val lengthMod = 1f - 0.35f * cos(angle * 2f).let { it * it }  // longer at sides
            val spineLen = (9f + level * 0.8f) * s * lengthMod

            val startX = cx + cos(angle) * bodyR * 0.92f
            val startY = cy + sin(angle) * bodyR * 0.92f
            val endX   = cx + cos(angle) * (bodyR + spineLen)
            val endY   = cy + sin(angle) * (bodyR + spineLen)

            // Don't draw spines going below the floor
            if (endY > y + 1f * s) continue

            val spineAlpha = if (i % 5 == 0) 0.85f else 0.65f
            drawLine(
                UrchinSpine.copy(alpha = spineAlpha),
                Offset(startX, startY),
                Offset(endX * 0.6f + startX * 0.4f, endY * 0.6f + startY * 0.4f),
                strokeWidth = (0.9f * s).coerceAtLeast(0.5f),
                cap = StrokeCap.Round
            )
            drawLine(
                UrchinTip.copy(alpha = 0.55f),
                Offset(endX * 0.6f + startX * 0.4f, endY * 0.6f + startY * 0.4f),
                Offset(endX, endY),
                strokeWidth = (0.6f * s).coerceAtLeast(0.4f),
                cap = StrokeCap.Round
            )
        }

        // ════════════════════════════════════════════════════════════════
        // BODY — hemispherical
        // ════════════════════════════════════════════════════════════════
        drawCircle(UrchinBody, bodyR, Offset(cx, cy))

        // 5-fold radial bands
        for (band in 0 until 5) {
            val bandAngle = band.toFloat() / 5f * 2f * PI.toFloat() - PI.toFloat() / 2f
            val bx0 = cx
            val by0 = cy
            val bx1 = cx + cos(bandAngle) * bodyR * 0.85f
            val by1 = cy + sin(bandAngle) * bodyR * 0.85f
            // Only draw bands going upward (not below floor)
            if (by1 < y) {
                drawLine(
                    UrchinBand.copy(alpha = 0.35f),
                    Offset(bx0, by0),
                    Offset(bx1, by1),
                    strokeWidth = (1.8f * s).coerceAtLeast(0.9f),
                    cap = StrokeCap.Round
                )
            }
        }

        // Body highlight (top)
        drawCircle(UrchinBand.copy(alpha = 0.20f), bodyR * 0.55f,
            Offset(cx - bodyR * 0.15f, cy - bodyR * 0.25f))

        // Body outline
        drawCircle(UrchinSpine.copy(alpha = 0.35f), bodyR, Offset(cx, cy),
            style = Stroke(width = (0.7f * s).coerceAtLeast(0.4f)))

        // ════════════════════════════════════════════════════════════════
        // MOUTH — tiny circle at top
        // ════════════════════════════════════════════════════════════════
        drawCircle(UrchinMouth, 0.9f * s, Offset(cx, cy - bodyR * 0.78f))
    }
}
