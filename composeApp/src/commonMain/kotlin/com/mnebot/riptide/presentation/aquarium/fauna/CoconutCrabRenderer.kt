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
private val BodyRed        = Color(0xFFAA3028)  // deep red body
private val BodyDarkRed    = Color(0xFF7A1818)  // darker red shading
private val BodyPurple     = Color(0xFF6A2848)  // purple-red tint
private val BodyDeepPurple = Color(0xFF4A1838)  // deep purple shadow
private val BodyBright     = Color(0xFFCC4438)  // brighter red highlight
private val ClawRed        = Color(0xFFBB3830)  // claw red
private val ClawDark       = Color(0xFF6A1810)  // dark claw shadow
private val ClawTip        = Color(0xFF4A1008)  // claw tip (near black)
private val ClawHighlight  = Color(0xFFDD5548)  // claw highlight
private val LegRed         = Color(0xFF8A2820)  // walking leg red
private val LegJoint       = Color(0xFF5A1810)  // leg joint dark
private val AbdomenPale    = Color(0xFFCC8878)  // abdomen pale underside
private val EyeBlack       = Color(0xFF100810)  // eye dark
private val EyeStalk       = Color(0xFF7A3028)  // eye stalk red
private val ShadowDark     = Color(0xFF180808)  // shadow
private val ShadowRed      = Color(0xFF3A0808)  // red shadow
private val PowerGlow      = Color(0xFF200818)  // dark glow (level 5+)

object CoconutCrabRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyW   = (9f + level * 0.5f) * s    // carapace width (side view)
        val bodyH   = (7f + level * 0.4f) * s    // carapace height
        val clawLen = (7f + level * 0.5f) * s    // oversized front claws
        val legLen  = (5f + level * 0.3f) * s

        // Level 3+: darker, more saturated coloring
        val satBoost = if (level >= 3) 0.12f else 0f

        // Walking motion
        val legPhase = t * 3.0f * PI.toFloat()
        val bodyBob  = sin(t * 1.8f * PI.toFloat()) * s * 0.15f
        val clawThreat = sin(t * 0.5f * PI.toFloat()) * s * 0.4f  // slow intimidation

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = bodyBob)
            }) {

            // ── WALKING LEGS (4 pairs) ──────────────────────────────────────
            for (i in 0 until 4) {
                val legBaseX = x - bodyW * 0.05f + i * bodyW * 0.22f
                val legSwing = sin(legPhase + i * 0.7f) * s * 1.2f

                for (side in intArrayOf(-1, 1)) {
                    val tipX = legBaseX + legSwing
                    val tipY = y + side * (bodyH * 0.45f + legLen * 0.7f)
                    val kneeX = legBaseX + legSwing * 0.4f
                    val kneeY = y + side * (bodyH * 0.45f + legLen * 0.3f)

                    // Upper leg
                    drawLine(
                        LegRed.copy(alpha = 0.70f),
                        Offset(legBaseX, y + side * bodyH * 0.30f),
                        Offset(kneeX, kneeY),
                        strokeWidth = (1.2f * s).coerceAtLeast(0.6f),
                        cap = StrokeCap.Round
                    )
                    // Joint
                    drawCircle(LegJoint.copy(alpha = 0.50f), s * 0.5f, Offset(kneeX, kneeY))
                    // Lower leg
                    drawLine(
                        LegRed.copy(alpha = 0.60f),
                        Offset(kneeX, kneeY),
                        Offset(tipX, tipY),
                        strokeWidth = (0.9f * s).coerceAtLeast(0.4f),
                        cap = StrokeCap.Round
                    )
                    // Foot tip
                    drawCircle(LegJoint.copy(alpha = 0.40f), s * 0.35f, Offset(tipX, tipY))
                }
            }

            // ── ABDOMEN (segmented, tucked under but visible) ───────────────
            val abdX = x + bodyW * 0.35f
            val abdPath = paths.obtain().apply {
                moveTo(abdX, y - bodyH * 0.25f)
                cubicTo(
                    abdX + bodyW * 0.25f, y - bodyH * 0.20f,
                    abdX + bodyW * 0.30f, y + bodyH * 0.20f,
                    abdX, y + bodyH * 0.25f
                )
                cubicTo(
                    abdX + bodyW * 0.10f, y + bodyH * 0.10f,
                    abdX + bodyW * 0.10f, y - bodyH * 0.10f,
                    abdX, y - bodyH * 0.25f
                )
                close()
            }
            drawPath(abdPath, AbdomenPale.copy(alpha = 0.50f))
            // Abdomen segments
            for (i in 1..3) {
                val segY = y - bodyH * 0.18f + i * bodyH * 0.10f
                drawLine(
                    BodyDarkRed.copy(alpha = 0.25f),
                    Offset(abdX + bodyW * 0.05f, segY),
                    Offset(abdX + bodyW * 0.22f, segY),
                    strokeWidth = (0.3f * s).coerceAtLeast(0.15f)
                )
            }

            // ── MAIN CARAPACE (large, rounded) ──────────────────────────────
            val carapaceShadow = paths.obtain().apply {
                moveTo(x - bodyW * 0.52f, y)
                cubicTo(x - bodyW * 0.52f, y - bodyH * 0.57f, x + bodyW * 0.42f, y - bodyH * 0.57f, x + bodyW * 0.42f, y)
                cubicTo(x + bodyW * 0.42f, y + bodyH * 0.57f, x - bodyW * 0.52f, y + bodyH * 0.57f, x - bodyW * 0.52f, y)
                close()
            }
            drawPath(carapaceShadow, ShadowRed.copy(alpha = 0.22f))

            val carapace = paths.obtain().apply {
                moveTo(x - bodyW * 0.50f, y)
                cubicTo(x - bodyW * 0.50f, y - bodyH * 0.55f, x + bodyW * 0.40f, y - bodyH * 0.55f, x + bodyW * 0.40f, y)
                cubicTo(x + bodyW * 0.40f, y + bodyH * 0.55f, x - bodyW * 0.50f, y + bodyH * 0.55f, x - bodyW * 0.50f, y)
                close()
            }
            val mainColor = if (level >= 3) BodyRed.copy(alpha = 0.90f + satBoost)
                            else BodyRed.copy(alpha = 0.85f)
            drawPath(carapace, mainColor)

            // Purple shading (lower half, characteristic of coconut crab)
            val purpleZone = paths.obtain().apply {
                moveTo(x - bodyW * 0.45f, y + bodyH * 0.05f)
                cubicTo(
                    x - bodyW * 0.40f, y + bodyH * 0.48f,
                    x + bodyW * 0.30f, y + bodyH * 0.48f,
                    x + bodyW * 0.35f, y + bodyH * 0.05f
                )
                cubicTo(
                    x + bodyW * 0.25f, y + bodyH * 0.35f,
                    x - bodyW * 0.30f, y + bodyH * 0.35f,
                    x - bodyW * 0.45f, y + bodyH * 0.05f
                )
                close()
            }
            drawPath(purpleZone, BodyPurple.copy(alpha = 0.35f + satBoost))

            // Upper highlight
            val hlPath = paths.obtain().apply {
                moveTo(x - bodyW * 0.35f, y - bodyH * 0.20f)
                cubicTo(
                    x - bodyW * 0.25f, y - bodyH * 0.45f,
                    x + bodyW * 0.15f, y - bodyH * 0.42f,
                    x + bodyW * 0.30f, y - bodyH * 0.15f
                )
                cubicTo(
                    x + bodyW * 0.10f, y - bodyH * 0.28f,
                    x - bodyW * 0.10f, y - bodyH * 0.30f,
                    x - bodyW * 0.35f, y - bodyH * 0.20f
                )
                close()
            }
            drawPath(hlPath, BodyBright.copy(alpha = 0.30f))

            // Carapace texture bumps
            for (i in 0 until 6) {
                val bx = x - bodyW * 0.25f + i * bodyW * 0.12f
                val by = y + sin(i.toFloat() * 2.1f) * bodyH * 0.15f
                drawCircle(BodyDarkRed.copy(alpha = 0.12f), bodyW * 0.06f, Offset(bx, by))
            }

            // Carapace outline
            drawPath(carapace, BodyDeepPurple.copy(alpha = 0.18f),
                style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f)))

            // ── OVERSIZED FRONT CLAWS (two massive chelipeds) ───────────────
            for (side in intArrayOf(-1, 1)) {
                val clawBaseX = x - bodyW * 0.48f
                val clawBaseY = y + side * bodyH * 0.15f

                // Arm segment (merus -> carpus)
                val armElbowX = clawBaseX - clawLen * 0.35f
                val armElbowY = clawBaseY + side * clawLen * 0.20f + clawThreat * side * 0.3f

                drawLine(
                    ClawRed.copy(alpha = 0.75f),
                    Offset(clawBaseX, clawBaseY),
                    Offset(armElbowX, armElbowY),
                    strokeWidth = (2.0f * s).coerceAtLeast(0.9f),
                    cap = StrokeCap.Round
                )
                drawCircle(LegJoint.copy(alpha = 0.40f), s * 0.7f, Offset(armElbowX, armElbowY))

                // Propodus (main claw body)
                val clawCx = armElbowX - clawLen * 0.35f
                val clawCy = armElbowY + side * clawThreat * 0.2f
                val clawW = clawLen * 0.40f
                val clawH = clawLen * 0.28f

                val propodus = paths.obtain().apply {
                    moveTo(armElbowX, clawCy - clawH)
                    cubicTo(
                        armElbowX - clawW * 0.3f, clawCy - clawH * 1.15f,
                        clawCx - clawW * 0.3f, clawCy - clawH * 1.1f,
                        clawCx - clawW, clawCy - clawH * 0.3f
                    )
                    lineTo(clawCx - clawW, clawCy + clawH * 0.3f)
                    cubicTo(
                        clawCx - clawW * 0.3f, clawCy + clawH * 1.1f,
                        armElbowX - clawW * 0.3f, clawCy + clawH * 1.15f,
                        armElbowX, clawCy + clawH
                    )
                    close()
                }
                // Claw shadow
                drawPath(propodus, ClawDark.copy(alpha = 0.30f))
                drawPath(propodus, ClawRed)
                // Claw highlight
                val clawHL = paths.obtain().apply {
                    moveTo(armElbowX - clawW * 0.1f, clawCy - clawH * 0.6f)
                    cubicTo(
                        armElbowX - clawW * 0.2f, clawCy - clawH * 0.85f,
                        clawCx - clawW * 0.1f, clawCy - clawH * 0.80f,
                        clawCx - clawW * 0.7f, clawCy - clawH * 0.3f
                    )
                    cubicTo(
                        clawCx - clawW * 0.2f, clawCy - clawH * 0.55f,
                        armElbowX - clawW * 0.15f, clawCy - clawH * 0.50f,
                        armElbowX - clawW * 0.1f, clawCy - clawH * 0.6f
                    )
                    close()
                }
                drawPath(clawHL, ClawHighlight.copy(alpha = 0.35f))

                // Dactylus (upper pincer finger)
                val upperFinger = paths.obtain().apply {
                    moveTo(clawCx - clawW * 0.8f, clawCy - clawH * 0.2f)
                    cubicTo(
                        clawCx - clawW * 1.3f, clawCy - clawH * 0.6f,
                        clawCx - clawW * 1.6f, clawCy - clawH * 0.2f,
                        clawCx - clawW * 1.4f, clawCy
                    )
                    close()
                }
                drawPath(upperFinger, ClawRed.copy(alpha = 0.85f))
                drawPath(upperFinger, ClawDark.copy(alpha = 0.15f),
                    style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))
                // Tip
                drawCircle(ClawTip, s * 0.4f,
                    Offset(clawCx - clawW * 1.4f, clawCy - clawH * 0.08f))

                // Pollex (lower pincer finger)
                val lowerFinger = paths.obtain().apply {
                    moveTo(clawCx - clawW * 0.8f, clawCy + clawH * 0.2f)
                    cubicTo(
                        clawCx - clawW * 1.3f, clawCy + clawH * 0.5f,
                        clawCx - clawW * 1.5f, clawCy + clawH * 0.15f,
                        clawCx - clawW * 1.3f, clawCy
                    )
                    close()
                }
                drawPath(lowerFinger, ClawRed.copy(alpha = 0.80f))
                drawPath(lowerFinger, ClawDark.copy(alpha = 0.15f),
                    style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))
                drawCircle(ClawTip, s * 0.35f,
                    Offset(clawCx - clawW * 1.3f, clawCy + clawH * 0.05f))

                // Serrated inner edge (teeth on claw)
                for (j in 0 until 3) {
                    val tx = clawCx - clawW * (0.9f + j * 0.15f)
                    drawCircle(ClawTip.copy(alpha = 0.35f), s * 0.2f,
                        Offset(tx, clawCy - clawH * 0.05f))
                }
            }

            // ── EYES (on short stalks) ──────────────────────────────────────
            val eyeR = (1.2f + level * 0.07f) * s
            for (side in intArrayOf(-1, 1)) {
                val stalkX = x - bodyW * 0.42f
                val stalkY = y + side * bodyH * 0.32f
                val eyeTipX = stalkX - bodyW * 0.12f
                val eyeTipY = stalkY + side * bodyH * 0.08f

                // Stalk
                drawLine(
                    EyeStalk, Offset(stalkX, stalkY), Offset(eyeTipX, eyeTipY),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.35f), cap = StrokeCap.Round
                )
                // Eye
                drawCircle(ShadowDark.copy(alpha = 0.25f), eyeR * 1.1f,
                    Offset(eyeTipX + s * 0.1f, eyeTipY + side * s * 0.1f))
                drawCircle(EyeBlack, eyeR, Offset(eyeTipX, eyeTipY))
                drawCircle(Color(0xFF603020), eyeR * 0.45f,
                    Offset(eyeTipX - eyeR * 0.08f, eyeTipY))
                drawCircle(Color.White, eyeR * 0.18f,
                    Offset(eyeTipX - eyeR * 0.20f, eyeTipY - side * eyeR * 0.15f))
            }

            // ── ANTENNAE ────────────────────────────────────────────────────
            for (side in intArrayOf(-1, 1)) {
                val antSway = sin(t * 3.5f * PI.toFloat() + side * 0.7f) * s * 0.8f
                drawLine(
                    LegRed.copy(alpha = 0.50f),
                    Offset(x - bodyW * 0.48f, y + side * bodyH * 0.20f),
                    Offset(x - bodyW * 0.72f + antSway, y + side * bodyH * 0.38f),
                    strokeWidth = (0.4f * s).coerceAtLeast(0.2f),
                    cap = StrokeCap.Round
                )
            }

            // ── LEVEL 5+: Faint aura of power (dark glow) ──────────────────
            if (level >= 5) {
                val glowAlpha = (sin(t * 0.7f * PI.toFloat()) * 0.5f + 0.5f) * 0.10f
                drawPath(carapace, PowerGlow.copy(alpha = glowAlpha),
                    style = Stroke(width = (4.0f * s).coerceAtLeast(1.5f)))
                // Subtle dark aura around entire form
                drawPath(carapace, BodyDeepPurple.copy(alpha = glowAlpha * 0.5f),
                    style = Stroke(width = (6.0f * s).coerceAtLeast(2.5f)))
                // Claw glow
                for (side in intArrayOf(-1, 1)) {
                    val glowX = x - bodyW * 0.48f - clawLen * 0.55f
                    val glowY = y + side * bodyH * 0.15f
                    drawCircle(BodyDarkRed.copy(alpha = glowAlpha * 1.5f),
                        clawLen * 0.5f, Offset(glowX, glowY))
                }
            }

            } // bob transform
        }
    }
}
