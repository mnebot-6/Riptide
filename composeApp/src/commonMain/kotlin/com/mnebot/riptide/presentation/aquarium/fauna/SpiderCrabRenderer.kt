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

// ── Palette ───────────────────────────────────────────────────────────────────
private val SpidBody = Color(0xFFB06020)  // orange-brown carapace
private val SpidLeg  = Color(0xFF904020)  // legs
private val SpidClaw = Color(0xFF804010)  // claws
private val SpidEye  = Color(0xFF000000)  // eye
private val SpidSpot = Color(0xFFCC8040)  // lighter spots on carapace

object SpiderCrabRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f
        val t = animTimeMs / 1000f

        val bodyW  = (5f + level * 0.3f) * s
        val bodyH  = (4f + level * 0.25f) * s
        val legLen = (15f + level * 1.0f) * s  // very long legs

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ════════════════════════════════════════════════════════════════
            // LEGS — 5 on each side, very long and spindly with joints
            // ════════════════════════════════════════════════════════════════
            // Left legs (go down-left from body)
            val leftLegAngles = floatArrayOf(
                -2.4f, -2.0f, -1.7f, -1.45f, -1.2f  // from upper-left to lower-left
            )
            // Right legs (mirror)
            val rightLegAngles = floatArrayOf(
                1.2f, 1.45f, 1.7f, 2.0f, 2.4f  // from lower-right to upper-right
            )

            for (side in 0..1) {
                val angles = if (side == 0) leftLegAngles else rightLegAngles
                val sideSign = if (side == 0) -1f else 1f

                for (i in angles.indices) {
                    val baseAngle = angles[i]
                    // Phase offset for slight animation
                    val phase = i * 0.4f + side * 1.2f
                    val legSway = sin(t * 1.5f * PI.toFloat() + phase) * 0.06f

                    val angle1 = baseAngle + legSway
                    val seg1Len = legLen * 0.45f
                    val joint1X = x + cos(angle1) * seg1Len
                    val joint1Y = y + sin(angle1) * seg1Len

                    val angle2 = angle1 + sideSign * 0.55f + legSway * 0.5f
                    val seg2Len = legLen * 0.40f
                    val tipX = joint1X + cos(angle2) * seg2Len
                    val tipY = joint1Y + sin(angle2) * seg2Len

                    // Third short segment (tarsus)
                    val angle3 = angle2 + sideSign * 0.35f
                    val seg3Len = legLen * 0.20f
                    val endX = tipX + cos(angle3) * seg3Len
                    val endY = tipY + sin(angle3) * seg3Len

                    val legStroke = (1.0f * s).coerceAtLeast(0.5f)

                    drawLine(SpidLeg, Offset(x, y), Offset(joint1X, joint1Y),
                        strokeWidth = legStroke, cap = StrokeCap.Round)
                    drawLine(SpidLeg, Offset(joint1X, joint1Y), Offset(tipX, tipY),
                        strokeWidth = legStroke * 0.8f, cap = StrokeCap.Round)
                    drawLine(SpidClaw.copy(alpha = 0.85f), Offset(tipX, tipY), Offset(endX, endY),
                        strokeWidth = legStroke * 0.65f, cap = StrokeCap.Round)

                    // Joint dot
                    drawCircle(SpidClaw, 0.7f * s, Offset(joint1X, joint1Y))
                }
            }

            // ════════════════════════════════════════════════════════════════
            // FRONT CLAWS (chelipeds) — extending forward
            // ════════════════════════════════════════════════════════════════
            val clawSway = sin(t * 0.8f * PI.toFloat()) * 0.08f
            for (side in 0..1) {
                val sideSign = if (side == 0) -1f else 1f
                val clawAngle = sideSign * 0.45f - PI.toFloat() / 2f + clawSway * sideSign
                val clawLen = legLen * 0.8f

                val j1x = x + cos(clawAngle) * clawLen * 0.45f
                val j1y = y + sin(clawAngle) * clawLen * 0.45f

                val clawAngle2 = clawAngle - sideSign * 0.3f
                val tipX = j1x + cos(clawAngle2) * clawLen * 0.45f
                val tipY = j1y + sin(clawAngle2) * clawLen * 0.45f

                drawLine(SpidClaw, Offset(x, y), Offset(j1x, j1y),
                    strokeWidth = (1.4f * s).coerceAtLeast(0.7f), cap = StrokeCap.Round)
                drawLine(SpidClaw, Offset(j1x, j1y), Offset(tipX, tipY),
                    strokeWidth = (1.2f * s).coerceAtLeast(0.6f), cap = StrokeCap.Round)

                // Claw tip (small pincer)
                val pincer1 = paths.obtain().apply {
                    val pAngle = clawAngle2 - sideSign * 0.4f
                    moveTo(tipX, tipY)
                    lineTo(tipX + cos(pAngle) * 2.5f * s, tipY + sin(pAngle) * 2.5f * s)
                }
                val pincer2 = paths.obtain().apply {
                    val pAngle = clawAngle2 + sideSign * 0.15f
                    moveTo(tipX, tipY)
                    lineTo(tipX + cos(pAngle) * 2.0f * s, tipY + sin(pAngle) * 2.0f * s)
                }
                drawPath(pincer1, SpidClaw, style = Stroke(width = (1.0f * s).coerceAtLeast(0.5f),
                    cap = StrokeCap.Round))
                drawPath(pincer2, SpidClaw, style = Stroke(width = (0.8f * s).coerceAtLeast(0.4f),
                    cap = StrokeCap.Round))
            }

            // ════════════════════════════════════════════════════════════════
            // CARAPACE — small oval body
            // ════════════════════════════════════════════════════════════════
            val carapace = paths.obtain().apply {
                moveTo(x, y - bodyH)
                cubicTo(
                    x + bodyW, y - bodyH * 0.6f,
                    x + bodyW * 0.9f, y + bodyH * 0.5f,
                    x, y + bodyH * 0.7f
                )
                cubicTo(
                    x - bodyW * 0.9f, y + bodyH * 0.5f,
                    x - bodyW, y - bodyH * 0.6f,
                    x, y - bodyH
                )
                close()
            }
            drawPath(carapace, SpidBody)

            // Spiny bumps on carapace surface
            val bumpCount = 5
            for (b in 0 until bumpCount) {
                val bAngle = -PI.toFloat() / 2f + b * PI.toFloat() / (bumpCount - 1)
                val bx = x + cos(bAngle) * bodyW * 0.55f
                val by = y + sin(bAngle) * bodyH * 0.55f
                drawCircle(SpidSpot.copy(alpha = 0.60f), 0.7f * s, Offset(bx, by))
            }

            // Carapace outline
            drawPath(carapace, SpidClaw.copy(alpha = 0.45f),
                style = Stroke(width = (0.6f * s).coerceAtLeast(0.4f)))

            // ── LEVEL 3+: Algae/sponge decoration on carapace (decorator crab) ──
            if (level >= 3) {
                val SpongeGreen = Color(0xFF3A6A22)
                val SpongeYellow = Color(0xFFAABB44)
                val decoPositions = listOf(
                    Triple(0f, -0.75f, SpongeGreen),
                    Triple(-0.55f, -0.20f, SpongeYellow),
                    Triple(0.55f, -0.20f, SpongeGreen),
                    Triple(0f,  0.42f, SpongeYellow)
                )
                for ((dx, dy, col) in decoPositions) {
                    drawCircle(col.copy(alpha = 0.65f),
                        (0.9f * s).coerceAtLeast(0.4f),
                        Offset(x + dx * bodyW, y + dy * bodyH))
                }
            }

            // ── LEVEL 5+: Dense sponge covering (heavily decorated) ───────────────
            if (level >= 5) {
                val SpongeOrange = Color(0xFFCC6622)
                val extraDecoPositions = listOf(
                    Triple(-0.30f, -0.55f, SpongeOrange),
                    Triple( 0.35f, -0.50f, SpongeOrange),
                    Triple(-0.55f,  0.20f, SpongeOrange),
                    Triple( 0.50f,  0.22f, SpongeOrange)
                )
                for ((dx, dy, col) in extraDecoPositions) {
                    drawCircle(col.copy(alpha = 0.55f),
                        (0.8f * s).coerceAtLeast(0.35f),
                        Offset(x + dx * bodyW, y + dy * bodyH))
                }
            }

            // ════════════════════════════════════════════════════════════════
            // STALKED EYES on top of carapace
            // ════════════════════════════════════════════════════════════════
            val eyeStalkLen = 2.0f * s
            for (side in 0..1) {
                val ex = x + (if (side == 0) -1f else 1f) * bodyW * 0.40f
                val ey = y - bodyH * 0.85f
                drawLine(SpidBody, Offset(ex, ey), Offset(ex, ey - eyeStalkLen),
                    strokeWidth = (0.8f * s).coerceAtLeast(0.4f))
                drawCircle(SpidSpot, 0.9f * s, Offset(ex, ey - eyeStalkLen))
                drawCircle(SpidEye, 0.5f * s, Offset(ex, ey - eyeStalkLen))
            }
        }
    }
}
