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
private val ShrBody  = Color(0xFFFFB8A0)  // translucent pinkish-orange
private val ShrShell = Color(0xFFCC8866)  // darker shell segments
private val ShrAnt   = Color(0xFFBB9980)  // antennae
private val ShrBlack = Color(0xFF2A1A1A)  // eye & outline
private val ShrWhite = Color(0xFFF8F0E8)  // eye white, belly

object ShrimpRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen  = (11f + level * 0.6f) * s
        val bodyH    = (3.5f + level * 0.2f) * s
        val segCount = 6

        // Body curve: shrimp arches its abdomen
        val archAnim = sin(t * 2.0f * PI.toFloat()) * 0.8f * s  // slow arch pulse
        val tailSway = sin(t * 5.0f * PI.toFloat()) * 2.0f * s  // fast tail flick

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FAN ───────────────────────────────────────────────────────
            val tailX = x + bodyLen * 0.5f
            val tailY = y + archAnim
            val fanW = bodyH * 1.4f
            val fanLen = (4f + level * 0.35f) * s
            val fanBlades = listOf(-0.8f, -0.4f, 0f, 0.4f, 0.8f)
            for (fb in fanBlades) {
                val fan = Path().apply {
                    moveTo(tailX, tailY + fb * fanW * 0.3f)
                    cubicTo(
                        tailX + fanLen * 0.5f, tailY + fb * fanW * 0.7f + tailSway * 0.5f,
                        tailX + fanLen * 0.8f, tailY + fb * fanW * 1.1f + tailSway,
                        tailX + fanLen, tailY + fb * fanW * 1.3f + tailSway
                    )
                    lineTo(tailX + fanLen * 0.9f, tailY + fb * fanW * 1.0f + tailSway)
                    close()
                }
                drawPath(fan, ShrBody.copy(alpha = 0.75f))
                drawPath(fan, ShrShell.copy(alpha = 0.35f), style = Stroke(width = (0.5f * s).coerceAtLeast(0.3f)))
            }

            // ── ABDOMEN SEGMENTS (arched) ──────────────────────────────────────
            val segLen = bodyLen / segCount
            for (i in 0 until segCount) {
                val progress = i.toFloat() / segCount.toFloat()
                val sx = x - bodyLen * 0.5f + i * segLen
                // Arch: segments curve upward in the middle
                val archY = y - archAnim * sin(progress * PI.toFloat()) * 2.0f
                val segH = bodyH * (1.0f - progress * 0.3f)  // taper toward tail

                val seg = Path().apply {
                    moveTo(sx, archY - segH)
                    cubicTo(sx + segLen * 0.3f, archY - segH * 1.05f,
                        sx + segLen * 0.7f, archY - segH * 1.05f,
                        sx + segLen, archY + archAnim * sin((progress + 1f / segCount) * PI.toFloat()) * 0.15f - segH)
                    lineTo(sx + segLen, archY + archAnim * sin((progress + 1f / segCount) * PI.toFloat()) * 0.15f + segH)
                    cubicTo(sx + segLen * 0.7f, archY + segH * 1.05f,
                        sx + segLen * 0.3f, archY + segH * 1.05f,
                        sx, archY + segH)
                    close()
                }
                val alpha = 0.55f + i * 0.04f
                drawPath(seg, ShrBody.copy(alpha = alpha))
                // Segment joint
                drawLine(ShrShell.copy(alpha = 0.50f),
                    Offset(sx, archY - segH),
                    Offset(sx, archY + segH),
                    strokeWidth = (0.6f * s).coerceAtLeast(0.3f))
            }

            // Swimmerets (pleopods) — small legs beneath abdomen
            val pleoCount = 5
            for (i in 0 until pleoCount) {
                val px = x - bodyLen * 0.3f + i * segLen
                val progress = i.toFloat() / pleoCount.toFloat()
                val archy = y - archAnim * sin(progress * PI.toFloat()) * 2.0f
                val pleoAnim = sin(t * 6f * PI.toFloat() + i * 0.6f) * 1.2f * s
                drawLine(ShrShell.copy(alpha = 0.60f),
                    Offset(px, archy + bodyH),
                    Offset(px + pleoAnim, archy + bodyH + 2.0f * s),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
            }

            // ── HEAD / CARAPACE ────────────────────────────────────────────────
            val headX = x - bodyLen * 0.5f
            val headH2 = bodyH * 1.3f
            val head = Path().apply {
                moveTo(headX - headH2 * 0.8f, y)
                cubicTo(headX - headH2 * 0.6f, y - headH2,
                    headX + headH2 * 0.4f, y - headH2 * 0.9f,
                    headX + headH2 * 0.8f, y - headH2 * 0.4f)
                lineTo(headX + headH2 * 0.8f, y + headH2 * 0.4f)
                cubicTo(headX + headH2 * 0.4f, y + headH2 * 0.9f,
                    headX - headH2 * 0.6f, y + headH2,
                    headX - headH2 * 0.8f, y)
                close()
            }
            drawPath(head, ShrBody.copy(alpha = 0.70f))
            drawPath(head, ShrShell.copy(alpha = 0.25f),
                style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))

            // ── ROSTRUM (spike on head) ────────────────────────────────────────
            val rostrumLen = (3.5f + level * 0.3f) * s
            drawLine(ShrShell,
                Offset(headX - headH2 * 0.6f, y - headH2 * 0.35f),
                Offset(headX - headH2 * 0.6f - rostrumLen, y - headH2 * 0.35f - rostrumLen * 0.2f),
                strokeWidth = (0.8f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round)

            // ── ANTENNAE (long, 2 pairs) ───────────────────────────────────────
            val antSway = sin(t * 2.0f * PI.toFloat()) * 2.5f * s
            val antLen  = (10f + level * 0.7f) * s
            val antBase = headX - headH2 * 0.7f
            drawLine(ShrAnt.copy(alpha = 0.85f),
                Offset(antBase, y - headH2 * 0.5f),
                Offset(antBase - antLen + antSway, y - headH2 * 2.2f),
                strokeWidth = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round)
            drawLine(ShrAnt.copy(alpha = 0.85f),
                Offset(antBase + headH2 * 0.1f, y - headH2 * 0.3f),
                Offset(antBase + headH2 * 0.1f - antLen * 0.7f - antSway, y - headH2 * 1.5f),
                strokeWidth = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round)
            // Antennules (short second pair)
            drawLine(ShrAnt.copy(alpha = 0.60f),
                Offset(antBase, y + headH2 * 0.1f),
                Offset(antBase - antLen * 0.35f + antSway * 0.5f, y + headH2 * 0.8f),
                strokeWidth = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round)

            // ── LEVEL 3+: White cleaner stripe along dorsal ──────────────────────
            if (level >= 3) {
                val stripeY = y - archAnim * 0.5f - bodyH * 0.85f
                drawLine(ShrWhite.copy(alpha = 0.55f),
                    Offset(x - bodyLen * 0.45f, stripeY),
                    Offset(x + bodyLen * 0.48f, stripeY - archAnim * 0.3f),
                    strokeWidth = (1.1f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round)
            }

            // ── LEVEL 5+: Iridescent highlight on carapace ───────────────────────
            if (level >= 5) {
                val shimmer = (sin(t * 2.0f * PI.toFloat()) * 0.5f + 0.5f) * 0.20f + 0.10f
                drawCircle(Color(0xFFAAFFDD).copy(alpha = shimmer),
                    bodyH * 1.0f, Offset(x - bodyLen * 0.45f + bodyH * 0.6f, y - bodyH * 0.5f))
            }

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.2f + level * 0.07f) * s
            val eyeX = headX - headH2 * 0.55f
            val eyeY = y - headH2 * 0.25f
            drawCircle(ShrWhite, eyeR, Offset(eyeX, eyeY))
            drawCircle(ShrBlack, eyeR * 0.62f, Offset(eyeX + eyeR * 0.08f, eyeY))
            drawCircle(ShrWhite, eyeR * 0.20f, Offset(eyeX - eyeR * 0.2f, eyeY - eyeR * 0.2f))
        }
    }
}
