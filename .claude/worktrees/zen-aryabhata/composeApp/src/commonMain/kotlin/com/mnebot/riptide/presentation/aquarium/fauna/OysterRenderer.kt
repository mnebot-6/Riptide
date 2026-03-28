package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val OystShell  = Color(0xFF888878)  // grey shell exterior
private val OystDark   = Color(0xFF555548)  // dark shell edge
private val OystNacre  = Color(0xFFEEE8DC)  // mother-of-pearl interior
private val OystNacre2 = Color(0xFFCCBBAA)  // nacre shadow
private val OystPearl  = Color(0xFFF8F5F0)  // pearl white
private val OystPink   = Color(0xFFDDAACC)  // pearl lustre

object OysterRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        // Fixed floor creature — y is base, draw upward
        val s = size / 28f
        val t = animTimeMs / 1000f

        val shellW  = (11f + level * 0.7f) * s
        val shellH  = (8f + level * 0.5f) * s
        val centerY = y - shellH * 0.5f

        // Open/close animation — very slow (one cycle per 6 seconds)
        val openCycle = (animTimeMs % 6000L).toFloat() / 6000f
        val openFrac  = sin(openCycle * PI.toFloat()).coerceAtLeast(0f) * 0.28f + 0.08f
        val gapY      = shellH * openFrac

        // ── LOWER SHELL (flat) ─────────────────────────────────────────────────
        val lowerShell = Path().apply {
            moveTo(x - shellW, centerY)
            cubicTo(x - shellW * 0.8f, centerY + shellH * 0.55f,
                x + shellW * 0.3f, centerY + shellH * 0.60f,
                x + shellW, centerY + shellH * 0.15f)
            cubicTo(x + shellW * 0.6f, centerY + shellH * 0.05f,
                x, centerY - shellH * 0.05f,
                x - shellW, centerY)
            close()
        }
        drawPath(lowerShell, OystShell)
        drawPath(lowerShell, OystDark.copy(alpha = 0.35f),
            style = Stroke(width = (0.9f * s).coerceAtLeast(0.4f)))

        // ── NACRE INTERIOR (visible when open) ────────────────────────────────
        val nacreOuter = Path().apply {
            moveTo(x - shellW * 0.85f, centerY + gapY)
            cubicTo(x - shellW * 0.7f, centerY + shellH * 0.42f + gapY * 0.5f,
                x + shellW * 0.2f, centerY + shellH * 0.45f + gapY * 0.5f,
                x + shellW * 0.85f, centerY + shellH * 0.10f + gapY * 0.3f)
            cubicTo(x + shellW * 0.5f, centerY + gapY,
                x, centerY - shellH * 0.06f + gapY,
                x - shellW * 0.85f, centerY + gapY)
            close()
        }
        drawPath(nacreOuter, OystNacre.copy(alpha = (openFrac * 5f).coerceIn(0.4f, 1.0f)))

        // ── UPPER SHELL (hinged, slightly open) ───────────────────────────────
        val upperShell = Path().apply {
            val openY = centerY - gapY
            moveTo(x - shellW, openY + shellH * 0.05f)
            cubicTo(x - shellW * 0.5f, openY - shellH * 0.6f,
                x + shellW * 0.4f, openY - shellH * 0.65f,
                x + shellW, openY + shellH * 0.1f)
            cubicTo(x + shellW * 0.5f, openY + shellH * 0.05f,
                x - shellW * 0.2f, openY - shellH * 0.05f,
                x - shellW, openY + shellH * 0.05f)
            close()
        }
        drawPath(upperShell, OystShell)

        // Shell ridges (irregular growth lines)
        val ridgeCount = 5 + level
        for (r in 1..ridgeCount) {
            val rf = r.toFloat() / (ridgeCount + 1f)
            val ry = centerY - gapY - shellH * 0.6f * rf
            val rx = shellW * (0.3f + rf * 0.55f)
            drawLine(
                OystDark.copy(alpha = 0.30f),
                Offset(x - rx, ry),
                Offset(x + rx * 0.85f, ry + shellH * 0.1f * (1f - rf)),
                strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                cap = StrokeCap.Round
            )
        }
        drawPath(upperShell, OystDark.copy(alpha = 0.35f),
            style = Stroke(width = (0.9f * s).coerceAtLeast(0.4f)))

        // ── PEARL (level 5+) ───────────────────────────────────────────────────
        if (level >= 5) {
            val pearlR = (2.2f + level * 0.15f) * s
            val pearlX = x + shellW * 0.15f
            val pearlY = centerY + shellH * 0.22f
            // Lustre glow
            drawCircle(OystPink.copy(alpha = 0.30f), pearlR * 1.6f, Offset(pearlX, pearlY))
            drawCircle(OystPearl, pearlR, Offset(pearlX, pearlY))
            drawCircle(Color.White.copy(alpha = 0.70f), pearlR * 0.35f,
                Offset(pearlX - pearlR * 0.3f, pearlY - pearlR * 0.3f))
        }

        // ── LEVEL 3+: Iridescent nacre shimmer ────────────────────────────────
        if (level >= 3) {
            val shimmer = (sin(t * 0.9f * PI.toFloat()) * 0.5f + 0.5f) * 0.35f
            drawCircle(
                OystPink.copy(alpha = shimmer),
                shellW * 0.5f,
                Offset(x + shellW * 0.1f, centerY + shellH * 0.22f)
            )
        }
    }
}
