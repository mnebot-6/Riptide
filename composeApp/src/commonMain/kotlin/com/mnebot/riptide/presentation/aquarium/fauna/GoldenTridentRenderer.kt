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
private val GoldMain       = Color(0xFFD4A830)   // main gold
private val GoldLight      = Color(0xFFE8C848)   // highlight gold
private val GoldBright     = Color(0xFFF0D860)   // bright shimmer
private val GoldDark       = Color(0xFFA07818)   // shadow gold
private val GoldDeep       = Color(0xFF785810)   // deep shadow
private val ShaftBrown     = Color(0xFFB89030)   // shaft color
private val SandColor      = Color(0xFFD8C898)   // sand mound
private val SandDark       = Color(0xFFB8A878)   // sand shadow
private val SandLight      = Color(0xFFE8DDB8)   // sand highlight
private val SparkleWhite   = Color(0xFFFFF8E0)   // sparkle
private val RippleBlue     = Color(0xFF80C0E0)   // water ripple (level 3+)
private val AuraGold       = Color(0xFFE8C040)   // golden aura (level 5+)

object GoldenTridentRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val totalH = (28f + level * 2f) * s
        val tiltAngle = -12f  // tilted slightly left (stuck in sand at angle)

        // Level 5+: ethereal golden glow aura
        if (level >= 5) {
            val auraPulse = 0.10f + sin(t * 1.2f * PI.toFloat()) * 0.06f
            drawCircle(
                color = AuraGold.copy(alpha = auraPulse),
                radius = totalH * 0.55f,
                center = Offset(x, y - totalH * 0.4f)
            )
            // Secondary inner aura
            drawCircle(
                color = AuraGold.copy(alpha = auraPulse * 1.3f),
                radius = totalH * 0.3f,
                center = Offset(x, y - totalH * 0.5f)
            )
        }

        // ── Sand mound at base ──────────────────────────────────────────────
        val moundW = (8f + level * 0.5f) * s
        val moundH = (3f + level * 0.3f) * s

        val moundPath = Path().apply {
            moveTo(x - moundW, y)
            cubicTo(
                x - moundW * 0.6f, y - moundH,
                x + moundW * 0.6f, y - moundH,
                x + moundW, y
            )
            close()
        }
        drawPath(moundPath, SandColor)

        // Sand shadow
        val moundShadow = Path().apply {
            moveTo(x, y)
            cubicTo(
                x + moundW * 0.3f, y - moundH * 0.7f,
                x + moundW * 0.8f, y - moundH * 0.5f,
                x + moundW, y
            )
            close()
        }
        drawPath(moundShadow, SandDark.copy(alpha = 0.35f))

        // Sand highlight
        val moundHL = Path().apply {
            moveTo(x - moundW * 0.6f, y - moundH * 0.2f)
            cubicTo(
                x - moundW * 0.3f, y - moundH * 0.85f,
                x + moundW * 0.1f, y - moundH * 0.85f,
                x + moundW * 0.2f, y - moundH * 0.3f
            )
            cubicTo(
                x, y - moundH * 0.6f,
                x - moundW * 0.4f, y - moundH * 0.5f,
                x - moundW * 0.6f, y - moundH * 0.2f
            )
            close()
        }
        drawPath(moundHL, SandLight.copy(alpha = 0.4f))

        // Sand grains (small dots)
        for (g in 0 until 6) {
            val gx = x + (g - 3) * moundW * 0.25f
            val gy = y - moundH * (0.2f + sin(g * 1.7f) * 0.15f)
            drawCircle(SandDark.copy(alpha = 0.2f), (0.4f * s).coerceAtLeast(0.2f), Offset(gx, gy))
        }

        // ── Trident (drawn at an angle) ─────────────────────────────────────
        withTransform({
            rotate(degrees = tiltAngle, pivot = Offset(x, y - moundH * 0.5f))
        }) {
            val baseY = y - moundH * 0.5f
            val shaftW = (1.5f * s).coerceAtLeast(0.7f)

            // Shaft
            val shaftTop = baseY - totalH * 0.65f
            val shaftBottom = baseY + totalH * 0.05f

            // Main shaft
            drawLine(
                color = ShaftBrown,
                start = Offset(x, shaftBottom),
                end = Offset(x, shaftTop),
                strokeWidth = shaftW * 1.8f,
                cap = StrokeCap.Round
            )
            // Shaft highlight (left side)
            drawLine(
                color = GoldLight.copy(alpha = 0.5f),
                start = Offset(x - shaftW * 0.4f, shaftBottom),
                end = Offset(x - shaftW * 0.4f, shaftTop),
                strokeWidth = shaftW * 0.5f,
                cap = StrokeCap.Round
            )

            // Ornate grip section (mid-shaft)
            val gripTop = baseY - totalH * 0.15f
            val gripBot = baseY
            val gripW = shaftW * 1.4f
            for (g in 0 until 3) {
                val gy = gripBot + (gripTop - gripBot) * (g + 0.5f) / 3f
                drawCircle(
                    color = GoldMain,
                    radius = gripW,
                    center = Offset(x, gy)
                )
                drawCircle(
                    color = GoldDark.copy(alpha = 0.3f),
                    radius = gripW * 0.5f,
                    center = Offset(x + gripW * 0.3f, gy)
                )
            }

            // ── Trident head ────────────────────────────────────────────────
            val headBase = shaftTop
            val prongH = totalH * 0.28f
            val prongSpacing = (4f + level * 0.3f) * s
            val prongW = (1.0f * s).coerceAtLeast(0.5f)

            // Cross guard
            val guardW = prongSpacing * 1.4f
            val guardH = 2f * s
            val guardPath = Path().apply {
                moveTo(x - guardW, headBase)
                cubicTo(
                    x - guardW * 0.5f, headBase - guardH,
                    x + guardW * 0.5f, headBase - guardH,
                    x + guardW, headBase
                )
                cubicTo(
                    x + guardW * 0.5f, headBase + guardH * 0.5f,
                    x - guardW * 0.5f, headBase + guardH * 0.5f,
                    x - guardW, headBase
                )
                close()
            }
            drawPath(guardPath, GoldMain)
            drawPath(guardPath, GoldDark.copy(alpha = 0.2f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.3f)))

            // Three prongs
            val prongs = listOf(-1f, 0f, 1f)
            for (pi in prongs.indices) {
                val dir = prongs[pi]
                val prongX = x + dir * prongSpacing
                val prongTop = headBase - prongH * (if (dir == 0f) 1.0f else 0.82f)
                val curve = dir * prongSpacing * 0.3f

                // Prong shaft
                val pPath = Path().apply {
                    moveTo(prongX - prongW, headBase)
                    cubicTo(
                        prongX - prongW + curve * 0.3f, headBase - prongH * 0.4f,
                        prongX - prongW * 0.5f + curve * 0.6f, headBase - prongH * 0.7f,
                        prongX + curve * 0.3f, prongTop
                    )
                    cubicTo(
                        prongX + prongW * 0.5f + curve * 0.6f, headBase - prongH * 0.7f,
                        prongX + prongW + curve * 0.3f, headBase - prongH * 0.4f,
                        prongX + prongW, headBase
                    )
                    close()
                }
                drawPath(pPath, GoldMain)

                // Prong tip (pointed)
                val tipPath = Path().apply {
                    moveTo(prongX + curve * 0.3f - prongW * 1.3f, prongTop + prongH * 0.08f)
                    lineTo(prongX + curve * 0.3f, prongTop - prongH * 0.06f)
                    lineTo(prongX + curve * 0.3f + prongW * 1.3f, prongTop + prongH * 0.08f)
                    close()
                }
                drawPath(tipPath, GoldLight)

                // Highlight on prong
                drawLine(
                    color = GoldBright.copy(alpha = 0.4f),
                    start = Offset(prongX - prongW * 0.3f, headBase - prongH * 0.1f),
                    end = Offset(prongX + curve * 0.2f, prongTop + prongH * 0.1f),
                    strokeWidth = prongW * 0.5f,
                    cap = StrokeCap.Round
                )

                // Level 3+: water ripple effect around prongs
                if (level >= 3) {
                    val ripplePhase = t * 2f + pi * 1.5f
                    val rippleR1 = (2f + sin(ripplePhase) * 1f) * s
                    val rippleR2 = (3f + sin(ripplePhase + 1f) * 1.2f) * s
                    val prongTipX = prongX + curve * 0.3f
                    drawCircle(
                        color = RippleBlue.copy(alpha = 0.15f + sin(ripplePhase) * 0.08f),
                        radius = rippleR1,
                        center = Offset(prongTipX, prongTop)
                    )
                    drawCircle(
                        color = RippleBlue.copy(alpha = 0.08f + sin(ripplePhase + 0.5f) * 0.05f),
                        radius = rippleR2,
                        center = Offset(prongTipX, prongTop)
                    )
                }
            }
        }

        // ── Animated sparkles (not affected by tilt transform) ──────────────
        val sparkleCount = 3 + level / 2
        for (sp in 0 until sparkleCount) {
            val phase = sp * 2.3f + 0.7f
            val cycle = ((t * 0.8f + phase) % 2.0f) / 2.0f
            val sparkAlpha = if (cycle < 0.5f) cycle * 2f else (1f - cycle) * 2f
            val spX = x + sin(phase * 1.7f) * totalH * 0.25f
            val spY = y - totalH * (0.2f + cycle * 0.6f) + sin(t + phase) * 2f * s
            val spR = (0.5f + sparkAlpha * 0.4f) * s

            // 4-point star sparkle
            drawLine(
                color = SparkleWhite.copy(alpha = sparkAlpha * 0.7f),
                start = Offset(spX - spR, spY),
                end = Offset(spX + spR, spY),
                strokeWidth = (0.4f * s).coerceAtLeast(0.2f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = SparkleWhite.copy(alpha = sparkAlpha * 0.7f),
                start = Offset(spX, spY - spR),
                end = Offset(spX, spY + spR),
                strokeWidth = (0.4f * s).coerceAtLeast(0.2f),
                cap = StrokeCap.Round
            )
            // Diagonal sparkle lines (smaller)
            drawLine(
                color = SparkleWhite.copy(alpha = sparkAlpha * 0.4f),
                start = Offset(spX - spR * 0.6f, spY - spR * 0.6f),
                end = Offset(spX + spR * 0.6f, spY + spR * 0.6f),
                strokeWidth = (0.3f * s).coerceAtLeast(0.15f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = SparkleWhite.copy(alpha = sparkAlpha * 0.4f),
                start = Offset(spX + spR * 0.6f, spY - spR * 0.6f),
                end = Offset(spX - spR * 0.6f, spY + spR * 0.6f),
                strokeWidth = (0.3f * s).coerceAtLeast(0.15f),
                cap = StrokeCap.Round
            )
        }
    }
}
