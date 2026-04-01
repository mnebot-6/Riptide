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

// ── Palette ─────────────────────────────────────────────────────────────────
private val BodyDarkBrown   = Color(0xFF5A3028)  // dark brown body
private val BodyMaroon      = Color(0xFF7A3838)  // maroon mid-tone
private val BodyBrown       = Color(0xFF8B5A40)  // lighter brown
private val BodyLight       = Color(0xFFA07058)  // highlight brown
private val BumpDark        = Color(0xFF402020)  // dark bump papillae
private val BumpLight       = Color(0xFF6A4838)  // lighter bump
private val TubeFootBeige   = Color(0xFFD0B090)  // tube feet color
private val TubeFootDark    = Color(0xFFA08060)  // tube feet shadow
private val BellyTan        = Color(0xFFCBA880)  // underside tan
private val ThreadWhite     = Color(0xFFF0E8E0)  // evisceration threads
private val ThreadGlow      = Color(0xFFFFE8D0)  // thread glow
private val ShadowDark      = Color(0xFF301818)  // dark shadow
private val MouthPink       = Color(0xFFD09090)  // mouth tentacles

object SeaCucumberRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyLen = (14f + level * 0.8f) * s
        val bodyH = (5f + level * 0.35f) * s

        // Slow breathing pulse
        val breathe = sin(t * 0.8f * PI.toFloat()) * 0.04f
        val bodyHDyn = bodyH * (1f + breathe)
        // Very slow lateral undulation
        val crawlWave = sin(t * 0.5f * PI.toFloat()) * 0.2f * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── GROUND SHADOW ────────────────────────────────────────────────
            drawOval(
                ShadowDark.copy(alpha = 0.18f),
                topLeft = Offset(x - bodyLen * 0.85f, y + bodyHDyn * 0.35f),
                size = androidx.compose.ui.geometry.Size(bodyLen * 1.7f, bodyHDyn * 0.2f)
            )

            // ── TUBE FEET (small protrusions along underside) ────────────────
            val feetCount = 10
            for (i in 0 until feetCount) {
                val frac = i.toFloat() / (feetCount - 1)
                val fx = x - bodyLen * 0.75f + bodyLen * frac * 1.5f
                val footWave = sin(t * 1.2f * PI.toFloat() + i * 0.4f) * s * 0.2f
                val fy = y + bodyHDyn * 0.3f
                val footLen = bodyHDyn * 0.15f

                drawLine(
                    TubeFootBeige.copy(alpha = 0.55f),
                    Offset(fx + footWave * 0.2f, fy),
                    Offset(fx + footWave, fy + footLen),
                    strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                    cap = StrokeCap.Round
                )
                drawCircle(TubeFootDark.copy(alpha = 0.40f), 0.3f * s, Offset(fx + footWave, fy + footLen))
            }

            // ── BODY SHADOW ──────────────────────────────────────────────────
            val bodyShadow = Path().apply {
                moveTo(x - bodyLen * 0.82f, y + crawlWave * 0.3f)
                cubicTo(
                    x - bodyLen * 0.7f, y - bodyHDyn * 0.62f,
                    x + bodyLen * 0.3f, y - bodyHDyn * 0.58f + crawlWave,
                    x + bodyLen * 0.82f, y + bodyHDyn * 0.05f + crawlWave
                )
                cubicTo(
                    x + bodyLen * 0.3f, y + bodyHDyn * 0.52f + crawlWave,
                    x - bodyLen * 0.5f, y + bodyHDyn * 0.48f,
                    x - bodyLen * 0.82f, y + crawlWave * 0.3f
                )
                close()
            }
            drawPath(bodyShadow, ShadowDark.copy(alpha = 0.20f))

            // ── MAIN BODY (plump cylindrical) ────────────────────────────────
            val body = Path().apply {
                moveTo(x - bodyLen * 0.8f, y + crawlWave * 0.3f)
                cubicTo(
                    x - bodyLen * 0.68f, y - bodyHDyn * 0.6f,
                    x + bodyLen * 0.28f, y - bodyHDyn * 0.55f + crawlWave,
                    x + bodyLen * 0.8f, y + bodyHDyn * 0.02f + crawlWave
                )
                cubicTo(
                    x + bodyLen * 0.28f, y + bodyHDyn * 0.5f + crawlWave,
                    x - bodyLen * 0.48f, y + bodyHDyn * 0.45f,
                    x - bodyLen * 0.8f, y + crawlWave * 0.3f
                )
                close()
            }
            drawPath(body, BodyMaroon)

            // Upper highlight
            val bodyHL = Path().apply {
                moveTo(x - bodyLen * 0.6f, y - bodyHDyn * 0.15f)
                cubicTo(
                    x - bodyLen * 0.4f, y - bodyHDyn * 0.48f,
                    x + bodyLen * 0.15f, y - bodyHDyn * 0.44f + crawlWave,
                    x + bodyLen * 0.55f, y - bodyHDyn * 0.05f + crawlWave
                )
                cubicTo(
                    x + bodyLen * 0.15f, y - bodyHDyn * 0.25f + crawlWave,
                    x - bodyLen * 0.25f, y - bodyHDyn * 0.28f,
                    x - bodyLen * 0.6f, y - bodyHDyn * 0.15f
                )
                close()
            }
            drawPath(bodyHL, BodyLight.copy(alpha = 0.35f))

            // Belly counter-shading
            val belly = Path().apply {
                moveTo(x - bodyLen * 0.7f, y + bodyHDyn * 0.08f)
                cubicTo(
                    x - bodyLen * 0.4f, y + bodyHDyn * 0.42f,
                    x + bodyLen * 0.2f, y + bodyHDyn * 0.40f + crawlWave,
                    x + bodyLen * 0.7f, y + bodyHDyn * 0.06f + crawlWave
                )
                cubicTo(
                    x + bodyLen * 0.2f, y + bodyHDyn * 0.28f + crawlWave,
                    x - bodyLen * 0.3f, y + bodyHDyn * 0.26f,
                    x - bodyLen * 0.7f, y + bodyHDyn * 0.08f
                )
                close()
            }
            drawPath(belly, BellyTan.copy(alpha = 0.25f))

            // ── BUMPY TEXTURE (papillae) ─────────────────────────────────────
            val bumpRows = 3
            val bumpsPerRow = 8 + (if (level >= 3) 4 else 0)
            for (row in 0 until bumpRows) {
                val rowFrac = row.toFloat() / bumpRows
                for (i in 0 until bumpsPerRow) {
                    val frac = i.toFloat() / bumpsPerRow
                    val bx = x - bodyLen * 0.65f + bodyLen * frac * 1.3f
                    val rowOffset = (rowFrac - 0.5f) * bodyHDyn * 0.6f
                    val by = y - bodyHDyn * 0.2f + rowOffset +
                             sin(frac * PI.toFloat()) * rowOffset * 0.3f +
                             crawlWave * frac
                    val bumpR = (0.6f + rowFrac * 0.2f) * s

                    // Level 3+: more defined bumps
                    val bumpAlpha = if (level >= 3) 0.35f else 0.22f

                    drawCircle(BumpDark.copy(alpha = bumpAlpha), bumpR, Offset(bx, by))
                    drawCircle(BumpLight.copy(alpha = bumpAlpha * 0.5f), bumpR * 0.5f,
                        Offset(bx - bumpR * 0.2f, by - bumpR * 0.2f))
                }
            }

            // ── BODY OUTLINE ─────────────────────────────────────────────────
            drawPath(
                body, BodyDarkBrown.copy(alpha = 0.25f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.25f))
            )

            // ── LATERAL LINE DETAIL ──────────────────────────────────────────
            drawLine(
                BodyDarkBrown.copy(alpha = 0.20f),
                Offset(x - bodyLen * 0.65f, y + bodyHDyn * 0.02f),
                Offset(x + bodyLen * 0.65f, y + bodyHDyn * 0.02f + crawlWave),
                strokeWidth = (0.6f * s).coerceAtLeast(0.3f),
                cap = StrokeCap.Round
            )

            // ── MOUTH / ORAL TENTACLES (front end) ──────────────────────────
            val mouthX = x - bodyLen * 0.8f
            val mouthY = y + crawlWave * 0.3f
            val tentCount = 5
            for (i in 0 until tentCount) {
                val tentAngle = (-0.8f + i * 0.4f)
                val tentWave = sin(t * 1.5f * PI.toFloat() + i * 0.6f) * s * 0.6f
                val tentLen = bodyHDyn * 0.35f

                val tentacle = Path().apply {
                    moveTo(mouthX, mouthY)
                    cubicTo(
                        mouthX - tentLen * 0.3f + tentWave * 0.3f,
                        mouthY + sin(tentAngle) * tentLen * 0.3f,
                        mouthX - tentLen * 0.7f + tentWave * 0.7f,
                        mouthY + sin(tentAngle) * tentLen * 0.6f,
                        mouthX - tentLen + tentWave,
                        mouthY + sin(tentAngle) * tentLen
                    )
                }
                drawPath(
                    tentacle, MouthPink.copy(alpha = 0.55f),
                    style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round)
                )
            }

            // ── REAR END (tapered) ──────────────────────────────────────────
            val rearX = x + bodyLen * 0.78f
            val rearY = y + bodyHDyn * 0.02f + crawlWave
            drawCircle(BodyDarkBrown.copy(alpha = 0.25f), bodyHDyn * 0.15f, Offset(rearX, rearY))

            // ── LEVEL 5+: Evisceration defense display (white threads) ──────
            if (level >= 5) {
                val evisPhase = (sin(t * 0.6f * PI.toFloat()) * 0.5f + 0.5f)
                val evisAlpha = evisPhase * 0.45f
                val threadCount = 7

                for (i in 0 until threadCount) {
                    val threadAngle = 0.5f + i * 0.25f
                    val threadWave = sin(t * 1.8f * PI.toFloat() + i * 0.5f) * s * 2.0f
                    val threadLen = bodyLen * 0.3f * (0.6f + evisPhase * 0.4f)

                    val thread = Path().apply {
                        moveTo(rearX, rearY)
                        cubicTo(
                            rearX + threadLen * 0.3f + threadWave * 0.2f,
                            rearY + sin(threadAngle) * threadLen * 0.2f,
                            rearX + threadLen * 0.6f + threadWave * 0.6f,
                            rearY + sin(threadAngle) * threadLen * 0.5f,
                            rearX + threadLen + threadWave,
                            rearY + sin(threadAngle) * threadLen * 0.8f
                        )
                    }

                    drawPath(
                        thread, ThreadGlow.copy(alpha = evisAlpha * 0.3f),
                        style = Stroke(width = (2.0f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round)
                    )
                    drawPath(
                        thread, ThreadWhite.copy(alpha = evisAlpha),
                        style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
