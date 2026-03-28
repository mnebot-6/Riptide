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
private val ManBody  = Color(0xFF7A8A8A)  // gray
private val ManBelly = Color(0xFFA5B5B5)  // lighter belly
private val ManFlip  = Color(0xFF6A7A7A)  // flippers
private val ManWrink = Color(0xFF5A6A6A)  // wrinkle lines

object ManateeRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen  = (14f + level * 0.8f) * s
        val bodyH    = (9f  + level * 0.6f) * s
        // Slow body undulation
        val bodyWave = sin(t * 1.2f * PI.toFloat()) * 1.5f * s
        val tailSway = sin(t * 1.2f * PI.toFloat()) * 3.0f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ════════════════════════════════════════════════════════════════
            // PADDLE TAIL — wide, rounded, horizontal spatula
            // ════════════════════════════════════════════════════════════════
            val tailCX = x + bodyLen * 0.48f
            val tailCY = y + tailSway
            val tailW  = (7f + level * 0.4f) * s
            val tailH  = (4f + level * 0.3f) * s
            val tail = Path().apply {
                moveTo(tailCX - tailW * 0.3f, tailCY - tailH * 0.2f)
                cubicTo(
                    tailCX, tailCY - tailH,
                    tailCX + tailW * 0.5f, tailCY - tailH * 0.95f,
                    tailCX + tailW, tailCY - tailH * 0.3f
                )
                cubicTo(
                    tailCX + tailW * 1.05f, tailCY,
                    tailCX + tailW * 0.9f, tailCY + tailH * 0.45f,
                    tailCX + tailW * 0.3f, tailCY + tailH * 0.55f
                )
                cubicTo(
                    tailCX, tailCY + tailH * 0.85f,
                    tailCX - tailW * 0.2f, tailCY + tailH * 0.6f,
                    tailCX - tailW * 0.3f, tailCY + tailH * 0.15f
                )
                close()
            }
            drawPath(tail, ManFlip)

            // ════════════════════════════════════════════════════════════════
            // MAIN BODY — big rounded potato
            // ════════════════════════════════════════════════════════════════
            val body = Path().apply {
                moveTo(x - bodyLen, y + bodyWave * 0.3f)  // blunt front snout
                cubicTo(
                    x - bodyLen * 0.85f, y - bodyH + bodyWave,
                    x - bodyLen * 0.1f,  y - bodyH * 1.05f + bodyWave * 0.5f,
                    x + bodyLen * 0.45f, y - bodyH * 0.55f + bodyWave * 0.2f
                )
                // Taper toward tail
                cubicTo(
                    x + bodyLen * 0.50f, y - bodyH * 0.20f,
                    x + bodyLen * 0.52f, y,
                    x + bodyLen * 0.50f, y + bodyH * 0.15f
                )
                cubicTo(
                    x + bodyLen * 0.50f, y + bodyH * 0.55f + bodyWave * 0.2f,
                    x - bodyLen * 0.1f,  y + bodyH * 1.05f + bodyWave * 0.5f,
                    x - bodyLen * 0.85f, y + bodyH - bodyWave
                )
                cubicTo(
                    x - bodyLen * 0.9f, y + bodyH * 0.6f,
                    x - bodyLen, y + bodyH * 0.2f + bodyWave * 0.3f,
                    x - bodyLen, y + bodyWave * 0.3f
                )
                close()
            }
            drawPath(body, ManBody)

            // Belly highlight
            val belly = Path().apply {
                moveTo(x - bodyLen * 0.7f, y + bodyH * 0.35f + bodyWave * 0.3f)
                cubicTo(
                    x - bodyLen * 0.2f, y + bodyH * 0.85f + bodyWave * 0.4f,
                    x + bodyLen * 0.2f, y + bodyH * 0.82f + bodyWave * 0.3f,
                    x + bodyLen * 0.44f, y + bodyH * 0.45f + bodyWave * 0.2f
                )
                cubicTo(
                    x + bodyLen * 0.25f, y + bodyH * 0.60f,
                    x, y + bodyH * 0.65f,
                    x - bodyLen * 0.7f, y + bodyH * 0.35f + bodyWave * 0.3f
                )
                close()
            }
            drawPath(belly, ManBelly.copy(alpha = 0.45f))

            // ════════════════════════════════════════════════════════════════
            // FRONT FLIPPERS — rounded paddle shapes
            // ════════════════════════════════════════════════════════════════
            val flipSway = sin(t * 1.5f * PI.toFloat()) * 2f * s
            val flipL = (5f + level * 0.3f) * s
            val flipH = (3.5f + level * 0.2f) * s

            val flipper = Path().apply {
                moveTo(x - bodyLen * 0.4f, y + bodyH * 0.3f)
                cubicTo(
                    x - bodyLen * 0.25f + flipSway, y + bodyH * 0.6f + flipH,
                    x - bodyLen * 0.0f + flipSway,  y + bodyH * 0.55f + flipH,
                    x - bodyLen * 0.0f + flipL, y + bodyH * 0.25f
                )
                cubicTo(
                    x - bodyLen * 0.0f, y + bodyH * 0.15f,
                    x - bodyLen * 0.2f, y + bodyH * 0.18f,
                    x - bodyLen * 0.4f, y + bodyH * 0.3f
                )
                close()
            }
            drawPath(flipper, ManFlip)

            // ════════════════════════════════════════════════════════════════
            // SKIN WRINKLES — subtle horizontal lines
            // ════════════════════════════════════════════════════════════════
            val wrinkleCount = 3
            for (w in 1..wrinkleCount) {
                val frac = w.toFloat() / (wrinkleCount + 1)
                val wy   = y + bodyWave * frac - bodyH * (0.3f - frac * 0.15f)
                val wx0  = x - bodyLen * (0.65f - frac * 0.15f)
                val wx1  = x + bodyLen * (0.35f - frac * 0.1f)
                val wrink = Path().apply {
                    moveTo(wx0, wy)
                    cubicTo(
                        wx0 + (wx1 - wx0) * 0.3f, wy - 0.8f * s,
                        wx0 + (wx1 - wx0) * 0.7f, wy + 0.5f * s,
                        wx1, wy - 0.3f * s
                    )
                }
                drawPath(wrink, ManWrink.copy(alpha = 0.28f),
                    style = Stroke(width = (0.7f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round))
            }

            // ════════════════════════════════════════════════════════════════
            // LEVEL 3+: barnacle-like spots
            // ════════════════════════════════════════════════════════════════
            if (level >= 3) {
                val spots = listOf(
                    Offset(x - bodyLen * 0.2f, y - bodyH * 0.55f),
                    Offset(x + bodyLen * 0.1f, y - bodyH * 0.40f),
                    Offset(x - bodyLen * 0.5f, y + bodyH * 0.20f),
                    Offset(x + bodyLen * 0.25f, y + bodyH * 0.30f),
                )
                for (sp in spots) {
                    drawCircle(ManWrink.copy(alpha = 0.40f), 1.2f * s, sp)
                }
            }

            // ════════════════════════════════════════════════════════════════
            // EYE — small, beady
            // ════════════════════════════════════════════════════════════════
            val eyeR = (1.2f + level * 0.08f) * s
            val eyeX = x - bodyLen * 0.78f
            val eyeY = y - bodyH * 0.25f + bodyWave * 0.2f
            drawCircle(Color.White.copy(alpha = 0.75f), eyeR, Offset(eyeX, eyeY))
            drawCircle(Color(0xFF222222), eyeR * 0.6f, Offset(eyeX, eyeY))

            // ════════════════════════════════════════════════════════════════
            // SNOUT — large blunt front with mouth and whiskers
            // ════════════════════════════════════════════════════════════════
            // Mouth hint
            val mouthPath = Path().apply {
                moveTo(x - bodyLen * 0.95f, y + bodyWave * 0.15f)
                quadraticTo(
                    x - bodyLen * 0.90f, y + bodyH * 0.12f + bodyWave * 0.2f,
                    x - bodyLen * 0.82f, y + bodyH * 0.10f + bodyWave * 0.1f
                )
            }
            drawPath(mouthPath, ManWrink.copy(alpha = 0.5f),
                style = Stroke(width = (0.9f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round))

            // Whiskers/bristles
            val whiskerCount = 5
            for (w in 0 until whiskerCount) {
                val wy = y - bodyH * 0.18f + w * bodyH * 0.1f + bodyWave * 0.1f
                drawLine(
                    ManWrink.copy(alpha = 0.45f),
                    Offset(x - bodyLen, wy),
                    Offset(x - bodyLen - 2.5f * s, wy - 0.5f * s),
                    strokeWidth = (0.6f * s).coerceAtLeast(0.4f),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
