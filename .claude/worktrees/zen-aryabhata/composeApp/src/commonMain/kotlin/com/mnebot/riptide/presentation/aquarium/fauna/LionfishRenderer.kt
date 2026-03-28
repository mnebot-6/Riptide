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
import kotlin.math.cos
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val LionBody   = Color(0xFF8B3020)  // dark red-brown
private val LionStripe = Color(0xFFF2E0C0)  // cream stripe
private val LionSpine  = Color(0xFFB04030)  // spine color
private val LionFin    = Color(0xFFD05030)  // pectoral fin
private val LionEye    = Color(0xFFFFCE40)  // yellow eye
private val LionWeb    = Color(0x559B3825)  // translucent webbing between spines

object LionfishRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen = (10f + level * 0.6f) * s
        val bodyH   = (8f  + level * 0.5f) * s
        val spineLen = (10f + level * 1.5f) * s  // level 5+: longer spines
        val spineExtra = if (level >= 5) 2.5f * s else 0f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ════════════════════════════════════════════════════════════════
            // PECTORAL FINS — large fan wings (drawn behind body)
            // ════════════════════════════════════════════════════════════════
            val pectSway = sin(t * 1.5f * PI.toFloat()) * 3f * s
            val pectW    = (10f + level * 0.8f) * s
            val pectH    = (8f  + level * 0.6f) * s

            // Top pectoral
            val pectTop = Path().apply {
                moveTo(x - bodyLen * 0.2f, y - bodyH * 0.3f)
                cubicTo(
                    x - bodyLen * 0.1f + pectSway * 0.3f, y - bodyH * 0.5f - pectH * 0.4f,
                    x + pectW * 0.4f   + pectSway * 0.7f, y - bodyH * 0.2f - pectH,
                    x + pectW          + pectSway,         y - bodyH * 0.1f - pectH * 0.3f
                )
                cubicTo(
                    x + pectW * 0.5f + pectSway * 0.5f, y + pectH * 0.1f,
                    x + bodyLen * 0.1f, y + bodyH * 0.1f,
                    x - bodyLen * 0.2f, y + bodyH * 0.1f
                )
                close()
            }
            drawPath(pectTop, LionFin.copy(alpha = 0.80f))

            // Fin rays on pectoral
            val rayCount = if (level >= 3) 7 else 5
            for (r in 0 until rayCount) {
                val frac = (r + 1).toFloat() / (rayCount + 1)
                val rayX = x + pectW * frac * 0.9f + pectSway * frac
                val rayY = y - bodyH * 0.1f - pectH * frac * 0.85f
                drawLine(
                    LionStripe.copy(alpha = if (level >= 3) 0.55f else 0.35f),
                    Offset(x - bodyLen * 0.15f, y),
                    Offset(rayX, rayY),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.5f),
                    cap = StrokeCap.Round
                )
            }

            // Bottom pectoral (mirror)
            val pectBot = Path().apply {
                moveTo(x - bodyLen * 0.2f, y + bodyH * 0.3f)
                cubicTo(
                    x - bodyLen * 0.1f + pectSway * 0.3f, y + bodyH * 0.5f + pectH * 0.4f,
                    x + pectW * 0.4f   + pectSway * 0.7f, y + bodyH * 0.2f + pectH,
                    x + pectW          + pectSway,         y + bodyH * 0.1f + pectH * 0.3f
                )
                cubicTo(
                    x + pectW * 0.5f + pectSway * 0.5f, y - pectH * 0.1f,
                    x + bodyLen * 0.1f, y - bodyH * 0.1f,
                    x - bodyLen * 0.2f, y - bodyH * 0.1f
                )
                close()
            }
            drawPath(pectBot, LionFin.copy(alpha = 0.80f))

            // ════════════════════════════════════════════════════════════════
            // DORSAL SPINES — fan of needle spines radiating upward
            // ════════════════════════════════════════════════════════════════
            val spineCount = if (level >= 5) 13 else 11
            val spineSway  = sin(t * 1.2f * PI.toFloat()) * 1.0f * s

            for (i in 0 until spineCount) {
                val frac   = i.toFloat() / (spineCount - 1)
                // Spines arc from front to back: angle from -60° to +10° (pointing up-forward to up-back)
                val angle  = -1.1f + frac * 1.4f  // radians from straight up
                val baseX  = x - bodyLen * 0.3f + frac * bodyLen * 0.7f
                val baseY  = y - bodyH * 0.8f
                val thisSpineLen = (spineLen + spineExtra) * (0.7f + sin(frac * PI.toFloat()) * 0.5f)
                val tipX   = baseX + sin(angle) * thisSpineLen + spineSway * (1f - frac)
                val tipY   = baseY - cos(angle) * thisSpineLen

                // Webbing between adjacent spines
                if (i > 0) {
                    val prevFrac  = (i - 1f) / (spineCount - 1)
                    val prevAngle = -1.1f + prevFrac * 1.4f
                    val prevBaseX = x - bodyLen * 0.3f + prevFrac * bodyLen * 0.7f
                    val prevLen   = (spineLen + spineExtra) * (0.7f + sin(prevFrac * PI.toFloat()) * 0.5f)
                    val prevTipX  = prevBaseX + sin(prevAngle) * prevLen * 0.55f + spineSway * (1f - prevFrac)
                    val prevTipY  = baseY     - cos(prevAngle) * prevLen * 0.55f
                    val midX      = baseX     + sin(angle)     * thisSpineLen    * 0.55f + spineSway * (1f - frac)
                    val midY      = baseY     - cos(angle)     * thisSpineLen    * 0.55f

                    val web = Path().apply {
                        moveTo(baseX, baseY)
                        lineTo(midX, midY)
                        lineTo(prevTipX, prevTipY)
                        lineTo(prevBaseX, baseY)
                        close()
                    }
                    drawPath(web, LionWeb)
                }

                // The spine itself
                drawLine(
                    LionSpine,
                    Offset(baseX, baseY),
                    Offset(tipX, tipY),
                    strokeWidth = (1.0f * s).coerceAtLeast(0.6f),
                    cap = StrokeCap.Round
                )
            }

            // ════════════════════════════════════════════════════════════════
            // MAIN BODY
            // ════════════════════════════════════════════════════════════════
            val body = Path().apply {
                moveTo(x - bodyLen, y)
                cubicTo(
                    x - bodyLen * 0.75f, y - bodyH,
                    x + bodyLen * 0.1f,  y - bodyH * 0.9f,
                    x + bodyLen * 0.5f,  y - bodyH * 0.25f
                )
                lineTo(x + bodyLen * 0.5f, y + bodyH * 0.25f)
                cubicTo(
                    x + bodyLen * 0.1f,  y + bodyH * 0.9f,
                    x - bodyLen * 0.75f, y + bodyH,
                    x - bodyLen, y
                )
                close()
            }
            drawPath(body, LionBody)

            // ── Cream vertical stripes on body
            val stripeCount = 6
            for (i in 1..stripeCount) {
                val sx = x - bodyLen + i * bodyLen * 1.4f / (stripeCount + 1)
                val stripeH = bodyH * (0.5f + sin(i.toFloat() / stripeCount * PI.toFloat()) * 0.4f)
                val stripePath = Path().apply {
                    moveTo(sx - 1.2f * s, y - stripeH * 0.85f)
                    cubicTo(
                        sx - 0.6f * s, y - stripeH,
                        sx + 0.6f * s, y - stripeH,
                        sx + 1.2f * s, y - stripeH * 0.85f
                    )
                    cubicTo(
                        sx + 1.8f * s, y, sx + 1.8f * s, y,
                        sx + 1.2f * s, y + stripeH * 0.85f
                    )
                    cubicTo(
                        sx + 0.6f * s, y + stripeH,
                        sx - 0.6f * s, y + stripeH,
                        sx - 1.2f * s, y + stripeH * 0.85f
                    )
                    cubicTo(
                        sx - 1.8f * s, y, sx - 1.8f * s, y,
                        sx - 1.2f * s, y - stripeH * 0.85f
                    )
                    close()
                }
                drawPath(stripePath, LionStripe.copy(alpha = 0.30f))
            }

            // ════════════════════════════════════════════════════════════════
            // EYE — large, yellow iris
            // ════════════════════════════════════════════════════════════════
            val eyeR = (2.2f + level * 0.15f) * s
            val eyeX = x - bodyLen * 0.60f
            val eyeY = y - bodyH * 0.28f
            drawCircle(Color.White, eyeR, Offset(eyeX, eyeY))
            drawCircle(LionEye, eyeR * 0.72f, Offset(eyeX, eyeY))
            drawCircle(Color(0xFF111111), eyeR * 0.38f, Offset(eyeX, eyeY))

            // Supraorbital tentacle above eye
            val tentSway = sin(t * 2f * PI.toFloat()) * 0.8f * s
            drawLine(
                LionBody,
                Offset(eyeX, eyeY - eyeR),
                Offset(eyeX + tentSway, eyeY - eyeR - 3f * s),
                strokeWidth = (1.4f * s).coerceAtLeast(0.7f),
                cap = StrokeCap.Round
            )
            drawCircle(LionBody, 0.8f * s, Offset(eyeX + tentSway, eyeY - eyeR - 3f * s))

            // ════════════════════════════════════════════════════════════════
            // VENTRAL / PELVIC FINS — small below
            // ════════════════════════════════════════════════════════════════
            val ventral = Path().apply {
                moveTo(x, y + bodyH * 0.85f)
                cubicTo(
                    x + bodyLen * 0.05f, y + bodyH * 1.35f,
                    x + bodyLen * 0.2f,  y + bodyH * 1.30f,
                    x + bodyLen * 0.25f, y + bodyH * 0.85f
                )
                close()
            }
            drawPath(ventral, LionSpine.copy(alpha = 0.7f))

            // Body outline
            drawPath(body, LionBody.copy(alpha = 0.4f),
                style = Stroke(width = (0.7f * s).coerceAtLeast(0.5f)))
        }
    }
}
