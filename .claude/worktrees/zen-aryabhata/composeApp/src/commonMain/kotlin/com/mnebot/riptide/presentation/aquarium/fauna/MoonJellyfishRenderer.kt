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
private val JellyBell   = Color(0xFFCCE8FF)  // pale translucent blue-white
private val JellyBell2  = Color(0xFFAAD0EE)  // bell rim
private val JellyGonad  = Color(0xFFDD6699)  // pink gonad rings
private val JellyArm    = Color(0xFF99AACC)  // oral arm
private val JellyTent   = Color(0xFFBBCCEE)  // marginal tentacles

object MoonJellyfishRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        // Pulsing bell contraction (1.6 Hz typical for moon jellies)
        val pulsePeriod = 625L  // ms per pulse
        val pulsePhase = (animTimeMs % pulsePeriod).toFloat() / pulsePeriod.toFloat()
        val pulse = sin(pulsePhase * 2f * PI.toFloat()) * 0.08f + 1f

        val bellR   = (8f + level * 0.5f) * s * pulse
        val armLen  = (8f + level * 0.6f) * s
        val tentLen = (5f + level * 0.4f) * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── ORAL ARMS (4, trailing below bell) — drawn first ──────────────
            val armCount = 4
            for (i in 0 until armCount) {
                val angle = i.toFloat() / armCount * 2f * PI.toFloat() + PI.toFloat() / 4f
                val armX = x + cos(angle) * bellR * 0.5f
                // Oral arms hang downward
                val aSway = sin(t * 1.8f * PI.toFloat() + i * 0.7f) * armLen * 0.35f
                val aLen = armLen * (0.8f + 0.2f * sin(i.toFloat()))
                val arm = Path().apply {
                    moveTo(armX, y + bellR * 0.85f)
                    cubicTo(
                        armX + aSway * 0.3f, y + bellR + aLen * 0.3f,
                        armX + aSway * 0.7f, y + bellR + aLen * 0.65f,
                        armX + aSway, y + bellR + aLen
                    )
                }
                // Frilly arm — draw with thicker alpha line + thin outline
                drawPath(arm, JellyArm.copy(alpha = 0.55f),
                    style = Stroke(width = (2.5f * s * (1f - i * 0.1f)).coerceAtLeast(0.8f),
                        cap = StrokeCap.Round))
                drawPath(arm, JellyArm.copy(alpha = 0.30f),
                    style = Stroke(width = (4.0f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round))
            }

            // ── MARGINAL TENTACLES (short, around bell rim) ───────────────────
            val tentCount = 16 + level * 4
            for (i in 0 until tentCount) {
                val angle = i.toFloat() / tentCount * 2f * PI.toFloat()
                val rimX = x + cos(angle) * bellR
                val rimY = y + sin(angle) * bellR * 0.5f  // flattened at bottom
                val tSway = sin(t * 2.5f * PI.toFloat() + i * 0.25f) * 1.5f * s
                drawLine(
                    JellyTent.copy(alpha = 0.55f),
                    Offset(rimX, rimY),
                    Offset(rimX + cos(angle) * tentLen + tSway, rimY + sin(angle) * tentLen),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.25f),
                    cap = StrokeCap.Round
                )
            }

            // ── BELL BODY (dome/hemisphere) ────────────────────────────────────
            val bell = Path().apply {
                // Top arc (dome)
                moveTo(x - bellR, y)
                cubicTo(
                    x - bellR, y - bellR * 1.05f,
                    x + bellR, y - bellR * 1.05f,
                    x + bellR, y
                )
                // Bottom (flat-ish, with slight curve)
                cubicTo(
                    x + bellR * 0.7f, y + bellR * 0.35f,
                    x - bellR * 0.7f, y + bellR * 0.35f,
                    x - bellR, y
                )
                close()
            }
            drawPath(bell, JellyBell.copy(alpha = 0.45f))

            // ── GONAD RINGS (4 horseshoe shapes inside bell) ─────────────────
            val gonadRadius = bellR * 0.38f
            for (g in 0 until 4) {
                val ga = g.toFloat() / 4f * 2f * PI.toFloat() + PI.toFloat() / 4f
                val gx = x + cos(ga) * gonadRadius * 0.55f
                val gy = y - bellR * 0.30f + sin(ga) * gonadRadius * 0.4f
                // Horseshoe shape
                val gonad = Path().apply {
                    val gr = gonadRadius * 0.4f
                    moveTo(gx - gr, gy)
                    cubicTo(gx - gr, gy - gr * 1.1f, gx + gr, gy - gr * 1.1f, gx + gr, gy)
                    cubicTo(gx + gr * 0.8f, gy + gr * 0.5f, gx - gr * 0.8f, gy + gr * 0.5f, gx - gr, gy)
                    close()
                }
                drawPath(gonad, JellyGonad.copy(alpha = 0.65f))
            }

            // Bell outer rim
            val rim = Path().apply {
                moveTo(x - bellR, y)
                cubicTo(
                    x - bellR * 0.7f, y + bellR * 0.35f,
                    x + bellR * 0.7f, y + bellR * 0.35f,
                    x + bellR, y
                )
            }
            drawPath(rim, JellyBell2.copy(alpha = 0.70f),
                style = Stroke(width = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round))

            // Bell outline
            drawPath(bell, JellyBell2.copy(alpha = 0.35f),
                style = Stroke(width = (0.8f * s).coerceAtLeast(0.3f)))

            // ── LEVEL 3+: Bioluminescent glow ─────────────────────────────────
            if (level >= 3) {
                val glow = (sin(t * 0.7f * PI.toFloat()) * 0.5f + 0.5f) * 0.18f
                drawCircle(JellyGonad.copy(alpha = glow), bellR * 0.9f, Offset(x, y - bellR * 0.3f))
            }
        }
    }
}
