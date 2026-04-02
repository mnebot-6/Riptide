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
private val SurgBody    = Color(0xFF1565C0)  // cobalt blue
private val SurgBlue2   = Color(0xFF1976D2)  // lighter body
private val SurgTail    = Color(0xFFFFD600)  // yellow tail
private val SurgMask    = Color(0xFF111111)  // black mask
private val SurgBelly   = Color(0xFFBBCCEE)  // pale belly
private val SurgScalpel = Color(0xFFDDEEFF)  // white scalpel spine
private val SurgFin     = Color(0xFF1251A0)  // darker fin
private val SurgElectric = Color(0xFF40C8FF) // electric blue lateral line

object SurgeonfishRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyLen  = (11f + level * 0.8f) * s
        val bodyH    = (7f  + level * 0.5f) * s
        val tailExt  = (5f  + level * 0.4f) * s   // level 5+ gets longer tail
        val finTailExtra = if (level >= 5) 1.5f * s else 0f

        val tailSway = sin(t * 3.5f * PI.toFloat()) * 2f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ════════════════════════════════════════════════════════════════
            // TAIL FIN — yellow crescent (drawn first, behind body)
            // ════════════════════════════════════════════════════════════════
            val tailX = x + bodyLen * 0.45f
            val crescentOpen = (tailExt + finTailExtra) * 0.7f

            val tailTop = paths.obtain().apply {
                moveTo(tailX, y - bodyH * 0.3f)
                cubicTo(
                    tailX + tailExt * 0.3f, y - bodyH * 0.3f + tailSway * 0.3f,
                    tailX + tailExt * 0.6f + finTailExtra * 0.3f, y - crescentOpen + tailSway,
                    tailX + tailExt + finTailExtra, y - crescentOpen * 1.2f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.8f + finTailExtra * 0.5f, y - bodyH * 0.1f + tailSway * 0.5f,
                    tailX + tailExt * 0.5f, y + tailSway * 0.2f,
                    tailX, y
                )
                close()
            }
            drawPath(tailTop, SurgTail)

            val tailBot = paths.obtain().apply {
                moveTo(tailX, y)
                cubicTo(
                    tailX + tailExt * 0.5f, y + tailSway * 0.2f,
                    tailX + tailExt * 0.8f + finTailExtra * 0.5f, y + bodyH * 0.1f + tailSway * 0.5f,
                    tailX + tailExt + finTailExtra, y + crescentOpen * 1.2f + tailSway
                )
                cubicTo(
                    tailX + tailExt * 0.6f + finTailExtra * 0.3f, y + crescentOpen + tailSway,
                    tailX + tailExt * 0.3f, y + bodyH * 0.3f + tailSway * 0.3f,
                    tailX, y + bodyH * 0.3f
                )
                close()
            }
            drawPath(tailBot, SurgTail)

            // ════════════════════════════════════════════════════════════════
            // DORSAL FIN — along top of body
            // ════════════════════════════════════════════════════════════════
            val dorsalSway = sin(t * 2.8f * PI.toFloat()) * 0.5f * s
            val dorsal = paths.obtain().apply {
                moveTo(x - bodyLen * 0.3f, y - bodyH * 0.85f)
                cubicTo(
                    x - bodyLen * 0.05f + dorsalSway, y - bodyH * 1.5f,
                    x + bodyLen * 0.2f + dorsalSway, y - bodyH * 1.45f,
                    x + bodyLen * 0.4f, y - bodyH * 0.85f
                )
                cubicTo(
                    x + bodyLen * 0.2f, y - bodyH * 0.8f,
                    x, y - bodyH * 0.82f,
                    x - bodyLen * 0.3f, y - bodyH * 0.85f
                )
                close()
            }
            drawPath(dorsal, SurgFin)

            // ════════════════════════════════════════════════════════════════
            // ANAL FIN — along bottom
            // ════════════════════════════════════════════════════════════════
            val anal = paths.obtain().apply {
                moveTo(x, y + bodyH * 0.82f)
                cubicTo(
                    x + bodyLen * 0.15f, y + bodyH * 1.3f,
                    x + bodyLen * 0.32f, y + bodyH * 1.25f,
                    x + bodyLen * 0.4f, y + bodyH * 0.82f
                )
                cubicTo(
                    x + bodyLen * 0.25f, y + bodyH * 0.78f,
                    x + bodyLen * 0.1f, y + bodyH * 0.80f,
                    x, y + bodyH * 0.82f
                )
                close()
            }
            drawPath(anal, SurgFin)

            // ════════════════════════════════════════════════════════════════
            // MAIN BODY — oval, cobalt blue, widest near head
            // ════════════════════════════════════════════════════════════════
            val body = paths.obtain().apply {
                moveTo(x - bodyLen, y)
                cubicTo(
                    x - bodyLen * 0.8f, y - bodyH,
                    x + bodyLen * 0.0f, y - bodyH * 0.95f,
                    x + bodyLen * 0.45f, y - bodyH * 0.25f
                )
                lineTo(x + bodyLen * 0.45f, y + bodyH * 0.25f)
                cubicTo(
                    x + bodyLen * 0.0f, y + bodyH * 0.95f,
                    x - bodyLen * 0.8f, y + bodyH,
                    x - bodyLen, y
                )
                close()
            }
            drawPath(body, SurgBody)

            // Belly highlight — pale strip along the lower body
            val belly = paths.obtain().apply {
                moveTo(x - bodyLen * 0.5f, y + bodyH * 0.3f)
                cubicTo(
                    x - bodyLen * 0.2f, y + bodyH * 0.85f,
                    x + bodyLen * 0.1f, y + bodyH * 0.85f,
                    x + bodyLen * 0.4f, y + bodyH * 0.2f
                )
                cubicTo(
                    x + bodyLen * 0.1f, y + bodyH * 0.65f,
                    x - bodyLen * 0.1f, y + bodyH * 0.65f,
                    x - bodyLen * 0.5f, y + bodyH * 0.3f
                )
                close()
            }
            drawPath(belly, SurgBelly.copy(alpha = 0.45f))

            // Body sheen highlight
            drawPath(body, SurgBlue2.copy(alpha = 0.3f),
                style = Stroke(width = (1.0f * s).coerceAtLeast(0.5f)))

            // ════════════════════════════════════════════════════════════════
            // PECTORAL FINS — sides
            // ════════════════════════════════════════════════════════════════
            val pectSway = sin(t * 4f * PI.toFloat()) * 1.5f * s
            val pect = paths.obtain().apply {
                moveTo(x - bodyLen * 0.25f, y)
                cubicTo(
                    x - bodyLen * 0.1f + pectSway, y + bodyH * 0.6f,
                    x + bodyLen * 0.05f + pectSway, y + bodyH * 0.7f,
                    x + bodyLen * 0.15f, y + bodyH * 0.4f
                )
                cubicTo(
                    x + bodyLen * 0.0f, y + bodyH * 0.3f,
                    x - bodyLen * 0.1f, y + bodyH * 0.15f,
                    x - bodyLen * 0.25f, y
                )
                close()
            }
            drawPath(pect, SurgBlue2.copy(alpha = 0.75f))

            // ════════════════════════════════════════════════════════════════
            // BLACK MASK — through eye to mouth corner
            // ════════════════════════════════════════════════════════════════
            val mask = paths.obtain().apply {
                moveTo(x - bodyLen * 0.82f, y - bodyH * 0.55f)
                cubicTo(
                    x - bodyLen * 0.7f, y - bodyH * 0.75f,
                    x - bodyLen * 0.55f, y - bodyH * 0.75f,
                    x - bodyLen * 0.42f, y - bodyH * 0.55f
                )
                cubicTo(
                    x - bodyLen * 0.38f, y - bodyH * 0.20f,
                    x - bodyLen * 0.60f, y + bodyH * 0.05f,
                    x - bodyLen * 0.82f, y + bodyH * 0.05f
                )
                close()
            }
            drawPath(mask, SurgMask.copy(alpha = 0.85f))

            // ════════════════════════════════════════════════════════════════
            // EYE — white circle + dark pupil, inside mask
            // ════════════════════════════════════════════════════════════════
            val eyeR  = (1.5f + level * 0.1f) * s
            val eyeX  = x - bodyLen * 0.62f
            val eyeY  = y - bodyH * 0.38f
            drawCircle(Color.White, eyeR, Offset(eyeX, eyeY))
            drawCircle(Color(0xFF111133), eyeR * 0.55f, Offset(eyeX + eyeR * 0.1f, eyeY))

            // ════════════════════════════════════════════════════════════════
            // SCALPEL / SPINE — small white blade at base of tail
            // ════════════════════════════════════════════════════════════════
            val scalpelX = tailX - 1.5f * s
            val scalpelY = y + bodyH * 0.18f
            val scalpel = paths.obtain().apply {
                moveTo(scalpelX, scalpelY)
                lineTo(scalpelX + 2.5f * s, scalpelY + 1.2f * s)
                lineTo(scalpelX, scalpelY + 2.5f * s)
                close()
            }
            drawPath(scalpel, SurgScalpel)

            // ════════════════════════════════════════════════════════════════
            // LEVEL 3+: Electric blue lateral line
            // ════════════════════════════════════════════════════════════════
            if (level >= 3) {
                drawLine(
                    SurgElectric.copy(alpha = 0.65f),
                    Offset(x - bodyLen * 0.6f, y - bodyH * 0.05f),
                    Offset(x + bodyLen * 0.42f, y - bodyH * 0.08f),
                    strokeWidth = (1.0f * s).coerceAtLeast(0.6f),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
