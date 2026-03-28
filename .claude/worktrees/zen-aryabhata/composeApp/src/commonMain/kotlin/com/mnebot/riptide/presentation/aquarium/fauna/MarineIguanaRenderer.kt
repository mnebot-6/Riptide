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
private val IgBody   = Color(0xFF202020)  // very dark grey-black body
private val IgBody2  = Color(0xFF383838)  // slightly lighter body highlight
private val IgBelly  = Color(0xFF4A4A40)  // lighter underbelly
private val IgSpine  = Color(0xFF2A2A2A)  // dorsal spines (darker)
private val IgRed    = Color(0xFF8B1A1A)  // red dewlap / crest hint (male coloring)
private val IgEye    = Color(0xFF111111)
private val IgEyeRim = Color(0xFF8B8B00)  // yellow eye ring

object MarineIguanaRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        // Marine iguana swims with full lateral body undulation (CRAWL in surface zone)
        val bodyLen  = (16f + level * 0.8f) * s
        val bodyH    = (4f  + level * 0.25f) * s
        val tailLen  = (10f + level * 0.6f) * s

        // Body undulation: 3 waves along body length
        val wave1 = sin(t * 1.8f * PI.toFloat()) * bodyH * 0.5f
        val wave2 = sin(t * 1.8f * PI.toFloat() + 1.2f) * bodyH * 0.4f
        val wave3 = sin(t * 1.8f * PI.toFloat() + 2.4f) * bodyH * 0.6f
        val tailWave = sin(t * 1.8f * PI.toFloat() + 3.6f) * bodyH * 0.9f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            val headX = x - bodyLen * 0.5f
            val body1X = headX + bodyLen * 0.25f
            val body2X = headX + bodyLen * 0.55f
            val tailBaseX = headX + bodyLen

            // ── TAIL (long, laterally flattened) ─────────────────────────────
            val tail = Path().apply {
                moveTo(tailBaseX, y - bodyH * 0.5f + wave3)
                cubicTo(
                    tailBaseX + tailLen * 0.3f, y - bodyH * 0.2f + tailWave * 0.5f,
                    tailBaseX + tailLen * 0.65f, y + tailWave * 0.85f,
                    tailBaseX + tailLen, y + tailWave
                )
                cubicTo(
                    tailBaseX + tailLen * 0.65f, y + tailWave * 0.9f,
                    tailBaseX + tailLen * 0.3f, y + bodyH * 0.15f + tailWave * 0.5f,
                    tailBaseX, y + bodyH * 0.5f + wave3
                )
                close()
            }
            drawPath(tail, IgBody)

            // ── DORSAL CREST ALONG TAIL ────────────────────────────────────────
            val crestCount = 12 + level
            for (i in 0 until crestCount) {
                val cf = i.toFloat() / crestCount.toFloat()
                val cx2 = tailBaseX + tailLen * cf
                val cw = sin(t * 1.8f * PI.toFloat() + 3.0f + cf * 2f) * bodyH * 0.5f + wave3 * (1f - cf) + tailWave * cf
                val cHeight = (bodyH * (0.8f - cf * 0.55f) * (1f + level * 0.05f)).coerceAtLeast(s * 0.5f)
                drawLine(
                    IgSpine.copy(alpha = 0.70f),
                    Offset(cx2, cw - cHeight * 0.5f),
                    Offset(cx2, cw - cHeight - bodyH * 0.4f),
                    strokeWidth = (1.0f * s * (1f - cf * 0.7f)).coerceAtLeast(0.3f),
                    cap = StrokeCap.Round
                )
            }

            // ── TUCKED HIND LEGS ─────────────────────────────────────────────
            val legAnim = sin(t * 1.8f * PI.toFloat()) * 0.8f * s
            drawLine(IgBody2,
                Offset(body2X, y + bodyH * 0.7f + wave2),
                Offset(body2X + bodyH * 0.5f, y + bodyH * 1.8f + wave2 + legAnim),
                strokeWidth = (2.0f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round)
            drawLine(IgBody2,
                Offset(body2X + bodyH * 0.5f, y + bodyH * 1.8f + wave2 + legAnim),
                Offset(body2X + bodyH * 0.3f, y + bodyH * 2.8f + wave2 + legAnim),
                strokeWidth = (1.5f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round)

            // ── FRONT LEGS (tucked close during swimming) ─────────────────────
            drawLine(IgBody2,
                Offset(body1X + bodyH * 0.2f, y + bodyH * 0.7f + wave1),
                Offset(body1X - bodyH * 0.3f, y + bodyH * 1.7f + wave1 - legAnim),
                strokeWidth = (2.0f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round)
            drawLine(IgBody2,
                Offset(body1X - bodyH * 0.3f, y + bodyH * 1.7f + wave1 - legAnim),
                Offset(body1X - bodyH * 0.1f, y + bodyH * 2.7f + wave1 - legAnim),
                strokeWidth = (1.5f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round)

            // ── BODY (3 sections, sinuating) ──────────────────────────────────
            // Section 1: head-shoulder
            val body1 = Path().apply {
                moveTo(headX + bodyH * 0.5f, y + wave1 * 0.2f - bodyH)
                cubicTo(body1X * 0.6f + headX * 0.4f, y + wave1 * 0.4f - bodyH * 0.9f,
                    body1X * 0.8f + headX * 0.2f, y + wave1 * 0.7f - bodyH * 0.85f,
                    body1X, y + wave1 - bodyH * 0.8f)
                lineTo(body1X, y + wave1 + bodyH * 0.8f)
                cubicTo(body1X * 0.8f + headX * 0.2f, y + wave1 * 0.7f + bodyH * 0.85f,
                    body1X * 0.6f + headX * 0.4f, y + wave1 * 0.4f + bodyH * 0.9f,
                    headX + bodyH * 0.5f, y + wave1 * 0.2f + bodyH)
                close()
            }
            drawPath(body1, IgBody)

            // Section 2: mid-body
            val body2 = Path().apply {
                moveTo(body1X, y + wave1 - bodyH * 0.8f)
                cubicTo(body1X * 0.5f + body2X * 0.5f, y + wave2 * 0.5f - bodyH * 0.9f,
                    body1X * 0.2f + body2X * 0.8f, y + wave2 * 0.8f - bodyH * 0.85f,
                    body2X, y + wave2 - bodyH * 0.75f)
                lineTo(body2X, y + wave2 + bodyH * 0.75f)
                cubicTo(body1X * 0.2f + body2X * 0.8f, y + wave2 * 0.8f + bodyH * 0.85f,
                    body1X * 0.5f + body2X * 0.5f, y + wave2 * 0.5f + bodyH * 0.9f,
                    body1X, y + wave1 + bodyH * 0.8f)
                close()
            }
            drawPath(body2, IgBody)

            // Section 3: tail-base
            val body3 = Path().apply {
                moveTo(body2X, y + wave2 - bodyH * 0.75f)
                cubicTo(body2X * 0.5f + tailBaseX * 0.5f, y + wave3 * 0.5f - bodyH * 0.65f,
                    body2X * 0.2f + tailBaseX * 0.8f, y + wave3 * 0.8f - bodyH * 0.55f,
                    tailBaseX, y + wave3 - bodyH * 0.5f)
                lineTo(tailBaseX, y + wave3 + bodyH * 0.5f)
                cubicTo(body2X * 0.2f + tailBaseX * 0.8f, y + wave3 * 0.8f + bodyH * 0.55f,
                    body2X * 0.5f + tailBaseX * 0.5f, y + wave3 * 0.5f + bodyH * 0.65f,
                    body2X, y + wave2 + bodyH * 0.75f)
                close()
            }
            drawPath(body3, IgBody)

            // Belly stripe
            drawLine(IgBelly.copy(alpha = 0.40f),
                Offset(headX + bodyH * 0.5f, y + bodyH * 0.9f + wave1 * 0.15f),
                Offset(tailBaseX, y + bodyH * 0.85f + wave3 * 0.15f),
                strokeWidth = (bodyH * 0.6f).coerceAtLeast(1.0f), cap = StrokeCap.Round)

            // ── DORSAL SPINES (along back) ─────────────────────────────────────
            val spineCount = 8 + level
            for (i in 0 until spineCount) {
                val sf = i.toFloat() / (spineCount - 1f)
                val spineX = headX + bodyLen * sf * 0.85f
                val spineWave = wave1 * (1f - sf) + wave2 * sf
                val spineH = (bodyH * (0.9f + level * 0.04f) * sin((sf * PI).toFloat())).coerceAtLeast(s * 0.5f)
                drawLine(
                    IgSpine,
                    Offset(spineX, spineWave - bodyH * 0.82f),
                    Offset(spineX, spineWave - bodyH * 0.82f - spineH),
                    strokeWidth = (0.9f * s).coerceAtLeast(0.4f),
                    cap = StrokeCap.Round
                )
            }

            // ── HEAD ──────────────────────────────────────────────────────────
            val headW = (4f + level * 0.25f) * s
            val head  = Path().apply {
                moveTo(headX - headW * 1.2f, y + wave1 * 0.1f)
                cubicTo(headX - headW, y + wave1 * 0.1f - bodyH * 0.9f,
                    headX + headW * 0.2f, y + wave1 * 0.05f - bodyH * 0.92f,
                    headX + headW * 0.5f, y + wave1 * 0.05f - bodyH * 0.65f)
                lineTo(headX + headW * 0.5f, y + wave1 * 0.05f + bodyH * 0.65f)
                cubicTo(headX + headW * 0.2f, y + wave1 * 0.05f + bodyH * 0.92f,
                    headX - headW, y + wave1 * 0.1f + bodyH * 0.9f,
                    headX - headW * 1.2f, y + wave1 * 0.1f)
                close()
            }
            drawPath(head, IgBody2)

            // Level 3+: red dewlap / crest accent
            if (level >= 3) {
                drawLine(IgRed.copy(alpha = 0.55f),
                    Offset(headX - headW, y + wave1 * 0.1f + bodyH * 0.3f),
                    Offset(headX - headW * 1.1f, y + wave1 * 0.1f + bodyH * 0.9f),
                    strokeWidth = (1.5f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round)
            }

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.0f + level * 0.07f) * s
            val eyeX = headX - headW * 0.55f
            val eyeY = y + wave1 * 0.08f - bodyH * 0.40f
            drawCircle(IgEyeRim, eyeR * 1.3f, Offset(eyeX, eyeY))
            drawCircle(IgEye, eyeR * 0.68f, Offset(eyeX, eyeY))
        }
    }
}
