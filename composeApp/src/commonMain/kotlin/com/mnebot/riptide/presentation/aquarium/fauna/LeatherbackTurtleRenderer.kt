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
private val BodyDarkBlue     = Color(0xFF18283C)  // dark blue-black body
private val BodyNavy         = Color(0xFF1E3450)  // navy mid-tone
private val BodySlate        = Color(0xFF2C4058)  // slate blue
private val SpotWhite        = Color(0xFFE0D8D0)  // white spots
private val SpotPink         = Color(0xFFD0A0A0)  // pinkish spots
private val RidgeDark        = Color(0xFF102030)  // ridge line dark
private val RidgeHighlight   = Color(0xFF3A5878)  // ridge highlight
private val BellyPale        = Color(0xFFB8B0C0)  // pale belly
private val FlipperDark      = Color(0xFF142838)  // flipper dark
private val FlipperHighlight = Color(0xFF385068)  // flipper highlight
private val ShadowDeep       = Color(0xFF081420)  // deep shadow
private val EyeDark          = Color(0xFF0A1018)  // near-black eye
private val BioGlow          = Color(0xFF60C0E0)  // bioluminescent blue

object LeatherbackTurtleRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        // Largest turtle -- big and imposing
        val bodyLen = (14f + level * 0.8f) * s
        val bodyH = (9f + level * 0.5f) * s

        // Powerful flipper strokes
        val flipperPhase = t * 1.2f * PI.toFloat()
        val frontFlipAngle = sin(flipperPhase) * 30f
        val rearFlipAngle = sin(flipperPhase + 1.0f) * 18f
        val bobY = sin(flipperPhase * 0.4f) * s * 0.5f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {
            withTransform({
                translate(left = 0f, top = bobY)
            }) {

                // ── REAR FLIPPERS ────────────────────────────────────────────
                for (side in listOf(-1f, 1f)) {
                    val flipX = x + bodyLen * 0.35f
                    val flipY = y + side * bodyH * 0.25f
                    val flipLen = bodyLen * 0.32f

                    withTransform({
                        rotate(degrees = rearFlipAngle * side, pivot = Offset(flipX, flipY))
                    }) {
                        val flipper = Path().apply {
                            moveTo(flipX, flipY)
                            cubicTo(flipX + flipLen * 0.3f, flipY + side * flipLen * 0.2f,
                                flipX + flipLen * 0.7f, flipY + side * flipLen * 0.3f,
                                flipX + flipLen, flipY + side * flipLen * 0.08f)
                            cubicTo(flipX + flipLen * 0.75f, flipY - side * flipLen * 0.02f,
                                flipX + flipLen * 0.35f, flipY - side * flipLen * 0.05f,
                                flipX, flipY)
                            close()
                        }
                        drawPath(flipper, FlipperDark)
                        drawPath(flipper, FlipperHighlight.copy(alpha = 0.20f))
                    }
                }

                // ── FRONT FLIPPERS (very long, powerful) ─────────────────────
                for (side in listOf(-1f, 1f)) {
                    val flipX = x - bodyLen * 0.20f
                    val flipY = y + side * bodyH * 0.32f
                    val flipLen = bodyLen * 0.85f

                    withTransform({
                        rotate(degrees = frontFlipAngle * side, pivot = Offset(flipX, flipY))
                    }) {
                        // Flipper shadow
                        drawCircle(ShadowDeep.copy(alpha = 0.08f), flipLen * 0.3f,
                            Offset(flipX - flipLen * 0.4f, flipY + side * flipLen * 0.25f))

                        val flipper = Path().apply {
                            moveTo(flipX, flipY)
                            cubicTo(flipX - flipLen * 0.25f, flipY + side * flipLen * 0.55f,
                                flipX - flipLen * 0.65f, flipY + side * flipLen * 0.5f,
                                flipX - flipLen, flipY + side * flipLen * 0.1f)
                            cubicTo(flipX - flipLen * 0.8f, flipY + side * flipLen * 0.02f,
                                flipX - flipLen * 0.3f, flipY - side * flipLen * 0.08f,
                                flipX, flipY)
                            close()
                        }
                        drawPath(flipper, FlipperDark)

                        // Flipper highlight
                        val flipHL = Path().apply {
                            moveTo(flipX - flipLen * 0.05f, flipY)
                            cubicTo(flipX - flipLen * 0.2f, flipY + side * flipLen * 0.30f,
                                flipX - flipLen * 0.5f, flipY + side * flipLen * 0.28f,
                                flipX - flipLen * 0.75f, flipY + side * flipLen * 0.08f)
                            cubicTo(flipX - flipLen * 0.55f, flipY + side * flipLen * 0.12f,
                                flipX - flipLen * 0.2f, flipY + side * flipLen * 0.06f,
                                flipX - flipLen * 0.05f, flipY)
                            close()
                        }
                        drawPath(flipHL, FlipperHighlight.copy(alpha = 0.25f))
                    }
                }

                // ── TAIL ─────────────────────────────────────────────────────
                val tailSway = sin(t * 1.0f * PI.toFloat()) * s * 0.6f
                val tail = Path().apply {
                    moveTo(x + bodyLen * 0.40f, y)
                    cubicTo(x + bodyLen * 0.50f, y - s * 1.5f + tailSway,
                        x + bodyLen * 0.58f, y - s * 0.6f + tailSway,
                        x + bodyLen * 0.65f, y + tailSway * 0.5f)
                    cubicTo(x + bodyLen * 0.58f, y + s * 0.6f + tailSway,
                        x + bodyLen * 0.50f, y + s * 1.5f + tailSway,
                        x + bodyLen * 0.40f, y)
                    close()
                }
                drawPath(tail, BodyDarkBlue)

                // ── HEAD (large, rounded) ────────────────────────────────────
                val headPath = Path().apply {
                    moveTo(x - bodyLen * 0.35f, y - bodyH * 0.18f)
                    cubicTo(x - bodyLen * 0.55f, y - bodyH * 0.28f,
                        x - bodyLen * 0.72f, y - bodyH * 0.15f,
                        x - bodyLen * 0.78f, y)
                    cubicTo(x - bodyLen * 0.72f, y + bodyH * 0.15f,
                        x - bodyLen * 0.55f, y + bodyH * 0.28f,
                        x - bodyLen * 0.35f, y + bodyH * 0.18f)
                    close()
                }
                drawPath(headPath, BodyNavy)

                // Head spots
                val headSpots = listOf(
                    Pair(-0.50f, -0.08f), Pair(-0.55f, 0.05f),
                    Pair(-0.62f, -0.03f), Pair(-0.45f, 0.10f),
                )
                for ((hx, hy) in headSpots) {
                    drawCircle(SpotWhite.copy(alpha = 0.25f), s * 0.5f,
                        Offset(x + bodyLen * hx, y + bodyH * hy))
                }

                // ── CARAPACE (leathery, no scutes) ───────────────────────────
                val shellShadow = Path().apply {
                    moveTo(x - bodyLen * 0.36f, y)
                    cubicTo(x - bodyLen * 0.32f, y - bodyH * 0.72f,
                        x + bodyLen * 0.32f, y - bodyH * 0.72f,
                        x + bodyLen * 0.42f, y)
                    cubicTo(x + bodyLen * 0.32f, y + bodyH * 0.72f,
                        x - bodyLen * 0.32f, y + bodyH * 0.72f,
                        x - bodyLen * 0.36f, y)
                    close()
                }
                drawPath(shellShadow, ShadowDeep.copy(alpha = 0.18f))

                val shell = Path().apply {
                    moveTo(x - bodyLen * 0.34f, y)
                    cubicTo(x - bodyLen * 0.30f, y - bodyH * 0.68f,
                        x + bodyLen * 0.30f, y - bodyH * 0.68f,
                        x + bodyLen * 0.40f, y)
                    cubicTo(x + bodyLen * 0.30f, y + bodyH * 0.68f,
                        x - bodyLen * 0.30f, y + bodyH * 0.68f,
                        x - bodyLen * 0.34f, y)
                    close()
                }
                drawPath(shell, BodyDarkBlue)

                // ── LONGITUDINAL RIDGES (characteristic of leatherback) ──────
                val ridgeCount = 5
                for (r in 0 until ridgeCount) {
                    val ridgeY = y + bodyH * (-0.40f + r * 0.20f)
                    val ridgeColor = if (level >= 3) RidgeHighlight else RidgeDark
                    val ridgeAlpha = if (level >= 3) 0.40f else 0.30f

                    drawLine(
                        ridgeColor.copy(alpha = ridgeAlpha),
                        Offset(x - bodyLen * 0.28f, ridgeY),
                        Offset(x + bodyLen * 0.35f, ridgeY),
                        strokeWidth = (1.0f * s).coerceAtLeast(0.4f),
                        cap = StrokeCap.Round
                    )
                    // Ridge highlight
                    drawLine(
                        RidgeHighlight.copy(alpha = 0.15f),
                        Offset(x - bodyLen * 0.26f, ridgeY - s * 0.3f),
                        Offset(x + bodyLen * 0.33f, ridgeY - s * 0.3f),
                        strokeWidth = (0.5f * s).coerceAtLeast(0.2f),
                        cap = StrokeCap.Round
                    )
                }

                // Shell upper highlight
                val shellHL = Path().apply {
                    moveTo(x - bodyLen * 0.25f, y - bodyH * 0.20f)
                    cubicTo(x - bodyLen * 0.18f, y - bodyH * 0.55f,
                        x + bodyLen * 0.15f, y - bodyH * 0.55f,
                        x + bodyLen * 0.28f, y - bodyH * 0.20f)
                    cubicTo(x + bodyLen * 0.12f, y - bodyH * 0.38f,
                        x - bodyLen * 0.08f, y - bodyH * 0.40f,
                        x - bodyLen * 0.25f, y - bodyH * 0.20f)
                    close()
                }
                drawPath(shellHL, BodySlate.copy(alpha = 0.30f))

                // ── WHITE/PINK SPOTS ON SHELL ────────────────────────────────
                val spotPositions = listOf(
                    Pair(0.05f, -0.35f), Pair(-0.10f, -0.25f), Pair(0.20f, -0.15f),
                    Pair(-0.05f, 0.30f), Pair(0.15f, 0.25f), Pair(-0.15f, 0.10f),
                    Pair(0.25f, 0.05f), Pair(0.00f, -0.45f), Pair(0.10f, 0.40f),
                    Pair(-0.20f, -0.40f), Pair(0.30f, -0.30f), Pair(-0.08f, 0.42f),
                )
                for ((idx, pos) in spotPositions.withIndex()) {
                    val spotX = x + bodyLen * pos.first
                    val spotY = y + bodyH * pos.second
                    val spotColor = if (idx % 3 == 0) SpotPink else SpotWhite
                    val spotR = s * (0.4f + (idx % 3) * 0.15f)
                    drawCircle(spotColor.copy(alpha = 0.30f), spotR, Offset(spotX, spotY))
                }

                // ── LEVEL 5+: BIOLUMINESCENT SPOTS ───────────────────────────
                if (level >= 5) {
                    for ((idx, pos) in spotPositions.take(8).withIndex()) {
                        val pulse = sin(t * 2.0f * PI.toFloat() + idx * 0.9f)
                        val bioAlpha = (0.15f + pulse * 0.15f).coerceIn(0.05f, 0.30f)
                        val spotX = x + bodyLen * pos.first
                        val spotY = y + bodyH * pos.second
                        // Glow halo
                        drawCircle(BioGlow.copy(alpha = bioAlpha * 0.4f),
                            s * 1.5f, Offset(spotX, spotY))
                        // Bright center
                        drawCircle(BioGlow.copy(alpha = bioAlpha),
                            s * 0.6f, Offset(spotX, spotY))
                    }
                }

                // Shell outline
                drawPath(shell, RidgeDark.copy(alpha = 0.25f),
                    style = Stroke(width = (0.7f * s).coerceAtLeast(0.3f)))

                // ── BELLY (pale underside) ───────────────────────────────────
                val belly = Path().apply {
                    moveTo(x - bodyLen * 0.28f, y)
                    cubicTo(x - bodyLen * 0.20f, y + bodyH * 0.48f,
                        x + bodyLen * 0.20f, y + bodyH * 0.48f,
                        x + bodyLen * 0.34f, y)
                    cubicTo(x + bodyLen * 0.18f, y + bodyH * 0.32f,
                        x - bodyLen * 0.12f, y + bodyH * 0.34f,
                        x - bodyLen * 0.28f, y)
                    close()
                }
                drawPath(belly, BellyPale.copy(alpha = 0.25f))

                // ── EYE ──────────────────────────────────────────────────────
                val eyeR = (1.3f + level * 0.08f) * s
                val eyeX = x - bodyLen * 0.60f
                val eyeY = y - bodyH * 0.06f

                drawCircle(ShadowDeep.copy(alpha = 0.35f), eyeR * 1.3f, Offset(eyeX, eyeY))
                drawCircle(Color(0xFFD8D0C8), eyeR, Offset(eyeX, eyeY))
                drawCircle(BodyNavy, eyeR * 0.60f, Offset(eyeX + eyeR * 0.05f, eyeY))
                drawCircle(EyeDark, eyeR * 0.35f, Offset(eyeX + eyeR * 0.07f, eyeY))
                drawCircle(Color.White, eyeR * 0.18f,
                    Offset(eyeX - eyeR * 0.15f, eyeY - eyeR * 0.15f))

                // ── BEAK / MOUTH ─────────────────────────────────────────────
                val beakPath = Path().apply {
                    moveTo(x - bodyLen * 0.72f, y - bodyH * 0.05f)
                    cubicTo(x - bodyLen * 0.76f, y,
                        x - bodyLen * 0.76f, y + bodyH * 0.02f,
                        x - bodyLen * 0.72f, y + bodyH * 0.06f)
                }
                drawPath(beakPath, RidgeDark.copy(alpha = 0.50f),
                    style = Stroke(width = (0.8f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round))

            } // bob transform
        }
    }
}
