package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.mnebot.riptide.presentation.aquarium.PathPool
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val TurtShell  = Color(0xFF3A4E1A)  // dark olive-green carapace
private val TurtShell2 = Color(0xFF506A28)  // lighter scute
private val TurtShell3 = Color(0xFF2A3812)  // dark scute edge
private val TurtHead   = Color(0xFF6A7A48)  // head/flipper colour
private val TurtBelly  = Color(0xFFAABB88)  // plastron (belly)
private val TurtEye    = Color(0xFF1A1A22)

object SeaTurtleRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val t = animTimeMs / 1000f

        val shellW  = (12f + level * 0.7f) * s
        val shellH  = (9f  + level * 0.5f) * s
        val flipLen = (7f  + level * 0.4f) * s

        // Flipper sweep (alternate front/back)
        val flipSweep = sin(t * 1.0f * PI.toFloat()) * 1.8f * s  // front flipper up-sweep
        val tailSway  = sin(t * 1.0f * PI.toFloat()) * 1.2f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── REAR FLIPPERS ─────────────────────────────────────────────────
            val rearFlip = paths.obtain().apply {
                moveTo(x + shellW * 0.5f, y + shellH * 0.3f)
                cubicTo(
                    x + shellW * 0.7f + flipSweep * 0.2f, y + shellH * 0.6f,
                    x + shellW * 0.8f, y + flipLen * 0.8f - flipSweep * 0.2f,
                    x + shellW * 0.6f, y + flipLen - flipSweep * 0.3f
                )
                cubicTo(
                    x + shellW * 0.5f, y + flipLen * 0.7f - flipSweep * 0.2f,
                    x + shellW * 0.4f, y + shellH * 0.6f,
                    x + shellW * 0.5f, y + shellH * 0.3f
                )
                close()
            }
            drawPath(rearFlip, TurtHead)

            // ── FRONT FLIPPERS (large, powerful) ─────────────────────────────
            // Front flipper extends forward and upward during power stroke
            val frontFlip = paths.obtain().apply {
                moveTo(x - shellW * 0.3f, y - shellH * 0.25f)
                cubicTo(
                    x - shellW * 0.6f, y - shellH * 0.5f - flipSweep * 0.5f,
                    x - shellW * 0.85f - flipSweep * 0.2f, y - flipLen * 0.3f - flipSweep * 0.7f,
                    x - shellW - flipSweep * 0.3f, y - flipLen * 0.5f - flipSweep
                )
                cubicTo(
                    x - shellW * 0.8f - flipSweep * 0.2f, y - shellH * 0.1f - flipSweep * 0.5f,
                    x - shellW * 0.5f, y + shellH * 0.1f,
                    x - shellW * 0.3f, y + shellH * 0.1f
                )
                close()
            }
            drawPath(frontFlip, TurtHead)
            // Flipper tip detail
            drawLine(
                TurtShell3.copy(alpha = 0.35f),
                Offset(x - shellW * 0.7f - flipSweep * 0.25f, y - shellH * 0.4f - flipSweep * 0.8f),
                Offset(x - shellW - flipSweep * 0.3f, y - flipLen * 0.5f - flipSweep),
                strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round
            )

            // ── CARAPACE (shell) ──────────────────────────────────────────────
            val shell = paths.obtain().apply {
                moveTo(x - shellW * 0.5f, y)  // head end (narrower)
                cubicTo(x - shellW * 0.5f, y - shellH,
                    x + shellW * 0.5f, y - shellH,
                    x + shellW * 0.55f, y)
                cubicTo(x + shellW * 0.55f, y + shellH,
                    x - shellW * 0.4f, y + shellH,
                    x - shellW * 0.5f, y)
                close()
            }
            drawPath(shell, TurtShell)

            // ── SCUTE PATTERN (hexagonal-ish plates) ─────────────────────────
            // Central scutes (vertebral)
            val vertebralCount = 4
            for (v in 0 until vertebralCount) {
                val vf = (v.toFloat() + 0.5f) / vertebralCount.toFloat()
                val vx = x - shellW * 0.35f + shellW * 0.8f * vf
                val vy = y - shellH * 0.05f
                val vw = shellW * 0.14f
                val vh = shellH * 0.65f
                val scute = paths.obtain().apply {
                    moveTo(vx, vy - vh)
                    lineTo(vx + vw, vy - vh * 0.6f)
                    lineTo(vx + vw, vy + vh * 0.6f)
                    lineTo(vx, vy + vh)
                    lineTo(vx - vw, vy + vh * 0.6f)
                    lineTo(vx - vw, vy - vh * 0.6f)
                    close()
                }
                drawPath(scute, TurtShell2)
                drawPath(scute, TurtShell3.copy(alpha = 0.40f),
                    style = Stroke(width = (0.7f * s).coerceAtLeast(0.3f)))
            }

            // Costal scutes (flanking)
            for (side in listOf(-1f, 1f)) {
                val costalCount = 3
                for (c in 0 until costalCount) {
                    val cf = (c.toFloat() + 0.5f) / costalCount.toFloat()
                    val cx2 = x - shellW * 0.2f + shellW * 0.6f * cf
                    val cy2 = y + side * shellH * 0.42f
                    val cw  = shellW * 0.14f
                    val ch  = shellH * 0.35f
                    val costal = paths.obtain().apply {
                        moveTo(cx2 - cw, cy2 - ch * 0.4f)
                        cubicTo(cx2 - cw * 0.5f, cy2 - ch,
                            cx2 + cw * 0.5f, cy2 - ch,
                            cx2 + cw, cy2 - ch * 0.4f)
                        lineTo(cx2 + cw, cy2 + ch * 0.4f)
                        cubicTo(cx2 + cw * 0.5f, cy2 + ch,
                            cx2 - cw * 0.5f, cy2 + ch,
                            cx2 - cw, cy2 + ch * 0.4f)
                        close()
                    }
                    drawPath(costal, TurtShell2)
                    drawPath(costal, TurtShell3.copy(alpha = 0.35f),
                        style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))
                }
            }

            // Shell outline
            drawPath(shell, TurtShell3.copy(alpha = 0.40f),
                style = Stroke(width = (1.0f * s).coerceAtLeast(0.5f)))

            // ── LEVEL 3+: Algae patches on carapace ──────────────────────────────
            if (level >= 3) {
                val AlgGreen = Color(0xFF2A5A18)
                val algaePositions = listOf(
                    Triple(-0.28f, -0.58f, 1.2f), Triple(0.12f, -0.65f, 0.9f),
                    Triple(0.36f, -0.42f, 1.1f),  Triple(-0.18f, 0.52f, 1.0f),
                    Triple(0.22f, 0.58f, 0.85f)
                )
                for ((ax, ay, ar) in algaePositions) {
                    drawCircle(AlgGreen.copy(alpha = 0.42f), (ar * s).coerceAtLeast(0.5f),
                        Offset(x + ax * shellW, y + ay * shellH))
                }
            }

            // ── LEVEL 5+: Barnacle clusters on leading shell edge ─────────────────
            if (level >= 5) {
                val BarnGrey = Color(0xFF8A8A7A)
                for (b in 0 until 5) {
                    val bx = x - shellW * 0.38f + b * shellW * 0.20f
                    drawCircle(BarnGrey.copy(alpha = 0.58f), (0.9f * s).coerceAtLeast(0.4f),
                        Offset(bx, y - shellH * 0.88f))
                    drawCircle(TurtShell3.copy(alpha = 0.35f), (0.9f * s).coerceAtLeast(0.4f),
                        Offset(bx, y - shellH * 0.88f),
                        style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))
                }
            }

            // ── HEAD ──────────────────────────────────────────────────────────
            val headR = (3.5f + level * 0.2f) * s
            val headX = x - shellW * 0.55f - headR * 0.7f
            val head  = paths.obtain().apply {
                moveTo(headX - headR * 0.8f, y)
                cubicTo(headX - headR * 0.7f, y - headR * 0.9f,
                    headX + headR * 0.5f, y - headR * 0.85f,
                    headX + headR * 0.9f, y - headR * 0.2f)
                lineTo(headX + headR * 0.9f, y + headR * 0.2f)
                cubicTo(headX + headR * 0.5f, y + headR * 0.85f,
                    headX - headR * 0.7f, y + headR * 0.9f,
                    headX - headR * 0.8f, y)
                close()
            }
            drawPath(head, TurtHead)

            // ── TAIL (short) ───────────────────────────────────────────────────
            drawLine(TurtHead,
                Offset(x + shellW * 0.52f, y),
                Offset(x + shellW * 0.65f, y + tailSway),
                strokeWidth = (2.0f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round)

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.1f + level * 0.07f) * s
            val eyeX = headX - headR * 0.35f
            val eyeY = y - headR * 0.32f
            drawCircle(TurtBelly, eyeR * 1.1f, Offset(eyeX, eyeY))
            drawCircle(TurtEye, eyeR * 0.62f, Offset(eyeX, eyeY))
            drawCircle(Color.White, eyeR * 0.20f, Offset(eyeX - eyeR * 0.2f, eyeY - eyeR * 0.2f))
        }
    }
}
