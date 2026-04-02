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
import kotlin.math.cos
import kotlin.math.sin

// ── Palette ──────────────────────────────────────────────────────────────────
private val BodyOrange     = Color(0xFFE8872E)  // warm orange body
private val BodyAmber      = Color(0xFFC86E1A)  // darker amber shading
private val BodyDarkBrown  = Color(0xFF8A4E12)  // deep brown shadow
private val BodyPaleOrange = Color(0xFFFAB86A)  // pale highlight
private val BodyGold       = Color(0xFFFFCC55)  // gold accents
private val BellyYellow    = Color(0xFFFEE08C)  // pale belly yellow
private val RidgeBrown     = Color(0xFF6A3A0A)  // body ridge dark
private val SnoutTan       = Color(0xFFD09840)  // elongated snout color
private val ShadowNavy     = Color(0xFF08082A)  // dark shadow
private val ShadowBrown    = Color(0xFF3A2008)  // warm shadow
private val CoronetGold    = Color(0xFFFFD740)  // coronet gold (level 3+)
private val BioGlow        = Color(0xFF80E0C0)  // bioluminescent glow (level 5+)

object SeahorseRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        // Seahorse is vertical: body height is primary dimension
        val bodyH   = (14f + level * 0.8f) * s
        val bodyW   = (5f + level * 0.3f) * s
        val snoutL  = (4f + level * 0.2f) * s
        val tailLen = (7f + level * 0.5f) * s

        // Gentle sway + dorsal fin flutter
        val swayX     = sin(t * 1.8f * PI.toFloat()) * s * 0.5f
        val dorsalFlut = sin(t * 8.0f * PI.toFloat()) * 0.4f * s  // rapid flutter

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = swayX, top = 0f)
            }) {

            // ── CURLED TAIL (spiral below body) ─────────────────────────────
            val tailBaseY = y + bodyH * 0.35f
            val tailCurl  = sin(t * 1.2f * PI.toFloat()) * 0.4f * s

            val tailPath = paths.obtain().apply {
                moveTo(x, tailBaseY)
                cubicTo(
                    x + bodyW * 0.3f + tailCurl, tailBaseY + tailLen * 0.25f,
                    x + bodyW * 0.8f + tailCurl, tailBaseY + tailLen * 0.45f,
                    x + bodyW * 0.6f, tailBaseY + tailLen * 0.6f
                )
                cubicTo(
                    x + bodyW * 0.2f, tailBaseY + tailLen * 0.72f,
                    x - bodyW * 0.2f - tailCurl, tailBaseY + tailLen * 0.82f,
                    x - bodyW * 0.1f, tailBaseY + tailLen * 0.92f
                )
                cubicTo(
                    x + bodyW * 0.1f, tailBaseY + tailLen,
                    x + bodyW * 0.3f, tailBaseY + tailLen * 0.95f,
                    x + bodyW * 0.2f, tailBaseY + tailLen * 0.88f
                )
            }
            // Tail shadow
            drawPath(tailPath, ShadowBrown.copy(alpha = 0.30f),
                style = Stroke(width = (3.5f * s).coerceAtLeast(1.5f), cap = StrokeCap.Round))
            // Tail main
            drawPath(tailPath, BodyAmber.copy(alpha = 0.85f),
                style = Stroke(width = (2.2f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round))
            // Tail highlight
            drawPath(tailPath, BodyPaleOrange.copy(alpha = 0.35f),
                style = Stroke(width = (1.0f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round))

            // Tail ridges (segments)
            for (i in 0 until 5) {
                val frac = 0.1f + i * 0.17f
                val segX = x + sin(frac * 3.5f + tailCurl * 0.3f) * bodyW * 0.3f
                val segY = tailBaseY + tailLen * frac
                drawLine(
                    RidgeBrown.copy(alpha = 0.30f),
                    Offset(segX - bodyW * 0.25f, segY),
                    Offset(segX + bodyW * 0.25f, segY),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.25f)
                )
            }

            // ── DORSAL FIN (rapid flutter) ──────────────────────────────────
            val dorsalY = y - bodyH * 0.1f
            val dorsal = paths.obtain().apply {
                moveTo(x + bodyW * 0.35f, dorsalY - bodyH * 0.12f)
                cubicTo(
                    x + bodyW * 0.75f + dorsalFlut, dorsalY - bodyH * 0.08f,
                    x + bodyW * 0.80f + dorsalFlut, dorsalY + bodyH * 0.08f,
                    x + bodyW * 0.35f, dorsalY + bodyH * 0.12f
                )
                cubicTo(
                    x + bodyW * 0.55f, dorsalY + bodyH * 0.05f,
                    x + bodyW * 0.55f, dorsalY - bodyH * 0.05f,
                    x + bodyW * 0.35f, dorsalY - bodyH * 0.12f
                )
                close()
            }
            drawPath(dorsal, BodyPaleOrange.copy(alpha = 0.55f))
            // Fin membrane lines
            for (i in 0 until 4) {
                val fy = dorsalY - bodyH * 0.08f + i * bodyH * 0.05f
                drawLine(
                    BodyAmber.copy(alpha = 0.30f),
                    Offset(x + bodyW * 0.38f, fy),
                    Offset(x + bodyW * 0.70f + dorsalFlut * 0.5f, fy),
                    strokeWidth = (0.3f * s).coerceAtLeast(0.15f)
                )
            }

            // ── MAIN BODY (S-curved torso) ──────────────────────────────────
            val bodyShadow = paths.obtain().apply {
                moveTo(x - bodyW * 0.42f, y - bodyH * 0.42f)
                cubicTo(
                    x - bodyW * 0.55f, y - bodyH * 0.15f,
                    x - bodyW * 0.50f, y + bodyH * 0.15f,
                    x - bodyW * 0.20f, y + bodyH * 0.38f
                )
                lineTo(x + bodyW * 0.25f, y + bodyH * 0.38f)
                cubicTo(
                    x + bodyW * 0.55f, y + bodyH * 0.15f,
                    x + bodyW * 0.50f, y - bodyH * 0.15f,
                    x + bodyW * 0.42f, y - bodyH * 0.42f
                )
                close()
            }
            drawPath(bodyShadow, ShadowBrown.copy(alpha = 0.20f))

            val bodyPath = paths.obtain().apply {
                moveTo(x - bodyW * 0.40f, y - bodyH * 0.40f)
                cubicTo(
                    x - bodyW * 0.52f, y - bodyH * 0.12f,
                    x - bodyW * 0.48f, y + bodyH * 0.12f,
                    x - bodyW * 0.18f, y + bodyH * 0.36f
                )
                lineTo(x + bodyW * 0.22f, y + bodyH * 0.36f)
                cubicTo(
                    x + bodyW * 0.52f, y + bodyH * 0.12f,
                    x + bodyW * 0.48f, y - bodyH * 0.12f,
                    x + bodyW * 0.40f, y - bodyH * 0.40f
                )
                close()
            }
            drawPath(bodyPath, BodyOrange)

            // Belly lighter zone
            val bellyPath = paths.obtain().apply {
                moveTo(x - bodyW * 0.30f, y - bodyH * 0.10f)
                cubicTo(
                    x - bodyW * 0.35f, y + bodyH * 0.10f,
                    x - bodyW * 0.28f, y + bodyH * 0.30f,
                    x, y + bodyH * 0.34f
                )
                cubicTo(
                    x + bodyW * 0.15f, y + bodyH * 0.25f,
                    x + bodyW * 0.10f, y + bodyH * 0.05f,
                    x - bodyW * 0.30f, y - bodyH * 0.10f
                )
                close()
            }
            drawPath(bellyPath, BellyYellow.copy(alpha = 0.35f))

            // Body ridges (horizontal bony segments)
            for (i in 0 until 8) {
                val ry = y - bodyH * 0.35f + i * bodyH * 0.09f
                val ridgeW = bodyW * (0.35f + 0.1f * sin(i.toFloat() * 1.2f))
                drawLine(
                    RidgeBrown.copy(alpha = 0.25f),
                    Offset(x - ridgeW, ry),
                    Offset(x + ridgeW, ry),
                    strokeWidth = (0.6f * s).coerceAtLeast(0.25f)
                )
            }

            // Upper body highlight
            val upperHL = paths.obtain().apply {
                moveTo(x - bodyW * 0.25f, y - bodyH * 0.38f)
                cubicTo(
                    x - bodyW * 0.15f, y - bodyH * 0.30f,
                    x + bodyW * 0.15f, y - bodyH * 0.28f,
                    x + bodyW * 0.35f, y - bodyH * 0.38f
                )
                cubicTo(
                    x + bodyW * 0.20f, y - bodyH * 0.32f,
                    x - bodyW * 0.05f, y - bodyH * 0.33f,
                    x - bodyW * 0.25f, y - bodyH * 0.38f
                )
                close()
            }
            drawPath(upperHL, BodyPaleOrange.copy(alpha = 0.35f))

            // ── HEAD (rounded, tilted forward) ──────────────────────────────
            val headCx = x - bodyW * 0.10f
            val headCy = y - bodyH * 0.48f
            val headR  = bodyW * 0.48f

            // Head shadow
            drawCircle(ShadowBrown.copy(alpha = 0.20f), headR * 1.08f, Offset(headCx + s * 0.3f, headCy + s * 0.3f))
            // Head main
            drawCircle(BodyOrange, headR, Offset(headCx, headCy))
            // Head highlight
            drawCircle(BodyPaleOrange.copy(alpha = 0.30f), headR * 0.55f,
                Offset(headCx - headR * 0.15f, headCy - headR * 0.20f))

            // ── SNOUT (elongated tubular mouth) ─────────────────────────────
            val snoutPath = paths.obtain().apply {
                moveTo(headCx - headR * 0.75f, headCy + headR * 0.05f)
                cubicTo(
                    headCx - headR * 0.85f - snoutL * 0.3f, headCy - headR * 0.15f,
                    headCx - headR * 0.85f - snoutL * 0.7f, headCy - headR * 0.10f,
                    headCx - headR * 0.85f - snoutL, headCy
                )
                cubicTo(
                    headCx - headR * 0.85f - snoutL * 0.7f, headCy + headR * 0.10f,
                    headCx - headR * 0.85f - snoutL * 0.3f, headCy + headR * 0.18f,
                    headCx - headR * 0.75f, headCy + headR * 0.15f
                )
                close()
            }
            drawPath(snoutPath, SnoutTan)
            drawPath(snoutPath, BodyDarkBrown.copy(alpha = 0.15f),
                style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))

            // Tiny mouth opening at tip
            drawCircle(ShadowNavy.copy(alpha = 0.5f), s * 0.4f,
                Offset(headCx - headR * 0.85f - snoutL * 0.95f, headCy))

            // ── CORONET (level 3+: crown-like bump on top of head) ──────────
            if (level >= 3) {
                val crownBase = headCy - headR * 0.85f
                val crownH = headR * 0.65f
                val crown = paths.obtain().apply {
                    moveTo(headCx - headR * 0.3f, crownBase)
                    // Five crown points
                    lineTo(headCx - headR * 0.25f, crownBase - crownH * 0.5f)
                    lineTo(headCx - headR * 0.12f, crownBase - crownH * 0.2f)
                    lineTo(headCx, crownBase - crownH)
                    lineTo(headCx + headR * 0.12f, crownBase - crownH * 0.2f)
                    lineTo(headCx + headR * 0.25f, crownBase - crownH * 0.5f)
                    lineTo(headCx + headR * 0.3f, crownBase)
                    close()
                }
                drawPath(crown, CoronetGold.copy(alpha = 0.75f))
                drawPath(crown, BodyGold.copy(alpha = 0.40f))
                drawPath(crown, BodyDarkBrown.copy(alpha = 0.20f),
                    style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))
            }

            // ── EYE ─────────────────────────────────────────────────────────
            val eyeR = (1.3f + level * 0.08f) * s
            val eyeX = headCx - headR * 0.25f
            val eyeY = headCy - headR * 0.10f

            drawCircle(ShadowNavy.copy(alpha = 0.30f), eyeR * 1.2f, Offset(eyeX, eyeY))
            drawCircle(Color(0xFFF0E8D0), eyeR, Offset(eyeX, eyeY))
            drawCircle(Color(0xFF4A3018), eyeR * 0.55f, Offset(eyeX + eyeR * 0.05f, eyeY))
            drawCircle(ShadowNavy, eyeR * 0.32f, Offset(eyeX + eyeR * 0.07f, eyeY + eyeR * 0.02f))
            drawCircle(Color.White, eyeR * 0.20f, Offset(eyeX - eyeR * 0.18f, eyeY - eyeR * 0.15f))
            drawCircle(Color.White.copy(alpha = 0.5f), eyeR * 0.09f, Offset(eyeX + eyeR * 0.14f, eyeY + eyeR * 0.10f))

            // ── LEVEL 5+: Bioluminescent belly dots ─────────────────────────
            if (level >= 5) {
                val dotPhase = t * 1.5f * PI.toFloat()
                for (i in 0 until 6) {
                    val dotY = y - bodyH * 0.15f + i * bodyH * 0.10f
                    val dotX = x - bodyW * 0.15f + sin(i.toFloat() * 0.8f) * bodyW * 0.08f
                    val dotAlpha = (sin(dotPhase + i * 0.5f) * 0.5f + 0.5f) * 0.55f
                    drawCircle(BioGlow.copy(alpha = dotAlpha), s * 0.7f, Offset(dotX, dotY))
                    drawCircle(BioGlow.copy(alpha = dotAlpha * 0.35f), s * 1.4f, Offset(dotX, dotY))
                }
            }

            } // sway transform
        }
    }
}
