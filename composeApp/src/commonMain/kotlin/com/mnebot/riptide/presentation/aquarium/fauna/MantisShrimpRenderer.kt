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

// ── Palette (rainbow mantis shrimp) ─────────────────────────────────────────
private val HeadGreen      = Color(0xFF2AAA4A)  // vivid green head/carapace
private val HeadDarkGreen  = Color(0xFF188838)  // darker green shading
private val SegGreen       = Color(0xFF30BB55)  // green segments
private val SegBlue        = Color(0xFF2080D0)  // blue segment band
private val SegDarkBlue    = Color(0xFF1858A0)  // dark blue
private val SegOrange      = Color(0xFFFF8830)  // orange segment
private val SegRed         = Color(0xFFE04030)  // red segment
private val SegDeepRed     = Color(0xFFB82820)  // deeper red
private val ClubRed        = Color(0xFFDD3020)  // striking club red
private val ClubOrange     = Color(0xFFFF6020)  // club orange-red
private val TailBlue       = Color(0xFF3070C0)  // tail fan blue
private val TailTeal       = Color(0xFF20A0A0)  // tail teal edge
private val EyeStalk       = Color(0xFF30AA50)  // eye stalk green
private val EyeBand        = Color(0xFFFF6030)  // eye band (trinocular strip)
private val BellyPale      = Color(0xFFE8E0C0)  // pale belly
private val ShadowNavy     = Color(0xFF080820)  // dark shadow
private val ShadowGreen    = Color(0xFF0A3A18)  // green shadow
private val RainbowShimmer = Color(0xFFFF60FF)  // rainbow pulse (level 5+)

object MantisShrimpRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyLen = (14f + level * 0.8f) * s
        val bodyH   = (4f + level * 0.3f) * s
        val clubLen = (4f + level * 0.3f) * s

        // Club strike: periodic fast strike every 3 seconds
        val strikeCycle = (animTimeMs % 3000L).toFloat() / 1000f
        val strikeBlend = if (strikeCycle < 0.15f) (strikeCycle / 0.15f) else
                          if (strikeCycle < 0.30f) (1f - (strikeCycle - 0.15f) / 0.15f) else 0f
        val strikeExt = strikeBlend * clubLen * 0.8f

        // Body sway
        val bodySway = sin(t * 2.5f * PI.toFloat()) * s * 0.3f

        // Color vividity multiplier for level 3+
        val vividMult = if (level >= 3) 1.15f else 1.0f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = bodySway)
            }) {

            // ── TAIL FAN (uropods) ──────────────────────────────────────────
            val tailX = x + bodyLen * 0.45f
            val tailSway = sin(t * 2.0f * PI.toFloat()) * s * 0.5f

            val tailFan = paths.obtain().apply {
                moveTo(tailX - bodyLen * 0.05f, y)
                cubicTo(
                    tailX + bodyLen * 0.1f, y - bodyH * 1.1f + tailSway,
                    tailX + bodyLen * 0.18f, y - bodyH * 0.8f + tailSway,
                    tailX + bodyLen * 0.15f, y - bodyH * 0.2f + tailSway * 0.3f
                )
                cubicTo(
                    tailX + bodyLen * 0.20f, y + tailSway * 0.2f,
                    tailX + bodyLen * 0.18f, y + bodyH * 0.8f + tailSway,
                    tailX + bodyLen * 0.1f, y + bodyH * 1.1f + tailSway
                )
                close()
            }
            drawPath(tailFan, TailBlue.copy(alpha = 0.75f))
            // Teal edge on tail fan
            drawPath(tailFan, TailTeal.copy(alpha = 0.30f),
                style = Stroke(width = (1.2f * s).coerceAtLeast(0.5f)))
            // Orange spots on tail fan
            drawCircle(SegOrange.copy(alpha = 0.45f), bodyH * 0.25f,
                Offset(tailX + bodyLen * 0.08f, y - bodyH * 0.4f + tailSway * 0.5f))
            drawCircle(SegOrange.copy(alpha = 0.45f), bodyH * 0.25f,
                Offset(tailX + bodyLen * 0.08f, y + bodyH * 0.4f + tailSway * 0.5f))

            // ── WALKING LEGS (underneath body) ──────────────────────────────
            for (i in 0 until 4) {
                val legX = x - bodyLen * 0.15f + i * bodyLen * 0.15f
                val legPhase = t * 4.0f * PI.toFloat() + i * 0.9f
                val legSwing = sin(legPhase) * s * 0.8f
                for (side in intArrayOf(-1, 1)) {
                    drawLine(
                        HeadDarkGreen.copy(alpha = 0.45f),
                        Offset(legX, y + side * bodyH * 0.3f),
                        Offset(legX + legSwing, y + side * (bodyH * 0.8f + s)),
                        strokeWidth = (0.4f * s).coerceAtLeast(0.2f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // ── SEGMENTED BODY (rainbow colored bands) ──────────────────────
            val segColors = listOf(
                SegDeepRed, SegRed, SegOrange, SegGreen, SegBlue, SegDarkBlue, SegGreen
            )
            val segCount = segColors.size

            for (i in 0 until segCount) {
                val segFrac = i.toFloat() / segCount
                val nextFrac = (i + 1).toFloat() / segCount
                val segStartX = x - bodyLen * 0.35f + segFrac * bodyLen * 0.75f
                val segEndX   = x - bodyLen * 0.35f + nextFrac * bodyLen * 0.75f
                val taper = (1f - (segFrac - 0.4f) * (segFrac - 0.4f) * 2.2f).coerceIn(0.5f, 1f)
                val halfH = bodyH * taper

                val seg = paths.obtain().apply {
                    moveTo(segStartX, y - halfH)
                    lineTo(segEndX, y - halfH * 0.98f)
                    lineTo(segEndX, y + halfH * 0.98f)
                    lineTo(segStartX, y + halfH)
                    close()
                }

                val baseColor = segColors[i]
                val alpha = if (vividMult > 1f) 0.85f else 0.70f
                drawPath(seg, baseColor.copy(alpha = alpha))

                // Segment boundary line
                drawLine(
                    ShadowGreen.copy(alpha = 0.20f),
                    Offset(segEndX, y - halfH),
                    Offset(segEndX, y + halfH),
                    strokeWidth = (0.3f * s).coerceAtLeast(0.15f)
                )
            }

            // Body highlight strip
            drawLine(
                BellyPale.copy(alpha = 0.20f),
                Offset(x - bodyLen * 0.30f, y - bodyH * 0.45f),
                Offset(x + bodyLen * 0.35f, y - bodyH * 0.40f),
                strokeWidth = (1.0f * s).coerceAtLeast(0.5f),
                cap = StrokeCap.Round
            )

            // ── CARAPACE / HEAD ─────────────────────────────────────────────
            val headX = x - bodyLen * 0.35f
            val headW = bodyLen * 0.25f
            val headPath = paths.obtain().apply {
                moveTo(headX, y - bodyH * 0.85f)
                cubicTo(
                    headX - headW * 0.7f, y - bodyH * 0.75f,
                    headX - headW * 0.7f, y + bodyH * 0.75f,
                    headX, y + bodyH * 0.85f
                )
                cubicTo(
                    headX + headW * 0.3f, y + bodyH * 0.80f,
                    headX + headW * 0.3f, y - bodyH * 0.80f,
                    headX, y - bodyH * 0.85f
                )
                close()
            }
            drawPath(headPath, HeadGreen)
            drawPath(headPath, HeadDarkGreen.copy(alpha = 0.25f))

            // Head highlight
            val headHL = paths.obtain().apply {
                moveTo(headX - headW * 0.3f, y - bodyH * 0.40f)
                cubicTo(
                    headX - headW * 0.45f, y - bodyH * 0.60f,
                    headX - headW * 0.15f, y - bodyH * 0.58f,
                    headX + headW * 0.1f, y - bodyH * 0.35f
                )
                close()
            }
            drawPath(headHL, SegGreen.copy(alpha = 0.35f))

            // ── RAPTORIAL APPENDAGES (striking clubs) ───────────────────────
            for (side in intArrayOf(-1, 1)) {
                val clubBaseX = headX - headW * 0.3f
                val clubBaseY = y + side * bodyH * 0.25f

                // Folded arm segment
                val armElbow = paths.obtain().apply {
                    moveTo(clubBaseX, clubBaseY)
                    cubicTo(
                        clubBaseX - clubLen * 0.3f, clubBaseY + side * clubLen * 0.15f,
                        clubBaseX - clubLen * 0.5f, clubBaseY - side * clubLen * 0.1f,
                        clubBaseX - clubLen * 0.4f - strikeExt * 0.5f, clubBaseY - side * clubLen * 0.3f
                    )
                }
                drawPath(armElbow, HeadGreen.copy(alpha = 0.70f),
                    style = Stroke(width = (1.5f * s).coerceAtLeast(0.7f), cap = StrokeCap.Round))

                // Club (strike end)
                val clubTipX = clubBaseX - clubLen * 0.5f - strikeExt
                val clubTipY = clubBaseY - side * clubLen * 0.35f
                val clubR = bodyH * 0.22f + strikeExt * 0.05f

                // Club shadow
                drawCircle(ShadowNavy.copy(alpha = 0.25f), clubR * 1.15f,
                    Offset(clubTipX + s * 0.2f, clubTipY + s * 0.2f))
                // Club main
                drawCircle(ClubRed, clubR, Offset(clubTipX, clubTipY))
                // Club highlight
                drawCircle(ClubOrange.copy(alpha = 0.60f), clubR * 0.55f,
                    Offset(clubTipX - clubR * 0.15f, clubTipY - clubR * 0.15f))

                // Strike flash
                if (strikeBlend > 0.3f) {
                    drawCircle(Color.White.copy(alpha = strikeBlend * 0.35f),
                        clubR * 2.5f, Offset(clubTipX, clubTipY))
                }
            }

            // ── EYES (stalked, compound, with trinocular band) ──────────────
            val eyeR = (1.4f + level * 0.08f) * s
            for (side in intArrayOf(-1, 1)) {
                val stalkBaseX = headX - headW * 0.4f
                val stalkBaseY = y + side * bodyH * 0.55f
                val stalkTipX = stalkBaseX - headW * 0.4f
                val stalkTipY = stalkBaseY + side * bodyH * 0.25f

                // Eye stalk
                drawLine(
                    EyeStalk, Offset(stalkBaseX, stalkBaseY), Offset(stalkTipX, stalkTipY),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round
                )

                // Compound eye
                drawCircle(ShadowNavy.copy(alpha = 0.25f), eyeR * 1.15f,
                    Offset(stalkTipX, stalkTipY))
                drawCircle(HeadGreen, eyeR, Offset(stalkTipX, stalkTipY))
                // Trinocular orange band (characteristic)
                drawLine(
                    EyeBand, Offset(stalkTipX - eyeR * 0.7f, stalkTipY),
                    Offset(stalkTipX + eyeR * 0.7f, stalkTipY),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.4f)
                )
                // Pupil
                drawCircle(ShadowNavy, eyeR * 0.35f,
                    Offset(stalkTipX - eyeR * 0.1f, stalkTipY))
                // Shine
                drawCircle(Color.White, eyeR * 0.18f,
                    Offset(stalkTipX - eyeR * 0.25f, stalkTipY - eyeR * 0.18f))
            }

            // ── ANTENNAE ────────────────────────────────────────────────────
            for (side in intArrayOf(-1, 1)) {
                val antSway = sin(t * 4.0f * PI.toFloat() + side * 0.6f) * s * 1.2f
                val antPath = paths.obtain().apply {
                    moveTo(headX - headW * 0.5f, y + side * bodyH * 0.3f)
                    quadraticTo(
                        headX - headW * 1.0f + antSway * 0.5f,
                        y + side * bodyH * 0.6f,
                        headX - headW * 1.2f + antSway,
                        y + side * bodyH * 0.15f
                    )
                }
                drawPath(antPath, HeadGreen.copy(alpha = 0.55f),
                    style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f), cap = StrokeCap.Round))
            }

            // ── LEVEL 5+: Rainbow shimmer pulse ─────────────────────────────
            if (level >= 5) {
                val pulsePhase = t * 1.5f * PI.toFloat()
                // Shimmer wave traveling along body
                for (i in 0 until 8) {
                    val shimFrac = i.toFloat() / 8f
                    val shimX = x - bodyLen * 0.30f + shimFrac * bodyLen * 0.70f
                    val shimAlpha = (sin(pulsePhase - shimFrac * 4f) * 0.5f + 0.5f) * 0.18f
                    val shimColor = when (i % 4) {
                        0 -> Color(0xFFFF4040)
                        1 -> Color(0xFFFFAA20)
                        2 -> Color(0xFF40FF60)
                        else -> Color(0xFF4080FF)
                    }
                    drawCircle(shimColor.copy(alpha = shimAlpha), bodyH * 0.6f,
                        Offset(shimX, y))
                }
                // Rainbow outline pulse
                val outlineAlpha = (sin(pulsePhase) * 0.5f + 0.5f) * 0.12f
                drawLine(
                    RainbowShimmer.copy(alpha = outlineAlpha),
                    Offset(x - bodyLen * 0.33f, y - bodyH * 0.9f),
                    Offset(x + bodyLen * 0.40f, y - bodyH * 0.85f),
                    strokeWidth = (1.5f * s).coerceAtLeast(0.7f),
                    cap = StrokeCap.Round
                )
            }

            } // sway transform
        }
    }
}
