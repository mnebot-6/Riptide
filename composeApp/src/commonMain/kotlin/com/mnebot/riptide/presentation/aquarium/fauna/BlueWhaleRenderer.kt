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
private val WhaleBlue  = Color(0xFF3A4E62)  // blue-grey body
private val WhaleBlue2 = Color(0xFF4E6278)  // lighter back
private val WhaleBelly = Color(0xFF8AAABB)  // pale yellowish-white belly
private val WhaleFin   = Color(0xFF2A3A4A)  // darker fin
private val WhaleEye   = Color(0xFF111122)

object BlueWhaleRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        // Massive body — the largest animal on Earth
        val bodyLen  = (22f + level * 1.2f) * s
        val bodyH    = (8f  + level * 0.5f) * s
        val tailExt  = (9f  + level * 0.6f) * s

        // Very slow, gentle tail movement
        val tailSway = sin(t * 0.9f * PI.toFloat()) * 2.0f * s
        val bodyArc  = sin(t * 0.9f * PI.toFloat()) * bodyH * 0.08f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FLUKES (massive horizontal) ──────────────────────────────
            val tailX = x + bodyLen * 0.47f
            val flukeSpan = tailExt * 0.85f
            val upperFluke = Path().apply {
                moveTo(tailX, y + bodyArc * 0.3f)
                cubicTo(tailX + flukeSpan * 0.4f, y - bodyH * 0.3f + tailSway,
                    tailX + flukeSpan * 0.8f, y - bodyH * 1.1f + tailSway,
                    tailX + flukeSpan, y - bodyH * 1.3f + tailSway)
                cubicTo(tailX + flukeSpan * 0.8f, y - bodyH * 0.8f + tailSway,
                    tailX + flukeSpan * 0.45f, y - bodyH * 0.2f + tailSway * 0.6f,
                    tailX, y + bodyArc * 0.3f)
                close()
            }
            drawPath(upperFluke, WhaleFin)

            val lowerFluke = Path().apply {
                moveTo(tailX, y + bodyArc * 0.3f)
                cubicTo(tailX + flukeSpan * 0.45f, y + bodyH * 0.2f + tailSway * 0.6f,
                    tailX + flukeSpan * 0.8f, y + bodyH * 0.8f + tailSway,
                    tailX + flukeSpan, y + bodyH * 1.3f + tailSway)
                cubicTo(tailX + flukeSpan * 0.8f, y + bodyH * 1.1f + tailSway,
                    tailX + flukeSpan * 0.4f, y + bodyH * 0.3f + tailSway,
                    tailX, y + bodyArc * 0.3f)
                close()
            }
            drawPath(lowerFluke, WhaleFin)

            // ── TINY DORSAL FIN (positioned 2/3 back, characteristic of blue whale) ─
            val dorsalX = x + bodyLen * 0.30f
            val dorsal  = Path().apply {
                moveTo(dorsalX - bodyLen * 0.04f, y - bodyH * 0.85f + bodyArc * 0.1f)
                cubicTo(dorsalX - bodyLen * 0.01f, y - bodyH * 1.30f + bodyArc * 0.1f,
                    dorsalX + bodyLen * 0.03f, y - bodyH * 1.25f + bodyArc * 0.1f,
                    dorsalX + bodyLen * 0.05f, y - bodyH * 0.85f + bodyArc * 0.1f)
                close()
            }
            drawPath(dorsal, WhaleFin)

            // ── PECTORAL FLIPPERS ─────────────────────────────────────────────
            val pect = Path().apply {
                moveTo(x - bodyLen * 0.15f, y + bodyH * 0.72f + bodyArc * 0.5f)
                cubicTo(x - bodyLen * 0.05f, y + bodyH * 1.55f + bodyArc * 0.35f,
                    x + bodyLen * 0.12f, y + bodyH * 1.62f + bodyArc * 0.25f,
                    x + bodyLen * 0.22f, y + bodyH * 0.95f + bodyArc * 0.35f)
                cubicTo(x + bodyLen * 0.12f, y + bodyH * 0.82f + bodyArc * 0.4f,
                    x + bodyLen * 0.02f, y + bodyH * 0.76f + bodyArc * 0.45f,
                    x - bodyLen * 0.15f, y + bodyH * 0.72f + bodyArc * 0.5f)
                close()
            }
            drawPath(pect, WhaleBlue)

            // ── MAIN BODY (enormous) ───────────────────────────────────────────
            val body = Path().apply {
                moveTo(x - bodyLen * 0.5f, y + bodyArc)  // wide rostrum
                cubicTo(x - bodyLen * 0.45f, y - bodyH * 0.78f + bodyArc * 0.85f,
                    x - bodyLen * 0.05f, y - bodyH + bodyArc * 0.5f,
                    x + bodyLen * 0.2f, y - bodyH * 0.92f + bodyArc * 0.2f)
                cubicTo(x + bodyLen * 0.35f, y - bodyH * 0.82f + bodyArc * 0.15f,
                    x + bodyLen * 0.45f, y - bodyH * 0.42f + bodyArc * 0.1f,
                    x + bodyLen * 0.47f, y - bodyH * 0.12f + bodyArc * 0.05f)
                lineTo(x + bodyLen * 0.47f, y + bodyH * 0.12f + bodyArc * 0.05f)
                cubicTo(x + bodyLen * 0.45f, y + bodyH * 0.42f + bodyArc * 0.1f,
                    x + bodyLen * 0.35f, y + bodyH * 0.82f + bodyArc * 0.15f,
                    x + bodyLen * 0.2f, y + bodyH * 0.92f + bodyArc * 0.2f)
                cubicTo(x - bodyLen * 0.05f, y + bodyH + bodyArc * 0.5f,
                    x - bodyLen * 0.45f, y + bodyH * 0.78f + bodyArc * 0.85f,
                    x - bodyLen * 0.5f, y + bodyArc)
                close()
            }
            drawPath(body, WhaleBlue2)

            // ── BELLY (pale, from throat to tail) ─────────────────────────────
            val belly = Path().apply {
                moveTo(x - bodyLen * 0.48f, y + bodyArc + bodyH * 0.05f)
                cubicTo(x - bodyLen * 0.35f, y + bodyH * 0.88f + bodyArc * 0.7f,
                    x + bodyLen * 0.2f, y + bodyH * 0.90f + bodyArc * 0.2f,
                    x + bodyLen * 0.44f, y + bodyH * 0.10f + bodyArc * 0.05f)
                cubicTo(x + bodyLen * 0.2f, y + bodyH * 0.60f + bodyArc * 0.2f,
                    x - bodyLen * 0.10f, y + bodyH * 0.65f + bodyArc * 0.5f,
                    x - bodyLen * 0.48f, y + bodyArc + bodyH * 0.05f)
                close()
            }
            drawPath(belly, WhaleBelly.copy(alpha = 0.55f))

            // ── VENTRAL PLEATS (throat grooves, characteristic of rorquals) ────
            val pleatCount = 6 + level / 2
            for (p in 0 until pleatCount) {
                val pf = (p.toFloat() + 0.5f) / pleatCount.toFloat()
                val px = x - bodyLen * 0.42f + bodyLen * 0.5f * pf
                drawLine(
                    WhaleBlue.copy(alpha = 0.30f),
                    Offset(px, y + bodyH * 0.12f + bodyArc * (1f - pf * 0.5f)),
                    Offset(px + bodyLen * 0.08f, y + bodyH * 0.85f + bodyArc * (1f - pf * 0.3f)),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.3f)
                )
            }

            // ── BLOW HOLE (tiny slit on top near head) ────────────────────────
            val blowX = x - bodyLen * 0.38f
            val blowY = y - bodyH * 0.82f + bodyArc * 0.85f
            drawLine(WhaleBlue.copy(alpha = 0.65f),
                Offset(blowX - bodyLen * 0.02f, blowY),
                Offset(blowX + bodyLen * 0.01f, blowY),
                strokeWidth = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round)

            // ── EYE (tiny, set back from mouth) ──────────────────────────────
            val eyeR = (1.2f + level * 0.07f) * s
            val eyeX = x - bodyLen * 0.38f
            val eyeY = y - bodyH * 0.55f + bodyArc * 0.85f
            drawCircle(WhaleBelly.copy(alpha = 0.70f), eyeR * 1.1f, Offset(eyeX, eyeY))
            drawCircle(WhaleEye, eyeR * 0.65f, Offset(eyeX, eyeY))
        }
    }
}
