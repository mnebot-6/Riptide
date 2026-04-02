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
private val WSharkBody  = Color(0xFF2A3A4A)  // dark blue-grey body
private val WSharkBody2 = Color(0xFF3A4E60)  // slightly lighter back
private val WSharkBelly = Color(0xFFCCD8E0)  // pale belly
private val WSharkSpot  = Color(0xFFEEF2F5)  // white spots / checkerboard
private val WSharkFin   = Color(0xFF1E2E3A)  // darker fin
private val WSharkEye   = Color(0xFF111122)
private val WSharkGill  = Color(0xFF1A2A38)  // gill slits

object WhaleSharkRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val t = animTimeMs / 1000f

        // Whale shark: very elongated, wide flat head
        val bodyLen  = (20f + level * 1.2f) * s
        val bodyH    = (7f  + level * 0.4f) * s
        val tailExt  = (8f  + level * 0.6f) * s

        val tailSway = sin(t * 1.2f * PI.toFloat()) * 2.5f * s  // slow, powerful tail

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL — large asymmetric (upper lobe longer) ────────────────────
            val tailX = x + bodyLen * 0.48f
            val upperLobe = paths.obtain().apply {
                moveTo(tailX, y - bodyH * 0.15f)
                cubicTo(tailX + tailExt * 0.4f, y - bodyH * 0.6f + tailSway,
                    tailX + tailExt * 0.8f, y - bodyH * 1.4f + tailSway,
                    tailX + tailExt, y - bodyH * 1.6f + tailSway)
                cubicTo(tailX + tailExt * 0.8f, y - bodyH * 1.0f + tailSway,
                    tailX + tailExt * 0.5f, y - bodyH * 0.5f + tailSway * 0.5f,
                    tailX, y)
                close()
            }
            drawPath(upperLobe, WSharkFin)

            val lowerLobe = paths.obtain().apply {
                moveTo(tailX, y)
                cubicTo(tailX + tailExt * 0.4f, y + bodyH * 0.3f + tailSway * 0.8f,
                    tailX + tailExt * 0.7f, y + bodyH * 0.8f + tailSway,
                    tailX + tailExt * 0.85f, y + bodyH + tailSway)
                cubicTo(tailX + tailExt * 0.7f, y + bodyH * 0.6f + tailSway * 0.8f,
                    tailX + tailExt * 0.4f, y + tailSway * 0.5f,
                    tailX, y + bodyH * 0.12f)
                close()
            }
            drawPath(lowerLobe, WSharkFin)

            // ── FIRST DORSAL FIN ──────────────────────────────────────────────
            val dorsalSway = sin(t * 1.2f * PI.toFloat()) * 0.8f * s
            val dorsal = paths.obtain().apply {
                moveTo(x + bodyLen * 0.05f, y - bodyH * 0.92f)
                cubicTo(x + bodyLen * 0.12f + dorsalSway, y - bodyH * 1.80f,
                    x + bodyLen * 0.22f + dorsalSway, y - bodyH * 1.75f,
                    x + bodyLen * 0.30f, y - bodyH * 0.92f)
                cubicTo(x + bodyLen * 0.22f, y - bodyH * 0.88f,
                    x + bodyLen * 0.12f, y - bodyH * 0.89f,
                    x + bodyLen * 0.05f, y - bodyH * 0.92f)
                close()
            }
            drawPath(dorsal, WSharkFin)

            // Second (smaller) dorsal fin
            val dorsal2 = paths.obtain().apply {
                moveTo(x + bodyLen * 0.35f, y - bodyH * 0.88f)
                cubicTo(x + bodyLen * 0.38f + dorsalSway * 0.5f, y - bodyH * 1.25f,
                    x + bodyLen * 0.42f + dorsalSway * 0.5f, y - bodyH * 1.22f,
                    x + bodyLen * 0.45f, y - bodyH * 0.88f)
                close()
            }
            drawPath(dorsal2, WSharkFin)

            // ── PECTORAL FINS (large, wide) ────────────────────────────────────
            val pect = paths.obtain().apply {
                moveTo(x - bodyLen * 0.1f, y + bodyH * 0.70f)
                cubicTo(x - bodyLen * 0.0f, y + bodyH * 1.60f,
                    x + bodyLen * 0.18f, y + bodyH * 1.65f,
                    x + bodyLen * 0.25f, y + bodyH * 0.90f)
                cubicTo(x + bodyLen * 0.18f, y + bodyH * 0.80f,
                    x + bodyLen * 0.05f, y + bodyH * 0.75f,
                    x - bodyLen * 0.1f, y + bodyH * 0.70f)
                close()
            }
            drawPath(pect, WSharkBody)

            // ── MAIN BODY — very elongated ─────────────────────────────────────
            val body = paths.obtain().apply {
                moveTo(x - bodyLen, y)  // snout (wide, flat)
                cubicTo(x - bodyLen * 0.85f, y - bodyH * 0.85f,
                    x - bodyLen * 0.1f, y - bodyH,
                    x + bodyLen * 0.48f, y - bodyH * 0.30f)
                lineTo(x + bodyLen * 0.48f, y + bodyH * 0.30f)
                cubicTo(x - bodyLen * 0.1f, y + bodyH,
                    x - bodyLen * 0.85f, y + bodyH * 0.85f,
                    x - bodyLen, y)
                close()
            }
            drawPath(body, WSharkBody2)

            // Belly (pale underside)
            val belly = paths.obtain().apply {
                moveTo(x - bodyLen * 0.6f, y + bodyH * 0.18f)
                cubicTo(x - bodyLen * 0.3f, y + bodyH * 0.88f,
                    x + bodyLen * 0.2f, y + bodyH * 0.90f,
                    x + bodyLen * 0.45f, y + bodyH * 0.25f)
                cubicTo(x + bodyLen * 0.2f, y + bodyH * 0.65f,
                    x - bodyLen * 0.1f, y + bodyH * 0.68f,
                    x - bodyLen * 0.6f, y + bodyH * 0.18f)
                close()
            }
            drawPath(belly, WSharkBelly.copy(alpha = 0.50f))

            // ── WHITE SPOTS (checkerboard pattern) ─────────────────────────────
            val spotPositions = listOf(
                // Row 1 (along upper back)
                Pair(-0.40f, -0.55f), Pair(-0.10f, -0.62f), Pair(0.18f, -0.58f),
                // Row 2
                Pair(-0.55f, -0.25f), Pair(-0.25f, -0.35f), Pair(0.02f, -0.32f), Pair(0.28f, -0.28f),
                // Row 3
                Pair(-0.50f, 0.05f),  Pair(-0.22f, -0.05f), Pair(0.05f, 0.00f),  Pair(0.30f, 0.02f),
                // Row 4
                Pair(-0.40f, 0.32f),  Pair(-0.12f, 0.30f),  Pair(0.14f, 0.30f),
            )
            val spotR = (1.4f + level * 0.1f) * s
            for ((sx, sy) in spotPositions) {
                drawCircle(WSharkSpot.copy(alpha = 0.70f), spotR,
                    Offset(x + sx * bodyLen, y + sy * bodyH))
            }

            // ── GILL SLITS (5 on each side) ────────────────────────────────────
            val gillCount = 5
            for (g in 0 until gillCount) {
                val gx = x - bodyLen * (0.60f - g * 0.06f)
                val gLen = bodyH * 0.40f
                drawLine(
                    WSharkGill.copy(alpha = 0.55f),
                    Offset(gx, y - gLen * 0.8f),
                    Offset(gx + bodyH * 0.08f, y + gLen * 0.9f),
                    strokeWidth = (0.9f * s).coerceAtLeast(0.4f),
                    cap = StrokeCap.Round
                )
            }

            // ── LEVEL 3+: Remora companion fish ──────────────────────────────────
            if (level >= 3) {
                val remX = x - bodyLen * 0.05f
                val remY = y - bodyH * 1.08f
                val rLen = bodyLen * 0.13f
                val rH   = bodyH * 0.17f
                val remora = paths.obtain().apply {
                    moveTo(remX - rLen * 0.5f, remY)
                    cubicTo(remX - rLen * 0.3f, remY - rH, remX + rLen * 0.3f, remY - rH, remX + rLen * 0.5f, remY)
                    cubicTo(remX + rLen * 0.3f, remY + rH, remX - rLen * 0.3f, remY + rH, remX - rLen * 0.5f, remY)
                    close()
                }
                drawPath(remora, WSharkGill.copy(alpha = 0.72f))
                // Suction disc stripes on head
                for (d in 0 until 4) {
                    drawLine(WSharkSpot.copy(alpha = 0.40f),
                        Offset(remX - rLen * 0.48f + d * rLen * 0.16f, remY - rH * 0.62f),
                        Offset(remX - rLen * 0.48f + d * rLen * 0.16f, remY + rH * 0.62f),
                        strokeWidth = (0.4f * s).coerceAtLeast(0.2f)
                    )
                }
                // Tail
                val remTail = paths.obtain().apply {
                    moveTo(remX + rLen * 0.5f, remY)
                    lineTo(remX + rLen * 0.75f, remY - rH)
                    lineTo(remX + rLen * 0.75f, remY + rH)
                    close()
                }
                drawPath(remTail, WSharkGill.copy(alpha = 0.55f))
            }

            // ── LEVEL 5+: Bioluminescent photophores along flank ─────────────────
            if (level >= 5) {
                val glowPulse = (sin(t * 1.8f * PI.toFloat()) * 0.5f + 0.5f) * 0.35f + 0.15f
                for (p in 0 until 7) {
                    val px = x - bodyLen * 0.45f + p * bodyLen * 0.15f
                    drawCircle(Color(0xFF88DDFF).copy(alpha = glowPulse),
                        (1.3f * s).coerceAtLeast(0.5f), Offset(px, y - bodyH * 0.10f))
                }
            }

            // Wide flat head / terminal mouth
            val mouth = paths.obtain().apply {
                moveTo(x - bodyLen, y - bodyH * 0.15f)
                cubicTo(x - bodyLen * 0.98f, y - bodyH * 0.35f,
                    x - bodyLen * 0.88f, y - bodyH * 0.45f,
                    x - bodyLen * 0.80f, y - bodyH * 0.40f)
                cubicTo(x - bodyLen * 0.88f, y - bodyH * 0.20f,
                    x - bodyLen * 0.95f, y - bodyH * 0.10f,
                    x - bodyLen, y - bodyH * 0.15f)
                close()
            }
            drawPath(mouth, WSharkBelly.copy(alpha = 0.55f))

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.4f + level * 0.08f) * s
            val eyeX = x - bodyLen * 0.75f
            val eyeY = y - bodyH * 0.48f
            drawCircle(WSharkBelly, eyeR * 1.1f, Offset(eyeX, eyeY))
            drawCircle(WSharkEye, eyeR * 0.65f, Offset(eyeX, eyeY))
            drawCircle(Color.White, eyeR * 0.22f, Offset(eyeX - eyeR * 0.2f, eyeY - eyeR * 0.2f))
        }
    }
}
