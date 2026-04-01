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

// ── Palette (from Recraft reference SVG) ─────────────────────────────────────
private val BellTeal      = Color(0xFF249492)  // main bell teal
private val BellDarkTeal  = Color(0xFF0A777E)  // deeper bell shading
private val BellCream     = Color(0xFFF2EEE2)  // cream highlight
private val BellWarmGray  = Color(0xFFE3DFD5)  // warm gray bell mid-tone
private val BellTaupe     = Color(0xFFD3CDC2)  // taupe shading
private val BellLightTeal = Color(0xFF77B2B1)  // light teal highlight
private val BellPaleTeal  = Color(0xFF84B9B8)  // pale teal accent
private val GonadOrange   = Color(0xFFED7A48)  // orange gonad/organs
private val GonadLight    = Color(0xFFF89666)  // lighter organ highlight
private val ShadowBlue    = Color(0xFF074065)  // dark blue shadow
private val ShadowMedium  = Color(0xFF0C5B80)  // medium shadow
private val ShadowNavy    = Color(0xFF042D51)  // navy shadow
private val DeepBlack     = Color(0xFF021330)  // near-black

object MoonJellyfishRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 28f
        val t = animTimeMs / 1000f

        // ── Jellyfish jet-propulsion pulse ────────────────────────────────────
        val pulsePeriod = 625L
        val pulsePhase  = (animTimeMs % pulsePeriod).toFloat() / pulsePeriod.toFloat()
        // contraction in [0,1]: 0=relaxed, 1=fully contracted
        val contraction = sin(pulsePhase * 2f * PI.toFloat()) * 0.5f + 0.5f

        val bellRBase = (8f + level * 0.5f) * s
        // Asymmetric squash: bell widens (X) and shortens (Y) on contraction
        val bellRX  = bellRBase * (1f + contraction * 0.14f)
        val bellRY  = bellRBase * (1f - contraction * 0.07f)
        // Jet thrust: brief upward lurch on peak contraction
        val jetY    = -contraction * s * 0.06f

        val armLen  = (8f + level * 0.6f) * s
        val tentLen = (5f + level * 0.4f) * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = jetY)
            }) {

            // ── ORAL ARMS (4, trailing below bell with hysteresis) ────────────
            val armCount = 4
            for (i in 0 until armCount) {
                val angle = i.toFloat() / armCount * 2f * PI.toFloat() + PI.toFloat() / 4f
                val armX = x + cos(angle) * bellRX * 0.5f
                // Arms lag 0.6 rad behind bell contraction → trailing hysteresis
                val aSway = sin(t * 1.8f * PI.toFloat() + i * 0.7f - 0.6f) * armLen * 0.50f
                val aLen = armLen * (0.8f + 0.2f * sin(i.toFloat()))

                val arm = Path().apply {
                    moveTo(armX, y + bellRY * 0.85f)
                    cubicTo(
                        armX + aSway * 0.3f, y + bellRY + aLen * 0.3f,
                        armX + aSway * 0.7f, y + bellRY + aLen * 0.65f,
                        armX + aSway, y + bellRY + aLen
                    )
                }
                // Outer glow
                drawPath(arm, ShadowBlue.copy(alpha = 0.15f),
                    style = Stroke(width = (5.0f * s).coerceAtLeast(1.5f), cap = StrokeCap.Round))
                // Frilly arm
                drawPath(arm, BellTaupe.copy(alpha = 0.50f),
                    style = Stroke(width = (3.0f * s * (1f - i * 0.08f)).coerceAtLeast(1.0f),
                        cap = StrokeCap.Round))
                // Core
                drawPath(arm, BellCream.copy(alpha = 0.40f),
                    style = Stroke(width = (1.5f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round))
            }

            // ── MARGINAL TENTACLES (short, around elliptical rim) ────────────
            val tentCount = 16 + level * 4
            for (i in 0 until tentCount) {
                val angle = i.toFloat() / tentCount * 2f * PI.toFloat()
                val rimX = x + cos(angle) * bellRX
                val rimY = y + sin(angle) * bellRY * 0.65f  // moved closer to actual bell margin
                val tSway = sin(t * 2.5f * PI.toFloat() + i * 0.25f) * 1.5f * s
                val tipWhip = sin(t * 4.0f * PI.toFloat() + i * 0.3f) * s * 0.5f
                val tipX = rimX + cos(angle) * tentLen + tSway + tipWhip
                val tipY = rimY + sin(angle) * tentLen + tipWhip * 0.3f
                // Curved tentacle path (quadratic bezier for organic look)
                val tentPath = Path().apply {
                    moveTo(rimX, rimY)
                    quadraticTo(
                        rimX + cos(angle) * tentLen * 0.5f + tSway * 0.2f,
                        rimY + sin(angle) * tentLen * 0.3f,
                        tipX, tipY
                    )
                }
                // Tentacle glow
                drawPath(tentPath, BellLightTeal.copy(alpha = 0.25f),
                    style = Stroke(width = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round))
                // Tentacle core
                drawPath(tentPath, BellWarmGray.copy(alpha = 0.50f),
                    style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f), cap = StrokeCap.Round))
            }

            // ── BELL SHADOW (beneath, offset) ────────────────────────────────
            val bellShadow = Path().apply {
                moveTo(x - bellRX * 1.03f, y + bellRY * 0.05f)
                cubicTo(
                    x - bellRX * 1.03f, y - bellRY * 1.0f,
                    x + bellRX * 1.03f, y - bellRY * 1.0f,
                    x + bellRX * 1.03f, y + bellRY * 0.05f
                )
                cubicTo(
                    x + bellRX * 0.73f, y + bellRY * 0.40f,
                    x - bellRX * 0.73f, y + bellRY * 0.40f,
                    x - bellRX * 1.03f, y + bellRY * 0.05f
                )
                close()
            }
            drawPath(bellShadow, ShadowBlue.copy(alpha = 0.20f))

            // ── BELL BODY (dome) — asymmetric squash ellipse ─────────────────
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
            drawPath(bell, BellTeal.copy(alpha = 0.40f))

            // Bell inner cream layer
            val bellInner = Path().apply {
                moveTo(x - bellRX * 0.85f, y + bellRY * 0.05f)
                cubicTo(
                    x - bellRX * 0.85f, y - bellRY * 0.85f,
                    x + bellRX * 0.85f, y - bellRY * 0.85f,
                    x + bellRX * 0.85f, y + bellRY * 0.05f
                )
                cubicTo(
                    x + bellRX * 0.55f, y + bellRY * 0.28f,
                    x - bellRX * 0.55f, y + bellRY * 0.28f,
                    x - bellRX * 0.85f, y + bellRY * 0.05f
                )
                close()
            }
            drawPath(bellInner, BellCream.copy(alpha = 0.25f))

            // Bell upper highlight
            val bellHighlight = Path().apply {
                moveTo(x - bellRX * 0.6f, y - bellRY * 0.3f)
                cubicTo(
                    x - bellRX * 0.5f, y - bellRY * 0.85f,
                    x + bellRX * 0.4f, y - bellRY * 0.82f,
                    x + bellRX * 0.3f, y - bellRY * 0.25f
                )
                cubicTo(
                    x + bellRX * 0.15f, y - bellRY * 0.55f,
                    x - bellRX * 0.3f,  y - bellRY * 0.58f,
                    x - bellRX * 0.6f,  y - bellRY * 0.3f
                )
                close()
            }
            drawPath(bellHighlight, BellWarmGray.copy(alpha = 0.30f))

            // Specular highlight (uses bellRBase so it doesn't squash)
            drawCircle(BellPaleTeal.copy(alpha = 0.20f), bellRBase * 0.25f,
                Offset(x - bellRX * 0.25f, y - bellRY * 0.55f))

            // ── RING CANAL (circular structure at bell margin — SVG anatomical feature) ──
            val ringCanalPath = Path().apply {
                moveTo(x + bellRX * 0.85f, y + bellRY * 0.12f)
                cubicTo(
                    x + bellRX * 0.85f, y - bellRY * 0.38f,
                    x - bellRX * 0.85f, y - bellRY * 0.38f,
                    x - bellRX * 0.85f, y + bellRY * 0.12f
                )
            }
            drawPath(ringCanalPath, BellTeal.copy(alpha = 0.15f),
                style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round))

            // ── GONAD RINGS (4 horseshoe shapes) — orange (uses bellRBase) ───
            val gonadRadius = bellRBase * 0.38f
            for (g in 0 until 4) {
                val ga = g.toFloat() / 4f * 2f * PI.toFloat() + PI.toFloat() / 4f
                val gx = x + cos(ga) * gonadRadius * 0.60f
                val gy = y - bellRBase * 0.20f + sin(ga) * gonadRadius * 0.4f
                val gr = gonadRadius * 0.4f

                val gonadShadow = Path().apply {
                    moveTo(gx - gr * 1.1f, gy + gr * 0.1f)
                    cubicTo(gx - gr * 1.1f, gy - gr * 1.0f, gx + gr * 1.1f, gy - gr * 1.0f, gx + gr * 1.1f, gy + gr * 0.1f)
                    cubicTo(gx + gr * 0.9f, gy + gr * 0.6f, gx - gr * 0.9f, gy + gr * 0.6f, gx - gr * 1.1f, gy + gr * 0.1f)
                    close()
                }
                drawPath(gonadShadow, ShadowMedium.copy(alpha = 0.20f))

                val gonad = Path().apply {
                    moveTo(gx - gr, gy)
                    cubicTo(gx - gr, gy - gr * 1.1f, gx + gr, gy - gr * 1.1f, gx + gr, gy)
                    cubicTo(gx + gr * 0.8f, gy + gr * 0.5f, gx - gr * 0.8f, gy + gr * 0.5f, gx - gr, gy)
                    close()
                }
                drawPath(gonad, GonadOrange.copy(alpha = 0.60f))

                val gonadHL = Path().apply {
                    val hgr = gr * 0.65f
                    moveTo(gx - hgr, gy - gr * 0.1f)
                    cubicTo(gx - hgr, gy - hgr * 1.0f, gx + hgr, gy - hgr * 1.0f, gx + hgr, gy - gr * 0.1f)
                    cubicTo(gx + hgr * 0.6f, gy + hgr * 0.2f, gx - hgr * 0.6f, gy + hgr * 0.2f, gx - hgr, gy - gr * 0.1f)
                    close()
                }
                drawPath(gonadHL, GonadLight.copy(alpha = 0.45f))
            }

            // ── BELL EDGE DETAILS ────────────────────────────────────────────
            val rim = Path().apply {
                moveTo(x - bellRX, y)
                cubicTo(
                    x - bellRX * 0.7f, y + bellRY * 0.35f,
                    x + bellRX * 0.7f, y + bellRY * 0.35f,
                    x + bellRX, y
                )
            }
            drawPath(rim, BellDarkTeal.copy(alpha = 0.55f),
                style = Stroke(width = (1.5f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round))
            drawPath(rim, BellLightTeal.copy(alpha = 0.25f),
                style = Stroke(width = (3.0f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round))
            drawPath(bell, BellDarkTeal.copy(alpha = 0.25f),
                style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))

            // ── RADIAL CANALS — brighten with each contraction ───────────────
            for (c in 0 until 8) {
                val ca = c.toFloat() / 8f * 2f * PI.toFloat()
                val innerR = bellRBase * 0.15f
                val outerR = bellRBase * 0.80f
                val cx1 = x + cos(ca) * innerR
                val cy1 = y - bellRY * 0.30f + sin(ca) * innerR * 0.6f
                val cx2 = x + cos(ca) * outerR
                val cy2 = y - bellRY * 0.10f + sin(ca) * outerR * 0.4f
                val canalAlpha = 0.12f + contraction * 0.20f
                drawLine(
                    BellTeal.copy(alpha = canalAlpha),
                    Offset(cx1, cy1), Offset(cx2, cy2),
                    strokeWidth = (0.4f * s).coerceAtLeast(0.2f),
                    cap = StrokeCap.Round
                )
            }

            // ── LEVEL 3+: Bioluminescent glow ───────────────────────────────
            if (level >= 3) {
                val glow = (sin(t * 0.7f * PI.toFloat()) * 0.5f + 0.5f) * 0.18f
                drawCircle(GonadOrange.copy(alpha = glow), bellRBase * 0.9f,
                    Offset(x, y - bellRY * 0.3f))
            }

            // ── LEVEL 5+: Pulsing teal aura ─────────────────────────────────
            if (level >= 5) {
                val aura = (sin(t * 1.2f * PI.toFloat()) * 0.5f + 0.5f) * 0.12f
                drawPath(bell, BellLightTeal.copy(alpha = aura),
                    style = Stroke(width = (3.0f * s).coerceAtLeast(1.2f)))
            }

            } // jet transform
        }
    }
}
