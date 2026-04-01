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

// ── Palette ──────────────────────────────────────────────────────────────────
private val BodyGreen      = Color(0xFF4A7A3C)  // olive-green body
private val BodyDarkGreen  = Color(0xFF2E5228)  // darker green
private val BodyLightGreen = Color(0xFF6A9A58)  // lighter green highlight
private val BodyBrown      = Color(0xFF5C5030)  // brown mottled tone
private val BodyDarkBrown  = Color(0xFF3A3218)  // dark brown spots
private val BellyYellow    = Color(0xFFB8A868)  // pale belly
private val BellyPale      = Color(0xFFD0C890)  // light belly highlight
private val MouthPink      = Color(0xFFCC6060)  // mouth interior
private val ToothWhite     = Color(0xFFF0ECE0)  // teeth
private val EyeYellow      = Color(0xFFD0C020)  // yellow iris
private val ShadowNavy     = Color(0xFF0A0A20)  // dark shadow
private val ShadowGreen    = Color(0xFF1A3A10)  // green shadow
private val SpotDark       = Color(0xFF2A3A18)  // spot color (level 3+)
private val GlowYellow     = Color(0xFFFFE060)  // eye glow (level 5+)

object MorayEelRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyLen = (20f + level * 1.2f) * s
        val bodyW   = (3.5f + level * 0.25f) * s

        // Sinusoidal undulation: multiple wave nodes along the body
        val waveAmp   = 2.5f * s
        val waveFreq  = 2.0f
        val jawOpen   = (sin(t * 0.6f * PI.toFloat()) * 0.5f + 0.5f) * 1.5f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── BODY: build a thick sinusoidal serpentine path ───────────────
            // We draw the body as a filled path tracing upper + lower edges
            val segments = 24
            val upperPoints = mutableListOf<Offset>()
            val lowerPoints = mutableListOf<Offset>()

            for (i in 0..segments) {
                val frac = i.toFloat() / segments
                val segX = x - bodyLen * 0.45f + frac * bodyLen
                // Taper width: thickest in middle, thinner at head and tail
                val taper = (1f - (frac - 0.4f) * (frac - 0.4f) * 2.5f).coerceIn(0.25f, 1f)
                val halfW = bodyW * taper
                // Wave offset: propagates from tail to head
                val waveOffset = sin(t * waveFreq * PI.toFloat() + frac * 3.0f * PI.toFloat()) * waveAmp * frac
                upperPoints.add(Offset(segX, y - halfW + waveOffset))
                lowerPoints.add(Offset(segX, y + halfW + waveOffset))
            }

            // Body shadow
            val bodyShadow = Path().apply {
                moveTo(upperPoints[0].x, upperPoints[0].y - s * 0.3f)
                for (i in 1 until upperPoints.size) {
                    lineTo(upperPoints[i].x, upperPoints[i].y - s * 0.3f)
                }
                for (i in lowerPoints.size - 1 downTo 0) {
                    lineTo(lowerPoints[i].x, lowerPoints[i].y + s * 0.3f)
                }
                close()
            }
            drawPath(bodyShadow, ShadowGreen.copy(alpha = 0.20f))

            // Main body fill
            val bodyPath = Path().apply {
                moveTo(upperPoints[0].x, upperPoints[0].y)
                for (i in 1 until upperPoints.size) {
                    lineTo(upperPoints[i].x, upperPoints[i].y)
                }
                for (i in lowerPoints.size - 1 downTo 0) {
                    lineTo(lowerPoints[i].x, lowerPoints[i].y)
                }
                close()
            }
            drawPath(bodyPath, BodyGreen)

            // Belly lighter zone (lower half)
            val bellyPath = Path().apply {
                val midIdx = segments / 2
                moveTo(lowerPoints[0].x, (upperPoints[0].y + lowerPoints[0].y) / 2f)
                for (i in 1 until lowerPoints.size) {
                    lineTo(lowerPoints[i].x, (upperPoints[i].y + lowerPoints[i].y) / 2f + bodyW * 0.15f)
                }
                for (i in lowerPoints.size - 1 downTo 0) {
                    lineTo(lowerPoints[i].x, lowerPoints[i].y)
                }
                close()
            }
            drawPath(bellyPath, BellyYellow.copy(alpha = 0.30f))

            // Upper highlight
            val hlPath = Path().apply {
                moveTo(upperPoints[1].x, upperPoints[1].y)
                for (i in 2 until upperPoints.size - 1) {
                    lineTo(upperPoints[i].x, upperPoints[i].y + bodyW * 0.15f)
                }
                for (i in upperPoints.size - 2 downTo 1) {
                    lineTo(upperPoints[i].x, upperPoints[i].y)
                }
                close()
            }
            drawPath(hlPath, BodyLightGreen.copy(alpha = 0.25f))

            // Mottled brown pattern
            for (i in 2 until segments - 2 step 2) {
                val mx = upperPoints[i].x
                val my = (upperPoints[i].y + lowerPoints[i].y) / 2f
                val mottleR = bodyW * 0.35f * (0.7f + 0.3f * sin(i.toFloat() * 1.7f))
                drawCircle(BodyBrown.copy(alpha = 0.25f), mottleR, Offset(mx, my))
                drawCircle(BodyBrown.copy(alpha = 0.15f), mottleR * 0.6f,
                    Offset(mx + mottleR * 0.3f, my - mottleR * 0.2f))
            }

            // Body outline
            drawPath(bodyPath, ShadowGreen.copy(alpha = 0.15f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f)))

            // ── LEVEL 3+: Spotted pattern ───────────────────────────────────
            if (level >= 3) {
                for (i in 1 until segments - 1 step 1) {
                    val spotFrac = i.toFloat() / segments
                    val spotX = x - bodyLen * 0.45f + spotFrac * bodyLen
                    val waveOff = sin(t * waveFreq * PI.toFloat() + spotFrac * 3.0f * PI.toFloat()) * waveAmp * spotFrac
                    // Two rows of spots
                    val spotR = s * 0.55f * (0.8f + 0.2f * sin(i.toFloat() * 2.3f))
                    drawCircle(SpotDark.copy(alpha = 0.40f), spotR,
                        Offset(spotX, y - bodyW * 0.3f + waveOff))
                    drawCircle(SpotDark.copy(alpha = 0.30f), spotR * 0.8f,
                        Offset(spotX + bodyW * 0.2f, y + bodyW * 0.2f + waveOff))
                }
            }

            // ── HEAD (slightly wider, at left end of body) ──────────────────
            val headX = x - bodyLen * 0.45f
            val headWave = sin(t * waveFreq * PI.toFloat()) * waveAmp * 0f // head is stable
            val headW = bodyW * 1.15f

            val headPath = Path().apply {
                moveTo(headX, y - headW + headWave)
                cubicTo(
                    headX - headW * 0.8f, y - headW * 0.7f + headWave,
                    headX - headW * 0.8f, y + headW * 0.7f + headWave,
                    headX, y + headW + headWave
                )
                cubicTo(
                    headX + headW * 0.4f, y + headW * 0.5f + headWave,
                    headX + headW * 0.4f, y - headW * 0.5f + headWave,
                    headX, y - headW + headWave
                )
                close()
            }
            drawPath(headPath, BodyGreen)
            drawPath(headPath, BodyLightGreen.copy(alpha = 0.20f))

            // ── OPEN MOUTH (gap between upper and lower jaw) ────────────────
            val mouthX = headX - headW * 0.6f
            val mouthY = y + headWave

            // Upper jaw
            val upperJaw = Path().apply {
                moveTo(mouthX + headW * 0.3f, mouthY - headW * 0.2f)
                cubicTo(
                    mouthX - headW * 0.2f, mouthY - headW * 0.3f - jawOpen * 0.3f,
                    mouthX - headW * 0.7f, mouthY - headW * 0.15f - jawOpen * 0.5f,
                    mouthX - headW * 0.5f, mouthY - jawOpen * 0.2f
                )
                lineTo(mouthX + headW * 0.1f, mouthY)
                close()
            }
            drawPath(upperJaw, BodyDarkGreen)

            // Lower jaw
            val lowerJaw = Path().apply {
                moveTo(mouthX + headW * 0.3f, mouthY + headW * 0.2f)
                cubicTo(
                    mouthX - headW * 0.2f, mouthY + headW * 0.3f + jawOpen * 0.3f,
                    mouthX - headW * 0.7f, mouthY + headW * 0.15f + jawOpen * 0.5f,
                    mouthX - headW * 0.5f, mouthY + jawOpen * 0.2f
                )
                lineTo(mouthX + headW * 0.1f, mouthY)
                close()
            }
            drawPath(lowerJaw, BodyDarkGreen)

            // Mouth interior
            val mouthInterior = Path().apply {
                moveTo(mouthX + headW * 0.1f, mouthY - jawOpen * 0.15f)
                cubicTo(
                    mouthX - headW * 0.3f, mouthY - jawOpen * 0.3f,
                    mouthX - headW * 0.3f, mouthY + jawOpen * 0.3f,
                    mouthX + headW * 0.1f, mouthY + jawOpen * 0.15f
                )
                close()
            }
            drawPath(mouthInterior, MouthPink.copy(alpha = 0.6f))

            // Teeth (upper row)
            for (i in 0 until 4) {
                val tx = mouthX - headW * 0.05f - i * headW * 0.13f
                drawLine(
                    ToothWhite.copy(alpha = 0.70f),
                    Offset(tx, mouthY - headW * 0.15f - jawOpen * 0.1f),
                    Offset(tx + s * 0.2f, mouthY - s * 0.2f),
                    strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                    cap = StrokeCap.Round
                )
            }
            // Teeth (lower row)
            for (i in 0 until 3) {
                val tx = mouthX - headW * 0.08f - i * headW * 0.15f
                drawLine(
                    ToothWhite.copy(alpha = 0.55f),
                    Offset(tx, mouthY + headW * 0.12f + jawOpen * 0.1f),
                    Offset(tx + s * 0.2f, mouthY + s * 0.15f),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.25f),
                    cap = StrokeCap.Round
                )
            }

            // ── EYE ─────────────────────────────────────────────────────────
            val eyeR = (1.3f + level * 0.08f) * s
            val eyeX = headX - headW * 0.15f
            val eyeY = y - headW * 0.45f + headWave

            drawCircle(ShadowNavy.copy(alpha = 0.30f), eyeR * 1.2f, Offset(eyeX, eyeY))
            drawCircle(EyeYellow, eyeR, Offset(eyeX, eyeY))
            drawCircle(ShadowNavy, eyeR * 0.42f, Offset(eyeX + eyeR * 0.05f, eyeY + eyeR * 0.02f))
            drawCircle(Color.White, eyeR * 0.18f, Offset(eyeX - eyeR * 0.15f, eyeY - eyeR * 0.15f))

            // ── LEVEL 5+: Eyes glow faintly ─────────────────────────────────
            if (level >= 5) {
                val glowAlpha = (sin(t * 1.0f * PI.toFloat()) * 0.5f + 0.5f) * 0.30f
                drawCircle(GlowYellow.copy(alpha = glowAlpha), eyeR * 2.5f, Offset(eyeX, eyeY))
                drawCircle(GlowYellow.copy(alpha = glowAlpha * 0.5f), eyeR * 4.0f, Offset(eyeX, eyeY))
            }
        }
    }
}
