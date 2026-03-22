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
private val BarrBody  = Color(0xFF3A5060)  // blue-gray
private val BarrBelly = Color(0xFFD8E8F0)  // white belly
private val BarrFin   = Color(0xFF2A4050)  // fins
private val BarrLine  = Color(0xFF2A3848)  // lateral line + jaw details
private val BarrTooth = Color(0xFFFFFFFF)  // teeth
private val BarrSheen = Color(0xFF5090B0)  // iridescent sheen (level 5+)

object BarracudaRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        // Barracuda is very elongated
        val bodyLen = (18f + level * 1.2f) * s
        val bodyH   = (4.5f + level * 0.35f) * s
        val tailSway = sin(t * 4.0f * PI.toFloat()) * 1.5f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ════════════════════════════════════════════════════════════════
            // TAIL FIN — forked
            // ════════════════════════════════════════════════════════════════
            val tailX = x + bodyLen * 0.45f
            val forkLen = 4.0f * s

            val tailTopFork = Path().apply {
                moveTo(tailX, y - bodyH * 0.2f)
                cubicTo(
                    tailX + forkLen * 0.3f, y - bodyH * 0.3f + tailSway * 0.3f,
                    tailX + forkLen * 0.7f, y - forkLen * 0.9f + tailSway,
                    tailX + forkLen, y - forkLen * 1.2f + tailSway
                )
                cubicTo(
                    tailX + forkLen * 0.7f, y + tailSway * 0.3f,
                    tailX + forkLen * 0.4f, y + bodyH * 0.1f + tailSway * 0.2f,
                    tailX, y
                )
                close()
            }
            drawPath(tailTopFork, BarrFin)

            val tailBotFork = Path().apply {
                moveTo(tailX, y)
                cubicTo(
                    tailX + forkLen * 0.4f, y + bodyH * 0.1f + tailSway * 0.2f,
                    tailX + forkLen * 0.7f, y + tailSway * 0.3f,
                    tailX + forkLen, y + forkLen * 1.2f + tailSway
                )
                cubicTo(
                    tailX + forkLen * 0.7f, y + forkLen * 0.9f + tailSway,
                    tailX + forkLen * 0.3f, y + bodyH * 0.3f + tailSway * 0.3f,
                    tailX, y + bodyH * 0.2f
                )
                close()
            }
            drawPath(tailBotFork, BarrFin)

            // ════════════════════════════════════════════════════════════════
            // DORSAL FINS — two separate fins
            // ════════════════════════════════════════════════════════════════
            val dorsalSway = sin(t * 3f * PI.toFloat()) * 0.4f * s
            // First dorsal (front, triangular)
            val dorsal1 = Path().apply {
                moveTo(x - bodyLen * 0.1f, y - bodyH * 0.85f)
                cubicTo(
                    x - bodyLen * 0.0f + dorsalSway, y - bodyH * 1.7f,
                    x + bodyLen * 0.08f + dorsalSway, y - bodyH * 1.65f,
                    x + bodyLen * 0.14f, y - bodyH * 0.85f
                )
                cubicTo(
                    x + bodyLen * 0.08f, y - bodyH * 0.75f,
                    x + bodyLen * 0.0f, y - bodyH * 0.78f,
                    x - bodyLen * 0.1f, y - bodyH * 0.85f
                )
                close()
            }
            drawPath(dorsal1, BarrFin)

            // Second dorsal (rear, small)
            val dorsal2 = Path().apply {
                moveTo(x + bodyLen * 0.22f, y - bodyH * 0.78f)
                cubicTo(
                    x + bodyLen * 0.27f + dorsalSway * 0.5f, y - bodyH * 1.2f,
                    x + bodyLen * 0.34f + dorsalSway * 0.5f, y - bodyH * 1.15f,
                    x + bodyLen * 0.38f, y - bodyH * 0.75f
                )
                cubicTo(
                    x + bodyLen * 0.34f, y - bodyH * 0.65f,
                    x + bodyLen * 0.27f, y - bodyH * 0.68f,
                    x + bodyLen * 0.22f, y - bodyH * 0.78f
                )
                close()
            }
            drawPath(dorsal2, BarrFin)

            // Anal fin (matching small fin below)
            val anal = Path().apply {
                moveTo(x + bodyLen * 0.22f, y + bodyH * 0.78f)
                cubicTo(
                    x + bodyLen * 0.27f, y + bodyH * 1.2f,
                    x + bodyLen * 0.34f, y + bodyH * 1.15f,
                    x + bodyLen * 0.38f, y + bodyH * 0.75f
                )
                cubicTo(
                    x + bodyLen * 0.34f, y + bodyH * 0.65f,
                    x + bodyLen * 0.27f, y + bodyH * 0.68f,
                    x + bodyLen * 0.22f, y + bodyH * 0.78f
                )
                close()
            }
            drawPath(anal, BarrFin)

            // ════════════════════════════════════════════════════════════════
            // MAIN BODY — very elongated torpedo
            // ════════════════════════════════════════════════════════════════
            val headX = x - bodyLen   // very long snout
            val body = Path().apply {
                moveTo(headX, y)      // upper jaw tip
                cubicTo(
                    headX + bodyLen * 0.05f, y - bodyH * 0.45f,
                    x - bodyLen * 0.3f, y - bodyH,
                    x + bodyLen * 0.5f, y - bodyH * 0.25f
                )
                lineTo(tailX, y)
                lineTo(x + bodyLen * 0.5f, y + bodyH * 0.25f)
                cubicTo(
                    x - bodyLen * 0.3f, y + bodyH,
                    headX + bodyLen * 0.05f, y + bodyH * 0.45f,
                    headX, y
                )
                close()
            }
            drawPath(body, BarrBody)

            // Belly counter-shading
            val belly = Path().apply {
                moveTo(headX + bodyLen * 0.05f, y + bodyH * 0.05f)
                cubicTo(
                    x - bodyLen * 0.2f, y + bodyH * 0.85f,
                    x + bodyLen * 0.3f, y + bodyH * 0.8f,
                    tailX, y + bodyH * 0.2f
                )
                cubicTo(
                    x + bodyLen * 0.3f, y + bodyH * 0.55f,
                    x - bodyLen * 0.2f, y + bodyH * 0.6f,
                    headX + bodyLen * 0.05f, y + bodyH * 0.05f
                )
                close()
            }
            drawPath(belly, BarrBelly.copy(alpha = 0.50f))

            // ════════════════════════════════════════════════════════════════
            // LOWER JAW — prominent underbite
            // ════════════════════════════════════════════════════════════════
            val lowerJaw = Path().apply {
                moveTo(headX - 1.5f * s, y + bodyH * 0.08f)
                cubicTo(
                    headX + bodyLen * 0.02f, y + bodyH * 0.35f,
                    headX + bodyLen * 0.06f, y + bodyH * 0.38f,
                    headX + bodyLen * 0.14f, y + bodyH * 0.3f
                )
                cubicTo(
                    headX + bodyLen * 0.08f, y + bodyH * 0.3f,
                    headX + bodyLen * 0.03f, y + bodyH * 0.18f,
                    headX - 1.5f * s, y + bodyH * 0.08f
                )
                close()
            }
            drawPath(lowerJaw, BarrBody.copy(alpha = 0.8f))

            // Teeth hints on lower jaw
            val toothCount = 4
            for (i in 0 until toothCount) {
                val tx = headX + bodyLen * 0.02f + i * bodyLen * 0.025f
                drawLine(
                    BarrTooth.copy(alpha = 0.7f),
                    Offset(tx, y + bodyH * 0.20f),
                    Offset(tx, y + bodyH * 0.10f),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.5f),
                    cap = StrokeCap.Round
                )
            }

            // ════════════════════════════════════════════════════════════════
            // LATERAL LINE
            // ════════════════════════════════════════════════════════════════
            drawLine(
                BarrLine.copy(alpha = 0.55f),
                Offset(headX + bodyLen * 0.10f, y - bodyH * 0.06f),
                Offset(tailX - forkLen * 0.3f, y - bodyH * 0.08f),
                strokeWidth = (0.7f * s).coerceAtLeast(0.4f),
                cap = StrokeCap.Round
            )

            // ════════════════════════════════════════════════════════════════
            // LEVEL 3+: darker flank blotches
            // ════════════════════════════════════════════════════════════════
            if (level >= 3) {
                for (b in 0 until 5) {
                    val bx = x - bodyLen * 0.3f + b * bodyLen * 0.16f
                    drawLine(
                        BarrLine.copy(alpha = 0.30f),
                        Offset(bx, y - bodyH * 0.55f),
                        Offset(bx + 0.5f * s, y + bodyH * 0.45f),
                        strokeWidth = (2.5f * s).coerceAtLeast(1.0f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // ════════════════════════════════════════════════════════════════
            // LEVEL 5+: iridescent sheen line
            // ════════════════════════════════════════════════════════════════
            if (level >= 5) {
                drawLine(
                    BarrSheen.copy(alpha = 0.45f),
                    Offset(headX + bodyLen * 0.08f, y - bodyH * 0.35f),
                    Offset(tailX, y - bodyH * 0.30f),
                    strokeWidth = (1.2f * s).coerceAtLeast(0.6f),
                    cap = StrokeCap.Round
                )
            }

            // ════════════════════════════════════════════════════════════════
            // EYE — large, forward-positioned
            // ════════════════════════════════════════════════════════════════
            val eyeR  = (1.6f + level * 0.1f) * s
            val eyeX  = headX + bodyLen * 0.10f
            val eyeY  = y - bodyH * 0.28f
            drawCircle(Color.White.copy(alpha = 0.85f), eyeR, Offset(eyeX, eyeY))
            drawCircle(Color(0xFF111122), eyeR * 0.55f, Offset(eyeX + eyeR * 0.1f, eyeY))
        }
    }
}
