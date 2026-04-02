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

// ── Palette ───────────────────────────────────────────────────────────────────
private val ClamShell  = Color(0xFFAAAB90)  // grey-white shell
private val ClamShell2 = Color(0xFF888A75)  // darker shell ridge
private val ClamInner  = Color(0xFF0085BB)  // brilliant blue interior
private val ClamInner2 = Color(0xFF00AADE)  // lighter blue centre
private val ClamMantle = Color(0xFF005580)  // dark mantle edge
private val ClamHinge  = Color(0xFF666655)  // hinge color

object GiantClamRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        // Fixed floor creature — y is base (floor), draw upward
        val s = size / 28f
        val t = animTimeMs / 1000f

        val clamW  = (14f + level * 0.8f) * s
        val clamH  = (9f  + level * 0.5f) * s
        val ribCount = 7 + level / 2
        val centerY = y - clamH * 0.45f

        // Very slow breathing (mantle opening/closing)
        val openCycle = (animTimeMs % 7000L).toFloat() / 7000f
        val gapFrac   = sin(openCycle * 2f * PI.toFloat()) * 0.12f + 0.18f
        val gapY      = clamH * gapFrac

        // ── LOWER VALVE ───────────────────────────────────────────────────────
        val lower = paths.obtain().apply {
            moveTo(x - clamW, centerY + clamH * 0.2f)
            cubicTo(x - clamW * 0.7f, centerY + clamH * 0.85f,
                x + clamW * 0.7f, centerY + clamH * 0.85f,
                x + clamW, centerY + clamH * 0.2f)
            cubicTo(x + clamW * 0.5f, centerY + clamH * 0.05f,
                x - clamW * 0.5f, centerY + clamH * 0.05f,
                x - clamW, centerY + clamH * 0.2f)
            close()
        }
        drawPath(lower, ClamShell)

        // Ribs on lower valve (radiating from hinge)
        for (r in 0..ribCount) {
            val rf = r.toFloat() / ribCount.toFloat()
            val rx = x - clamW * (1f - 2f * rf)
            drawLine(
                ClamShell2.copy(alpha = 0.45f),
                Offset(x, centerY + clamH * 0.1f),
                Offset(rx, centerY + clamH * 0.82f),
                strokeWidth = (0.8f * s).coerceAtLeast(0.4f)
            )
        }
        drawPath(lower, ClamShell2.copy(alpha = 0.30f),
            style = Stroke(width = (1.0f * s).coerceAtLeast(0.5f)))

        // ── BLUE INTERIOR (mantle tissue) ─────────────────────────────────────
        val inner = paths.obtain().apply {
            moveTo(x - clamW * 0.8f, centerY + gapY + clamH * 0.18f)
            cubicTo(x - clamW * 0.6f, centerY + gapY + clamH * 0.70f,
                x + clamW * 0.6f, centerY + gapY + clamH * 0.70f,
                x + clamW * 0.8f, centerY + gapY + clamH * 0.18f)
            cubicTo(x + clamW * 0.4f, centerY + gapY + clamH * 0.05f,
                x - clamW * 0.4f, centerY + gapY + clamH * 0.05f,
                x - clamW * 0.8f, centerY + gapY + clamH * 0.18f)
            close()
        }
        drawPath(inner, ClamInner.copy(alpha = (gapFrac * 3.5f).coerceIn(0.6f, 1.0f)))
        // Inner glow
        drawCircle(ClamInner2.copy(alpha = 0.40f), clamW * 0.35f, Offset(x, centerY + gapY + clamH * 0.40f))

        // Mantle edge (wavy, blue-dark)
        val mantleEdge = paths.obtain().apply {
            val edgeY = centerY + gapY
            moveTo(x - clamW * 0.85f, edgeY + clamH * 0.15f)
            val steps = 10
            for (step in 0..steps) {
                val sf = step.toFloat() / steps.toFloat()
                val ex = x - clamW + 2f * clamW * sf
                val ey = edgeY + clamH * 0.15f + sin(sf * 5f * PI.toFloat() + t * 2f * PI.toFloat()) * clamH * 0.05f
                if (step == 0) moveTo(ex, ey) else lineTo(ex, ey)
            }
        }
        drawPath(mantleEdge, ClamMantle.copy(alpha = 0.55f),
            style = Stroke(width = (1.2f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round))

        // ── UPPER VALVE (slightly open) ───────────────────────────────────────
        val upper = paths.obtain().apply {
            val topY = centerY - gapY
            moveTo(x - clamW, topY + clamH * 0.12f)
            cubicTo(x - clamW * 0.6f, topY - clamH * 0.6f,
                x + clamW * 0.6f, topY - clamH * 0.6f,
                x + clamW, topY + clamH * 0.12f)
            // Wavy upper edge (characteristic giant clam shape)
            val steps = 8
            val waveAmp = clamH * 0.10f
            for (step in steps downTo 0) {
                val sf = step.toFloat() / steps.toFloat()
                val ux = x - clamW + 2f * clamW * sf
                val uy = topY + clamH * 0.12f + sin(sf * 4f * PI.toFloat()) * waveAmp
                lineTo(ux, uy)
            }
            close()
        }
        drawPath(upper, ClamShell)

        // Ribs on upper valve
        for (r in 0..ribCount) {
            val rf = r.toFloat() / ribCount.toFloat()
            val rx = x - clamW * (1f - 2f * rf)
            val gapYActual = centerY - gapY
            drawLine(
                ClamShell2.copy(alpha = 0.45f),
                Offset(x, gapYActual - clamH * 0.05f),
                Offset(rx, gapYActual - clamH * 0.58f),
                strokeWidth = (0.8f * s).coerceAtLeast(0.4f)
            )
        }
        drawPath(upper, ClamShell2.copy(alpha = 0.30f),
            style = Stroke(width = (1.0f * s).coerceAtLeast(0.5f)))

        // ── HINGE (centre bottom) ──────────────────────────────────────────────
        drawCircle(ClamHinge, (1.8f * s).coerceAtLeast(0.8f), Offset(x, centerY + clamH * 0.15f))

        // ── LEVEL 3+: Iridescent blue shimmer ─────────────────────────────────
        if (level >= 3) {
            val shimmer = (sin(t * 1.2f * PI.toFloat()) * 0.5f + 0.5f) * 0.25f
            drawCircle(ClamInner2.copy(alpha = shimmer), clamW * 0.6f, Offset(x, centerY + gapY + clamH * 0.35f))
        }
    }
}
