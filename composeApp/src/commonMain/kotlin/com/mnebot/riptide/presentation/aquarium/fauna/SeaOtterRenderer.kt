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
private val OtterBrown  = Color(0xFF6B3F1C)  // rich dark brown fur
private val OtterBrown2 = Color(0xFF8B5A30)  // lighter brown highlight
private val OtterFace   = Color(0xFFAA8060)  // lighter face
private val OtterBelly  = Color(0xFF9B7545)  // tan belly
private val OtterEye    = Color(0xFF1A1010)
private val OtterWhisk  = Color(0xFFE0D0C0)  // pale whiskers

object SeaOtterRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen = (11f + level * 0.55f) * s
        val bodyH   = (5f  + level * 0.30f) * s
        val tailLen = (4f  + level * 0.25f) * s

        // Sea otters swim belly-up sometimes — simulate with gentle body roll
        val bodyRoll = sin(t * 0.6f * PI.toFloat()) * bodyH * 0.08f
        val pawPaddle= sin(t * 2.4f * PI.toFloat()) * bodyH * 0.35f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL (tapered, slightly flattened) ─────────────────────────────
            val tailX = x + bodyLen * 0.5f
            val tailSway = sin(t * 2.0f * PI.toFloat()) * 1.8f * s
            val tail = Path().apply {
                moveTo(tailX, y - bodyH * 0.18f + bodyRoll)
                cubicTo(tailX + tailLen * 0.5f, y - bodyH * 0.1f + tailSway * 0.5f,
                    tailX + tailLen * 0.85f, y - bodyH * 0.05f + tailSway,
                    tailX + tailLen, y + tailSway)
                cubicTo(tailX + tailLen * 0.85f, y + bodyH * 0.12f + tailSway,
                    tailX + tailLen * 0.5f, y + bodyH * 0.18f + tailSway * 0.5f,
                    tailX, y + bodyH * 0.18f + bodyRoll)
                close()
            }
            drawPath(tail, OtterBrown)

            // ── HIND FEET (large, paddle-like) ────────────────────────────────
            val hindX = x + bodyLen * 0.30f
            val hindY = y + bodyH * 0.70f + bodyRoll * 0.6f
            val footSway = sin(t * 1.4f * PI.toFloat()) * 1.2f * s
            val hindFoot = Path().apply {
                moveTo(hindX, hindY)
                cubicTo(hindX + tailLen * 0.3f + footSway, hindY + bodyH * 0.8f,
                    hindX + tailLen * 0.5f + footSway, hindY + bodyH * 1.0f,
                    hindX + tailLen * 0.4f, hindY + bodyH * 1.1f)
                cubicTo(hindX + tailLen * 0.2f + footSway * 0.5f, hindY + bodyH * 0.9f,
                    hindX, hindY + bodyH * 0.65f,
                    hindX, hindY)
                close()
            }
            drawPath(hindFoot, OtterBrown)

            // ── MAIN BODY (plump, rounded) ────────────────────────────────────
            val body = Path().apply {
                moveTo(x - bodyLen * 0.5f, y + bodyRoll)
                cubicTo(x - bodyLen * 0.4f, y - bodyH + bodyRoll * 0.8f,
                    x + bodyLen * 0.2f, y - bodyH * 0.92f + bodyRoll * 0.4f,
                    x + bodyLen * 0.5f, y - bodyH * 0.28f + bodyRoll * 0.1f)
                lineTo(x + bodyLen * 0.5f, y + bodyH * 0.28f + bodyRoll * 0.1f)
                cubicTo(x + bodyLen * 0.2f, y + bodyH * 0.92f + bodyRoll * 0.4f,
                    x - bodyLen * 0.4f, y + bodyH + bodyRoll * 0.8f,
                    x - bodyLen * 0.5f, y + bodyRoll)
                close()
            }
            drawPath(body, OtterBrown2)

            // ── FUR TEXTURE (short strokes radiating around body) ─────────────
            val furCount = 16
            val furLen   = bodyH * 0.22f
            for (i in 0 until furCount) {
                val fFrac = i.toFloat() / furCount.toFloat()
                // Only around the perimeter, upper half
                val angle = fFrac * 2.0f * PI.toFloat() - PI.toFloat() * 0.7f
                val bx = x + kotlin.math.cos(angle) * bodyLen * 0.42f
                val by = y + kotlin.math.sin(angle) * bodyH * 0.90f + bodyRoll * 0.5f
                if (by < y + bodyRoll + bodyH * 0.2f) {  // only upper portions
                    val fx = bx + kotlin.math.cos(angle) * furLen
                    val fy = by + kotlin.math.sin(angle) * furLen
                    drawLine(OtterBrown.copy(alpha = 0.45f),
                        Offset(bx, by), Offset(fx, fy),
                        strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
                }
            }

            // Belly (lighter underside)
            val belly = Path().apply {
                moveTo(x - bodyLen * 0.38f, y + bodyH * 0.15f + bodyRoll)
                cubicTo(x - bodyLen * 0.2f, y + bodyH * 0.90f + bodyRoll * 0.6f,
                    x + bodyLen * 0.15f, y + bodyH * 0.90f + bodyRoll * 0.4f,
                    x + bodyLen * 0.40f, y + bodyH * 0.25f + bodyRoll * 0.1f)
                cubicTo(x + bodyLen * 0.12f, y + bodyH * 0.65f + bodyRoll * 0.4f,
                    x - bodyLen * 0.08f, y + bodyH * 0.68f + bodyRoll * 0.6f,
                    x - bodyLen * 0.38f, y + bodyH * 0.15f + bodyRoll)
                close()
            }
            drawPath(belly, OtterBelly.copy(alpha = 0.50f))

            // ── FRONT PAWS (used for tool use, gesturing while floating) ───────
            val pawX = x - bodyLen * 0.25f
            val pawY = y + bodyH * 0.25f + bodyRoll * 0.5f
            // Left paw
            drawCircle(OtterFace, (2.0f * s).coerceAtLeast(1.0f),
                Offset(pawX + pawPaddle * 0.3f, pawY + bodyH * 0.5f + pawPaddle * 0.5f))
            // Right paw
            drawCircle(OtterFace, (2.0f * s).coerceAtLeast(1.0f),
                Offset(pawX + bodyLen * 0.1f + pawPaddle * 0.3f, pawY + bodyH * 0.6f - pawPaddle * 0.3f))

            // Level 3+: holding a rock/tool
            if (level >= 3) {
                drawCircle(Color(0xFF888880).copy(alpha = 0.75f), (1.5f * s).coerceAtLeast(0.8f),
                    Offset(pawX + bodyLen * 0.05f + pawPaddle * 0.15f, pawY + bodyH * 0.5f + pawPaddle * 0.1f))
            }

            // ── HEAD (round, cute) ─────────────────────────────────────────────
            val headR = (4.0f + level * 0.22f) * s
            val headX = x - bodyLen * 0.52f
            drawCircle(OtterBrown2, headR, Offset(headX, y + bodyRoll))

            // Face (lighter muzzle)
            drawCircle(OtterFace.copy(alpha = 0.70f), headR * 0.65f,
                Offset(headX - headR * 0.2f, y + bodyRoll + headR * 0.12f))

            // Round ears (small)
            for (side in listOf(-1f, 1f)) {
                drawCircle(OtterBrown.copy(alpha = 0.85f), headR * 0.22f,
                    Offset(headX + side * headR * 0.55f, y + bodyRoll - headR * 0.80f))
            }

            // Nose (dark circle)
            drawCircle(OtterEye, headR * 0.20f, Offset(headX - headR * 0.55f, y + bodyRoll + headR * 0.1f))

            // ── WHISKERS ──────────────────────────────────────────────────────
            val whiskLen = (2.5f + level * 0.15f) * s
            val whiskY   = y + bodyRoll + headR * 0.15f
            val whiskX   = headX - headR * 0.40f
            for (w in -1..1) {
                val wf = w.toFloat()
                drawLine(OtterWhisk.copy(alpha = 0.80f),
                    Offset(whiskX, whiskY + wf * headR * 0.18f),
                    Offset(whiskX - whiskLen, whiskY + wf * headR * 0.22f + wf * whiskLen * 0.12f),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round)
                drawLine(OtterWhisk.copy(alpha = 0.80f),
                    Offset(whiskX + headR * 0.25f, whiskY + wf * headR * 0.18f),
                    Offset(whiskX + headR * 0.25f + whiskLen, whiskY + wf * headR * 0.22f + wf * whiskLen * 0.12f),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round)
            }

            // ── EYES ───────────────────────────────────────────────────────────
            val eyeR = (1.2f + level * 0.07f) * s
            for (side in listOf(-0.35f, 0.20f)) {
                val eyeX = headX + side * headR
                val eyeY = y + bodyRoll - headR * 0.28f
                drawCircle(OtterFace, eyeR * 1.1f, Offset(eyeX, eyeY))
                drawCircle(OtterEye, eyeR * 0.70f, Offset(eyeX, eyeY))
                drawCircle(Color.White, eyeR * 0.22f, Offset(eyeX - eyeR * 0.2f, eyeY - eyeR * 0.2f))
            }
        }
    }
}
