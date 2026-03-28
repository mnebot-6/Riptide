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
private val ChestWood  = Color(0xFF6B4010)  // dark wood
private val ChestWood2 = Color(0xFF8B5A20)  // lighter plank
private val ChestGold  = Color(0xFFD4A820)  // metal bands
private val ChestGold2 = Color(0xFFFFCC00)  // bright gold glow
private val ChestGem   = Color(0xFF4488FF)  // gem color inside
private val ChestRed   = Color(0xFFCC2222)  // red gem accent
private val ChestDark  = Color(0xFF3A1A08)  // deep shadow

object TreasureChestRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        // Fixed floor creature — y is base (floor), draw upward
        val s = size / 28f
        val t = animTimeMs / 1000f

        val chestW = (13f + level * 0.6f) * s
        val chestH = (9f  + level * 0.4f) * s
        val lidH   = (5f  + level * 0.3f) * s

        val centerY = y - chestH * 0.5f - lidH * 0.5f
        val baseY   = y                    // bottom of chest
        val topY    = y - chestH - lidH   // top of lid (closed)

        // Level 5+: lid opens slightly
        val lidOpen = if (level >= 5) {
            sin(t * 0.5f * PI.toFloat()) * lidH * 0.45f
        } else 0f

        // Level 3+: glow pulsing
        val glowPulse = if (level >= 3) {
            (sin(t * 1.2f * PI.toFloat()) * 0.5f + 0.5f) * 0.45f + 0.15f
        } else 0f

        // ── GLOW BEHIND CHEST ─────────────────────────────────────────────────
        if (level >= 3) {
            drawCircle(ChestGold2.copy(alpha = glowPulse * 0.6f),
                chestW * 1.2f, Offset(x, centerY))
        }

        // ── MAIN CHEST BOX ────────────────────────────────────────────────────
        val box = Path().apply {
            moveTo(x - chestW, baseY)
            lineTo(x - chestW, baseY - chestH)
            lineTo(x + chestW, baseY - chestH)
            lineTo(x + chestW, baseY)
            close()
        }
        drawPath(box, ChestWood)

        // Wood planks (horizontal lines)
        val plankCount = 3
        for (p in 1..plankCount) {
            val py = baseY - chestH * p.toFloat() / (plankCount + 1f)
            drawLine(ChestDark.copy(alpha = 0.40f),
                Offset(x - chestW, py), Offset(x + chestW, py),
                strokeWidth = (0.7f * s).coerceAtLeast(0.3f))
        }
        // Vertical grain lines
        for (v in listOf(-0.3f, 0.0f, 0.3f)) {
            drawLine(ChestWood2.copy(alpha = 0.30f),
                Offset(x + v * chestW, baseY - chestH),
                Offset(x + v * chestW, baseY),
                strokeWidth = (0.6f * s).coerceAtLeast(0.3f))
        }

        // ── GOLD BANDS (horizontal metal straps) ──────────────────────────────
        for (bandY in listOf(baseY - chestH * 0.25f, baseY - chestH * 0.70f)) {
            val band = Path().apply {
                moveTo(x - chestW, bandY - 1.2f * s)
                lineTo(x + chestW, bandY - 1.2f * s)
                lineTo(x + chestW, bandY + 1.2f * s)
                lineTo(x - chestW, bandY + 1.2f * s)
                close()
            }
            drawPath(band, ChestGold)
            // Rivet dots
            for (rv in listOf(-0.7f, 0.0f, 0.7f)) {
                drawCircle(ChestGold2, (0.9f * s).coerceAtLeast(0.4f), Offset(x + rv * chestW, bandY))
            }
        }

        // ── CORNER BRACKETS (gold corners) ────────────────────────────────────
        val bracketSize = 2.0f * s
        for ((cx2, cy2) in listOf(
            Pair(x - chestW, baseY - chestH), Pair(x + chestW, baseY - chestH),
            Pair(x - chestW, baseY),          Pair(x + chestW, baseY)
        )) {
            drawCircle(ChestGold, bracketSize, Offset(cx2, cy2))
        }

        // Box outline
        drawPath(box, ChestDark.copy(alpha = 0.40f),
            style = Stroke(width = (1.0f * s).coerceAtLeast(0.5f)))

        // ── LID (rounded top) ─────────────────────────────────────────────────
        val lidBaseY = baseY - chestH
        val lid = Path().apply {
            moveTo(x - chestW, lidBaseY - lidOpen)
            cubicTo(x - chestW * 0.8f, lidBaseY - lidH - lidOpen,
                x + chestW * 0.8f, lidBaseY - lidH - lidOpen,
                x + chestW, lidBaseY - lidOpen)
            lineTo(x + chestW, lidBaseY)
            lineTo(x - chestW, lidBaseY)
            close()
        }
        drawPath(lid, ChestWood2)

        // Lid bands
        val lidBandY = lidBaseY - lidH * 0.5f - lidOpen
        drawLine(ChestGold,
            Offset(x - chestW + bracketSize, lidBandY),
            Offset(x + chestW - bracketSize, lidBandY),
            strokeWidth = (2.0f * s).coerceAtLeast(0.8f))

        // Lid corner brackets
        for ((cx2) in listOf(Pair(x - chestW, 0f), Pair(x + chestW, 0f))) {
            drawCircle(ChestGold, bracketSize, Offset(cx2, lidBaseY))
        }

        // Lid outline
        drawPath(lid, ChestDark.copy(alpha = 0.35f),
            style = Stroke(width = (0.9f * s).coerceAtLeast(0.4f)))

        // ── LOCK (central clasp) ───────────────────────────────────────────────
        val lockX = x
        val lockY = lidBaseY - lidOpen * 0.4f
        val lockW = 2.8f * s
        val lockH = 2.2f * s
        // Lock body (rectangle)
        val lockBody = Path().apply {
            moveTo(lockX - lockW, lockY)
            lineTo(lockX + lockW, lockY)
            lineTo(lockX + lockW, lockY + lockH)
            lineTo(lockX - lockW, lockY + lockH)
            close()
        }
        drawPath(lockBody, ChestGold2)
        drawPath(lockBody, ChestGold.copy(alpha = 0.60f),
            style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))
        // Lock shackle (arch above)
        val shackle = Path().apply {
            moveTo(lockX - lockW * 0.5f, lockY)
            cubicTo(lockX - lockW * 0.5f, lockY - lockH * 1.2f,
                lockX + lockW * 0.5f, lockY - lockH * 1.2f,
                lockX + lockW * 0.5f, lockY)
        }
        drawPath(shackle, ChestGold, style = Stroke(width = (1.5f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round))

        // ── LEVEL 5+: Coins and gems spilling out ─────────────────────────────
        if (level >= 5) {
            val gemPositions = listOf(
                Pair(x - chestW * 0.3f, lidBaseY - lidH * 0.3f - lidOpen * 0.8f),
                Pair(x + chestW * 0.2f, lidBaseY - lidH * 0.2f - lidOpen * 0.9f),
                Pair(x - chestW * 0.1f, lidBaseY - lidH * 0.5f - lidOpen),
            )
            for ((gx, gy) in gemPositions) {
                drawCircle(ChestGold2, (1.5f * s).coerceAtLeast(0.8f), Offset(gx, gy))
                drawCircle(ChestGold2.copy(alpha = glowPulse * 0.8f), (3f * s).coerceAtLeast(1.5f), Offset(gx, gy))
            }
            // Blue gem
            drawCircle(ChestGem.copy(alpha = 0.90f), (1.8f * s).coerceAtLeast(0.9f),
                Offset(x - chestW * 0.5f, lidBaseY - lidH * 0.6f - lidOpen))
            // Red gem
            drawCircle(ChestRed.copy(alpha = 0.85f), (1.5f * s).coerceAtLeast(0.7f),
                Offset(x + chestW * 0.4f, lidBaseY - lidH * 0.7f - lidOpen * 0.9f))

            // Glow from inside
            drawCircle(ChestGold2.copy(alpha = glowPulse * 0.7f),
                chestW * 0.7f, Offset(x, lidBaseY - lidH * 0.5f - lidOpen * 0.5f))
        }
    }
}
