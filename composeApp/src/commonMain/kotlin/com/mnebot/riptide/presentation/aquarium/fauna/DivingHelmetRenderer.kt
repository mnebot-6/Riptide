package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mnebot.riptide.presentation.aquarium.CreatureRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Palette ───────────────────────────────────────────────────────────────────
private val BrassBody      = Color(0xFFB8864C)   // main brass dome
private val BrassLight     = Color(0xFFD8A86C)   // highlight brass
private val BrassDark      = Color(0xFF886030)   // shadow brass
private val BrassDeep      = Color(0xFF604018)   // deep shadows
private val CopperRing     = Color(0xFFCC7840)   // neck ring copper
private val CopperDark     = Color(0xFF884820)   // ring shadow
private val ViewportBlue   = Color(0xFF3088B8)   // viewport glass
private val ViewportLight  = Color(0xFF60B8E0)   // glass highlight
private val ViewportDeep   = Color(0xFF185068)   // deep glass
private val BoltColor      = Color(0xFFD0A060)   // bolt heads
private val BubbleColor    = Color(0xFFD0F0FF)   // bubbles
private val GlowBlue       = Color(0xFF40A0E0)   // viewport glow (level 3+)
private val FishReflect    = Color(0xFF88C8E0)   // fish reflection (level 5+)

object DivingHelmetRenderer : CreatureRenderer {

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        val s = size / 30f
        val t = animTimeMs / 1000f

        val domeR = (10f + level * 0.8f) * s
        val domeCy = y - domeR * 0.8f  // dome center slightly above base

        // ── Neck ring / collar ──────────────────────────────────────────────
        val ringW = domeR * 1.15f
        val ringH = domeR * 0.25f
        val ringY = y - ringH * 0.5f

        // Ring shadow
        drawOval(
            color = CopperDark,
            topLeft = Offset(x - ringW * 1.05f, ringY - ringH * 0.5f),
            size = Size(ringW * 2.1f, ringH * 1.4f)
        )
        // Main ring
        drawOval(
            color = CopperRing,
            topLeft = Offset(x - ringW, ringY - ringH * 0.4f),
            size = Size(ringW * 2f, ringH * 1.2f)
        )
        // Ring highlight
        drawOval(
            color = BrassLight.copy(alpha = 0.3f),
            topLeft = Offset(x - ringW * 0.6f, ringY - ringH * 0.35f),
            size = Size(ringW * 1.2f, ringH * 0.4f)
        )

        // ── Bolts around the neck ring ──────────────────────────────────────
        val boltCount = 8
        val boltR = (0.8f * s).coerceAtLeast(0.4f)
        for (i in 0 until boltCount) {
            val angle = (i.toFloat() / boltCount) * 2f * PI.toFloat()
            val boltX = x + cos(angle) * ringW * 0.9f
            val boltY = ringY + sin(angle) * ringH * 0.4f
            drawCircle(BoltColor, boltR, Offset(boltX, boltY))
            drawCircle(BrassDark.copy(alpha = 0.4f), boltR * 0.5f, Offset(boltX, boltY))
        }

        // ── Main dome ───────────────────────────────────────────────────────
        // Dome shape — semicircle from neck ring upward
        val domePath = Path().apply {
            moveTo(x - domeR, ringY - ringH * 0.1f)
            cubicTo(
                x - domeR, domeCy - domeR * 0.7f,
                x - domeR * 0.5f, domeCy - domeR,
                x, domeCy - domeR
            )
            cubicTo(
                x + domeR * 0.5f, domeCy - domeR,
                x + domeR, domeCy - domeR * 0.7f,
                x + domeR, ringY - ringH * 0.1f
            )
            close()
        }
        drawPath(domePath, BrassBody)

        // Dome shadow (lower right)
        val shadowPath = Path().apply {
            moveTo(x + domeR * 0.2f, ringY - ringH * 0.1f)
            cubicTo(
                x + domeR * 0.9f, domeCy - domeR * 0.3f,
                x + domeR * 0.85f, domeCy - domeR * 0.7f,
                x + domeR * 0.3f, domeCy - domeR * 0.95f
            )
            cubicTo(
                x + domeR * 0.7f, domeCy - domeR * 0.6f,
                x + domeR * 0.75f, domeCy - domeR * 0.2f,
                x + domeR * 0.2f, ringY - ringH * 0.1f
            )
            close()
        }
        drawPath(shadowPath, BrassDark.copy(alpha = 0.35f))

        // Dome highlight (upper left)
        drawCircle(
            color = BrassLight.copy(alpha = 0.45f),
            radius = domeR * 0.3f,
            center = Offset(x - domeR * 0.3f, domeCy - domeR * 0.6f)
        )

        // Rivets on dome
        val rivetPositions = listOf(
            Offset(x - domeR * 0.6f, domeCy - domeR * 0.5f),
            Offset(x + domeR * 0.6f, domeCy - domeR * 0.5f),
            Offset(x, domeCy - domeR * 0.92f)
        )
        for (pos in rivetPositions) {
            drawCircle(BoltColor, boltR * 0.7f, pos)
        }

