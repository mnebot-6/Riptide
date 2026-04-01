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
import kotlin.math.cos

// ── Palette (reddish-brown / golden lion's mane) ────────────────────────────
private val BellRed        = Color(0xFFA83820)  // reddish-brown bell
private val BellDarkRed    = Color(0xFF882818)  // darker red
private val BellGold       = Color(0xFFD08830)  // golden highlight
private val BellLightGold  = Color(0xFFE8A848)  // lighter gold
private val BellAmber      = Color(0xFFC07028)  // amber mid-tone
private val TentRed        = Color(0xFF903020)  // tentacle red
private val TentOrange     = Color(0xFFD06030)  // tentacle orange
private val TentGold       = Color(0xFFD89040)  // tentacle gold (mane)
private val TentDark       = Color(0xFF601810)  // darkest tentacle
private val BioGlow        = Color(0xFFFF8040)  // bioluminescent glow
private val BioGlowPink    = Color(0xFFFF6060)  // pink bioluminescence
private val ShadowDark     = Color(0xFF401008)  // dark shadow
private val BellCream      = Color(0xFFFFE8D0)  // cream highlight

object LionsmaneJellyfishRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        // ── Bell pulse (gentle jet propulsion) ───────────────────────────────
        val pulsePeriod = 800L
        val pulsePhase = (animTimeMs % pulsePeriod).toFloat() / pulsePeriod.toFloat()
        val contraction = sin(pulsePhase * 2f * PI.toFloat()) * 0.5f + 0.5f

        val bellRBase = (9f + level * 0.6f) * s
        val bellRX = bellRBase * (1f + contraction * 0.10f)
        val bellRY = bellRBase * (1f - contraction * 0.06f)
        val jetY = -contraction * s * 0.05f

        val tentLen = (14f + level * 1.0f) * s
        val maneLen = (10f + level * 0.8f) * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = jetY)
            }) {

            // ── LONG TRAILING TENTACLES (the "mane") ─────────────────────────
            val mainTentCount = 12 + level * 2
            for (i in 0 until mainTentCount) {
                val angle = (i.toFloat() / mainTentCount - 0.5f) * 1.6f
                val baseX = x + sin(angle) * bellRX * 0.7f
                val baseY = y + bellRY * 0.6f

                // Multi-segment tentacle with hysteresis
                val seg1Sway = sin(t * 1.2f * PI.toFloat() + i * 0.4f) * tentLen * 0.12f
                val seg2Sway = sin(t * 1.2f * PI.toFloat() + i * 0.4f - 0.8f) * tentLen * 0.25f
                val seg3Sway = sin(t * 1.2f * PI.toFloat() + i * 0.4f - 1.6f) * tentLen * 0.35f

                val tLen = tentLen * (0.6f + 0.4f * sin(i.toFloat() * 0.7f))

                val tentacle = Path().apply {
                    moveTo(baseX, baseY)
                    cubicTo(
                        baseX + seg1Sway, baseY + tLen * 0.3f,
                        baseX + seg2Sway, baseY + tLen * 0.6f,
                        baseX + seg3Sway, baseY + tLen
                    )
                }

                // Outer glow
                drawPath(
                    tentacle, TentDark.copy(alpha = 0.10f),
                    style = Stroke(width = (3.5f * s).coerceAtLeast(1.2f), cap = StrokeCap.Round)
                )
                // Main tentacle
                val tentColor = if (i % 3 == 0) TentGold else if (i % 3 == 1) TentOrange else TentRed
                drawPath(
                    tentacle, tentColor.copy(alpha = 0.35f),
                    style = Stroke(
                        width = (1.5f * s * (1f - i * 0.02f)).coerceAtLeast(0.5f),
                        cap = StrokeCap.Round
                    )
                )
                // Core
                drawPath(
                    tentacle, TentGold.copy(alpha = 0.18f),
                    style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round)
                )
            }

            // ── ORAL ARMS (thicker, shorter, from center) ───────────────────
            val armCount = 6
            for (i in 0 until armCount) {
                val armAngle = (i.toFloat() / armCount - 0.5f) * 1.0f
                val armX = x + sin(armAngle) * bellRX * 0.35f
                val armY = y + bellRY * 0.5f

                val armSway = sin(t * 1.5f * PI.toFloat() + i * 0.8f - 0.5f) * maneLen * 0.20f
                val armPath = Path().apply {
                    moveTo(armX, armY)
                    cubicTo(
                        armX + armSway * 0.3f, armY + maneLen * 0.3f,
                        armX + armSway * 0.7f, armY + maneLen * 0.6f,
                        armX + armSway, armY + maneLen
                    )
                }

                // Frilly oral arm
                drawPath(
                    armPath, BellAmber.copy(alpha = 0.30f),
                    style = Stroke(width = (4.0f * s).coerceAtLeast(1.5f), cap = StrokeCap.Round)
                )
                drawPath(
                    armPath, BellGold.copy(alpha = 0.25f),
                    style = Stroke(width = (2.0f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round)
                )
                drawPath(
                    armPath, BellCream.copy(alpha = 0.15f),
                    style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round)
                )
            }

            // ── LEVEL 3+: More tentacles visible (additional fine ones) ─────
            if (level >= 3) {
                val extraCount = 8
                for (i in 0 until extraCount) {
                    val angle = (i.toFloat() / extraCount - 0.5f) * 1.8f
                    val baseX = x + sin(angle) * bellRX * 0.85f
                    val baseY = y + bellRY * 0.55f
                    val fineSway = sin(t * 1.5f * PI.toFloat() + i * 0.6f + 2f) * tentLen * 0.18f
                    val fineLen = tentLen * 0.5f

                    val fine = Path().apply {
                        moveTo(baseX, baseY)
                        quadraticTo(
                            baseX + fineSway * 0.5f, baseY + fineLen * 0.5f,
                            baseX + fineSway, baseY + fineLen
                        )
                    }
                    drawPath(
                        fine, TentOrange.copy(alpha = 0.20f),
                        style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
                    )
                }
            }

            // ── BELL SHADOW ──────────────────────────────────────────────────
            val bellShadow = Path().apply {
                moveTo(x - bellRX * 1.04f, y + bellRY * 0.05f)
                cubicTo(
                    x - bellRX * 1.04f, y - bellRY * 1.0f,
                    x + bellRX * 1.04f, y - bellRY * 1.0f,
                    x + bellRX * 1.04f, y + bellRY * 0.05f
                )
                cubicTo(
                    x + bellRX * 0.74f, y + bellRY * 0.42f,
                    x - bellRX * 0.74f, y + bellRY * 0.42f,
                    x - bellRX * 1.04f, y + bellRY * 0.05f
                )
                close()
            }
            drawPath(bellShadow, ShadowDark.copy(alpha = 0.18f))

            // ── BELL DOME (main body) ────────────────────────────────────────
            val bell = Path().apply {
                moveTo(x - bellRX, y)
                cubicTo(
                    x - bellRX, y - bellRY * 0.95f,
                    x + bellRX, y - bellRY * 0.95f,
                    x + bellRX, y
                )
                cubicTo(
                    x + bellRX * 0.7f, y + bellRY * 0.35f,
                    x - bellRX * 0.7f, y + bellRY * 0.35f,
                    x - bellRX, y
                )
                close()
            }
            drawPath(bell, BellRed.copy(alpha = 0.50f))

            // Inner bell layer
            val bellInner = Path().apply {
                moveTo(x - bellRX * 0.88f, y + bellRY * 0.03f)
                cubicTo(
                    x - bellRX * 0.88f, y - bellRY * 0.88f,
                    x + bellRX * 0.88f, y - bellRY * 0.88f,
                    x + bellRX * 0.88f, y + bellRY * 0.03f
                )
                cubicTo(
                    x + bellRX * 0.58f, y + bellRY * 0.30f,
                    x - bellRX * 0.58f, y + bellRY * 0.30f,
                    x - bellRX * 0.88f, y + bellRY * 0.03f
                )
                close()
            }
            drawPath(bellInner, BellGold.copy(alpha = 0.25f))

            // Upper highlight
            val bellHL = Path().apply {
                moveTo(x - bellRX * 0.55f, y - bellRY * 0.35f)
                cubicTo(
                    x - bellRX * 0.4f, y - bellRY * 0.82f,
                    x + bellRX * 0.35f, y - bellRY * 0.80f,
                    x + bellRX * 0.3f, y - bellRY * 0.30f
                )
                cubicTo(
                    x + bellRX * 0.15f, y - bellRY * 0.55f,
                    x - bellRX * 0.25f, y - bellRY * 0.58f,
                    x - bellRX * 0.55f, y - bellRY * 0.35f
                )
                close()
            }
            drawPath(bellHL, BellLightGold.copy(alpha = 0.30f))

            // Specular highlight
            drawCircle(BellCream.copy(alpha = 0.18f), bellRBase * 0.22f,
                Offset(x - bellRX * 0.22f, y - bellRY * 0.55f))

            // ── RADIAL CANALS (visible through translucent bell) ────────────
            for (c in 0 until 8) {
                val ca = c.toFloat() / 8f * 2f * PI.toFloat()
                val innerR = bellRBase * 0.12f
                val outerR = bellRBase * 0.78f
                val cx1 = x + cos(ca) * innerR
                val cy1 = y - bellRY * 0.28f + sin(ca) * innerR * 0.5f
                val cx2 = x + cos(ca) * outerR
                val cy2 = y - bellRY * 0.08f + sin(ca) * outerR * 0.35f
                val canalAlpha = 0.10f + contraction * 0.15f
                drawLine(
                    BellDarkRed.copy(alpha = canalAlpha),
                    Offset(cx1, cy1), Offset(cx2, cy2),
                    strokeWidth = (0.4f * s).coerceAtLeast(0.2f),
                    cap = StrokeCap.Round
                )
            }

            // ── BELL RIM ─────────────────────────────────────────────────────
            val rim = Path().apply {
                moveTo(x - bellRX, y)
                cubicTo(
                    x - bellRX * 0.7f, y + bellRY * 0.35f,
                    x + bellRX * 0.7f, y + bellRY * 0.35f,
                    x + bellRX, y
                )
            }
            drawPath(
                rim, BellDarkRed.copy(alpha = 0.45f),
                style = Stroke(width = (1.5f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round)
            )
            drawPath(
                rim, BellGold.copy(alpha = 0.20f),
                style = Stroke(width = (3.0f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round)
            )

            // Bell outline
            drawPath(
                bell, BellDarkRed.copy(alpha = 0.22f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f))
            )

            // ── LEVEL 5+: Bioluminescent pulse through bell ─────────────────
            if (level >= 5) {
                val bioPhase = (sin(t * 0.6f * PI.toFloat()) * 0.5f + 0.5f)
                val bioAlpha = bioPhase * 0.18f

                drawPath(bell, BioGlow.copy(alpha = bioAlpha))
                drawCircle(BioGlowPink.copy(alpha = bioAlpha * 0.6f), bellRBase * 0.6f,
                    Offset(x, y - bellRY * 0.25f))

                // Glow along rim during pulse
                drawPath(
                    rim, BioGlow.copy(alpha = bioAlpha * 0.8f),
                    style = Stroke(width = (2.5f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round)
                )
            }

            } // jet transform
        }
    }
}
