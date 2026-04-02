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
import kotlin.math.abs
import kotlin.math.sin

// ── Paleta Bimba ─────────────────────────────────────────────────────────────
private val LabGold      = Color(0xFFF0C060)   // pelaje dorado claro
private val LabGoldDark  = Color(0xFFD4933A)   // pelaje dorado oscuro / sombras
private val LabGoldDeep  = Color(0xFFB87830)   // contorno / orejas
private val LabCream     = Color(0xFFFFF0C0)   // pecho y barriga
private val LabNose      = Color(0xFF3A2010)   // trufa
private val LabEye       = Color(0xFF1A1008)   // ojo
private val LabTongue    = Color(0xFFE86060)   // lengua afuera (feliz)
private val WaterWhite   = Color(0xAAFFFFFF)   // estela en la superficie

/**
 * Renderer de Bimba — labrador amarilla de 12 años con tres patas (falta la trasera izquierda).
 * Nada en la superficie, feliz. Easter egg desbloqueado con "premio" o "treat".
 *
 * La posición Y ya está fijada en la superficie por SwimZone.SURFACE en AquariumCreature.kt.
 */
object BimbaRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 30f
        val t = animTimeMs / 1000f

        // Parámetros de cuerpo
        val bodyLen  = 13f * s
        val bodyH    = 5.5f * s
        val headR    = 4.8f * s
        val earLen   = 5.0f * s

        // Animación: Bimba se mueve suavemente en la superficie, con chapoteo
        val paddle   = sin(t * 2.2f * PI.toFloat())          // palada de patas
        val bodyBob  = sin(t * 1.1f * PI.toFloat()) * 1.2f * s  // bamboleo vertical leve
        val tailWag  = sin(t * 3.5f * PI.toFloat()) * 3.0f * s  // cola feliz y rápida
        val tongueOut = abs(sin(t * 0.8f * PI.toFloat()))       // lengua asomándose

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── ESTELA DE AGUA ────────────────────────────────────────────────
            // Pequeñas ondas alrededor de Bimba nadando en superficie
            val wakeAlpha = 0.25f + sin(t * 2.0f * PI.toFloat()) * 0.08f
            for (i in 0..2) {
                val wx = x + bodyLen * (0.3f + i * 0.3f)
                val wr = (2.5f - i * 0.6f) * s
                drawCircle(WaterWhite.copy(alpha = wakeAlpha - i * 0.05f), wr, Offset(wx, y + bodyH * 0.85f))
            }

            // ── COLA (rabo muy movido — labrador feliz) ───────────────────────
            val tailBase = Offset(x + bodyLen * 0.45f, y - bodyH * 0.3f + bodyBob * 0.3f)
            val tailPath = paths.obtain().apply {
                moveTo(tailBase.x, tailBase.y)
                cubicTo(
                    tailBase.x + 2.5f * s, tailBase.y - 2.5f * s + tailWag * 0.4f,
                    tailBase.x + 5.0f * s, tailBase.y - 4.5f * s + tailWag,
                    tailBase.x + 6.5f * s, tailBase.y - 6.0f * s + tailWag
                )
            }
            drawPath(tailPath, LabGoldDark, style = Stroke(width = (2.2f * s).coerceAtLeast(1.2f), cap = StrokeCap.Round))
            // Cola más gruesa en la base
            drawPath(tailPath, LabGold.copy(alpha = 0.6f), style = Stroke(width = (1.2f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round))

            // ── CUERPO ────────────────────────────────────────────────────────
            val body = paths.obtain().apply {
                // Silueta redondeada típica de labrador
                moveTo(x - bodyLen * 0.42f, y + bodyBob)
                cubicTo(
                    x - bodyLen * 0.42f, y - bodyH * 0.9f + bodyBob,
                    x + bodyLen * 0.1f,  y - bodyH * 0.95f + bodyBob * 0.5f,
                    x + bodyLen * 0.45f, y - bodyH * 0.25f + bodyBob * 0.3f
                )
                cubicTo(
                    x + bodyLen * 0.45f, y + bodyH * 0.55f + bodyBob * 0.3f,
                    x + bodyLen * 0.1f,  y + bodyH * 0.80f + bodyBob * 0.5f,
                    x - bodyLen * 0.42f, y + bodyBob
                )
                close()
            }
            drawPath(body, LabGold)

            // Sombra dorsal (lomo ligeramente más oscuro)
            val dorsalShade = paths.obtain().apply {
                moveTo(x - bodyLen * 0.35f, y - bodyH * 0.7f + bodyBob)
                cubicTo(
                    x - bodyLen * 0.1f, y - bodyH * 0.95f + bodyBob * 0.5f,
                    x + bodyLen * 0.2f, y - bodyH * 0.88f + bodyBob * 0.4f,
                    x + bodyLen * 0.40f, y - bodyH * 0.25f + bodyBob * 0.3f
                )
                cubicTo(
                    x + bodyLen * 0.20f, y - bodyH * 0.55f + bodyBob * 0.35f,
                    x - bodyLen * 0.05f, y - bodyH * 0.72f + bodyBob * 0.45f,
                    x - bodyLen * 0.35f, y - bodyH * 0.7f + bodyBob
                )
                close()
            }
            drawPath(dorsalShade, LabGoldDark.copy(alpha = 0.35f))

            // Barriga crema
            val belly = paths.obtain().apply {
                moveTo(x - bodyLen * 0.30f, y + bodyBob * 0.8f)
                cubicTo(
                    x - bodyLen * 0.10f, y + bodyH * 0.78f + bodyBob * 0.5f,
                    x + bodyLen * 0.18f, y + bodyH * 0.72f + bodyBob * 0.4f,
                    x + bodyLen * 0.38f, y + bodyH * 0.35f + bodyBob * 0.3f
                )
                cubicTo(
                    x + bodyLen * 0.20f, y + bodyH * 0.60f + bodyBob * 0.35f,
                    x - bodyLen * 0.05f, y + bodyH * 0.65f + bodyBob * 0.45f,
                    x - bodyLen * 0.30f, y + bodyBob * 0.8f
                )
                close()
            }
            drawPath(belly, LabCream.copy(alpha = 0.50f))

            // ── PATAS (solo 3 — le falta la trasera izquierda) ───────────────
            // Las patas hacen palada alternada en el agua
            val paddleA = paddle * 1.8f * s
            val paddleB = -paddle * 1.8f * s

            // Pata delantera derecha (visible)
            drawLine(
                LabGoldDark,
                Offset(x - bodyLen * 0.22f, y + bodyH * 0.70f + bodyBob * 0.5f),
                Offset(x - bodyLen * 0.22f + paddleA * 0.5f, y + bodyH * 1.55f + paddleA + bodyBob * 0.3f),
                strokeWidth = (2.0f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round
            )
            // Pata delantera izquierda
            drawLine(
                LabGoldDark,
                Offset(x - bodyLen * 0.05f, y + bodyH * 0.75f + bodyBob * 0.5f),
                Offset(x - bodyLen * 0.05f + paddleB * 0.5f, y + bodyH * 1.55f + paddleB + bodyBob * 0.3f),
                strokeWidth = (2.0f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round
            )
            // Pata trasera derecha (única pata trasera que tiene)
            drawLine(
                LabGoldDark,
                Offset(x + bodyLen * 0.28f, y + bodyH * 0.60f + bodyBob * 0.3f),
                Offset(x + bodyLen * 0.28f + paddleA * 0.4f, y + bodyH * 1.45f + paddleA * 0.8f + bodyBob * 0.2f),
                strokeWidth = (1.8f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round
            )
            // NO hay pata trasera izquierda — Bimba perdió esa pata

            // ── CABEZA ────────────────────────────────────────────────────────
            val headX = x - bodyLen * 0.42f - headR * 0.6f
            val headY = y - bodyH * 0.25f + bodyBob
            drawCircle(LabGold, headR, Offset(headX, headY))
            // Morro (un poco más oscuro y prominente)
            drawCircle(LabGoldDark.copy(alpha = 0.6f), headR * 0.55f,
                Offset(headX - headR * 0.55f, headY + headR * 0.15f))

            // ── OREJAS (caídas, como labrador) ────────────────────────────────
            // Oreja derecha (visible)
            val earSwing = sin(t * 2.2f * PI.toFloat()) * 0.8f * s
            val earPath = paths.obtain().apply {
                moveTo(headX - headR * 0.3f, headY - headR * 0.6f)
                cubicTo(
                    headX - headR * 0.5f, headY - headR * 0.1f + earSwing,
                    headX - headR * 0.8f, headY + headR * 0.5f + earSwing,
                    headX - headR * 0.6f, headY + headR * 0.9f + earSwing
                )
                cubicTo(
                    headX - headR * 0.3f, headY + headR * 0.9f + earSwing * 0.8f,
                    headX + headR * 0.0f, headY + headR * 0.4f + earSwing * 0.5f,
                    headX + headR * 0.1f, headY - headR * 0.5f
                )
                close()
            }
            drawPath(earPath, LabGoldDeep)
            drawPath(earPath, LabGoldDark.copy(alpha = 0.4f))

            // ── TRUFA ──────────────────────────────────────────────────────────
            val noseX = headX - headR * 0.85f
            val noseY = headY + headR * 0.10f
            drawCircle(LabNose, headR * 0.28f, Offset(noseX, noseY))
            // Brillo de trufa
            drawCircle(Color.White.copy(alpha = 0.30f), headR * 0.10f,
                Offset(noseX - headR * 0.10f, noseY - headR * 0.10f))

            // ── LENGUA (saca la lengua de vez en cuando) ──────────────────────
            if (tongueOut > 0.3f) {
                val tongueAlpha = ((tongueOut - 0.3f) / 0.7f).coerceIn(0f, 1f)
                val tongueLen = 2.0f * s * tongueAlpha
                val tonguePath = paths.obtain().apply {
                    moveTo(noseX + headR * 0.15f, noseY + headR * 0.25f)
                    cubicTo(
                        noseX - headR * 0.05f, noseY + headR * 0.55f + tongueLen * 0.3f,
                        noseX + headR * 0.35f, noseY + headR * 0.65f + tongueLen,
                        noseX + headR * 0.20f, noseY + headR * 0.80f + tongueLen
                    )
                }
                drawPath(tonguePath, LabTongue.copy(alpha = tongueAlpha),
                    style = Stroke(width = (1.5f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round))
            }

            // ── OJO ───────────────────────────────────────────────────────────
            val eyeX = headX - headR * 0.35f
            val eyeY = headY - headR * 0.20f
            val eyeR = (1.1f + level * 0.05f) * s
            drawCircle(LabGoldDark, eyeR * 1.5f, Offset(eyeX, eyeY))        // parche
            drawCircle(LabCream.copy(alpha = 0.7f), eyeR * 1.1f, Offset(eyeX, eyeY))  // esclera
            drawCircle(LabEye, eyeR * 0.75f, Offset(eyeX + eyeR * 0.05f, eyeY))  // pupila
            drawCircle(Color.White.copy(alpha = 0.55f), eyeR * 0.22f,
                Offset(eyeX - eyeR * 0.25f, eyeY - eyeR * 0.28f))          // brillo

            // ── Collar azul marino (siempre visible — Bimba es decoración) ────
            run {
                val collarX = headX + headR * 0.3f
                val collarY = headY + headR * 0.7f
                drawLine(
                    Color(0xFF2A6A9A).copy(alpha = 0.85f),
                    Offset(collarX - headR * 0.4f, collarY),
                    Offset(collarX + headR * 0.5f, collarY - headR * 0.1f),
                    strokeWidth = (1.6f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round
                )
                // Colgante del collar (corazón pequeño)
                drawCircle(Color(0xFF4A9ACA), (0.7f * s).coerceAtLeast(0.5f),
                    Offset(collarX + headR * 0.05f, collarY + headR * 0.25f))
            }

            // ── Destello dorado (siempre visible — Bimba es decoración) ───────
            run {
                val sheenPulse = (sin(t * 1.4f * PI.toFloat()) * 0.5f + 0.5f) * 0.18f + 0.10f
                drawCircle(
                    Color(0xFFFFD060).copy(alpha = sheenPulse),
                    bodyH * 1.8f,
                    Offset(x, y - bodyH * 0.1f + bodyBob)
                )
            }
        }
    }
}
