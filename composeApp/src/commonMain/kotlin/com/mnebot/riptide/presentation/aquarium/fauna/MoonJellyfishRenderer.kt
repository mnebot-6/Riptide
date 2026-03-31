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

        // Pulsing bell contraction
        val pulsePeriod = 625L
        val pulsePhase = (animTimeMs % pulsePeriod).toFloat() / pulsePeriod.toFloat()
        val pulse = sin(pulsePhase * 2f * PI.toFloat()) * 0.08f + 1f

        val bellR   = (8f + level * 0.5f) * s * pulse
        val armLen  = (8f + level * 0.6f) * s
        val tentLen = (5f + level * 0.4f) * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── ORAL ARMS (4, trailing below bell) ───────────────────────────
            val armCount = 4
            for (i in 0 until armCount) {
                val angle = i.toFloat() / armCount * 2f * PI.toFloat() + PI.toFloat() / 4f
                val armX = x + cos(angle) * bellR * 0.5f
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

            // ── MARGINAL TENTACLES (short, around bell rim) ──────────────────
            val tentCount = 16 + level * 4
            for (i in 0 until tentCount) {
                val angle = i.toFloat() / tentCount * 2f * PI.toFloat()
                val rimX = x + cos(angle) * bellR
                val rimY = y + sin(angle) * bellR * 0.5f
                val tSway = sin(t * 2.5f * PI.toFloat() + i * 0.25f) * 1.5f * s
                // Tentacle glow
                drawLine(
                    BellLightTeal.copy(alpha = 0.25f),
                    Offset(rimX, rimY),
                    Offset(rimX + cos(angle) * tentLen + tSway, rimY + sin(angle) * tentLen),
                    strokeWidth = (1.2f * s).coerceAtLeast(0.5f),
                    cap = StrokeCap.Round
                )
                // Tentacle core
                drawLine(
                    BellWarmGray.copy(alpha = 0.50f),
                    Offset(rimX, rimY),
                    Offset(rimX + cos(angle) * tentLen + tSway, rimY + sin(angle) * tentLen),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.25f),
                    cap = StrokeCap.Round
                )
            }

            // ── BELL SHADOW (beneath, offset) ────────────────────────────────
            val bellShadow = Path().apply {
                moveTo(x - bellR * 1.03f, y + bellR * 0.05f)
                cubicTo(
                    x - bellR * 1.03f, y - bellR * 1.0f,
                    x + bellR * 1.03f, y - bellR * 1.0f,
                    x + bellR * 1.03f, y + bellR * 0.05f
                )
                cubicTo(
                    x + bellR * 0.73f, y + bellR * 0.40f,
                    x - bellR * 0.73f, y + bellR * 0.40f,
                    x - bellR * 1.03f, y + bellR * 0.05f
                )
                close()
            }
            drawPath(bellShadow, ShadowBlue.copy(alpha = 0.20f))

            // ── BELL BODY (dome) — teal base ─────────────────────────────────
            val bell = Path().apply {
                moveTo(x - bellR, y)
                cubicTo(
                    x - bellR, y - bellR * 1.05f,
                    x + bellR, y - bellR * 1.05f,
                    x + bellR, y
                )
                cubicTo(
                    x + bellR * 0.7f, y + bellR * 0.35f,
                    x - bellR * 0.7f, y + bellR * 0.35f,
                    x - bellR, y
                )
                close()
            }
            drawPath(bell, BellTeal.copy(alpha = 0.40f))

            // Bell inner cream layer (translucent overlay)
            val bellInner = Path().apply {
                moveTo(x - bellR * 0.85f, y + bellR * 0.05f)
                cubicTo(
                    x - bellR * 0.85f, y - bellR * 0.85f,
                    x + bellR * 0.85f, y - bellR * 0.85f,
                    x + bellR * 0.85f, y + bellR * 0.05f
                )
                cubicTo(
                    x + bellR * 0.55f, y + bellR * 0.28f,
                    x - bellR * 0.55f, y + bellR * 0.28f,
                    x - bellR * 0.85f, y + bellR * 0.05f
                )
                close()
            }
            drawPath(bellInner, BellCream.copy(alpha = 0.25f))

            // Bell upper highlight (warm gray, catches light)
            val bellHighlight = Path().apply {
                moveTo(x - bellR * 0.6f, y - bellR * 0.3f)
                cubicTo(
                    x - bellR * 0.5f, y - bellR * 0.85f,
                    x + bellR * 0.4f, y - bellR * 0.82f,
                    x + bellR * 0.3f, y - bellR * 0.25f
                )
                cubicTo(
                    x + bellR * 0.15f, y - bellR * 0.55f,
                    x - bellR * 0.3f, y - bellR * 0.58f,
                    x - bellR * 0.6f, y - bellR * 0.3f
                )
                close()
            }
            drawPath(bellHighlight, BellWarmGray.copy(alpha = 0.30f))

            // Specular highlight (pale teal spot)
            drawCircle(BellPaleTeal.copy(alpha = 0.20f), bellR * 0.25f,
                Offset(x - bellR * 0.25f, y - bellR * 0.55f))

            // ── GONAD RINGS (4 horseshoe shapes) — orange ────────────────────
            val gonadRadius = bellR * 0.38f
            for (g in 0 until 4) {
                val ga = g.toFloat() / 4f * 2f * PI.toFloat() + PI.toFloat() / 4f
                val gx = x + cos(ga) * gonadRadius * 0.55f
                val gy = y - bellR * 0.30f + sin(ga) * gonadRadius * 0.4f
                val gr = gonadRadius * 0.4f

                // Shadow beneath gonad
                val gonadShadow = Path().apply {
                    moveTo(gx - gr * 1.1f, gy + gr * 0.1f)
                    cubicTo(gx - gr * 1.1f, gy - gr * 1.0f, gx + gr * 1.1f, gy - gr * 1.0f, gx + gr * 1.1f, gy + gr * 0.1f)
                    cubicTo(gx + gr * 0.9f, gy + gr * 0.6f, gx - gr * 0.9f, gy + gr * 0.6f, gx - gr * 1.1f, gy + gr * 0.1f)
                    close()
                }
                drawPath(gonadShadow, ShadowMedium.copy(alpha = 0.20f))

                // Gonad base (orange)
                val gonad = Path().apply {
                    moveTo(gx - gr, gy)
                    cubicTo(gx - gr, gy - gr * 1.1f, gx + gr, gy - gr * 1.1f, gx + gr, gy)
                    cubicTo(gx + gr * 0.8f, gy + gr * 0.5f, gx - gr * 0.8f, gy + gr * 0.5f, gx - gr, gy)
                    close()
                }
                drawPath(gonad, GonadOrange.copy(alpha = 0.60f))

                // Gonad highlight
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
            // Rim line (darker teal)
            val rim = Path().apply {
                moveTo(x - bellR, y)
                cubicTo(
                    x - bellR * 0.7f, y + bellR * 0.35f,
                    x + bellR * 0.7f, y + bellR * 0.35f,
                    x + bellR, y
                )
            }
            drawPath(rim, BellDarkTeal.copy(alpha = 0.55f),
                style = Stroke(width = (1.5f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round))

            // Outer rim glow
            drawPath(rim, BellLightTeal.copy(alpha = 0.25f),
                style = Stroke(width = (3.0f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round))

            // Bell outline (subtle)
            drawPath(bell, BellDarkTeal.copy(alpha = 0.25f),
                style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))

            // ── RADIAL CANALS (faint lines inside bell) ──────────────────────
            for (c in 0 until 8) {
                val ca = c.toFloat() / 8f * 2f * PI.toFloat()
                val innerR = bellR * 0.15f
                val outerR = bellR * 0.80f
                val cx1 = x + cos(ca) * innerR
                val cy1 = y - bellR * 0.30f + sin(ca) * innerR * 0.6f
                val cx2 = x + cos(ca) * outerR
                val cy2 = y - bellR * 0.10f + sin(ca) * outerR * 0.4f
                drawLine(
                    BellTeal.copy(alpha = 0.15f),
                    Offset(cx1, cy1), Offset(cx2, cy2),
                    strokeWidth = (0.4f * s).coerceAtLeast(0.2f),
                    cap = StrokeCap.Round
                )
            }

            // ── LEVEL 3+: Bioluminescent glow ───────────────────────────────
            if (level >= 3) {
                val glow = (sin(t * 0.7f * PI.toFloat()) * 0.5f + 0.5f) * 0.18f
                drawCircle(GonadOrange.copy(alpha = glow), bellR * 0.9f,
                    Offset(x, y - bellR * 0.3f))
            }

            // ── LEVEL 5+: Pulsing teal aura ─────────────────────────────────
            if (level >= 5) {
                val aura = (sin(t * 1.2f * PI.toFloat()) * 0.5f + 0.5f) * 0.12f
                drawPath(bell, BellLightTeal.copy(alpha = aura),
                    style = Stroke(width = (3.0f * s).coerceAtLeast(1.2f)))
            }
        }
    }
}
