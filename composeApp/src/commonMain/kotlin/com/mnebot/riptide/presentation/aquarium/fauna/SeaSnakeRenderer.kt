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

// ── Palette ─────────────────────────────────────────────────────────────────
private val BandBlack       = Color(0xFF101820)  // black bands
private val BandDarkBlue    = Color(0xFF1A2838)  // dark blue-black
private val BandLightBlue   = Color(0xFF68B8D8)  // light blue bands
private val BandCyan        = Color(0xFF50A0C0)  // cyan mid-tone
private val BandYellow      = Color(0xFFE8D060)  // yellow accent bands
private val BandPaleYellow  = Color(0xFFF0E088)  // pale yellow highlight
private val BellyPale       = Color(0xFFD0E0E8)  // pale belly
private val ShadowDeep      = Color(0xFF081018)  // deep shadow
private val EyeGold         = Color(0xFFD0A830)  // golden eye
private val VenomYellow     = Color(0xFFE8D040)  // venom aura

object SeaSnakeRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyLen = (18f + level * 1.0f) * s
        val bodyThick = (2.2f + level * 0.15f) * s

        // High-frequency sinusoidal body undulation
        val waveFreq = 3.5f * PI.toFloat()
        val waveAmp = bodyThick * 1.8f

        // Number of segments for the spine
        val segments = 32
        val bandCount = if (level >= 3) 14 else 10

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── LEVEL 5+: VENOM AURA (faint yellow haze) ────────────────
            if (level >= 5) {
                val auraPulse = sin(t * 2.0f * PI.toFloat()) * 0.06f
                // Draw aura along body spine
                for (seg in 0..segments step 4) {
                    val frac = seg.toFloat() / segments
                    val sx = x - bodyLen * 0.5f + bodyLen * frac
                    val sy = y + sin(t * waveFreq + frac * PI.toFloat() * 3f) * waveAmp * frac
                    val auraR = bodyThick * (2.5f + sin(frac * PI.toFloat()) * 0.5f)
                    drawCircle(VenomYellow.copy(alpha = 0.06f + auraPulse),
                        auraR, Offset(sx, sy))
                }
            }

            // ── BODY SPINE: compute positions ────────────────────────────
            val spineX = FloatArray(segments + 1)
            val spineY = FloatArray(segments + 1)
            for (seg in 0..segments) {
                val frac = seg.toFloat() / segments
                spineX[seg] = x - bodyLen * 0.5f + bodyLen * frac
                // Wave amplitude increases from head to tail
                val ampScale = frac * frac  // quadratic increase
                spineY[seg] = y + sin(t * waveFreq + frac * PI.toFloat() * 3f) * waveAmp * ampScale
            }

            // ── BODY SHADOW ──────────────────────────────────────────────
            val shadowPath = paths.obtain().apply {
                moveTo(spineX[0] + s * 0.3f, spineY[0] + s * 0.4f)
                for (seg in 1..segments) {
                    lineTo(spineX[seg] + s * 0.3f, spineY[seg] + s * 0.4f)
                }
            }
            drawPath(shadowPath, ShadowDeep.copy(alpha = 0.15f),
                style = Stroke(width = bodyThick * 2.4f, cap = StrokeCap.Round))

            // ── MAIN BODY (thick stroke along spine) ─────────────────────
            val bodyPath = paths.obtain().apply {
                moveTo(spineX[0], spineY[0])
                for (seg in 1..segments) {
                    lineTo(spineX[seg], spineY[seg])
                }
            }
            drawPath(bodyPath, BandLightBlue,
                style = Stroke(width = bodyThick * 2.0f, cap = StrokeCap.Round))

            // ── BELLY HIGHLIGHT (lighter underside) ──────────────────────
            val bellyPath = paths.obtain().apply {
                moveTo(spineX[0], spineY[0] + bodyThick * 0.3f)
                for (seg in 1..segments) {
                    lineTo(spineX[seg], spineY[seg] + bodyThick * 0.3f)
                }
            }
            drawPath(bellyPath, BellyPale.copy(alpha = 0.35f),
                style = Stroke(width = bodyThick * 0.8f, cap = StrokeCap.Round))

            // ── BLACK BANDING PATTERN ────────────────────────────────────
            for (b in 0 until bandCount) {
                val bFrac = (b.toFloat() + 0.5f) / bandCount
                val seg = (bFrac * segments).toInt().coerceIn(1, segments - 1)
                val bx = spineX[seg]
                val by = spineY[seg]

                // Perpendicular to body direction
                val dx = spineX[(seg + 1).coerceAtMost(segments)] - spineX[(seg - 1).coerceAtLeast(0)]
                val dy = spineY[(seg + 1).coerceAtMost(segments)] - spineY[(seg - 1).coerceAtLeast(0)]
                val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
                val nx = -dy / len
                val ny = dx / len

                val bandHalf = bodyThick * 1.1f
                val bandColor = if (level >= 3) BandBlack else BandDarkBlue
                val bandAlpha = if (level >= 3) 0.80f else 0.65f

                drawLine(
                    bandColor.copy(alpha = bandAlpha),
                    Offset(bx + nx * bandHalf, by + ny * bandHalf),
                    Offset(bx - nx * bandHalf, by - ny * bandHalf),
                    strokeWidth = (bodyLen / bandCount * 0.35f).coerceAtLeast(1.0f * s),
                    cap = StrokeCap.Butt
                )

                // Level 3+: vivid colored band between black bands
                if (level >= 3 && b % 2 == 0) {
                    val midSeg = (bFrac * segments + segments.toFloat() / bandCount * 0.5f).toInt()
                        .coerceIn(0, segments)
                    val mx = spineX[midSeg]
                    val my = spineY[midSeg]
                    drawLine(
                        BandYellow.copy(alpha = 0.30f),
                        Offset(mx + nx * bandHalf * 0.7f, my + ny * bandHalf * 0.7f),
                        Offset(mx - nx * bandHalf * 0.7f, my - ny * bandHalf * 0.7f),
                        strokeWidth = (bodyLen / bandCount * 0.15f).coerceAtLeast(0.5f * s),
                        cap = StrokeCap.Butt
                    )
                }
            }

            // ── PADDLE-SHAPED TAIL ───────────────────────────────────────
            val tailIdx = segments
            val tailX = spineX[tailIdx]
            val tailY = spineY[tailIdx]
            val prevIdx = (segments - 2).coerceAtLeast(0)
            val tailDx = tailX - spineX[prevIdx]
            val tailDy = tailY - spineY[prevIdx]
            val tailLen2 = kotlin.math.sqrt(tailDx * tailDx + tailDy * tailDy).coerceAtLeast(0.001f)
            val tnx = -tailDy / tailLen2
            val tny = tailDx / tailLen2

            val paddleW = bodyThick * 1.8f
            val paddleL = bodyThick * 2.0f
            val paddle = paths.obtain().apply {
                moveTo(tailX, tailY)
                cubicTo(tailX + tnx * paddleW + tailDx / tailLen2 * paddleL * 0.3f,
                    tailY + tny * paddleW + tailDy / tailLen2 * paddleL * 0.3f,
                    tailX + tnx * paddleW * 0.5f + tailDx / tailLen2 * paddleL,
                    tailY + tny * paddleW * 0.5f + tailDy / tailLen2 * paddleL,
                    tailX + tailDx / tailLen2 * paddleL,
                    tailY + tailDy / tailLen2 * paddleL)
                cubicTo(tailX - tnx * paddleW * 0.5f + tailDx / tailLen2 * paddleL,
                    tailY - tny * paddleW * 0.5f + tailDy / tailLen2 * paddleL,
                    tailX - tnx * paddleW + tailDx / tailLen2 * paddleL * 0.3f,
                    tailY - tny * paddleW + tailDy / tailLen2 * paddleL * 0.3f,
                    tailX, tailY)
                close()
            }
            drawPath(paddle, BandCyan.copy(alpha = 0.70f))
            drawPath(paddle, BandBlack.copy(alpha = 0.25f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.2f)))

            // ── HEAD (slightly wider, distinct) ──────────────────────────
            val headX = spineX[0]
            val headY = spineY[0]
            val headLen = bodyThick * 2.5f
            val headW = bodyThick * 1.4f

            val headPath = paths.obtain().apply {
                moveTo(headX, headY - headW)
                cubicTo(headX - headLen * 0.4f, headY - headW * 0.9f,
                    headX - headLen * 0.85f, headY - headW * 0.4f,
                    headX - headLen, headY)
                cubicTo(headX - headLen * 0.85f, headY + headW * 0.4f,
                    headX - headLen * 0.4f, headY + headW * 0.9f,
                    headX, headY + headW)
                close()
            }
            drawPath(headPath, BandDarkBlue)

            // Head highlight
            val headHL = paths.obtain().apply {
                moveTo(headX - headLen * 0.1f, headY - headW * 0.6f)
                cubicTo(headX - headLen * 0.35f, headY - headW * 0.55f,
                    headX - headLen * 0.6f, headY - headW * 0.3f,
                    headX - headLen * 0.75f, headY - headW * 0.05f)
                cubicTo(headX - headLen * 0.55f, headY - headW * 0.25f,
                    headX - headLen * 0.25f, headY - headW * 0.4f,
                    headX - headLen * 0.1f, headY - headW * 0.6f)
                close()
            }
            drawPath(headHL, BandLightBlue.copy(alpha = 0.35f))

            // ── EYE ──────────────────────────────────────────────────────
            val eyeR = (1.0f + level * 0.06f) * s
            val eyeXp = headX - headLen * 0.35f
            val eyeYp = headY - headW * 0.30f

            drawCircle(ShadowDeep.copy(alpha = 0.35f), eyeR * 1.2f, Offset(eyeXp, eyeYp))
            drawCircle(Color(0xFFF0E8D0), eyeR, Offset(eyeXp, eyeYp))
            drawCircle(EyeGold, eyeR * 0.55f, Offset(eyeXp + eyeR * 0.05f, eyeYp))
            // Vertical slit pupil
            val pupil = paths.obtain().apply {
                moveTo(eyeXp + eyeR * 0.05f, eyeYp - eyeR * 0.32f)
                cubicTo(eyeXp + eyeR * 0.12f, eyeYp - eyeR * 0.32f,
                    eyeXp + eyeR * 0.12f, eyeYp + eyeR * 0.32f,
                    eyeXp + eyeR * 0.05f, eyeYp + eyeR * 0.32f)
                cubicTo(eyeXp - eyeR * 0.02f, eyeYp + eyeR * 0.32f,
                    eyeXp - eyeR * 0.02f, eyeYp - eyeR * 0.32f,
                    eyeXp + eyeR * 0.05f, eyeYp - eyeR * 0.32f)
                close()
            }
            drawPath(pupil, ShadowDeep)
            drawCircle(Color.White, eyeR * 0.15f,
                Offset(eyeXp - eyeR * 0.12f, eyeYp - eyeR * 0.15f))

            // ── TONGUE FLICK (subtle, periodic) ──────────────────────────
            val tonguePeriod = 4000L
            val tonguePhase = (animTimeMs % tonguePeriod).toFloat() / 1000f
            if (tonguePhase < 0.4f) {
                val tongueExt = sin(tonguePhase / 0.4f * PI.toFloat()) * headLen * 0.5f
                val tongueY1 = headY - s * 0.15f
                val tongueY2 = headY + s * 0.15f
                drawLine(BandBlack.copy(alpha = 0.6f),
                    Offset(headX - headLen * 0.9f, headY),
                    Offset(headX - headLen - tongueExt, tongueY1),
                    strokeWidth = (0.4f * s).coerceAtLeast(0.2f), cap = StrokeCap.Round)
                drawLine(BandBlack.copy(alpha = 0.6f),
                    Offset(headX - headLen * 0.9f, headY),
                    Offset(headX - headLen - tongueExt, tongueY2),
                    strokeWidth = (0.4f * s).coerceAtLeast(0.2f), cap = StrokeCap.Round)
            }
        }
    }
}
