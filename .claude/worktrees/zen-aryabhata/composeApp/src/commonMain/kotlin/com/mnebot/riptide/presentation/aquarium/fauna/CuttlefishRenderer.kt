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
private val CuttBody  = Color(0xFF6A3050)  // dark purple-brown
private val CuttBody2 = Color(0xFF7A4060)  // lighter body
private val CuttFin   = Color(0xFF8A4070)  // fin color
private val CuttArm   = Color(0xFF5A2040)  // tentacles
private val CuttEye   = Color(0xFFE8C050)  // eye iris
private val CuttPupil = Color(0xFF000000)  // W-shaped pupil
private val CuttShim  = Color(0x55AA88CC)  // iridescent shimmer

object CuttlefishRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen  = (11f + level * 0.7f) * s
        val bodyH    = (6f  + level * 0.4f) * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ════════════════════════════════════════════════════════════════
            // SIDE FIN SKIRT — undulating fin along both sides
            // ════════════════════════════════════════════════════════════════
            val finSegments = 14
            val finAmplitude = (2.5f + level * 0.2f) * s

            // Top fin skirt
            val topFin = Path().apply {
                moveTo(x - bodyLen * 0.45f, y - bodyH * 0.78f)
                for (i in 0..finSegments) {
                    val frac  = i.toFloat() / finSegments
                    val fx    = x - bodyLen * 0.45f + frac * bodyLen * 0.9f
                    val phase = frac * 4f * PI.toFloat()
                    val wave  = sin(t * 3f * PI.toFloat() + phase) * finAmplitude
                    val baseY = y - bodyH * 0.78f  // at body edge
                    val finY  = baseY - finAmplitude - wave * 0.8f
                    if (i == 0) moveTo(fx, finY)
                    else lineTo(fx, finY)
                }
                // Back along body edge
                for (i in finSegments downTo 0) {
                    val frac = i.toFloat() / finSegments
                    val fx   = x - bodyLen * 0.45f + frac * bodyLen * 0.9f
                    lineTo(fx, y - bodyH * 0.78f)
                }
                close()
            }
            drawPath(topFin, CuttFin.copy(alpha = 0.70f))

            // Bottom fin skirt (mirror)
            val botFin = Path().apply {
                moveTo(x - bodyLen * 0.45f, y + bodyH * 0.78f)
                for (i in 0..finSegments) {
                    val frac  = i.toFloat() / finSegments
                    val fx    = x - bodyLen * 0.45f + frac * bodyLen * 0.9f
                    val phase = frac * 4f * PI.toFloat()
                    val wave  = sin(t * 3f * PI.toFloat() + phase + 0.8f) * finAmplitude
                    val baseY = y + bodyH * 0.78f
                    val finY  = baseY + finAmplitude + wave * 0.8f
                    if (i == 0) moveTo(fx, finY)
                    else lineTo(fx, finY)
                }
                for (i in finSegments downTo 0) {
                    val frac = i.toFloat() / finSegments
                    val fx   = x - bodyLen * 0.45f + frac * bodyLen * 0.9f
                    lineTo(fx, y + bodyH * 0.78f)
                }
                close()
            }
            drawPath(botFin, CuttFin.copy(alpha = 0.70f))

            // ════════════════════════════════════════════════════════════════
            // MAIN BODY MANTLE — oval, somewhat flattened
            // ════════════════════════════════════════════════════════════════
            val body = Path().apply {
                moveTo(x - bodyLen * 0.5f, y)  // rear (blunt)
                cubicTo(
                    x - bodyLen * 0.5f, y - bodyH,
                    x + bodyLen * 0.2f, y - bodyH,
                    x + bodyLen * 0.5f, y - bodyH * 0.35f
                )
                cubicTo(
                    x + bodyLen * 0.58f, y, x + bodyLen * 0.5f, y + bodyH * 0.35f,
                    x + bodyLen * 0.2f, y + bodyH
                )
                cubicTo(
                    x - bodyLen * 0.2f, y + bodyH,
                    x - bodyLen * 0.5f, y + bodyH,
                    x - bodyLen * 0.5f, y
                )
                close()
            }
            drawPath(body, CuttBody)

            // Body stripes / chromatophore pattern
            for (i in 0 until 4) {
                val frac = i.toFloat() / 3f
                val strX = x - bodyLen * 0.3f + frac * bodyLen * 0.65f
                val strH = bodyH * (0.5f + sin(frac * PI.toFloat()) * 0.35f)
                drawLine(
                    CuttBody2.copy(alpha = 0.35f),
                    Offset(strX, y - strH * 0.8f),
                    Offset(strX, y + strH * 0.8f),
                    strokeWidth = (2.2f * s).coerceAtLeast(1.0f),
                    cap = StrokeCap.Round
                )
            }

            // Iridescent shimmer highlights
            drawLine(
                CuttShim,
                Offset(x - bodyLen * 0.2f, y - bodyH * 0.35f),
                Offset(x + bodyLen * 0.35f, y - bodyH * 0.25f),
                strokeWidth = (3.0f * s).coerceAtLeast(1.5f),
                cap = StrokeCap.Round
            )

            // ════════════════════════════════════════════════════════════════
            // ARMS — 8 short arms at front + 2 longer tentacles
            // ════════════════════════════════════════════════════════════════
            val armBaseX = x + bodyLen * 0.5f
            val armBaseY = y

            // 4 arms top + 4 arms bottom
            for (a in 0 until 4) {
                val armSway = sin(t * 2f * PI.toFloat() + a * 0.5f) * 1.5f * s
                val spread  = (a.toFloat() / 3f - 0.5f) * bodyH * 1.0f

                // Short arms (upper group)
                val armLen = (3.5f + level * 0.2f) * s
                val tipX   = armBaseX + armLen + armSway
                val tipY   = armBaseY + spread - bodyH * 0.1f
                val arm = Path().apply {
                    moveTo(armBaseX, armBaseY + spread * 0.5f - bodyH * 0.05f)
                    quadraticTo(
                        armBaseX + armLen * 0.5f + armSway * 0.5f,
                        armBaseY + spread * 0.7f - bodyH * 0.05f,
                        tipX, tipY
                    )
                }
                drawPath(arm, CuttArm, style = Stroke(
                    width = (1.2f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round))
            }

            // 2 longer feeding tentacles
            for (a in 0..1) {
                val tentSway = sin(t * 2.5f * PI.toFloat() + a * PI.toFloat()) * 2f * s
                val tentLen  = (7f + level * 0.4f) * s
                val spread   = (a - 0.5f) * bodyH * 0.4f
                val tent = Path().apply {
                    moveTo(armBaseX, armBaseY + spread)
                    cubicTo(
                        armBaseX + tentLen * 0.3f + tentSway * 0.3f, armBaseY + spread,
                        armBaseX + tentLen * 0.6f + tentSway, armBaseY + spread + (if (a == 0) -1f else 1f) * bodyH * 0.3f,
                        armBaseX + tentLen + tentSway, armBaseY + spread + (if (a == 0) -1f else 1f) * bodyH * 0.5f
                    )
                }
                drawPath(tent, CuttArm, style = Stroke(
                    width = (1.0f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round))
                // Club at tip
                drawCircle(CuttArm.copy(alpha = 0.8f), 1.2f * s,
                    Offset(armBaseX + tentLen + tentSway,
                        armBaseY + spread + (if (a == 0) -1f else 1f) * bodyH * 0.5f))
            }

            // ════════════════════════════════════════════════════════════════
            // EYE — distinctive W/U-shaped pupil
            // ════════════════════════════════════════════════════════════════
            val eyeR  = (2.5f + level * 0.15f) * s
            val eyeX  = x + bodyLen * 0.20f
            val eyeY  = y - bodyH * 0.15f
            // Sclera
            drawCircle(Color(0xFFEEEECC), eyeR, Offset(eyeX, eyeY))
            // Iris
            drawCircle(CuttEye, eyeR * 0.80f, Offset(eyeX, eyeY))
            // W-shaped pupil: draw as horizontal thick bar with notch in center
            val pupilW = eyeR * 1.2f
            val pupilH = eyeR * 0.45f
            val pupil = Path().apply {
                moveTo(eyeX - pupilW, eyeY - pupilH * 0.2f)
                lineTo(eyeX - pupilW * 0.35f, eyeY - pupilH * 0.2f)
                lineTo(eyeX - pupilW * 0.15f, eyeY + pupilH * 0.6f)
                lineTo(eyeX, eyeY)
                lineTo(eyeX + pupilW * 0.15f, eyeY + pupilH * 0.6f)
                lineTo(eyeX + pupilW * 0.35f, eyeY - pupilH * 0.2f)
                lineTo(eyeX + pupilW, eyeY - pupilH * 0.2f)
                cubicTo(
                    eyeX + pupilW, eyeY + pupilH * 0.3f,
                    eyeX - pupilW, eyeY + pupilH * 0.3f,
                    eyeX - pupilW, eyeY - pupilH * 0.2f
                )
                close()
            }
            drawPath(pupil, CuttPupil)
        }
    }
}
