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
private val SealGrey   = Color(0xFF4A5A60)  // dark grey body
private val SealGrey2  = Color(0xFF607080)  // lighter back highlight
private val SealBelly  = Color(0xFFAABBC0)  // pale belly
private val SealEye    = Color(0xFF111122)
private val SealWhisker= Color(0xFFCCCCBB)  // whisker lines

object SealRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen = (13f + level * 0.7f) * s
        val bodyH   = (5f  + level * 0.3f) * s
        val flipLen = (5f  + level * 0.3f) * s

        // Undulating body
        val bodyWave = sin(t * 1.4f * PI.toFloat()) * bodyH * 0.20f
        val tailSway = sin(t * 1.4f * PI.toFloat() + 0.8f) * 2.5f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── HIND FLIPPERS (merged, spread like fish tail) ─────────────────
            val hfX = x + bodyLen * 0.45f
            val leftFlip = Path().apply {
                moveTo(hfX, y + bodyH * 0.2f + bodyWave)
                cubicTo(hfX + flipLen * 0.4f, y + bodyH * 0.1f + tailSway,
                    hfX + flipLen * 0.8f, y - bodyH * 0.3f + tailSway,
                    hfX + flipLen, y - bodyH * 0.6f + tailSway)
                cubicTo(hfX + flipLen * 0.8f, y - bodyH * 0.1f + tailSway,
                    hfX + flipLen * 0.4f, y + bodyH * 0.2f + tailSway * 0.5f,
                    hfX, y + bodyH * 0.3f + bodyWave)
                close()
            }
            drawPath(leftFlip, SealGrey)

            val rightFlip = Path().apply {
                moveTo(hfX, y - bodyH * 0.2f + bodyWave)
                cubicTo(hfX + flipLen * 0.4f, y - bodyH * 0.1f + tailSway,
                    hfX + flipLen * 0.8f, y + bodyH * 0.4f + tailSway,
                    hfX + flipLen, y + bodyH * 0.7f + tailSway)
                cubicTo(hfX + flipLen * 0.8f, y + bodyH * 0.2f + tailSway,
                    hfX + flipLen * 0.4f, y - bodyH * 0.1f + tailSway * 0.5f,
                    hfX, y - bodyH * 0.3f + bodyWave)
                close()
            }
            drawPath(rightFlip, SealGrey)

            // ── FRONT FLIPPERS (paddle-shaped, flipper stroke) ────────────────
            val flipSweep = sin(t * 2.8f * PI.toFloat()) * flipLen * 0.5f
            val frontFlip = Path().apply {
                moveTo(x - bodyLen * 0.10f, y + bodyH * 0.65f + bodyWave * 0.4f)
                cubicTo(x + flipLen * 0.2f + flipSweep * 0.3f, y + bodyH * 1.30f + bodyWave * 0.3f,
                    x + flipLen * 0.5f + flipSweep * 0.6f, y + bodyH * 1.40f + bodyWave * 0.2f,
                    x + flipLen * 0.7f, y + bodyH * 0.90f + bodyWave * 0.3f)
                cubicTo(x + flipLen * 0.4f + flipSweep * 0.3f, y + bodyH * 0.80f + bodyWave * 0.35f,
                    x + flipLen * 0.1f, y + bodyH * 0.74f + bodyWave * 0.38f,
                    x - bodyLen * 0.10f, y + bodyH * 0.65f + bodyWave * 0.4f)
                close()
            }
            drawPath(frontFlip, SealGrey2)

            // ── MAIN BODY (torpedo, thicker at shoulders) ─────────────────────
            val body = Path().apply {
                moveTo(x - bodyLen * 0.5f, y + bodyWave)  // nose
                cubicTo(x - bodyLen * 0.35f, y - bodyH + bodyWave * 0.8f,
                    x + bodyLen * 0.1f, y - bodyH * 1.02f + bodyWave * 0.4f,
                    x + bodyLen * 0.45f, y - bodyH * 0.42f + bodyWave * 0.1f)
                lineTo(x + bodyLen * 0.45f, y + bodyH * 0.42f + bodyWave * 0.1f)
                cubicTo(x + bodyLen * 0.1f, y + bodyH * 1.02f + bodyWave * 0.4f,
                    x - bodyLen * 0.35f, y + bodyH + bodyWave * 0.8f,
                    x - bodyLen * 0.5f, y + bodyWave)
                close()
            }
            drawPath(body, SealGrey2)

            // Belly
            val belly = Path().apply {
                moveTo(x - bodyLen * 0.45f, y + bodyWave + bodyH * 0.08f)
                cubicTo(x - bodyLen * 0.25f, y + bodyH * 0.88f + bodyWave * 0.55f,
                    x + bodyLen * 0.1f, y + bodyH * 0.95f + bodyWave * 0.35f,
                    x + bodyLen * 0.42f, y + bodyH * 0.38f + bodyWave * 0.12f)
                cubicTo(x + bodyLen * 0.1f, y + bodyH * 0.62f + bodyWave * 0.35f,
                    x - bodyLen * 0.1f, y + bodyH * 0.68f + bodyWave * 0.45f,
                    x - bodyLen * 0.45f, y + bodyWave + bodyH * 0.08f)
                close()
            }
            drawPath(belly, SealBelly.copy(alpha = 0.50f))

            // ── HEAD (rounded, short snout) ────────────────────────────────────
            val headR = (4.0f + level * 0.22f) * s
            val headX = x - bodyLen * 0.5f
            val head  = Path().apply {
                moveTo(headX - headR * 1.0f, y + bodyWave)
                cubicTo(headX - headR * 0.85f, y - headR * 0.9f + bodyWave * 0.9f,
                    headX + headR * 0.4f, y - headR * 0.92f + bodyWave * 0.9f,
                    headX + headR * 0.7f, y - headR * 0.30f + bodyWave * 0.9f)
                lineTo(headX + headR * 0.7f, y + headR * 0.30f + bodyWave * 0.9f)
                cubicTo(headX + headR * 0.4f, y + headR * 0.92f + bodyWave * 0.9f,
                    headX - headR * 0.85f, y + headR * 0.9f + bodyWave * 0.9f,
                    headX - headR * 1.0f, y + bodyWave)
                close()
            }
            drawPath(head, SealGrey2)
            drawCircle(SealBelly.copy(alpha = 0.35f), headR * 0.65f,
                Offset(headX - headR * 0.5f, y + bodyWave * 0.9f + headR * 0.2f))

            // Nose (dark round tip)
            drawCircle(SealGrey, headR * 0.18f, Offset(headX - headR * 0.92f, y + bodyWave))

            // ── WHISKERS ──────────────────────────────────────────────────────
            val whiskLen = (3f + level * 0.15f) * s
            val whiskY   = y + bodyWave * 0.9f - headR * 0.05f
            val whiskX   = headX - headR * 0.70f
            for (w in -2..2) {
                val wf = w.toFloat() / 2f
                drawLine(SealWhisker.copy(alpha = 0.70f),
                    Offset(whiskX, whiskY + wf * headR * 0.25f),
                    Offset(whiskX - whiskLen, whiskY + wf * headR * 0.25f + wf * whiskLen * 0.2f),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round)
            }

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.4f + level * 0.08f) * s
            val eyeX = headX - headR * 0.40f
            val eyeY = y - headR * 0.42f + bodyWave * 0.9f
            drawCircle(SealBelly, eyeR * 1.15f, Offset(eyeX, eyeY))
            drawCircle(SealEye, eyeR * 0.70f, Offset(eyeX, eyeY))
            drawCircle(Color.White, eyeR * 0.25f, Offset(eyeX - eyeR * 0.22f, eyeY - eyeR * 0.22f))
        }
    }
}
