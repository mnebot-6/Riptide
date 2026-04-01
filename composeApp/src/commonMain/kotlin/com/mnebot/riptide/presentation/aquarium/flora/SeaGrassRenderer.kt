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
private val GrassBase      = Color(0xFF1A7A3A)   // dark green base
private val GrassMid       = Color(0xFF2E9B50)   // mid-blade green
private val GrassTip       = Color(0xFF88CC44)   // yellow-green tip
private val GrassHighlight = Color(0xFFB0E860)   // bright tip highlight
private val BubbleColor    = Color(0xFFD0F0FF)   // rising bubbles (level 3+)
private val FishShadow     = Color(0xFF1A3A28)   // fish silhouettes (level 5+)

object SeaGrassRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bladeCount = when {
            level <= 2 -> 5
            level <= 4 -> 6
            else -> 7
        }

        // Blade positions — spread evenly with slight randomness (seeded by index)
        val spread = 8f * s
        for (i in 0 until bladeCount) {
            val frac = (i.toFloat() / (bladeCount - 1).coerceAtLeast(1)) - 0.5f
            val bx = x + frac * spread * 2f
            // Vary height per blade
            val heightMul = 0.65f + (((i * 7 + 3) % 5) / 5f) * 0.35f
            val bladeH = (14f + level * 2.5f) * s * heightMul
            val phase = i * 1.37f
            drawBlade(bx, y, s, t, bladeH, phase, level)
        }

        // Level 3+: small bubbles rising from the grass
        if (level >= 3) {
            val bubbleCount = if (level >= 5) 5 else 3
            for (b in 0 until bubbleCount) {
                val phase = b * 2.1f + 0.5f
                val cycle = ((t * 0.4f + phase) % 3.0f) / 3.0f
                val bx = x + sin(phase * 1.3f) * spread * 0.8f
                val by = y - cycle * 18f * s
                val bAlpha = (1f - cycle) * 0.45f
                drawCircle(
                    color = BubbleColor.copy(alpha = bAlpha),
                    radius = (0.5f + cycle * 0.3f) * s,
                    center = Offset(bx + sin(t * 1.5f + phase) * s, by)
                )
            }
        }

        // Level 5+: tiny fish silhouettes hiding in the grass
        if (level >= 5) {
            for (f in 0 until 2) {
                val phase = f * 3.7f
                val fishX = x + sin(t * 0.5f + phase) * spread * 0.5f
                val fishY = y - (6f + f * 5f) * s
                val fishSize = 2f * s
                drawFishSilhouette(fishX, fishY, fishSize, t, phase)
            }
        }
    }

    private fun DrawScope.drawBlade(
        bx: Float, baseY: Float, s: Float, t: Float,
        height: Float, phase: Float, level: Int
    ) {
        val segments = 10
        val segH = height / segments
        val strokeBase = (2.2f * s).coerceAtLeast(1.0f)

        var prevX = bx
        var prevY = baseY

        for (i in 1..segments) {
            val progress = i.toFloat() / segments

            // Independent sway per blade — grows stronger toward tip
            val sway = sin(t * 1.2f * PI.toFloat() + phase + progress * 1.5f) * 3f * s * progress
            val sway2 = sin(t * 0.8f * PI.toFloat() + phase * 0.7f + progress * 2f) * 1.5f * s * progress

            val nextX = bx + sway + sway2
            val nextY = baseY - segH * i

            // Color gradient: dark base to yellow-green tip
            val color = when {
                progress < 0.4f -> GrassBase
                progress < 0.7f -> GrassMid
                progress < 0.9f -> GrassTip
                else -> GrassHighlight
            }

            // Stroke tapers toward tip
            val strokeW = strokeBase * (1f - progress * 0.55f)

            drawLine(
                color = color,
                start = Offset(prevX, prevY),
                end = Offset(nextX, nextY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            prevX = nextX
            prevY = nextY
        }

        // Bright tip dot
        drawCircle(
            color = GrassHighlight.copy(alpha = 0.7f),
            radius = (0.8f * s).coerceAtLeast(0.4f),
            center = Offset(prevX, prevY)
        )
    }

    private fun DrawScope.drawFishSilhouette(
        fx: Float, fy: Float, sz: Float, t: Float, phase: Float
    ) {
        val sway = sin(t * 2f + phase) * sz * 0.3f
        val cx = fx + sway

        // Tiny fish body — simple diamond shape
        val bodyPath = Path().apply {
            moveTo(cx - sz, fy)
            lineTo(cx, fy - sz * 0.4f)
            lineTo(cx + sz * 0.7f, fy)
            lineTo(cx, fy + sz * 0.4f)
            close()
        }
        drawPath(bodyPath, FishShadow.copy(alpha = 0.35f))

        // Tail
        val tailPath = Path().apply {
            moveTo(cx + sz * 0.6f, fy)
            lineTo(cx + sz * 1.2f, fy - sz * 0.35f)
            lineTo(cx + sz * 1.2f, fy + sz * 0.35f)
            close()
        }
        drawPath(tailPath, FishShadow.copy(alpha = 0.30f))
    }
}
