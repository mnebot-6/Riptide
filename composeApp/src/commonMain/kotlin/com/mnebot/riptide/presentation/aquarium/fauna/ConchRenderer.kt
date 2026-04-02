package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.mnebot.riptide.presentation.aquarium.PathPool
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

// ── Palette ─────────────────────────────────────────────────────────────────
private val ShellCream     = Color(0xFFF5E6D0)  // cream exterior
private val ShellTan       = Color(0xFFD4B896)  // tan mid-tone
private val ShellBrown     = Color(0xFFB08D6A)  // brown ridges
private val ShellDarkBrown = Color(0xFF8A6B4A)  // dark brown shadow
private val InteriorPink   = Color(0xFFF2A0B0)  // pink interior
private val InteriorDeep   = Color(0xFFD87090)  // deeper pink
private val InteriorHot    = Color(0xFFE85888)  // hot pink accent
private val PearlWhite     = Color(0xFFFFF8F0)  // pearl shimmer
private val PearlIridesc   = Color(0xFFE8D8F0)  // iridescent pearl
private val ShadowWarm     = Color(0xFF6B4E35)  // warm shadow
private val MagicGold      = Color(0xFFFFD080)  // magical glow
private val MagicPink      = Color(0xFFFF90C0)  // magical pink glow

object ConchRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        val shellW = (12f + level * 0.6f) * s
        val shellH = (9f + level * 0.5f) * s

        // Conch sits on bottom -- no mirrored transform needed (fixed creature)

        // ── SHELL SHADOW (ground contact) ────────────────────────────────────
        drawOval(
            ShadowWarm.copy(alpha = 0.25f),
            topLeft = Offset(x - shellW * 0.9f, y - shellH * 0.08f),
            size = androidx.compose.ui.geometry.Size(shellW * 1.8f, shellH * 0.18f)
        )

        // ── OUTER SHELL BODY (main spiral form) ─────────────────────────────
        val outerShell = paths.obtain().apply {
            moveTo(x - shellW * 0.7f, y - shellH * 0.1f)
            cubicTo(
                x - shellW * 0.85f, y - shellH * 0.5f,
                x - shellW * 0.6f, y - shellH * 1.0f,
                x - shellW * 0.1f, y - shellH * 1.05f
            )
            cubicTo(
                x + shellW * 0.4f, y - shellH * 1.1f,
                x + shellW * 0.8f, y - shellH * 0.7f,
                x + shellW * 0.85f, y - shellH * 0.2f
            )
            cubicTo(
                x + shellW * 0.88f, y + shellH * 0.05f,
                x + shellW * 0.6f, y + shellH * 0.1f,
                x + shellW * 0.3f, y + shellH * 0.05f
            )
            cubicTo(
                x - shellW * 0.1f, y + shellH * 0.08f,
                x - shellW * 0.5f, y + shellH * 0.05f,
                x - shellW * 0.7f, y - shellH * 0.1f
            )
            close()
        }
        drawPath(outerShell, ShellCream)

        // ── SHELL SHADING (lower half darker) ───────────────────────────────
        val shellShade = paths.obtain().apply {
            moveTo(x - shellW * 0.65f, y - shellH * 0.1f)
            cubicTo(
                x - shellW * 0.3f, y + shellH * 0.08f,
                x + shellW * 0.3f, y + shellH * 0.10f,
                x + shellW * 0.85f, y - shellH * 0.15f
            )
            cubicTo(
                x + shellW * 0.6f, y + shellH * 0.1f,
                x - shellW * 0.1f, y + shellH * 0.08f,
                x - shellW * 0.65f, y - shellH * 0.1f
            )
            close()
        }
        drawPath(shellShade, ShellTan.copy(alpha = 0.55f))

        // ── UPPER HIGHLIGHT ─────────────────────────────────────────────────
        val shellHL = paths.obtain().apply {
            moveTo(x - shellW * 0.3f, y - shellH * 0.95f)
            cubicTo(
                x + shellW * 0.1f, y - shellH * 1.0f,
                x + shellW * 0.5f, y - shellH * 0.85f,
                x + shellW * 0.7f, y - shellH * 0.5f
            )
            cubicTo(
                x + shellW * 0.4f, y - shellH * 0.7f,
                x + shellW * 0.05f, y - shellH * 0.8f,
                x - shellW * 0.3f, y - shellH * 0.95f
            )
            close()
        }
        drawPath(shellHL, PearlWhite.copy(alpha = 0.35f))

        // ── SPIRAL RIDGES (concentric curves) ───────────────────────────────
        val ridgeCount = 6
        for (i in 1..ridgeCount) {
            val frac = i.toFloat() / (ridgeCount + 1)
            val ridgeX = x - shellW * 0.5f + shellW * frac * 1.2f
            val ridgeCurve = shellH * 0.15f * sin(frac * PI.toFloat())

            val ridge = paths.obtain().apply {
                moveTo(ridgeX, y - shellH * (0.4f + frac * 0.55f))
                cubicTo(
                    ridgeX + shellW * 0.08f, y - shellH * (0.6f + frac * 0.3f) + ridgeCurve,
                    ridgeX + shellW * 0.12f, y - shellH * 0.2f + ridgeCurve,
                    ridgeX + shellW * 0.05f, y + shellH * 0.02f
                )
            }
            drawPath(
                ridge, ShellBrown.copy(alpha = 0.35f + frac * 0.15f),
                style = Stroke(
                    width = (0.8f * s * (1f + frac * 0.3f)).coerceAtLeast(0.4f),
                    cap = StrokeCap.Round
                )
            )
        }

        // ── SPIRAL APEX (tight coil at top-right) ───────────────────────────
        val apexX = x + shellW * 0.55f
        val apexY = y - shellH * 0.65f
        val apexR = shellH * 0.18f

        // Spiral coil rings
        for (ring in 0 until 3) {
            val rFrac = 1f - ring * 0.3f
            drawCircle(
                ShellDarkBrown.copy(alpha = 0.30f + ring * 0.08f),
                apexR * rFrac,
                Offset(apexX + ring * s * 0.5f, apexY + ring * s * 0.3f),
                style = Stroke(width = (0.7f * s).coerceAtLeast(0.3f))
            )
        }
        // Apex center dot
        drawCircle(ShellDarkBrown.copy(alpha = 0.50f), apexR * 0.2f, Offset(apexX + s * 1.2f, apexY + s * 0.8f))

        // ── SHELL OPENING (lip / interior visible) ──────────────────────────
        val opening = paths.obtain().apply {
            moveTo(x - shellW * 0.7f, y - shellH * 0.1f)
            cubicTo(
                x - shellW * 0.75f, y - shellH * 0.5f,
                x - shellW * 0.55f, y - shellH * 0.85f,
                x - shellW * 0.2f, y - shellH * 0.9f
            )
            cubicTo(
                x - shellW * 0.45f, y - shellH * 0.7f,
                x - shellW * 0.60f, y - shellH * 0.3f,
                x - shellW * 0.55f, y - shellH * 0.05f
            )
            cubicTo(
                x - shellW * 0.55f, y + shellH * 0.06f,
                x - shellW * 0.65f, y + shellH * 0.02f,
                x - shellW * 0.7f, y - shellH * 0.1f
            )
            close()
        }
        drawPath(opening, InteriorPink)

        // Deeper interior
        val deepInterior = paths.obtain().apply {
            moveTo(x - shellW * 0.65f, y - shellH * 0.15f)
            cubicTo(
                x - shellW * 0.68f, y - shellH * 0.45f,
                x - shellW * 0.50f, y - shellH * 0.7f,
                x - shellW * 0.30f, y - shellH * 0.78f
            )
            cubicTo(
                x - shellW * 0.45f, y - shellH * 0.55f,
                x - shellW * 0.55f, y - shellH * 0.25f,
                x - shellW * 0.52f, y - shellH * 0.05f
            )
            close()
        }
        drawPath(deepInterior, InteriorDeep.copy(alpha = 0.65f))

        // Hot pink accent line along lip
        val lip = paths.obtain().apply {
            moveTo(x - shellW * 0.72f, y - shellH * 0.08f)
            cubicTo(
                x - shellW * 0.78f, y - shellH * 0.5f,
                x - shellW * 0.58f, y - shellH * 0.88f,
                x - shellW * 0.18f, y - shellH * 0.92f
            )
        }
        drawPath(
            lip, InteriorHot.copy(alpha = 0.55f),
            style = Stroke(width = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round)
        )

        // ── OUTER SHELL EDGE ────────────────────────────────────────────────
        drawPath(
            outerShell, ShellDarkBrown.copy(alpha = 0.30f),
            style = Stroke(width = (0.6f * s).coerceAtLeast(0.3f))
        )

        // ── LEVEL 3+: Pearl shimmer inside opening ──────────────────────────
        if (level >= 3) {
            val shimmer = (sin(t * 1.5f * PI.toFloat()) * 0.5f + 0.5f) * 0.45f
            val pearlX = x - shellW * 0.50f
            val pearlY = y - shellH * 0.35f
            val pearlR = 1.8f * s

            drawCircle(PearlIridesc.copy(alpha = shimmer * 0.5f), pearlR * 2.0f, Offset(pearlX, pearlY))
            drawCircle(PearlWhite.copy(alpha = shimmer), pearlR, Offset(pearlX, pearlY))
            drawCircle(Color.White, pearlR * 0.35f, Offset(pearlX - pearlR * 0.25f, pearlY - pearlR * 0.25f))
        }

        // ── LEVEL 5+: Magical glow from within ─────────────────────────────
        if (level >= 5) {
            val glowPulse = (sin(t * 0.8f * PI.toFloat()) * 0.5f + 0.5f)
            val glowAlpha = 0.10f + glowPulse * 0.15f
            val glowX = x - shellW * 0.45f
            val glowY = y - shellH * 0.40f

            drawCircle(MagicGold.copy(alpha = glowAlpha), shellH * 0.5f, Offset(glowX, glowY))
            drawCircle(MagicPink.copy(alpha = glowAlpha * 0.6f), shellH * 0.35f, Offset(glowX, glowY))

            // Rays emanating from opening
            for (ray in 0 until 5) {
                val angle = -2.2f + ray * 0.35f
                val rayLen = shellH * 0.4f * (0.7f + glowPulse * 0.3f)
                drawLine(
                    MagicGold.copy(alpha = glowAlpha * 0.5f),
                    Offset(glowX, glowY),
                    Offset(glowX + cos(angle) * rayLen, glowY + sin(angle) * rayLen),
                    strokeWidth = (0.5f * s).coerceAtLeast(0.3f),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
