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

// ── Palette ─────────────────────────────────────────────────────────────────
private val CarapaceOlive    = Color(0xFF5C8040)  // olive green shell
private val CarapaceDark     = Color(0xFF3A5828)  // dark olive
private val CarapaceLight    = Color(0xFF88A860)  // light olive highlight
private val ScuteLine        = Color(0xFF4A6830)  // scute line color
private val BellyCream       = Color(0xFFF0E8C8)  // cream plastron
private val BellyLight       = Color(0xFFF8F0D8)  // light cream highlight
private val SkinOlive        = Color(0xFF6B9050)  // skin green
private val SkinLight        = Color(0xFF90B870)  // lighter skin
private val SkinDark         = Color(0xFF3E5828)  // dark skin shadow
private val BarnacleGray     = Color(0xFF8A8878)  // barnacle color
private val BarnacleLight    = Color(0xFFB0A898)  // light barnacle
private val ShadowDeep       = Color(0xFF1E3010)  // deep shadow
private val EyeDark          = Color(0xFF182810)  // eye dark
private val GoldGlow         = Color(0xFFFFD050)  // golden outline glow

object GreenSeaTurtleRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyLen = (10f + level * 0.6f) * s
        val bodyH = (7f + level * 0.4f) * s

        // Flipper paddling
        val flipperPhase = t * 1.8f * PI.toFloat()
        val frontFlipAngle = sin(flipperPhase) * 25f
        val rearFlipAngle = sin(flipperPhase + 1.2f) * 15f
        val bobY = sin(flipperPhase * 0.5f) * s * 0.4f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = bobY)
            }) {

                // ── REAR FLIPPERS ────────────────────────────────────────────
                for (side in listOf(-1f, 1f)) {
                    val flipX = x + bodyLen * 0.35f
                    val flipY = y + side * bodyH * 0.3f
                    val flipLen = bodyLen * 0.35f
                    val angle = rearFlipAngle * side

                    withTransform({
                        rotate(degrees = angle, pivot = Offset(flipX, flipY))
                    }) {
                        val flipper = Path().apply {
                            moveTo(flipX, flipY)
                            cubicTo(flipX + flipLen * 0.3f, flipY + side * flipLen * 0.15f,
                                flipX + flipLen * 0.7f, flipY + side * flipLen * 0.25f,
                                flipX + flipLen, flipY + side * flipLen * 0.1f)
                            cubicTo(flipX + flipLen * 0.8f, flipY + side * flipLen * 0.05f,
                                flipX + flipLen * 0.4f, flipY - side * flipLen * 0.05f,
                                flipX, flipY)
                            close()
                        }
                        drawPath(flipper, SkinOlive.copy(alpha = 0.75f))
                        drawPath(flipper, SkinDark.copy(alpha = 0.20f),
                            style = Stroke(width = (0.5f * s).coerceAtLeast(0.2f)))
                    }
                }

                // ── FRONT FLIPPERS (large, paddle-shaped) ────────────────────
                for (side in listOf(-1f, 1f)) {
                    val flipX = x - bodyLen * 0.25f
                    val flipY = y + side * bodyH * 0.35f
                    val flipLen = bodyLen * 0.65f
                    val angle = frontFlipAngle * side

                    withTransform({
                        rotate(degrees = angle, pivot = Offset(flipX, flipY))
                    }) {
                        // Flipper shadow
                        val flipShadow = Path().apply {
                            moveTo(flipX, flipY)
                            cubicTo(flipX - flipLen * 0.35f, flipY + side * flipLen * 0.55f,
                                flipX - flipLen * 0.75f, flipY + side * flipLen * 0.5f,
                                flipX - flipLen, flipY + side * flipLen * 0.15f)
                            cubicTo(flipX - flipLen * 0.8f, flipY + side * flipLen * 0.08f,
                                flipX - flipLen * 0.3f, flipY - side * flipLen * 0.05f,
                                flipX, flipY)
                            close()
                        }
                        drawPath(flipShadow, SkinDark.copy(alpha = 0.15f))

                        val flipper = Path().apply {
                            moveTo(flipX, flipY)
                            cubicTo(flipX - flipLen * 0.3f, flipY + side * flipLen * 0.5f,
                                flipX - flipLen * 0.7f, flipY + side * flipLen * 0.45f,
                                flipX - flipLen * 0.95f, flipY + side * flipLen * 0.12f)
                            cubicTo(flipX - flipLen * 0.75f, flipY + side * flipLen * 0.05f,
                                flipX - flipLen * 0.25f, flipY - side * flipLen * 0.05f,
                                flipX, flipY)
                            close()
                        }
                        drawPath(flipper, SkinOlive)
                        // Flipper lighter edge
                        drawPath(flipper, SkinLight.copy(alpha = 0.30f),
                            style = Stroke(width = (0.8f * s).coerceAtLeast(0.3f)))
                    }
                }

                // ── TAIL ─────────────────────────────────────────────────────
                val tailSway = sin(t * 1.2f * PI.toFloat()) * s * 0.8f
                val tail = Path().apply {
                    moveTo(x + bodyLen * 0.42f, y)
                    cubicTo(x + bodyLen * 0.55f, y - s * 1.2f + tailSway,
                        x + bodyLen * 0.65f, y - s * 0.5f + tailSway,
                        x + bodyLen * 0.72f, y + tailSway)
                    cubicTo(x + bodyLen * 0.65f, y + s * 0.5f + tailSway,
                        x + bodyLen * 0.55f, y + s * 1.2f + tailSway,
                        x + bodyLen * 0.42f, y)
                    close()
                }
                drawPath(tail, SkinOlive)

                // ── HEAD ─────────────────────────────────────────────────────
                val headPath = Path().apply {
                    moveTo(x - bodyLen * 0.38f, y - bodyH * 0.18f)
                    cubicTo(x - bodyLen * 0.55f, y - bodyH * 0.25f,
                        x - bodyLen * 0.72f, y - bodyH * 0.12f,
                        x - bodyLen * 0.78f, y)
                    cubicTo(x - bodyLen * 0.72f, y + bodyH * 0.12f,
                        x - bodyLen * 0.55f, y + bodyH * 0.25f,
                        x - bodyLen * 0.38f, y + bodyH * 0.18f)
                    close()
                }
                drawPath(headPath, SkinOlive)
                drawPath(headPath, SkinLight.copy(alpha = 0.25f))

                // ── CARAPACE (shell, oval) ───────────────────────────────────
                val shellShadow = Path().apply {
                    moveTo(x - bodyLen * 0.38f, y)
                    cubicTo(x - bodyLen * 0.35f, y - bodyH * 0.72f,
                        x + bodyLen * 0.3f, y - bodyH * 0.72f,
                        x + bodyLen * 0.42f, y)
                    cubicTo(x + bodyLen * 0.3f, y + bodyH * 0.72f,
                        x - bodyLen * 0.35f, y + bodyH * 0.72f,
                        x - bodyLen * 0.38f, y)
                    close()
                }
                drawPath(shellShadow, ShadowDeep.copy(alpha = 0.15f))

                val shell = Path().apply {
                    moveTo(x - bodyLen * 0.36f, y)
                    cubicTo(x - bodyLen * 0.33f, y - bodyH * 0.68f,
                        x + bodyLen * 0.28f, y - bodyH * 0.68f,
                        x + bodyLen * 0.40f, y)
                    cubicTo(x + bodyLen * 0.28f, y + bodyH * 0.68f,
                        x - bodyLen * 0.33f, y + bodyH * 0.68f,
                        x - bodyLen * 0.36f, y)
                    close()
                }
                drawPath(shell, CarapaceOlive)

                // Shell upper highlight
                val shellHL = Path().apply {
                    moveTo(x - bodyLen * 0.28f, y - bodyH * 0.15f)
                    cubicTo(x - bodyLen * 0.22f, y - bodyH * 0.55f,
                        x + bodyLen * 0.15f, y - bodyH * 0.55f,
                        x + bodyLen * 0.28f, y - bodyH * 0.15f)
                    cubicTo(x + bodyLen * 0.12f, y - bodyH * 0.35f,
                        x - bodyLen * 0.12f, y - bodyH * 0.38f,
                        x - bodyLen * 0.28f, y - bodyH * 0.15f)
                    close()
                }
                drawPath(shellHL, CarapaceLight.copy(alpha = 0.35f))

                // ── SCUTE PATTERN (hexagonal-like lines) ─────────────────────
                // Center ridge
                drawLine(ScuteLine.copy(alpha = 0.40f),
                    Offset(x - bodyLen * 0.30f, y),
                    Offset(x + bodyLen * 0.35f, y),
                    strokeWidth = (0.7f * s).coerceAtLeast(0.3f))
                // Lateral scute lines
                val scutePositions = listOf(-0.15f, 0.05f, 0.20f)
                for (sx in scutePositions) {
                    val cx = x + bodyLen * sx
                    drawLine(ScuteLine.copy(alpha = 0.35f),
                        Offset(cx, y - bodyH * 0.55f),
                        Offset(cx, y + bodyH * 0.55f),
                        strokeWidth = (0.6f * s).coerceAtLeast(0.2f))
                    // Diagonal scute lines
                    drawLine(ScuteLine.copy(alpha = 0.25f),
                        Offset(cx - bodyLen * 0.08f, y - bodyH * 0.35f),
                        Offset(cx + bodyLen * 0.08f, y),
                        strokeWidth = (0.5f * s).coerceAtLeast(0.2f))
                    drawLine(ScuteLine.copy(alpha = 0.25f),
                        Offset(cx - bodyLen * 0.08f, y + bodyH * 0.35f),
                        Offset(cx + bodyLen * 0.08f, y),
                        strokeWidth = (0.5f * s).coerceAtLeast(0.2f))
                }

                // Shell outline
                drawPath(shell, CarapaceDark.copy(alpha = 0.30f),
                    style = Stroke(width = (0.8f * s).coerceAtLeast(0.3f)))

                // ── LEVEL 3+: BARNACLES ON SHELL ─────────────────────────────
                if (level >= 3) {
                    val barnacles = listOf(
                        Pair(0.10f, -0.40f), Pair(-0.10f, 0.35f),
                        Pair(0.25f, -0.20f), Pair(0.00f, -0.48f),
                        Pair(0.18f, 0.30f),
                    )
                    for ((bx, by) in barnacles) {
                        val barnX = x + bodyLen * bx
                        val barnY = y + bodyH * by
                        val barnR = s * 0.8f
                        drawCircle(BarnacleGray.copy(alpha = 0.60f), barnR, Offset(barnX, barnY))
                        drawCircle(BarnacleLight.copy(alpha = 0.30f), barnR * 0.6f,
                            Offset(barnX - barnR * 0.15f, barnY - barnR * 0.15f))
                        drawCircle(BarnacleGray.copy(alpha = 0.40f), barnR,
                            Offset(barnX, barnY),
                            style = Stroke(width = (0.4f * s).coerceAtLeast(0.2f)))
                    }
                }

                // ── LEVEL 5+: GOLDEN OUTLINE GLOW ────────────────────────────
                if (level >= 5) {
                    val glowPulse = sin(t * 1.8f * PI.toFloat()) * 0.08f
                    drawPath(shell, GoldGlow.copy(alpha = 0.18f + glowPulse),
                        style = Stroke(width = (2.5f * s).coerceAtLeast(1.0f)))
                    drawPath(shell, GoldGlow.copy(alpha = 0.08f + glowPulse * 0.5f),
                        style = Stroke(width = (4.0f * s).coerceAtLeast(1.5f)))
                }

                // ── PLASTRON (belly, lighter) ────────────────────────────────
                val plastron = Path().apply {
                    moveTo(x - bodyLen * 0.32f, y)
                    cubicTo(x - bodyLen * 0.25f, y + bodyH * 0.45f,
                        x + bodyLen * 0.20f, y + bodyH * 0.45f,
                        x + bodyLen * 0.35f, y)
                    cubicTo(x + bodyLen * 0.20f, y + bodyH * 0.30f,
                        x - bodyLen * 0.15f, y + bodyH * 0.32f,
                        x - bodyLen * 0.32f, y)
                    close()
                }
                drawPath(plastron, BellyCream.copy(alpha = 0.35f))

                // ── EYE ──────────────────────────────────────────────────────
                val eyeR = (1.2f + level * 0.07f) * s
                val eyeX = x - bodyLen * 0.62f
                val eyeY = y - bodyH * 0.06f

                drawCircle(ShadowDeep.copy(alpha = 0.30f), eyeR * 1.25f, Offset(eyeX, eyeY))
                drawCircle(Color(0xFFF0E8D0), eyeR, Offset(eyeX, eyeY))
                drawCircle(EyeDark.copy(alpha = 0.85f), eyeR * 0.50f,
                    Offset(eyeX + eyeR * 0.06f, eyeY))
                drawCircle(Color.White, eyeR * 0.18f,
                    Offset(eyeX - eyeR * 0.15f, eyeY - eyeR * 0.15f))

                // ── BEAK (mouth) ─────────────────────────────────────────────
                val beakPath = Path().apply {
                    moveTo(x - bodyLen * 0.75f, y - s * 0.3f)
                    cubicTo(x - bodyLen * 0.80f, y,
                        x - bodyLen * 0.80f, y,
                        x - bodyLen * 0.75f, y + s * 0.3f)
                }
                drawPath(beakPath, SkinDark.copy(alpha = 0.45f),
                    style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round))

            } // bob transform
        }
    }
}
