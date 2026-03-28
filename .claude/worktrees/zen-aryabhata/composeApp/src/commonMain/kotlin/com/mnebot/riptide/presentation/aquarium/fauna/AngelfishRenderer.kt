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
// Marine angelfish (Pomacanthidae) — electric blue with yellow accents
private val AngelBlue   = Color(0xFF1A55A8)  // deep royal blue body
private val AngelBlue2  = Color(0xFF2E7DD4)  // lighter blue highlight
private val AngelYellow = Color(0xFFFFCA28)  // golden yellow trim & tail
private val AngelBlack  = Color(0xFF111122)  // black stripes
private val AngelWhite  = Color(0xFFDDEEFF)  // white belly tinge
private val AngelFin    = Color(0xFF0A3A80)  // darker fin blue

object AngelfishRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        // Angelfish: tall body, wide fins
        val bodyLen = (8f + level * 0.5f) * s
        val bodyH   = (9f + level * 0.6f) * s   // taller than wide
        val tailExt = (4f + level * 0.4f) * s

        val tailSway = sin(t * 3.2f * PI.toFloat()) * 1.6f * s
        val finSway  = sin(t * 2.5f * PI.toFloat()) * 0.7f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FIN — yellow forked ──────────────────────────────────────
            val tailX = x + bodyLen * 0.5f
            val tail = Path().apply {
                moveTo(tailX, y - bodyH * 0.15f)
                cubicTo(
                    tailX + tailExt * 0.5f, y - bodyH * 0.6f + tailSway,
                    tailX + tailExt * 0.9f, y - bodyH * 1.0f + tailSway,
                    tailX + tailExt, y - bodyH * 1.1f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.7f, y + tailSway * 0.3f,
                    tailX + tailExt * 0.4f, y + bodyH * 0.3f + tailSway * 0.3f,
                    tailX + tailExt, y + bodyH * 1.1f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.9f, y + bodyH * 1.0f + tailSway,
                    tailX + tailExt * 0.5f, y + bodyH * 0.6f + tailSway,
                    tailX, y + bodyH * 0.15f
                )
                close()
            }
            drawPath(tail, AngelYellow)

            // ── DORSAL FIN — tall, trailing ───────────────────────────────────
            val dorsal = Path().apply {
                moveTo(x - bodyLen * 0.6f, y - bodyH * 0.92f)
                cubicTo(
                    x - bodyLen * 0.2f + finSway, y - bodyH * 1.85f,
                    x + bodyLen * 0.15f + finSway, y - bodyH * 1.80f,
                    x + bodyLen * 0.48f, y - bodyH * 0.92f
                )
                cubicTo(
                    x + bodyLen * 0.2f, y - bodyH * 0.86f,
                    x - bodyLen * 0.1f, y - bodyH * 0.88f,
                    x - bodyLen * 0.6f, y - bodyH * 0.92f
                )
                close()
            }
            drawPath(dorsal, AngelFin)
            // Yellow trim on dorsal leading edge
            drawLine(
                AngelYellow.copy(alpha = 0.75f),
                Offset(x - bodyLen * 0.6f, y - bodyH * 0.92f),
                Offset(x - bodyLen * 0.1f + finSway, y - bodyH * 1.82f),
                strokeWidth = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round
            )

            // ── ANAL FIN — tall, trailing ──────────────────────────────────────
            val anal = Path().apply {
                moveTo(x - bodyLen * 0.4f, y + bodyH * 0.92f)
                cubicTo(
                    x - bodyLen * 0.1f - finSway, y + bodyH * 1.75f,
                    x + bodyLen * 0.2f - finSway, y + bodyH * 1.70f,
                    x + bodyLen * 0.5f, y + bodyH * 0.92f
                )
                cubicTo(
                    x + bodyLen * 0.2f, y + bodyH * 0.86f,
                    x, y + bodyH * 0.88f,
                    x - bodyLen * 0.4f, y + bodyH * 0.92f
                )
                close()
            }
            drawPath(anal, AngelFin)
            drawLine(
                AngelYellow.copy(alpha = 0.75f),
                Offset(x - bodyLen * 0.4f, y + bodyH * 0.92f),
                Offset(x + bodyLen * 0.1f - finSway, y + bodyH * 1.72f),
                strokeWidth = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round
            )

            // ── PECTORAL FIN ──────────────────────────────────────────────────
            val pectSway = sin(t * 4.0f * PI.toFloat()) * 1.5f * s
            val pect = Path().apply {
                moveTo(x - bodyLen * 0.4f, y - bodyH * 0.05f)
                cubicTo(
                    x - bodyLen * 0.15f + pectSway, y + bodyH * 0.6f,
                    x + bodyLen * 0.05f + pectSway, y + bodyH * 0.7f,
                    x + bodyLen * 0.2f, y + bodyH * 0.3f
                )
                cubicTo(x, y + bodyH * 0.15f, x - bodyLen * 0.2f, y, x - bodyLen * 0.4f, y - bodyH * 0.05f)
                close()
            }
            drawPath(pect, AngelBlue2.copy(alpha = 0.7f))

            // ── MAIN BODY — tall, rounded rhombus ─────────────────────────────
            val body = Path().apply {
                moveTo(x - bodyLen, y)
                cubicTo(
                    x - bodyLen * 0.7f, y - bodyH,
                    x + bodyLen * 0.05f, y - bodyH * 0.98f,
                    x + bodyLen * 0.5f, y - bodyH * 0.25f
                )
                lineTo(x + bodyLen * 0.5f, y + bodyH * 0.25f)
                cubicTo(
                    x + bodyLen * 0.05f, y + bodyH * 0.98f,
                    x - bodyLen * 0.7f, y + bodyH,
                    x - bodyLen, y
                )
                close()
            }
            drawPath(body, AngelBlue)

            // White belly zone
            val belly = Path().apply {
                moveTo(x - bodyLen * 0.55f, y - bodyH * 0.35f)
                cubicTo(x - bodyLen * 0.2f, y - bodyH * 0.75f,
                    x + bodyLen * 0.2f, y - bodyH * 0.70f,
                    x + bodyLen * 0.48f, y - bodyH * 0.22f)
                cubicTo(x + bodyLen * 0.2f, y - bodyH * 0.40f,
                    x - bodyLen * 0.1f, y - bodyH * 0.45f,
                    x - bodyLen * 0.55f, y - bodyH * 0.35f)
                close()
            }
            drawPath(belly, AngelWhite.copy(alpha = 0.30f))

            // Body sheen
            drawPath(body, AngelBlue2.copy(alpha = 0.25f),
                style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f)))

            // ── BLACK VERTICAL STRIPES (2) ────────────────────────────────────
            val stripes = listOf(
                Pair(x - bodyLen * 0.52f, bodyH * 0.85f),
                Pair(x - bodyLen * 0.02f, bodyH * 0.97f),
            )
            for ((sx, sh) in stripes) {
                drawLine(
                    AngelBlack.copy(alpha = 0.70f),
                    Offset(sx, y - sh),
                    Offset(sx, y + sh),
                    strokeWidth = (2.0f * s).coerceAtLeast(0.8f),
                    cap = StrokeCap.Round
                )
            }

            // Yellow outline around body
            drawPath(body, AngelYellow.copy(alpha = 0.45f),
                style = Stroke(width = (0.9f * s).coerceAtLeast(0.4f)))

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.5f + level * 0.08f) * s
            val eyeX = x - bodyLen * 0.70f
            val eyeY = y - bodyH * 0.28f
            drawCircle(AngelYellow, eyeR * 1.2f, Offset(eyeX, eyeY))
            drawCircle(AngelBlack, eyeR * 0.65f, Offset(eyeX + eyeR * 0.1f, eyeY))
            drawCircle(Color.White, eyeR * 0.22f, Offset(eyeX - eyeR * 0.2f, eyeY - eyeR * 0.2f))

            // ── LEVEL 4+: Trailing thread fins ────────────────────────────────
            if (level >= 4) {
                val threadLen = (6f + level * 0.5f) * s
                drawLine(
                    AngelYellow.copy(alpha = 0.60f),
                    Offset(x + bodyLen * 0.1f + finSway, y - bodyH * 1.78f),
                    Offset(x + bodyLen * 0.3f + finSway + threadLen, y - bodyH * 1.78f - threadLen * 0.5f),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round
                )
                drawLine(
                    AngelYellow.copy(alpha = 0.60f),
                    Offset(x + bodyLen * 0.1f - finSway, y + bodyH * 1.68f),
                    Offset(x + bodyLen * 0.3f - finSway + threadLen, y + bodyH * 1.68f + threadLen * 0.5f),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round
                )
            }
        }
    }
}
