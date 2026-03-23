package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val LobRed    = Color(0xFFB82020)  // body red
private val LobRed2   = Color(0xFFD84030)  // brighter carapace
private val LobJoint  = Color(0xFF882010)  // darker joint/segment
private val LobOrange = Color(0xFFE86030)  // leg & claw highlight
private val LobBlack  = Color(0xFF1A1010)  // outlines
private val LobWhite  = Color(0xFFF0D0C0)  // underbelly, eye

object LobsterRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        // Walking leg animation
        val legWalk = sin(t * 4.0f * PI.toFloat())

        // Body proportions: head forward (left when going right), tail segments behind
        val headW  = (7f + level * 0.4f) * s
        val headH  = (5f + level * 0.3f) * s
        val segLen = (3.5f + level * 0.25f) * s
        val segH   = (3.5f + level * 0.25f) * s
        val segCount = 5
        val totalBodyLen = headW + segLen * segCount

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // Centre origin: place centre of creature at (x,y)
            val startX = x - totalBodyLen * 0.5f + headW * 0.5f

            // ── TAIL FAN (drawn first, behind body) ───────────────────────────
            val tailX = startX + segLen * segCount + headW * 0.5f
            val tailSway = sin(t * 2.5f * PI.toFloat()) * 1.0f * s
            val fanH = headH * 0.9f
            // 5 uropod/telson fan blades
            val fanAngles = listOf(-0.7f, -0.35f, 0f, 0.35f, 0.7f)
            for (fa in fanAngles) {
                val fanLen = (5f + level * 0.4f) * s * (1f - 0.1f * kotlin.math.abs(fa))
                val fan = Path().apply {
                    moveTo(tailX, y + fa * fanH * 0.3f)
                    cubicTo(
                        tailX + fanLen * 0.5f, y + fa * fanH * 0.5f + tailSway * 0.4f,
                        tailX + fanLen * 0.8f, y + fa * fanH + tailSway,
                        tailX + fanLen, y + fa * fanH * 1.2f + tailSway
                    )
                    lineTo(tailX + fanLen * 0.9f, y + fa * fanH * 0.9f + tailSway)
                    close()
                }
                drawPath(fan, LobRed)
                drawPath(fan, LobJoint.copy(alpha = 0.40f), style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))
            }

            // ── ABDOMEN SEGMENTS ──────────────────────────────────────────────
            for (i in (0 until segCount).reversed()) {
                val sx = startX + headW * 0.3f + i * segLen
                val segPath = Path().apply {
                    moveTo(sx, y - segH * 0.5f)
                    cubicTo(sx + segLen * 0.3f, y - segH * 0.55f,
                        sx + segLen * 0.7f, y - segH * 0.55f,
                        sx + segLen, y - segH * 0.5f)
                    lineTo(sx + segLen, y + segH * 0.5f)
                    cubicTo(sx + segLen * 0.7f, y + segH * 0.55f,
                        sx + segLen * 0.3f, y + segH * 0.55f,
                        sx, y + segH * 0.5f)
                    close()
                }
                val shade = if (i % 2 == 0) LobRed else LobRed2
                drawPath(segPath, shade)
                // Segment joint line
                drawLine(LobJoint.copy(alpha = 0.5f),
                    Offset(sx, y - segH * 0.5f), Offset(sx, y + segH * 0.5f),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.3f))
            }

            // ── CEPHALOTHORAX (HEAD+THORAX) ───────────────────────────────────
            val hx = startX
            val hy = y
            val head = Path().apply {
                moveTo(hx - headW, hy)
                cubicTo(hx - headW * 0.8f, hy - headH,
                    hx, hy - headH * 0.95f,
                    hx + headW * 0.5f, hy - headH * 0.5f)
                lineTo(hx + headW * 0.5f, hy + headH * 0.5f)
                cubicTo(hx, hy + headH * 0.95f,
                    hx - headW * 0.8f, hy + headH,
                    hx - headW, hy)
                close()
            }
            drawPath(head, LobRed2)
            // Carapace groove
            drawLine(LobJoint.copy(alpha = 0.40f),
                Offset(hx - headW * 0.3f, hy - headH * 0.7f),
                Offset(hx - headW * 0.3f, hy + headH * 0.7f),
                strokeWidth = (0.7f * s).coerceAtLeast(0.3f))

            // ── WALKING LEGS (5 pairs, stylised as 3 visible) ─────────────────
            val legCount = 5
            for (i in 0 until legCount) {
                val legX = startX + headW * 0.3f + i * segLen * 0.85f
                val phase = i * 0.4f
                val legSway = sin(t * 4.0f * PI.toFloat() + phase) * 1.8f * s
                // Upper leg segment
                drawLine(LobOrange,
                    Offset(legX, hy + segH * 0.45f),
                    Offset(legX + legSway, hy + segH * 1.5f),
                    strokeWidth = (1.0f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round)
                // Lower leg segment
                drawLine(LobOrange,
                    Offset(legX + legSway, hy + segH * 1.5f),
                    Offset(legX + legSway * 0.7f, hy + segH * 2.4f),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round)
            }

            // ── LARGE CLAW (chela) — dominant, extends forward ─────────────────
            val clawBaseX = hx - headW * 0.75f
            val clawArm = Path().apply {
                moveTo(clawBaseX, hy - headH * 0.3f)
                lineTo(clawBaseX - headW * 0.9f, hy - headH * 0.2f)
            }
            drawPath(clawArm, LobRed2, style = Stroke(width = (2.5f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round))

            // Upper claw finger
            val clawTipX = clawBaseX - headW * 0.9f
            drawLine(LobRed2,
                Offset(clawTipX, hy - headH * 0.2f),
                Offset(clawTipX - headW * 0.35f, hy - headH * 0.55f),
                strokeWidth = (2.0f * s).coerceAtLeast(0.9f), cap = StrokeCap.Round)
            // Lower claw finger (slightly open — level 3+ fully open)
            val clawGape = if (level >= 3) headH * 0.32f else headH * 0.20f
            drawLine(LobRed,
                Offset(clawTipX, hy - headH * 0.2f),
                Offset(clawTipX - headW * 0.30f, hy - headH * 0.2f + clawGape),
                strokeWidth = (2.0f * s).coerceAtLeast(0.9f), cap = StrokeCap.Round)

            // ── SMALL CLAW (second pair, smaller) ─────────────────────────────
            drawLine(LobRed,
                Offset(clawBaseX + headW * 0.2f, hy + headH * 0.1f),
                Offset(clawBaseX - headW * 0.5f, hy + headH * 0.3f),
                strokeWidth = (1.6f * s).coerceAtLeast(0.7f), cap = StrokeCap.Round)
            drawLine(LobRed,
                Offset(clawBaseX - headW * 0.5f, hy + headH * 0.3f),
                Offset(clawBaseX - headW * 0.75f, hy + headH * 0.08f),
                strokeWidth = (1.4f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round)
            drawLine(LobJoint,
                Offset(clawBaseX - headW * 0.5f, hy + headH * 0.3f),
                Offset(clawBaseX - headW * 0.72f, hy + headH * 0.50f),
                strokeWidth = (1.4f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round)

            // ── ANTENNAE (long) ────────────────────────────────────────────────
            val antSway = sin(t * 1.5f * PI.toFloat()) * 2.5f * s
            val antLen  = (12f + level * 1.0f) * s
            drawLine(LobOrange.copy(alpha = 0.85f),
                Offset(hx - headW, hy - headH * 0.5f),
                Offset(hx - headW - antLen + antSway, hy - headH * 1.8f),
                strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
            drawLine(LobOrange.copy(alpha = 0.85f),
                Offset(hx - headW, hy + headH * 0.2f),
                Offset(hx - headW - antLen * 0.7f - antSway, hy - headH * 1.0f),
                strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)

            // ── EYES (on stalks) ───────────────────────────────────────────────
            val eyeStalkLen = (2.5f + level * 0.1f) * s
            val eyeR = (1.2f + level * 0.08f) * s
            for (ey in listOf(-headH * 0.45f, headH * 0.45f)) {
                drawLine(LobBlack,
                    Offset(hx - headW, hy + ey),
                    Offset(hx - headW - eyeStalkLen, hy + ey - eyeStalkLen * 0.3f),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
                drawCircle(LobWhite, eyeR, Offset(hx - headW - eyeStalkLen, hy + ey - eyeStalkLen * 0.3f))
                drawCircle(LobBlack, eyeR * 0.60f, Offset(hx - headW - eyeStalkLen + eyeR * 0.1f, hy + ey - eyeStalkLen * 0.3f))
            }
        }
    }
}
