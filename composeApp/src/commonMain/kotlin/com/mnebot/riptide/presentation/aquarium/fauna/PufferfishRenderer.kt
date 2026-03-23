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
private val PuffYellow  = Color(0xFFCCB840)  // yellow-green body
private val PuffYellow2 = Color(0xFFE8D060)  // lighter belly
private val PuffSpot    = Color(0xFF4A3A10)  // dark spots
private val PuffFin     = Color(0xFF9A8830)  // fin colour
private val PuffBlack   = Color(0xFF1A1A1A)  // outline & eye
private val PuffWhite   = Color(0xFFF5F5F5)  // eye white

object PufferfishRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        // Pufferfish "puffs" gently every ~3 seconds
        val puffCycle = (animTimeMs % 4000L).toFloat() / 4000f
        val puffScale = 1f + sin(puffCycle * PI.toFloat() * 2f).coerceAtLeast(0f) * 0.10f

        val baseR = (7f + level * 0.5f) * s * puffScale
        val tailLen = (3.5f + level * 0.3f) * s

        val tailSway = sin(t * 2.8f * PI.toFloat()) * 1.4f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL FIN — small rounded ──────────────────────────────────────
            val tailX = x + baseR * 0.85f
            val tail = Path().apply {
                moveTo(tailX, y - baseR * 0.2f)
                cubicTo(
                    tailX + tailLen * 0.4f, y - tailLen * 0.6f + tailSway,
                    tailX + tailLen, y - tailLen * 0.9f + tailSway,
                    tailX + tailLen, y - tailLen * 0.5f + tailSway
                )
                cubicTo(
                    tailX + tailLen * 0.8f, y + tailSway * 0.3f,
                    tailX + tailLen * 0.4f, y + tailLen * 0.5f + tailSway * 0.6f,
                    tailX + tailLen, y + tailLen * 0.9f + tailSway
                )
                cubicTo(
                    tailX + tailLen, y + tailLen * 0.6f + tailSway,
                    tailX + tailLen * 0.4f, y + tailLen * 0.6f + tailSway,
                    tailX, y + baseR * 0.2f
                )
                close()
            }
            drawPath(tail, PuffFin)

            // ── DORSAL FIN — tiny ─────────────────────────────────────────────
            val dorsal = Path().apply {
                moveTo(x - baseR * 0.1f, y - baseR * 0.9f)
                cubicTo(
                    x + baseR * 0.1f, y - baseR * 1.35f,
                    x + baseR * 0.3f, y - baseR * 1.30f,
                    x + baseR * 0.4f, y - baseR * 0.9f
                )
                close()
            }
            drawPath(dorsal, PuffFin)

            // ── PECTORAL FIN ──────────────────────────────────────────────────
            val pectSway = sin(t * 5f * PI.toFloat()) * 1.5f * s
            val pect = Path().apply {
                moveTo(x - baseR * 0.2f, y + baseR * 0.1f)
                cubicTo(
                    x + baseR * 0.1f + pectSway, y + baseR * 0.7f,
                    x + baseR * 0.4f + pectSway, y + baseR * 0.8f,
                    x + baseR * 0.55f, y + baseR * 0.4f
                )
                cubicTo(x + baseR * 0.3f, y + baseR * 0.2f, x, y + baseR * 0.15f, x - baseR * 0.2f, y + baseR * 0.1f)
                close()
            }
            drawPath(pect, PuffFin.copy(alpha = 0.80f))

            // ── ROUND BODY ────────────────────────────────────────────────────
            drawCircle(PuffYellow, baseR, Offset(x, y))

            // Belly highlight (lighter bottom half)
            drawCircle(PuffYellow2.copy(alpha = 0.50f), baseR * 0.8f, Offset(x, y + baseR * 0.2f))

            // ── SPOTS ─────────────────────────────────────────────────────────
            val spotPositions = listOf(
                Pair(-0.35f, -0.40f), Pair(0.15f, -0.50f), Pair(0.45f, -0.30f),
                Pair(-0.20f, 0.10f),  Pair(0.30f, 0.15f),  Pair(-0.50f, 0.10f),
                Pair(0.05f, 0.50f),   Pair(-0.30f, 0.50f),
            )
            val spotR = baseR * 0.12f
            for ((sx, sy) in spotPositions) {
                drawCircle(PuffSpot.copy(alpha = 0.55f), spotR, Offset(x + sx * baseR, y + sy * baseR))
            }

            // Body outline
            drawCircle(
                PuffSpot.copy(alpha = 0.30f), baseR,
                style = Stroke(width = (0.7f * s).coerceAtLeast(0.3f))
            )

            // ── SPINES (level 3+) ─────────────────────────────────────────────
            if (level >= 3) {
                val spineCount = 14 + level * 2
                val spineLen   = (1.5f + level * 0.2f) * s
                for (i in 0 until spineCount) {
                    val angle = i.toFloat() / spineCount.toFloat() * 2f * PI.toFloat()
                    val bx = x + cos(angle) * baseR * 0.88f
                    val by = y + sin(angle) * baseR * 0.88f
                    val ex = x + cos(angle) * (baseR + spineLen)
                    val ey = y + sin(angle) * (baseR + spineLen)
                    drawLine(
                        PuffSpot.copy(alpha = 0.65f),
                        Offset(bx, by), Offset(ex, ey),
                        strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // ── EYE ───────────────────────────────────────────────────────────
            val eyeR = (1.8f + level * 0.1f) * s
            val eyeX = x - baseR * 0.45f
            val eyeY = y - baseR * 0.20f
            drawCircle(PuffWhite, eyeR, Offset(eyeX, eyeY))
            drawCircle(PuffBlack, eyeR * 0.60f, Offset(eyeX + eyeR * 0.08f, eyeY))
            drawCircle(PuffWhite, eyeR * 0.22f, Offset(eyeX - eyeR * 0.22f, eyeY - eyeR * 0.22f))

            // Tiny mouth (downturned)
            val mouthPath = Path().apply {
                moveTo(x - baseR * 0.1f, y + baseR * 0.2f)
                cubicTo(
                    x - baseR * 0.05f, y + baseR * 0.32f,
                    x + baseR * 0.05f, y + baseR * 0.32f,
                    x + baseR * 0.1f, y + baseR * 0.2f
                )
            }
            drawPath(mouthPath, PuffBlack, style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round))
        }
    }
}
