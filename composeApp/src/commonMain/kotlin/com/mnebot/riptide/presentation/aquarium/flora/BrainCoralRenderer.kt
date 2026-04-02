package com.mnebot.riptide.presentation.aquarium.flora

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.mnebot.riptide.presentation.aquarium.PathPool
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

// ── Palette ───────────────────────────────────────────────────────────────────
private val CoralBase      = Color(0xFFD08060)   // warm salmon base
private val CoralDark      = Color(0xFFA85A38)   // shadowed underside
private val CoralLight     = Color(0xFFECAA80)   // lit top
private val CoralHighlight = Color(0xFFF8CCAA)   // specular highlight
private val GrooveColor    = Color(0xFF8A4020)   // maze groove lines

object BrainCoralRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val lobeCount = when {
            level <= 2 -> 1
            level <= 5 -> 2
            else       -> 3
        }

        for (i in 0 until lobeCount) {
            val offsetX = when (lobeCount) {
                1    -> 0f
                2    -> (i - 0.5f) * 9f * s
                else -> (i - 1f)  * 8f * s
            }
            val isCentral  = (i == lobeCount / 2)
            val sizeScale  = if (isCentral) 1.0f else 0.78f
            drawLobe(x + offsetX, y, s, level, sizeScale)
        }
    }

    // ── Single coral lobe ─────────────────────────────────────────────────────
    private fun DrawScope.drawLobe(
        cx: Float, baseY: Float, s: Float, level: Int, sizeScale: Float
    ) {
        val r   = (7f + level * 0.9f) * s * sizeScale
        val topY = baseY - r         // apex of the dome
        val k   = 0.552f             // Bézier circle constant

        // ── Dome shape (half-circle via Bézier) ───────────────────────────────
        val domePath = paths.obtain().apply {
            moveTo(cx - r, baseY)
            cubicTo(cx - r, baseY - r * k,  cx - r * k, topY,  cx,      topY)
            cubicTo(cx + r * k, topY,       cx + r, baseY - r * k,  cx + r, baseY)
            close()
        }

        drawPath(
            domePath,
            brush = Brush.verticalGradient(
                colors = listOf(CoralLight, CoralBase, CoralDark),
                startY = topY,
                endY   = baseY
            )
        )

        // ── Brain maze grooves ────────────────────────────────────────────────
        // Horizontal wavy lines at different heights, chord-clipped to the dome.
        // The dome is a semicircle with:
        //   • circle center at (cx, baseY)
        //   • radius r
        // For a row at height rowY: halfChord = sqrt(r² − (baseY−rowY)²)
        val grooveCount = (3 + level).coerceAtMost(9)
        val grooveStroke = (0.9f * s).coerceAtLeast(0.5f)

        for (g in 1..grooveCount) {
            val frac = g.toFloat() / (grooveCount + 1)
            val rowY = baseY - frac * r          // height of this groove row
            val dy   = baseY - rowY              // = frac * r
            val halfChordSq = r * r - dy * dy
            if (halfChordSq <= 0f) continue
            val halfChord = sqrt(halfChordSq)
            if (halfChord < 1.2f * s) continue   // skip tiny slivers near the top

            // Slightly different wave params per row for a maze-like appearance
            val phase     = g * 1.618f           // golden-ratio phase offset
            val waveFreq  = 3.0f + g * 0.35f
            val waveAmp   = halfChord * 0.10f

            val steps = 22
            val path  = paths.obtain()
            for (step in 0..steps) {
                val t  = step.toFloat() / steps
                val px = cx - halfChord + t * 2f * halfChord
                val py = rowY + sin(t * PI.toFloat() * waveFreq + phase) * waveAmp
                if (step == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(
                path,
                GrooveColor.copy(alpha = 0.38f),
                style = Stroke(width = grooveStroke, cap = StrokeCap.Round)
            )
        }

        // ── Soft specular highlight — upper-left quadrant ─────────────────────
        drawCircle(
            color  = CoralHighlight.copy(alpha = 0.38f),
            radius = r * 0.28f,
            center = Offset(cx - r * 0.27f, baseY - r * 0.72f)
        )

        // ── Level 3+: secondary diffuse glow ─────────────────────────────────
        if (level >= 3) {
            drawCircle(
                color  = CoralLight.copy(alpha = 0.16f),
                radius = r * 0.48f,
                center = Offset(cx - r * 0.12f, baseY - r * 0.55f)
            )
        }
    }
}
