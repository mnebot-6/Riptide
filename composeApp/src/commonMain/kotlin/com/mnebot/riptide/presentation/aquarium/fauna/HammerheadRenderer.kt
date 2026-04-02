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
private val HamDorsal  = Color(0xFF3A4A5A)  // dark blue-gray
private val HamBelly   = Color(0xFFD0D8E0)  // white/light gray
private val HamFin     = Color(0xFF2A3A4A)  // fins darker
private val HamEdge    = Color(0xFF1A2A3A)  // outline
private val HamEye     = Color(0xFF111111)  // eye

object HammerheadRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen  = (14f + level * 1.0f) * s
        val bodyH    = (5.5f + level * 0.35f) * s
        val hammerW  = (9f   + level * 0.6f) * s  // half-width of the hammerhead
        val tailSway = sin(t * 3.0f * PI.toFloat()) * 2.5f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ════════════════════════════════════════════════════════════════
            // TAIL FIN — crescent shape
            // ════════════════════════════════════════════════════════════════
            val tailX = x + bodyLen * 0.45f
            val tailTop = paths.obtain().apply {
                moveTo(tailX, y - bodyH * 0.35f)
                cubicTo(
                    tailX + bodyLen * 0.12f, y - bodyH * 0.2f + tailSway * 0.3f,
                    tailX + bodyLen * 0.22f, y - bodyH * 0.8f + tailSway,
                    tailX + bodyLen * 0.30f, y - bodyH * 1.1f + tailSway
                )
                cubicTo(
                    tailX + bodyLen * 0.22f, y - bodyH * 0.25f + tailSway * 0.4f,
                    tailX + bodyLen * 0.1f,  y + tailSway * 0.1f,
                    tailX, y
                )
                close()
            }
            drawPath(tailTop, HamFin)
            val tailBot = paths.obtain().apply {
                moveTo(tailX, y)
                cubicTo(
                    tailX + bodyLen * 0.1f,  y + tailSway * 0.1f,
                    tailX + bodyLen * 0.22f, y + bodyH * 0.25f + tailSway * 0.4f,
                    tailX + bodyLen * 0.30f, y + bodyH * 1.1f + tailSway
                )
                cubicTo(
                    tailX + bodyLen * 0.22f, y + bodyH * 0.8f + tailSway,
                    tailX + bodyLen * 0.12f, y + bodyH * 0.2f + tailSway * 0.3f,
                    tailX, y + bodyH * 0.35f
                )
                close()
            }
            drawPath(tailBot, HamFin)

            // ════════════════════════════════════════════════════════════════
            // DORSAL FIN
            // ════════════════════════════════════════════════════════════════
            val dorsalSway = sin(t * 2f * PI.toFloat()) * 0.8f * s
            val dorsal = paths.obtain().apply {
                moveTo(x - bodyLen * 0.05f, y - bodyH * 0.9f)
                cubicTo(
                    x + bodyLen * 0.05f + dorsalSway, y - bodyH * 2.0f,
                    x + bodyLen * 0.20f + dorsalSway, y - bodyH * 1.9f,
                    x + bodyLen * 0.32f, y - bodyH * 0.85f
                )
                cubicTo(
                    x + bodyLen * 0.20f, y - bodyH * 0.75f,
                    x + bodyLen * 0.05f, y - bodyH * 0.80f,
                    x - bodyLen * 0.05f, y - bodyH * 0.9f
                )
                close()
            }
            drawPath(dorsal, HamFin)

            // Level 5+: second smaller dorsal fin
            if (level >= 5) {
                val dorsal2 = paths.obtain().apply {
                    moveTo(x + bodyLen * 0.28f, y - bodyH * 0.82f)
                    cubicTo(
                        x + bodyLen * 0.32f + dorsalSway * 0.5f, y - bodyH * 1.3f,
                        x + bodyLen * 0.40f + dorsalSway * 0.5f, y - bodyH * 1.2f,
                        x + bodyLen * 0.44f, y - bodyH * 0.78f
                    )
                    cubicTo(
                        x + bodyLen * 0.40f, y - bodyH * 0.70f,
                        x + bodyLen * 0.32f, y - bodyH * 0.75f,
                        x + bodyLen * 0.28f, y - bodyH * 0.82f
                    )
                    close()
                }
                drawPath(dorsal2, HamFin)
            }

            // ════════════════════════════════════════════════════════════════
            // PECTORAL FINS
            // ════════════════════════════════════════════════════════════════
            val pect = paths.obtain().apply {
                moveTo(x - bodyLen * 0.15f, y + bodyH * 0.55f)
                cubicTo(
                    x + bodyLen * 0.0f,  y + bodyH * 1.5f,
                    x + bodyLen * 0.18f, y + bodyH * 1.55f,
                    x + bodyLen * 0.30f, y + bodyH * 0.75f
                )
                cubicTo(
                    x + bodyLen * 0.15f, y + bodyH * 0.5f,
                    x, y + bodyH * 0.48f,
                    x - bodyLen * 0.15f, y + bodyH * 0.55f
                )
                close()
            }
            drawPath(pect, HamFin.copy(alpha = 0.85f))

            // ════════════════════════════════════════════════════════════════
            // MAIN BODY — torpedo shape
            // ════════════════════════════════════════════════════════════════
            val body = paths.obtain().apply {
                // From hammer junction to tail
                moveTo(x - bodyLen * 0.1f, y - bodyH)
                cubicTo(
                    x + bodyLen * 0.1f, y - bodyH,
                    x + bodyLen * 0.5f, y - bodyH * 0.8f,
                    tailX, y - bodyH * 0.3f
                )
                lineTo(tailX, y + bodyH * 0.3f)
                cubicTo(
                    x + bodyLen * 0.5f, y + bodyH * 0.8f,
                    x + bodyLen * 0.1f, y + bodyH,
                    x - bodyLen * 0.1f, y + bodyH
                )
                close()
            }
            drawPath(body, HamDorsal)

            // Belly counter-shading
            val belly = paths.obtain().apply {
                moveTo(x - bodyLen * 0.1f, y + bodyH * 0.1f)
                cubicTo(
                    x + bodyLen * 0.15f, y + bodyH * 0.9f,
                    x + bodyLen * 0.4f,  y + bodyH * 0.75f,
                    tailX, y + bodyH * 0.15f
                )
                cubicTo(
                    x + bodyLen * 0.4f,  y + bodyH * 0.35f,
                    x + bodyLen * 0.15f, y + bodyH * 0.5f,
                    x - bodyLen * 0.1f, y + bodyH * 0.1f
                )
                close()
            }
            drawPath(belly, HamBelly.copy(alpha = 0.55f))

            // ════════════════════════════════════════════════════════════════
            // HAMMER HEAD — the key feature!
            // ════════════════════════════════════════════════════════════════
            val headX = x - bodyLen * 0.15f  // center of hammer
            val hammer = paths.obtain().apply {
                // Top lobe
                moveTo(headX - hammerW, y - bodyH * 0.12f)
                quadraticTo(headX - hammerW * 1.1f, y - bodyH * 1.05f,
                             headX - hammerW * 0.5f, y - bodyH * 1.0f)
                cubicTo(
                    headX - hammerW * 0.2f, y - bodyH * 0.95f,
                    headX + hammerW * 0.2f, y - bodyH * 0.95f,
                    headX + hammerW * 0.5f, y - bodyH * 1.0f
                )
                quadraticTo(headX + hammerW * 1.1f, y - bodyH * 1.05f,
                             headX + hammerW, y - bodyH * 0.12f)
                // Bottom lobe (underside of hammer)
                cubicTo(
                    headX + hammerW * 0.6f, y + bodyH * 0.55f,
                    headX - hammerW * 0.6f, y + bodyH * 0.55f,
                    headX - hammerW, y - bodyH * 0.12f
                )
                close()
            }
            drawPath(hammer, HamDorsal)

            // Counter-shading on underside of hammer
            val hammerBelly = paths.obtain().apply {
                moveTo(headX - hammerW * 0.9f, y + bodyH * 0.10f)
                cubicTo(
                    headX - hammerW * 0.5f, y + bodyH * 0.52f,
                    headX + hammerW * 0.5f, y + bodyH * 0.52f,
                    headX + hammerW * 0.9f, y + bodyH * 0.10f
                )
                cubicTo(
                    headX + hammerW * 0.5f, y + bodyH * 0.35f,
                    headX - hammerW * 0.5f, y + bodyH * 0.35f,
                    headX - hammerW * 0.9f, y + bodyH * 0.10f
                )
                close()
            }
            drawPath(hammerBelly, HamBelly.copy(alpha = 0.45f))

            // Hammer outline
            drawPath(hammer, HamEdge.copy(alpha = 0.35f),
                style = Stroke(width = (0.7f * s).coerceAtLeast(0.4f)))

            // ════════════════════════════════════════════════════════════════
            // EYES — at each tip of the hammer
            // ════════════════════════════════════════════════════════════════
            val eyeR  = (1.4f + level * 0.1f) * s
            val eyeYPos = y - bodyH * 0.55f
            // Left eye
            drawCircle(Color.White.copy(alpha = 0.85f), eyeR, Offset(headX - hammerW * 0.88f, eyeYPos))
            drawCircle(HamEye, eyeR * 0.55f, Offset(headX - hammerW * 0.88f, eyeYPos))
            // Right eye
            drawCircle(Color.White.copy(alpha = 0.85f), eyeR, Offset(headX + hammerW * 0.88f, eyeYPos))
            drawCircle(HamEye, eyeR * 0.55f, Offset(headX + hammerW * 0.88f, eyeYPos))

            // Small mouth on underside of hammer
            val mouthPath = paths.obtain().apply {
                moveTo(headX - 2f * s, y + bodyH * 0.42f)
                quadraticTo(headX, y + bodyH * 0.50f, headX + 2f * s, y + bodyH * 0.42f)
            }
            drawPath(mouthPath, HamEdge.copy(alpha = 0.6f),
                style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round))

            // ════════════════════════════════════════════════════════════════
            // LEVEL 3+: gill slits
            // ════════════════════════════════════════════════════════════════
            if (level >= 3) {
                val gillCount = 4
                for (g in 0 until gillCount) {
                    val gx = x - bodyLen * 0.05f + g * 1.8f * s
                    drawLine(
                        HamEdge.copy(alpha = 0.40f),
                        Offset(gx, y - bodyH * 0.70f),
                        Offset(gx + 0.5f * s, y - bodyH * 0.25f),
                        strokeWidth = (0.7f * s).coerceAtLeast(0.4f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
