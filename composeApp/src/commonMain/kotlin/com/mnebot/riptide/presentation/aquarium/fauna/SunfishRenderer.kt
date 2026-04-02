package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
private val SunBody   = Color(0xFF7A8FA8)  // silver-gray
private val SunBelly  = Color(0xFFBBCCD8)  // lighter sides
private val SunFin    = Color(0xFF6A7F98)  // fin
private val SunEdge   = Color(0xFF5A6F88)  // outline
private val SunMouth  = Color(0xFF4A5F78)  // mouth

object SunfishRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyR    = (9f + level * 0.7f) * s   // body radius (near-circular)
        val finH     = (8f + level * 0.6f) * s   // dorsal/ventral fin height
        val finSway  = sin(t * 1.8f * PI.toFloat()) * 2.5f * s
        val claviusSway = sin(t * 1.2f * PI.toFloat()) * 1.5f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ════════════════════════════════════════════════════════════════
            // DORSAL FIN — tall, triangular, extending upward
            // ════════════════════════════════════════════════════════════════
            val dorsal = paths.obtain().apply {
                moveTo(x - bodyR * 0.2f, y - bodyR * 0.85f)
                cubicTo(
                    x - bodyR * 0.3f + finSway * 0.3f, y - bodyR - finH * 0.5f,
                    x + bodyR * 0.15f + finSway,        y - bodyR - finH,
                    x + bodyR * 0.3f  + finSway,        y - bodyR - finH * 1.05f
                )
                cubicTo(
                    x + bodyR * 0.5f + finSway * 0.7f,  y - bodyR * 0.95f,
                    x + bodyR * 0.35f, y - bodyR * 0.85f,
                    x - bodyR * 0.2f, y - bodyR * 0.85f
                )
                close()
            }
            drawPath(dorsal, SunFin)

            // ════════════════════════════════════════════════════════════════
            // VENTRAL FIN — mirror of dorsal, extending downward
            // ════════════════════════════════════════════════════════════════
            val ventral = paths.obtain().apply {
                moveTo(x - bodyR * 0.2f, y + bodyR * 0.85f)
                cubicTo(
                    x - bodyR * 0.3f - finSway * 0.3f, y + bodyR + finH * 0.5f,
                    x + bodyR * 0.15f - finSway,        y + bodyR + finH,
                    x + bodyR * 0.3f  - finSway,        y + bodyR + finH * 1.05f
                )
                cubicTo(
                    x + bodyR * 0.5f - finSway * 0.7f,  y + bodyR * 0.95f,
                    x + bodyR * 0.35f, y + bodyR * 0.85f,
                    x - bodyR * 0.2f, y + bodyR * 0.85f
                )
                close()
            }
            drawPath(ventral, SunFin)

            // ════════════════════════════════════════════════════════════════
            // MAIN BODY — near-circular disc
            // ════════════════════════════════════════════════════════════════
            drawCircle(SunBody, bodyR, Offset(x, y))

            // Belly highlight — lighter oval on sides
            val bellyPath = paths.obtain().apply {
                moveTo(x - bodyR * 0.6f, y)
                cubicTo(
                    x - bodyR * 0.6f, y - bodyR * 0.7f,
                    x + bodyR * 0.4f, y - bodyR * 0.55f,
                    x + bodyR * 0.5f, y
                )
                cubicTo(
                    x + bodyR * 0.4f, y + bodyR * 0.55f,
                    x - bodyR * 0.6f, y + bodyR * 0.7f,
                    x - bodyR * 0.6f, y
                )
                close()
            }
            drawPath(bellyPath, SunBelly.copy(alpha = 0.50f))

            // ════════════════════════════════════════════════════════════════
            // CLAVUS — wavy right edge (pseudo-tail)
            // ════════════════════════════════════════════════════════════════
            val claviusX = x + bodyR * 0.85f
            val numBumps = 4
            val bumpAmp  = 2.5f * s
            for (b in 0 until numBumps) {
                val by0 = y - bodyR * 0.65f + b * (bodyR * 1.3f / numBumps)
                val by1 = by0 + bodyR * 1.3f / numBumps
                val bx  = claviusX + sin(b.toFloat() + t * 2f) * bumpAmp + claviusSway
                drawLine(
                    SunEdge.copy(alpha = 0.55f),
                    Offset(claviusX + bumpAmp * 0.3f, by0),
                    Offset(bx, (by0 + by1) / 2f),
                    strokeWidth = (2.5f * s).coerceAtLeast(1.2f),
                    cap = StrokeCap.Round
                )
            }
            // Clavus fill area
            val clavius = paths.obtain().apply {
                moveTo(x + bodyR * 0.75f, y - bodyR * 0.65f)
                for (b in 0..numBumps) {
                    val by   = y - bodyR * 0.65f + b * (bodyR * 1.3f / numBumps)
                    val bxOff = sin(b.toFloat() + t * 2f) * bumpAmp + claviusSway
                    lineTo(x + bodyR * 0.9f + bxOff, by)
                }
                lineTo(x + bodyR * 0.75f, y + bodyR * 0.65f)
                close()
            }
            drawPath(clavius, SunFin.copy(alpha = 0.45f))

            // ════════════════════════════════════════════════════════════════
            // SKIN TEXTURE — subtle oval patches (level 3+)
            // ════════════════════════════════════════════════════════════════
            if (level >= 3) {
                val patches = listOf(
                    Offset(x - bodyR * 0.3f, y - bodyR * 0.4f) to 2.0f,
                    Offset(x + bodyR * 0.1f, y - bodyR * 0.55f) to 1.5f,
                    Offset(x - bodyR * 0.5f, y + bodyR * 0.2f) to 1.8f,
                    Offset(x + bodyR * 0.25f, y + bodyR * 0.4f) to 1.6f,
                    Offset(x - bodyR * 0.1f, y + bodyR * 0.1f) to 2.2f,
                    Offset(x + bodyR * 0.4f, y - bodyR * 0.2f) to 1.4f,
                )
                for ((center, radiusFactor) in patches) {
                    drawCircle(SunBelly.copy(alpha = 0.18f), radiusFactor * s, center)
                }
            }

            // Body outline
            drawCircle(SunEdge.copy(alpha = 0.40f), bodyR, Offset(x, y),
                style = Stroke(width = (0.8f * s).coerceAtLeast(0.5f)))

            // ════════════════════════════════════════════════════════════════
            // EYE
            // ════════════════════════════════════════════════════════════════
            val eyeR = (1.8f + level * 0.12f) * s
            val eyeX = x - bodyR * 0.52f
            val eyeY = y - bodyR * 0.18f
            drawCircle(Color.White.copy(alpha = 0.85f), eyeR, Offset(eyeX, eyeY))
            drawCircle(Color(0xFF222244), eyeR * 0.55f, Offset(eyeX + eyeR * 0.1f, eyeY))

            // ════════════════════════════════════════════════════════════════
            // MOUTH — small protrusion (protruding O-shape)
            // ════════════════════════════════════════════════════════════════
            val mouthX = x - bodyR * 0.88f
            val mouthY = y + bodyR * 0.05f
            drawCircle(SunMouth, 1.8f * s, Offset(mouthX, mouthY))
            drawCircle(Color(0xFF2A3040), 0.9f * s, Offset(mouthX, mouthY))
        }
    }
}