        // ── Viewport window (rectangular with rounded feel) ─────────────────
        val vpW = domeR * 0.55f
        val vpH = domeR * 0.65f
        val vpCy = domeCy - domeR * 0.15f

        // Viewport frame
        val vpFrame = Path().apply {
            val r = vpW * 0.2f
            moveTo(x - vpW + r, vpCy - vpH)
            lineTo(x + vpW - r, vpCy - vpH)
            cubicTo(x + vpW, vpCy - vpH, x + vpW, vpCy - vpH + r, x + vpW, vpCy - vpH + r)
            lineTo(x + vpW, vpCy + vpH - r)
            cubicTo(x + vpW, vpCy + vpH, x + vpW - r, vpCy + vpH, x + vpW - r, vpCy + vpH)
            lineTo(x - vpW + r, vpCy + vpH)
            cubicTo(x - vpW, vpCy + vpH, x - vpW, vpCy + vpH - r, x - vpW, vpCy + vpH - r)
            lineTo(x - vpW, vpCy - vpH + r)
            cubicTo(x - vpW, vpCy - vpH, x - vpW + r, vpCy - vpH, x - vpW + r, vpCy - vpH)
            close()
        }
        drawPath(vpFrame, ViewportDeep)
        drawPath(vpFrame, ViewportBlue)

        // Glass reflection — diagonal highlight
        val reflPath = Path().apply {
            moveTo(x - vpW * 0.6f, vpCy - vpH * 0.8f)
            lineTo(x - vpW * 0.2f, vpCy - vpH * 0.8f)
            lineTo(x + vpW * 0.3f, vpCy + vpH * 0.2f)
            lineTo(x - vpW * 0.1f, vpCy + vpH * 0.2f)
            close()
        }
        drawPath(reflPath, ViewportLight.copy(alpha = 0.3f))

        // Frame border
        drawPath(
            vpFrame,
            BrassDark,
            style = Stroke(width = (1.2f * s).coerceAtLeast(0.6f))
        )

        // Corner bolts on viewport
        val vpCorners = listOf(
            Offset(x - vpW * 0.85f, vpCy - vpH * 0.85f),
            Offset(x + vpW * 0.85f, vpCy - vpH * 0.85f),
            Offset(x - vpW * 0.85f, vpCy + vpH * 0.85f),
            Offset(x + vpW * 0.85f, vpCy + vpH * 0.85f)
        )
        for (corner in vpCorners) {
            drawCircle(BoltColor, boltR * 0.6f, corner)
        }

        // Level 3+: viewport faint blue glow
        if (level >= 3) {
            val glowPulse = 0.15f + sin(t * 1.5f * PI.toFloat()) * 0.08f
            drawPath(vpFrame, GlowBlue.copy(alpha = glowPulse))
        }

        // Level 5+: small fish reflection in viewport glass
        if (level >= 5) {
            val fishPhase = t * 1.2f
            val fishX = x - vpW * 0.3f + sin(fishPhase) * vpW * 0.4f
            val fishY = vpCy + cos(fishPhase * 0.7f) * vpH * 0.3f
            val fishSz = 1.8f * s
            // Tiny reflected fish shape
            val fishPath = Path().apply {
                moveTo(fishX - fishSz, fishY)
                lineTo(fishX, fishY - fishSz * 0.35f)
                lineTo(fishX + fishSz * 0.8f, fishY)
                lineTo(fishX, fishY + fishSz * 0.35f)
                close()
            }
            drawPath(fishPath, FishReflect.copy(alpha = 0.35f))
            // Tiny tail
            val tailPath = Path().apply {
                moveTo(fishX + fishSz * 0.6f, fishY)
                lineTo(fishX + fishSz * 1.1f, fishY - fishSz * 0.3f)
                lineTo(fishX + fishSz * 1.1f, fishY + fishSz * 0.3f)
                close()
            }
            drawPath(tailPath, FishReflect.copy(alpha = 0.25f))
        }

        // ── Bubbles rising from the top ─────────────────────────────────────
        val bubbleCount = 3 + level / 2
        for (b in 0 until bubbleCount) {
            val phase = b * 1.8f + 0.3f
            val cycle = ((t * 0.5f + phase) % 2.5f) / 2.5f
            val bx = x + sin(t * 1.0f + phase) * domeR * 0.3f
            val by = (domeCy - domeR) - cycle * 15f * s
            val bAlpha = (1f - cycle) * 0.5f
            val bRadius = (0.6f + cycle * 0.5f + sin(phase) * 0.2f) * s
            drawCircle(
                color = BubbleColor.copy(alpha = bAlpha),
                radius = bRadius.coerceAtLeast(0.3f),
                center = Offset(bx, by)
            )
            // Bubble shine
            drawCircle(
                color = Color.White.copy(alpha = bAlpha * 0.5f),
                radius = bRadius * 0.3f,
                center = Offset(bx - bRadius * 0.25f, by - bRadius * 0.25f)
            )
        }
    }
}
