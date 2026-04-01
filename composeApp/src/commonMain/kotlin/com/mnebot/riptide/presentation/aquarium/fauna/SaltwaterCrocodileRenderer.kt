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
private val BodyOlive       = Color(0xFF485838)  // olive green body
private val BodyDarkOlive   = Color(0xFF2E3C20)  // dark olive
private val BodyMudGreen    = Color(0xFF3A4C2A)  // muddy green
private val ScuteOlive      = Color(0xFF506840)  // scute highlight
private val ScuteDark       = Color(0xFF283818)  // dark scute line
private val BellyCream       = Color(0xFFD8D0A8)  // cream belly
private val BellyYellow      = Color(0xFFE0D8B0)  // yellowish belly
private val ToothWhite       = Color(0xFFF0E8D8)  // tooth color
private val GumPink          = Color(0xFFC08080)  // gum line
private val EyeAmber         = Color(0xFFD0A020)  // amber eye
private val EyeGreen         = Color(0xFF607820)  // green-gold iris
private val ShadowDeep       = Color(0xFF141C08)  // deep shadow
private val NostrilDark      = Color(0xFF202810)  // nostril dark
private val ScarGray         = Color(0xFF889078)  // scar tissue

object SaltwaterCrocodileRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val bodyLen = (20f + level * 1.2f) * s
        val bodyH = (5f + level * 0.35f) * s

        // Tail propulsion S-wave
        val tailFreq = 1.2f * PI.toFloat()
        val tailSway = sin(t * tailFreq) * bodyH * 0.3f
        val midSway = sin(t * tailFreq + 0.5f) * bodyH * 0.1f
        val jawOpen = (sin(t * 0.4f * PI.toFloat()) * 0.5f).coerceAtLeast(0f) * s

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── TAIL (long, powerful, S-wave) ────────────────────────────
            val tailX = x + bodyLen * 0.5f
            val tailEnd = tailX + bodyLen * 0.35f

            val tailPath = Path().apply {
                moveTo(tailX, y - bodyH * 0.22f)
                cubicTo(tailX + bodyLen * 0.1f, y - bodyH * 0.25f + midSway * 0.3f,
                    tailEnd - bodyLen * 0.1f, y - bodyH * 0.10f + tailSway * 0.6f,
                    tailEnd, y + tailSway)
                cubicTo(tailEnd - bodyLen * 0.1f, y + bodyH * 0.10f + tailSway * 0.6f,
                    tailX + bodyLen * 0.1f, y + bodyH * 0.25f + midSway * 0.3f,
                    tailX, y + bodyH * 0.22f)
                close()
            }
            drawPath(tailPath, BodyDarkOlive)

            // Tail dorsal ridge
            val tailRidge = Path().apply {
                moveTo(tailX, y - bodyH * 0.22f)
                cubicTo(tailX + bodyLen * 0.08f, y - bodyH * 0.35f + midSway * 0.2f,
                    tailEnd - bodyLen * 0.12f, y - bodyH * 0.20f + tailSway * 0.5f,
                    tailEnd, y + tailSway)
            }
            drawPath(tailRidge, ScuteDark.copy(alpha = 0.40f),
                style = Stroke(width = (1.5f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round))

            // Tail scutes (armor plates on tail)
            for (ts in 0 until 6) {
                val tFrac = ts.toFloat() / 5f
                val tsx = tailX + (tailEnd - tailX) * tFrac
                val tsy = y + tailSway * tFrac * tFrac
                val tsSize = bodyH * (0.22f - tFrac * 0.08f)
                drawLine(ScuteOlive.copy(alpha = 0.35f),
                    Offset(tsx, tsy - tsSize),
                    Offset(tsx, tsy + tsSize),
                    strokeWidth = (1.2f * s).coerceAtLeast(0.4f))
            }

            // ── LEGS (stubby, splayed out) ───────────────────────────────
            val legSway = sin(t * 1.5f * PI.toFloat()) * s * 0.8f
            // Rear legs
            for (side in listOf(-1f, 1f)) {
                val leg = Path().apply {
                    val lx = x + bodyLen * 0.30f
                    val ly = y + side * bodyH * 0.45f
                    moveTo(lx, ly)
                    cubicTo(lx + s * 1.5f, ly + side * bodyH * 0.6f + legSway * side,
                        lx + s * 2.5f, ly + side * bodyH * 0.8f + legSway * side,
                        lx + s * 3.5f, ly + side * bodyH * 0.55f + legSway * side)
                    cubicTo(lx + s * 2.8f, ly + side * bodyH * 0.4f + legSway * side,
                        lx + s * 1.5f, ly + side * bodyH * 0.25f,
                        lx, ly)
                    close()
                }
                drawPath(leg, BodyMudGreen.copy(alpha = 0.70f))
            }
            // Front legs
            for (side in listOf(-1f, 1f)) {
                val leg = Path().apply {
                    val lx = x - bodyLen * 0.18f
                    val ly = y + side * bodyH * 0.50f
                    moveTo(lx, ly)
                    cubicTo(lx - s * 1.0f, ly + side * bodyH * 0.7f - legSway * side,
                        lx - s * 2.0f, ly + side * bodyH * 0.9f - legSway * side,
                        lx - s * 3.5f, ly + side * bodyH * 0.6f - legSway * side)
                    cubicTo(lx - s * 2.5f, ly + side * bodyH * 0.4f - legSway * side,
                        lx - s * 1.0f, ly + side * bodyH * 0.25f,
                        lx, ly)
                    close()
                }
                drawPath(leg, BodyMudGreen.copy(alpha = 0.70f))
            }

            // ── MAIN BODY (armored, torpedo-like) ────────────────────────
            val bodyShadow = Path().apply {
                moveTo(x - bodyLen * 0.50f, y)
                cubicTo(x - bodyLen * 0.40f, y - bodyH * 1.04f,
                    x + bodyLen * 0.30f, y - bodyH * 1.04f,
                    x + bodyLen * 0.52f, y)
                cubicTo(x + bodyLen * 0.30f, y + bodyH * 1.04f,
                    x - bodyLen * 0.40f, y + bodyH * 1.04f,
                    x - bodyLen * 0.50f, y)
                close()
            }
            drawPath(bodyShadow, ShadowDeep.copy(alpha = 0.15f))

            val body = Path().apply {
                moveTo(x - bodyLen * 0.48f, y)
                cubicTo(x - bodyLen * 0.38f, y - bodyH,
                    x + bodyLen * 0.28f, y - bodyH,
                    x + bodyLen * 0.50f, y)
                cubicTo(x + bodyLen * 0.28f, y + bodyH,
                    x - bodyLen * 0.38f, y + bodyH,
                    x - bodyLen * 0.48f, y)
                close()
            }
            drawPath(body, BodyOlive)

            // Belly (lighter underside)
            val belly = Path().apply {
                moveTo(x - bodyLen * 0.40f, y + bodyH * 0.1f)
                cubicTo(x - bodyLen * 0.28f, y + bodyH * 0.85f,
                    x + bodyLen * 0.22f, y + bodyH * 0.85f,
                    x + bodyLen * 0.42f, y + bodyH * 0.1f)
                cubicTo(x + bodyLen * 0.22f, y + bodyH * 0.55f,
                    x - bodyLen * 0.18f, y + bodyH * 0.58f,
                    x - bodyLen * 0.40f, y + bodyH * 0.1f)
                close()
            }
            drawPath(belly, BellyCream.copy(alpha = 0.40f))

            // ── DORSAL SCUTES (armored ridges along back) ────────────────
            val scuteCount = 10
            for (sc in 0 until scuteCount) {
                val scFrac = sc.toFloat() / (scuteCount - 1)
                val scX = x - bodyLen * 0.35f + bodyLen * 0.75f * scFrac
                val scH = bodyH * (0.12f + sin(scFrac * PI.toFloat()) * 0.08f)
                // Scute pair
                drawLine(ScuteOlive.copy(alpha = 0.50f),
                    Offset(scX, y - bodyH * 0.75f + scH),
                    Offset(scX, y - bodyH * 0.85f),
                    strokeWidth = (1.5f * s).coerceAtLeast(0.5f))
                drawLine(ScuteDark.copy(alpha = 0.25f),
                    Offset(scX + 0.3f * s, y - bodyH * 0.75f + scH),
                    Offset(scX + 0.3f * s, y - bodyH * 0.85f),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.2f))
            }

            // Upper body highlight
            val upperHL = Path().apply {
                moveTo(x - bodyLen * 0.35f, y - bodyH * 0.30f)
                cubicTo(x - bodyLen * 0.20f, y - bodyH * 0.80f,
                    x + bodyLen * 0.15f, y - bodyH * 0.78f,
                    x + bodyLen * 0.35f, y - bodyH * 0.30f)
                cubicTo(x + bodyLen * 0.12f, y - bodyH * 0.55f,
                    x - bodyLen * 0.10f, y - bodyH * 0.58f,
                    x - bodyLen * 0.35f, y - bodyH * 0.30f)
                close()
            }
            drawPath(upperHL, ScuteOlive.copy(alpha = 0.20f))

            // ── SNOUT (long, narrow) ─────────────────────────────────────
            val snoutLen = bodyLen * 0.32f
            val snoutH = bodyH * 0.35f
            val snoutX = x - bodyLen * 0.48f

            // Upper jaw
            val upperJaw = Path().apply {
                moveTo(snoutX, y - snoutH)
                cubicTo(snoutX - snoutLen * 0.4f, y - snoutH * 0.9f,
                    snoutX - snoutLen * 0.85f, y - snoutH * 0.4f,
                    snoutX - snoutLen, y - snoutH * 0.1f)
                lineTo(snoutX - snoutLen, y)
                cubicTo(snoutX - snoutLen * 0.5f, y - snoutH * 0.05f,
                    snoutX - snoutLen * 0.2f, y - snoutH * 0.1f,
                    snoutX, y)
                close()
            }
            drawPath(upperJaw, BodyDarkOlive)

            // Lower jaw (opens with jawOpen)
            val lowerJaw = Path().apply {
                moveTo(snoutX, y + jawOpen)
                cubicTo(snoutX - snoutLen * 0.5f, y + snoutH * 0.05f + jawOpen,
                    snoutX - snoutLen * 0.8f, y + snoutH * 0.2f + jawOpen,
                    snoutX - snoutLen, y + snoutH * 0.1f + jawOpen)
                lineTo(snoutX - snoutLen, y + jawOpen * 0.5f)
                cubicTo(snoutX - snoutLen * 0.5f, y + snoutH * 0.35f + jawOpen,
                    snoutX - snoutLen * 0.2f, y + snoutH * 0.3f + jawOpen,
                    snoutX, y + snoutH * 0.5f + jawOpen)
                close()
            }
            drawPath(lowerJaw, BodyMudGreen)
            drawPath(lowerJaw, BellyCream.copy(alpha = 0.15f))

            // Teeth
            val toothCount = 7
            for (ti in 0 until toothCount) {
                val tFrac = (ti + 0.5f) / toothCount
                val tx = snoutX - snoutLen * tFrac
                // Upper teeth (down)
                drawLine(ToothWhite.copy(alpha = 0.70f),
                    Offset(tx, y - snoutH * 0.05f),
                    Offset(tx, y + snoutH * 0.12f),
                    strokeWidth = (0.6f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
                // Lower teeth (up, follow jaw)
                if (ti % 2 == 0) {
                    drawLine(ToothWhite.copy(alpha = 0.55f),
                        Offset(tx, y + snoutH * 0.20f + jawOpen),
                        Offset(tx, y + snoutH * 0.05f + jawOpen),
                        strokeWidth = (0.5f * s).coerceAtLeast(0.2f), cap = StrokeCap.Round)
                }
            }

            // ── LEVEL 3+: SCARRED TEXTURE ON SNOUT ───────────────────────
            if (level >= 3) {
                val scars = listOf(
                    Pair(-0.55f, -0.12f), Pair(-0.62f, -0.05f), Pair(-0.70f, -0.08f)
                )
                for ((sx, sy) in scars) {
                    drawLine(ScarGray.copy(alpha = 0.35f),
                        Offset(x + bodyLen * sx, y + bodyH * sy),
                        Offset(x + bodyLen * (sx - 0.04f), y + bodyH * (sy + 0.06f)),
                        strokeWidth = (0.7f * s).coerceAtLeast(0.3f), cap = StrokeCap.Round)
                }
                // Extra nostril detail
                drawCircle(NostrilDark.copy(alpha = 0.40f), s * 0.6f,
                    Offset(snoutX - snoutLen * 0.85f, y - snoutH * 0.25f))
            }

            // Nostrils (at tip of snout)
            drawCircle(NostrilDark.copy(alpha = 0.50f), s * 0.5f,
                Offset(snoutX - snoutLen * 0.90f, y - snoutH * 0.18f))

            // ── EYE (raised, reptilian) ──────────────────────────────────
            val eyeR = (1.5f + level * 0.08f) * s
            val eyeX = x - bodyLen * 0.38f
            val eyeY = y - bodyH * 0.55f

            // Eye ridge (raised bump)
            drawCircle(BodyDarkOlive.copy(alpha = 0.40f), eyeR * 2.0f, Offset(eyeX, eyeY))
            drawCircle(ScuteOlive.copy(alpha = 0.30f), eyeR * 1.6f, Offset(eyeX, eyeY))

            // Eye socket
            drawCircle(ShadowDeep.copy(alpha = 0.40f), eyeR * 1.25f, Offset(eyeX, eyeY))
            // Sclera
            drawCircle(Color(0xFFE8E0C0), eyeR, Offset(eyeX, eyeY))
            // Iris
            drawCircle(EyeGreen, eyeR * 0.65f, Offset(eyeX + eyeR * 0.05f, eyeY))
            // Vertical slit pupil
            val pupil = Path().apply {
                val px = eyeX + eyeR * 0.06f
                val py = eyeY
                moveTo(px, py - eyeR * 0.38f)
                cubicTo(px + eyeR * 0.10f, py - eyeR * 0.38f,
                    px + eyeR * 0.10f, py + eyeR * 0.38f,
                    px, py + eyeR * 0.38f)
                cubicTo(px - eyeR * 0.02f, py + eyeR * 0.38f,
                    px - eyeR * 0.02f, py - eyeR * 0.38f,
                    px, py - eyeR * 0.38f)
                close()
            }
            drawPath(pupil, ShadowDeep)
            drawCircle(Color.White, eyeR * 0.18f,
                Offset(eyeX - eyeR * 0.18f, eyeY - eyeR * 0.18f))

            // ── LEVEL 5+: PREDATORY EYE GLOW (amber) ────────────────────
            if (level >= 5) {
                val glowPulse = sin(t * 1.5f * PI.toFloat()) * 0.10f
                drawCircle(EyeAmber.copy(alpha = 0.20f + glowPulse),
                    eyeR * 2.5f, Offset(eyeX, eyeY))
                drawCircle(EyeAmber.copy(alpha = 0.10f + glowPulse * 0.5f),
                    eyeR * 3.5f, Offset(eyeX, eyeY))
            }

            // Body outline
            drawPath(body, ScuteDark.copy(alpha = 0.18f),
                style = Stroke(width = (0.5f * s).coerceAtLeast(0.2f)))
        }
    }
}
