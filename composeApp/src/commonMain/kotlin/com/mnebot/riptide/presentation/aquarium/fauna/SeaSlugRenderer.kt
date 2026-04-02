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
import kotlin.math.cos

// ── Palette (nudibranch electric colors) ────────────────────────────────────
private val BodyBlue       = Color(0xFF2060D0)  // electric blue body
private val BodyDeepBlue   = Color(0xFF1848A8)  // deeper blue
private val BodyLightBlue  = Color(0xFF50A0F0)  // light blue highlight
private val CerataOrange   = Color(0xFFFF8830)  // orange cerata tips
private val CerataDeep     = Color(0xFFE06820)  // deeper orange
private val CerataYellow   = Color(0xFFFFB840)  // yellow cerata base
private val PurpleAccent   = Color(0xFF9040C0)  // purple accents
private val PurpleLight    = Color(0xFFB870E0)  // light purple
private val PurpleDeep     = Color(0xFF6828A0)  // deep purple
private val RhinoWhite     = Color(0xFFF0E8FF)  // rhinophore white
private val BioGlow        = Color(0xFF40D0FF)  // bioluminescent glow
private val BioGlowPurple  = Color(0xFFA060FF)  // purple bioluminescence
private val ShadowNavy     = Color(0xFF0A1840)  // dark shadow

object SeaSlugRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyLen = (13f + level * 0.7f) * s
        val bodyH = (4f + level * 0.3f) * s

        // Gentle undulating crawl
        val crawlWave = sin(t * 1.5f * PI.toFloat()) * 0.3f * s
        val bodyUndulate = sin(t * 2.0f * PI.toFloat()) * 0.15f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = crawlWave)
            }) {

            // ── BODY SHADOW ──────────────────────────────────────────────────
            val bodyShadow = paths.obtain().apply {
                moveTo(x - bodyLen * 0.95f, y + bodyH * 0.12f)
                cubicTo(
                    x - bodyLen * 0.7f, y - bodyH * 0.72f,
                    x + bodyLen * 0.3f, y - bodyH * 0.68f + bodyUndulate,
                    x + bodyLen * 0.92f, y + bodyH * 0.18f + bodyUndulate
                )
                cubicTo(
                    x + bodyLen * 0.3f, y + bodyH * 0.72f + bodyUndulate,
                    x - bodyLen * 0.5f, y + bodyH * 0.68f,
                    x - bodyLen * 0.95f, y + bodyH * 0.12f
                )
                close()
            }
            drawPath(bodyShadow, ShadowNavy.copy(alpha = 0.18f))

            // ── MAIN BODY (elongated slug shape) ─────────────────────────────
            val body = paths.obtain().apply {
                moveTo(x - bodyLen * 0.9f, y + bodyH * 0.1f)
                cubicTo(
                    x - bodyLen * 0.7f, y - bodyH * 0.7f,
                    x + bodyLen * 0.3f, y - bodyH * 0.65f + bodyUndulate,
                    x + bodyLen * 0.9f, y + bodyH * 0.15f + bodyUndulate
                )
                cubicTo(
                    x + bodyLen * 0.3f, y + bodyH * 0.7f + bodyUndulate,
                    x - bodyLen * 0.5f, y + bodyH * 0.65f,
                    x - bodyLen * 0.9f, y + bodyH * 0.1f
                )
                close()
            }
            drawPath(body, BodyBlue)

            // Body highlight (upper)
            val bodyHL = paths.obtain().apply {
                moveTo(x - bodyLen * 0.7f, y - bodyH * 0.2f)
                cubicTo(
                    x - bodyLen * 0.5f, y - bodyH * 0.55f,
                    x + bodyLen * 0.2f, y - bodyH * 0.50f + bodyUndulate,
                    x + bodyLen * 0.6f, y - bodyH * 0.05f + bodyUndulate
                )
                cubicTo(
                    x + bodyLen * 0.15f, y - bodyH * 0.30f + bodyUndulate,
                    x - bodyLen * 0.3f, y - bodyH * 0.35f,
                    x - bodyLen * 0.7f, y - bodyH * 0.2f
                )
                close()
            }
            drawPath(bodyHL, BodyLightBlue.copy(alpha = 0.35f))

            // Purple dorsal stripe
            val dorsalStripe = paths.obtain().apply {
                moveTo(x - bodyLen * 0.75f, y - bodyH * 0.1f)
                cubicTo(
                    x - bodyLen * 0.4f, y - bodyH * 0.45f,
                    x + bodyLen * 0.1f, y - bodyH * 0.40f + bodyUndulate,
                    x + bodyLen * 0.7f, y + bodyH * 0.05f + bodyUndulate
                )
            }
            drawPath(
                dorsalStripe, PurpleAccent.copy(alpha = 0.50f),
                style = Stroke(width = (1.8f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round)
            )

            // ── CERATA (finger-like projections on back) ─────────────────────
            val cerataCount = 8 + level
            for (i in 0 until cerataCount) {
                val frac = i.toFloat() / (cerataCount - 1)
                val baseX = x - bodyLen * 0.6f + bodyLen * frac * 1.2f
                val baseUndulate = sin(t * 2.0f * PI.toFloat() + frac * 3f) * bodyH * 0.08f
                val baseY = y - bodyH * (0.3f + sin(frac * PI.toFloat()) * 0.35f) + baseUndulate

                // Cerata wave gently
                val cerataWave = sin(t * 2.5f * PI.toFloat() + i * 0.5f) * bodyH * 0.15f
                val cerataLen = bodyH * (0.5f + sin(frac * PI.toFloat()) * 0.3f)

                val cerata = paths.obtain().apply {
                    moveTo(baseX, baseY)
                    cubicTo(
                        baseX + cerataWave * 0.3f, baseY - cerataLen * 0.4f,
                        baseX + cerataWave * 0.7f, baseY - cerataLen * 0.7f,
                        baseX + cerataWave, baseY - cerataLen
                    )
                }

                // Cerata glow
                drawPath(
                    cerata, CerataYellow.copy(alpha = 0.25f),
                    style = Stroke(width = (2.5f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round)
                )
                // Main cerata stroke
                drawPath(
                    cerata, CerataOrange.copy(alpha = 0.75f),
                    style = Stroke(width = (1.5f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round)
                )
                // Tip (deeper orange)
                drawCircle(
                    CerataDeep.copy(alpha = 0.65f),
                    0.8f * s,
                    Offset(baseX + cerataWave, baseY - cerataLen)
                )
            }

            // ── PURPLE EDGE MARKINGS (along body perimeter) ──────────────────
            val markingCount = 6
            for (i in 0 until markingCount) {
                val frac = i.toFloat() / markingCount
                val mx = x - bodyLen * 0.6f + bodyLen * frac * 1.1f
                val my = y + bodyH * (0.35f + sin(frac * PI.toFloat()) * 0.25f)

                drawCircle(
                    PurpleLight.copy(alpha = 0.40f),
                    1.2f * s,
                    Offset(mx, my)
                )
                drawCircle(
                    PurpleDeep.copy(alpha = 0.30f),
                    0.7f * s,
                    Offset(mx, my)
                )
            }

            // ── RHINOPHORES (head tentacles) ─────────────────────────────────
            val headX = x - bodyLen * 0.82f
            val headY = y - bodyH * 0.15f
            val rhinoLen = bodyH * (if (level >= 3) 0.75f else 0.55f)
            val rhinoWave = sin(t * 1.8f * PI.toFloat()) * s * 0.5f

            for (side in listOf(-1f, 1f)) {
                val rBaseX = headX
                val rBaseY = headY - bodyH * 0.15f
                val rTipX = headX - rhinoLen * 0.3f + rhinoWave * 0.3f
                val rTipY = rBaseY - rhinoLen + side * rhinoLen * 0.15f

                val rhino = paths.obtain().apply {
                    moveTo(rBaseX, rBaseY)
                    cubicTo(
                        rBaseX + side * s * 0.5f, rBaseY - rhinoLen * 0.3f,
                        rTipX + side * s * 0.8f, rTipY + rhinoLen * 0.2f,
                        rTipX + side * s * 1.2f, rTipY
                    )
                }
                drawPath(
                    rhino, RhinoWhite.copy(alpha = 0.70f),
                    style = Stroke(width = (1.0f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round)
                )

                // Level 3+: rhinophores have visible lamellae texture
                if (level >= 3) {
                    for (lam in 1..3) {
                        val lamFrac = lam.toFloat() / 4f
                        val lx = rBaseX + (rTipX + side * s * 1.2f - rBaseX) * lamFrac
                        val ly = rBaseY + (rTipY - rBaseY) * lamFrac
                        drawLine(
                            PurpleLight.copy(alpha = 0.35f),
                            Offset(lx - s * 0.4f, ly),
                            Offset(lx + s * 0.4f, ly),
                            strokeWidth = (0.5f * s).coerceAtLeast(0.25f),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // ── TINY EYES ────────────────────────────────────────────────────
            val eyeR = 0.8f * s
            drawCircle(ShadowNavy.copy(alpha = 0.40f), eyeR, Offset(headX + s * 0.3f, headY - bodyH * 0.22f))
            drawCircle(Color.White, eyeR * 0.45f, Offset(headX + s * 0.1f, headY - bodyH * 0.25f))

            // ── FOOT FRINGE (bottom edge detail) ─────────────────────────────
            val footFringe = paths.obtain().apply {
                moveTo(x - bodyLen * 0.80f, y + bodyH * 0.25f)
                cubicTo(
                    x - bodyLen * 0.4f, y + bodyH * 0.55f,
                    x + bodyLen * 0.2f, y + bodyH * 0.52f + bodyUndulate,
                    x + bodyLen * 0.75f, y + bodyH * 0.20f + bodyUndulate
                )
            }
            drawPath(
                footFringe, BodyDeepBlue.copy(alpha = 0.35f),
                style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round)
            )

            // ── LEVEL 5+: Bioluminescent trail ──────────────────────────────
            if (level >= 5) {
                val trailCount = 6
                for (i in 0 until trailCount) {
                    val trailFrac = i.toFloat() / trailCount
                    val trailAlpha = (sin(t * 1.5f * PI.toFloat() + i * 0.8f) * 0.5f + 0.5f) * 0.25f
                    val trailX = x + bodyLen * (0.5f + trailFrac * 0.6f)
                    val trailY = y + bodyH * 0.1f + sin(t * 2f * PI.toFloat() + i * 0.5f) * bodyH * 0.2f
                    val trailR = (1.5f - trailFrac * 0.8f) * s

                    drawCircle(BioGlow.copy(alpha = trailAlpha), trailR * 2.5f, Offset(trailX, trailY))
                    drawCircle(BioGlowPurple.copy(alpha = trailAlpha * 0.7f), trailR, Offset(trailX, trailY))
                }
            }

            } // crawl transform
        }
    }
}
