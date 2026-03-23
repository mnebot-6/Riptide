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
private val ShipWood   = Color(0xFF4A3020)  // dark aged hull wood
private val ShipWood2  = Color(0xFF6A4A30)  // lighter plank
private val ShipWood3  = Color(0xFF2A1808)  // deep shadow
private val ShipMetal  = Color(0xFF3A3A4A)  // iron fittings
private val ShipRust   = Color(0xFF8B4018)  // rust
private val ShipCoral  = Color(0xFFDD5533)  // coral growth
private val ShipCoral2 = Color(0xFFFF9966)  // lighter coral
private val ShipKelp   = Color(0xFF3A6A20)  // seaweed
private val ShipPort   = Color(0xFF112233)  // porthole glass

object SunkenShipRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        // Fixed floor creature — y is base, draw upward
        val s = size / 28f
        val t = animTimeMs / 1000f

        val hullW  = (22f + level * 1.0f) * s
        val hullH  = (12f + level * 0.5f) * s
        val deckH  = (4f  + level * 0.2f) * s   // deck above waterline
        val mastH  = (14f + level * 0.7f) * s

        val hullBaseY = y             // keel (bottom)
        val deckY     = y - hullH     // deck level
        val mastBaseX = x - hullW * 0.2f   // broken mast position

        // ── SEAWEED (level 2+) — drawn behind ship ─────────────────────────────
        if (level >= 2) {
            val seaweedCount = 3 + level / 3
            for (sw in 0 until seaweedCount) {
                val swf  = sw.toFloat() / (seaweedCount - 1f)
                val swx  = x - hullW * 0.7f + hullW * 1.4f * swf
                val swH  = (5f + sw * 2f + level * 0.3f) * s
                val swSway = sin(t * 1.5f * PI.toFloat() + sw * 0.8f) * swH * 0.3f
                val weed = Path().apply {
                    moveTo(swx, hullBaseY)
                    cubicTo(swx + swSway * 0.3f, hullBaseY - swH * 0.4f,
                        swx + swSway * 0.7f, hullBaseY - swH * 0.7f,
                        swx + swSway, hullBaseY - swH)
                }
                drawPath(weed, ShipKelp.copy(alpha = 0.75f),
                    style = Stroke(width = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round))
            }
        }

        // ── HULL ──────────────────────────────────────────────────────────────
        val hull = Path().apply {
            // Keel (pointed at ends, widest amidships)
            moveTo(x - hullW, hullBaseY - hullH * 0.15f)  // stern bow
            cubicTo(x - hullW * 0.8f, hullBaseY,
                x + hullW * 0.8f, hullBaseY,
                x + hullW, hullBaseY - hullH * 0.15f)    // stern stern
            // Topsides
            lineTo(x + hullW * 0.85f, deckY)
            lineTo(x - hullW * 0.85f, deckY)
            close()
        }
        drawPath(hull, ShipWood)

        // Plank lines (hull planking)
        val plankCount = 6 + level / 3
        for (p in 1..plankCount) {
            val pf = p.toFloat() / (plankCount + 1f)
            val py = hullBaseY - hullH * pf
            val pw = hullW * (0.7f + (1f - pf) * 0.15f)
            drawLine(ShipWood3.copy(alpha = 0.40f),
                Offset(x - pw * 0.85f, py), Offset(x + pw * 0.85f, py),
                strokeWidth = (0.7f * s).coerceAtLeast(0.3f))
        }

        // Hull highlight (lighter upper section)
        drawLine(ShipWood2.copy(alpha = 0.50f),
            Offset(x - hullW * 0.82f, deckY + hullH * 0.12f),
            Offset(x + hullW * 0.82f, deckY + hullH * 0.12f),
            strokeWidth = (hullH * 0.20f).coerceAtLeast(2.0f))

        // Hull outline
        drawPath(hull, ShipWood3.copy(alpha = 0.45f),
            style = Stroke(width = (1.0f * s).coerceAtLeast(0.5f)))

        // ── DECK RAILING ──────────────────────────────────────────────────────
        drawLine(ShipWood2,
            Offset(x - hullW * 0.85f, deckY),
            Offset(x + hullW * 0.85f, deckY),
            strokeWidth = (2.5f * s).coerceAtLeast(1.0f))

        // ── BROKEN MAST ───────────────────────────────────────────────────────
        val mastBreakY = deckY - mastH * 0.55f  // where the mast snaps
        // Lower mast section (still standing, slightly tilted)
        val mastPath = Path().apply {
            moveTo(mastBaseX, deckY)
            lineTo(mastBaseX - mastH * 0.04f, mastBreakY)
        }
        drawPath(mastPath, ShipWood2,
            style = Stroke(width = (3.5f * s).coerceAtLeast(2.0f), cap = StrokeCap.Round))

        // Upper mast section (fallen, angled to one side)
        val fallMast = Path().apply {
            moveTo(mastBaseX - mastH * 0.04f, mastBreakY)
            cubicTo(
                mastBaseX - mastH * 0.04f + hullW * 0.15f, mastBreakY - mastH * 0.10f,
                mastBaseX + hullW * 0.45f,               mastBreakY + mastH * 0.12f,
                mastBaseX + hullW * 0.7f,                mastBreakY + mastH * 0.40f
            )
        }
        drawPath(fallMast, ShipWood3,
            style = Stroke(width = (2.8f * s).coerceAtLeast(1.5f), cap = StrokeCap.Round))

        // Torn rigging
        drawLine(ShipMetal.copy(alpha = 0.50f),
            Offset(mastBaseX - mastH * 0.04f, mastBreakY),
            Offset(mastBaseX + hullW * 0.5f, deckY + hullH * 0.2f),
            strokeWidth = (0.6f * s).coerceAtLeast(0.3f))

        // ── PORTHOLES ─────────────────────────────────────────────────────────
        val portHoles = listOf(
            Pair(x - hullW * 0.45f, hullBaseY - hullH * 0.55f),
            Pair(x + hullW * 0.10f, hullBaseY - hullH * 0.50f),
            Pair(x + hullW * 0.55f, hullBaseY - hullH * 0.48f),
        )
        val portR = (2.2f + level * 0.1f) * s
        for ((px, py) in portHoles) {
            drawCircle(ShipMetal, portR * 1.15f, Offset(px, py))
            drawCircle(ShipPort, portR * 0.85f, Offset(px, py))
            // Porthole glass sheen
            drawCircle(Color.White.copy(alpha = 0.20f), portR * 0.45f,
                Offset(px - portR * 0.25f, py - portR * 0.25f))
            drawCircle(ShipMetal.copy(alpha = 0.55f), portR * 1.1f, Offset(px, py),
                style = Stroke(width = (0.8f * s).coerceAtLeast(0.3f)))
        }

        // ── RUST STREAKS ──────────────────────────────────────────────────────
        val rustPositions = listOf(
            Pair(x - hullW * 0.55f, hullBaseY - hullH * 0.30f),
            Pair(x + hullW * 0.40f, hullBaseY - hullH * 0.60f),
            Pair(x - hullW * 0.10f, hullBaseY - hullH * 0.15f),
        )
        for ((rx, ry) in rustPositions) {
            drawLine(ShipRust.copy(alpha = 0.45f),
                Offset(rx, ry),
                Offset(rx + hullW * 0.04f, ry + hullH * 0.18f),
                strokeWidth = (1.0f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round)
        }

        // ── LEVEL 3+: Coral growth ─────────────────────────────────────────────
        if (level >= 3) {
            val coralCount = 8 + level * 2
            val coralPositions = listOf(
                Pair(x - hullW * 0.70f, hullBaseY - hullH * 0.20f),
                Pair(x - hullW * 0.40f, hullBaseY - hullH * 0.40f),
                Pair(x + hullW * 0.60f, hullBaseY - hullH * 0.30f),
                Pair(x + hullW * 0.30f, hullBaseY - hullH * 0.55f),
                Pair(x - hullW * 0.05f, hullBaseY - hullH * 0.70f),
                Pair(x + hullW * 0.75f, deckY + hullH * 0.08f),
            )
            for ((i, cp) in coralPositions.withIndex()) {
                val (cx2, cy2) = cp
                val coralR = (1.5f + i * 0.3f + level * 0.1f) * s
                val pulseCoral = (sin(t * (0.6f + i * 0.1f) * PI.toFloat()) * 0.5f + 0.5f) * 0.2f
                drawCircle(ShipCoral.copy(alpha = 0.75f + pulseCoral.toFloat()),
                    coralR, Offset(cx2, cy2))
                drawCircle(ShipCoral2.copy(alpha = 0.45f), coralR * 0.6f, Offset(cx2, cy2))
            }
        }

        // ── LEVEL 5+: Small fish silhouettes near portholes ───────────────────
        if (level >= 5) {
            val fishAnim = sin(t * 2.5f * PI.toFloat())
            for ((px, py) in portHoles.take(2)) {
                val fOff = fishAnim * portR * 1.5f
                drawLine(ShipKelp.copy(alpha = 0.60f),
                    Offset(px + fOff, py + portR * 2f),
                    Offset(px + fOff + portR, py + portR * 2f),
                    strokeWidth = (portR * 0.5f).coerceAtLeast(0.5f), cap = StrokeCap.Round)
            }
        }
    }
}
