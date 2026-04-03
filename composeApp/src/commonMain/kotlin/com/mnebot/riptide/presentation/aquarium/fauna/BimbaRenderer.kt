package com.mnebot.riptide.presentation.aquarium.fauna

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
private val LabGold      = Color(0xFFE8C870)   // pelaje dorado cálido
private val LabGoldDark  = Color(0xFFCDA040)   // pelaje dorado oscuro / sombras
private val LabGoldDeep  = Color(0xFFB08030)   // contorno / orejas
private val LabCream     = Color(0xFFFFF2D0)   // pecho y barriga crema suave
private val LabNose      = Color(0xFF3A2010)   // trufa
private val LabEye       = Color(0xFF2A1808)   // ojo cálido
private val LabTongue    = Color(0xFFE86878)   // lengua rosada feliz
private val BimbaPink    = Color(0xFFF48FB1)   // rosa favorito de Bimba
private val BimbaPinkDark= Color(0xFFE06090)   // rosa más intenso (corazón collar)
private val WaterWhite   = Color(0xAAFFFFFF)   // estela en la superficie
private val LabWhiteMuzzle = Color(0xFFF5E8D0) // hocico canoso (perra mayor)

/**
 * Renderer de Bimba — labrador hembra mayor amarilla con tres patas (falta la trasera izquierda).
 * Nada en la superficie, feliz. Super buena y super dulce. Su color favorito es el rosa.
 * Lleva un collar rosa con un colgante de corazón.
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

        // Parámetros de cuerpo — proporciones suaves de labrador mayor
        val bodyLen  = 13f * s
        val bodyH    = 5.8f * s
        val headR    = 5.0f * s
        val earLen   = 5.0f * s

        // Animación: Bimba nada suavemente — movimientos tranquilos, digna
        val paddle   = sin(t * 1.8f * PI.toFloat())          // palada de patas (más lenta, mayor)
        val bodyBob  = sin(t * 0.9f * PI.toFloat()) * 1.0f * s  // bamboleo suave
        val tailWag  = sin(t * 3.0f * PI.toFloat()) * 2.8f * s  // cola feliz
        val tongueOut = abs(sin(t * 0.6f * PI.toFloat()))       // lengua asomándose

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ── ESTELA DE AGUA ────────────────────────────────────────────────
            val wakeAlpha = 0.22f + sin(t * 1.8f * PI.toFloat()) * 0.06f
            for (i in 0..2) {
                val wx = x + bodyLen * (0.3f + i * 0.3f)
                val wr = (2.2f - i * 0.5f) * s
                drawCircle(WaterWhite.copy(alpha = wakeAlpha - i * 0.04f), wr, Offset(wx, y + bodyH * 0.85f))
            }

            // ── COLA (movimiento relajado de labrador feliz) ─────────────────
            val tailBase = Offset(x + bodyLen * 0.45f, y - bodyH * 0.3f + bodyBob * 0.3f)
            val tailPath = paths.obtain().apply {
                moveTo(tailBase.x, tailBase.y)
                cubicTo(
                    tailBase.x + 2.0f * s, tailBase.y - 2.5f * s + tailWag * 0.3f,
                    tailBase.x + 4.5f * s, tailBase.y - 4.5f * s + tailWag * 0.7f,
                    tailBase.x + 6.0f * s, tailBase.y - 5.5f * s + tailWag
                )
            }
            drawPath(tailPath, LabGoldDark, style = Stroke(width = (2.4f * s).coerceAtLeast(1.2f), cap = StrokeCap.Round))
            drawPath(tailPath, LabGold.copy(alpha = 0.5f), style = Stroke(width = (1.4f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round))

            // ── CUERPO — silueta redondeada, un poco más llenita (perra mayor) ─
            val body = paths.obtain().apply {
                moveTo(x - bodyLen * 0.42f, y + bodyBob)
                cubicTo(
                    x - bodyLen * 0.42f, y - bodyH * 0.85f + bodyBob,
                    x + bodyLen * 0.08f,  y - bodyH * 0.92f + bodyBob * 0.5f,
                    x + bodyLen * 0.45f, y - bodyH * 0.22f + bodyBob * 0.3f
                )
                cubicTo(
                    x + bodyLen * 0.45f, y + bodyH * 0.58f + bodyBob * 0.3f,
                    x + bodyLen * 0.1f,  y + bodyH * 0.82f + bodyBob * 0.5f,
                    x - bodyLen * 0.42f, y + bodyBob
                )
                close()
            }
            drawPath(body, LabGold)

            // Sombra dorsal suave (lomo)
            val dorsalShade = paths.obtain().apply {
                moveTo(x - bodyLen * 0.30f, y - bodyH * 0.65f + bodyBob)
                cubicTo(
                    x - bodyLen * 0.05f, y - bodyH * 0.90f + bodyBob * 0.5f,
                    x + bodyLen * 0.18f, y - bodyH * 0.82f + bodyBob * 0.4f,
                    x + bodyLen * 0.38f, y - bodyH * 0.22f + bodyBob * 0.3f
                )
                cubicTo(
                    x + bodyLen * 0.18f, y - bodyH * 0.50f + bodyBob * 0.35f,
                    x - bodyLen * 0.02f, y - bodyH * 0.65f + bodyBob * 0.45f,
                    x - bodyLen * 0.30f, y - bodyH * 0.65f + bodyBob
                )
                close()
            }
            drawPath(dorsalShade, LabGoldDark.copy(alpha = 0.28f))

            // Barriga crema cálida
            val belly = paths.obtain().apply {
                moveTo(x - bodyLen * 0.28f, y + bodyBob * 0.8f)
                cubicTo(
                    x - bodyLen * 0.08f, y + bodyH * 0.78f + bodyBob * 0.5f,
                    x + bodyLen * 0.18f, y + bodyH * 0.72f + bodyBob * 0.4f,
                    x + bodyLen * 0.36f, y + bodyH * 0.35f + bodyBob * 0.3f
                )
                cubicTo(
                    x + bodyLen * 0.20f, y + bodyH * 0.60f + bodyBob * 0.35f,
                    x - bodyLen * 0.03f, y + bodyH * 0.65f + bodyBob * 0.45f,
                    x - bodyLen * 0.28f, y + bodyBob * 0.8f
                )
                close()
            }
            drawPath(belly, LabCream.copy(alpha = 0.55f))

            // ── PATAS (solo 3 — le falta la trasera izquierda) ───────────────
            val paddleA = paddle * 1.5f * s
            val paddleB = -paddle * 1.5f * s

            // Pata delantera derecha
            drawLine(
                LabGoldDark,
                Offset(x - bodyLen * 0.22f, y + bodyH * 0.70f + bodyBob * 0.5f),
                Offset(x - bodyLen * 0.22f + paddleA * 0.4f, y + bodyH * 1.50f + paddleA + bodyBob * 0.3f),
                strokeWidth = (2.0f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round
            )
            // Pata delantera izquierda
            drawLine(
                LabGoldDark,
                Offset(x - bodyLen * 0.05f, y + bodyH * 0.75f + bodyBob * 0.5f),
                Offset(x - bodyLen * 0.05f + paddleB * 0.4f, y + bodyH * 1.50f + paddleB + bodyBob * 0.3f),
                strokeWidth = (2.0f * s).coerceAtLeast(1.0f), cap = StrokeCap.Round
            )
            // Pata trasera derecha (única pata trasera)
            drawLine(
                LabGoldDark,
                Offset(x + bodyLen * 0.28f, y + bodyH * 0.60f + bodyBob * 0.3f),
                Offset(x + bodyLen * 0.28f + paddleA * 0.3f, y + bodyH * 1.40f + paddleA * 0.7f + bodyBob * 0.2f),
                strokeWidth = (1.8f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round
            )
            // NO hay pata trasera izquierda — Bimba perdió esa pata

            // ── CABEZA — más grande y dulce, hocico canoso ──────────────────
            val headX = x - bodyLen * 0.42f - headR * 0.55f
            val headY = y - bodyH * 0.22f + bodyBob

            // Cabeza principal
            drawCircle(LabGold, headR, Offset(headX, headY))

            // Hocico — canoso/blanquecino (perra mayor)
            drawCircle(LabWhiteMuzzle.copy(alpha = 0.7f), headR * 0.52f,
                Offset(headX - headR * 0.52f, headY + headR * 0.18f))
            drawCircle(LabGoldDark.copy(alpha = 0.3f), headR * 0.52f,
                Offset(headX - headR * 0.52f, headY + headR * 0.18f))

            // ── OREJAS (caídas, suaves, de labrador) ─────────────────────────
            val earSwing = sin(t * 1.8f * PI.toFloat()) * 0.6f * s
            val earPath = paths.obtain().apply {
                moveTo(headX - headR * 0.25f, headY - headR * 0.55f)
                cubicTo(
                    headX - headR * 0.5f, headY - headR * 0.05f + earSwing,
                    headX - headR * 0.75f, headY + headR * 0.55f + earSwing,
                    headX - headR * 0.55f, headY + headR * 0.95f + earSwing
                )
                cubicTo(
                    headX - headR * 0.25f, headY + headR * 0.90f + earSwing * 0.8f,
                    headX + headR * 0.05f, headY + headR * 0.40f + earSwing * 0.5f,
                    headX + headR * 0.15f, headY - headR * 0.45f
                )
                close()
            }
            drawPath(earPath, LabGoldDeep)
            drawPath(earPath, LabGoldDark.copy(alpha = 0.35f))

            // Oreja trasera (sutil, visible por detrás de la cabeza)
            val earBack = paths.obtain().apply {
                moveTo(headX + headR * 0.35f, headY - headR * 0.50f)
                cubicTo(
                    headX + headR * 0.55f, headY + headR * 0.10f + earSwing * 0.5f,
                    headX + headR * 0.70f, headY + headR * 0.60f + earSwing * 0.5f,
                    headX + headR * 0.55f, headY + headR * 0.85f + earSwing * 0.5f
                )
                cubicTo(
                    headX + headR * 0.35f, headY + headR * 0.70f + earSwing * 0.3f,
                    headX + headR * 0.20f, headY + headR * 0.30f + earSwing * 0.2f,
                    headX + headR * 0.35f, headY - headR * 0.50f
                )
                close()
            }
            drawPath(earBack, LabGoldDeep.copy(alpha = 0.6f))

            // ── TRUFA ──────────────────────────────────────────────────────────
            val noseX = headX - headR * 0.82f
            val noseY = headY + headR * 0.12f
            drawCircle(LabNose, headR * 0.26f, Offset(noseX, noseY))
            // Brillo de trufa húmeda
            drawCircle(Color.White.copy(alpha = 0.35f), headR * 0.09f,
                Offset(noseX - headR * 0.08f, noseY - headR * 0.08f))

            // ── BOCA / SONRISA sutil ──────────────────────────────────────────
            val smilePath = paths.obtain().apply {
                moveTo(noseX + headR * 0.08f, noseY + headR * 0.22f)
                cubicTo(
                    noseX + headR * 0.20f, noseY + headR * 0.38f,
                    noseX + headR * 0.45f, noseY + headR * 0.35f,
                    noseX + headR * 0.55f, noseY + headR * 0.25f
                )
            }
            drawPath(smilePath, LabGoldDark.copy(alpha = 0.5f),
                style = Stroke(width = (0.8f * s).coerceAtLeast(0.5f), cap = StrokeCap.Round))

            // ── LENGUA (saca la lengua de vez en cuando — feliz) ─────────────
            if (tongueOut > 0.35f) {
                val tongueAlpha = ((tongueOut - 0.35f) / 0.65f).coerceIn(0f, 1f)
                val tongueLen = 1.8f * s * tongueAlpha
                val tonguePath = paths.obtain().apply {
                    moveTo(noseX + headR * 0.15f, noseY + headR * 0.28f)
                    cubicTo(
                        noseX + headR * 0.05f, noseY + headR * 0.52f + tongueLen * 0.3f,
                        noseX + headR * 0.25f, noseY + headR * 0.62f + tongueLen,
                        noseX + headR * 0.18f, noseY + headR * 0.78f + tongueLen
                    )
                }
                drawPath(tonguePath, LabTongue.copy(alpha = tongueAlpha),
                    style = Stroke(width = (1.6f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round))
            }

            // ── OJO — dulce, expresivo, de perra mayor ───────────────────────
            val eyeX = headX - headR * 0.30f
            val eyeY = headY - headR * 0.18f
            val eyeR = (1.2f + level * 0.04f) * s
            // Parche alrededor del ojo (más claro que el cuerpo)
            drawCircle(LabCream.copy(alpha = 0.4f), eyeR * 1.8f, Offset(eyeX, eyeY))
            // Esclera
            drawCircle(LabCream.copy(alpha = 0.8f), eyeR * 1.15f, Offset(eyeX, eyeY))
            // Pupila (marrón cálido, no negro — mirada dulce)
            drawCircle(Color(0xFF4A2810), eyeR * 0.8f, Offset(eyeX + eyeR * 0.03f, eyeY))
            // Brillo
            drawCircle(Color.White.copy(alpha = 0.60f), eyeR * 0.25f,
                Offset(eyeX - eyeR * 0.22f, eyeY - eyeR * 0.25f))
            // Ceja sutil (expresión dulce)
            drawLine(
                LabGoldDark.copy(alpha = 0.4f),
                Offset(eyeX - eyeR * 1.2f, eyeY - eyeR * 1.3f),
                Offset(eyeX + eyeR * 0.8f, eyeY - eyeR * 1.5f),
                strokeWidth = (0.7f * s).coerceAtLeast(0.4f), cap = StrokeCap.Round
            )

            // ── COLLAR ROSA (su color favorito) ──────────────────────────────
            run {
                val collarX = headX + headR * 0.35f
                val collarY = headY + headR * 0.72f
                // Collar principal — rosa
                drawLine(
                    BimbaPink.copy(alpha = 0.90f),
                    Offset(collarX - headR * 0.45f, collarY),
                    Offset(collarX + headR * 0.55f, collarY - headR * 0.08f),
                    strokeWidth = (2.0f * s).coerceAtLeast(1.2f), cap = StrokeCap.Round
                )
                // Colgante — corazón rosa más intenso
                val heartX = collarX + headR * 0.05f
                val heartY = collarY + headR * 0.30f
                val hs = (0.8f * s).coerceAtLeast(0.5f)
                val heartPath = paths.obtain().apply {
                    moveTo(heartX, heartY + hs * 1.2f) // punta inferior
                    cubicTo(
                        heartX - hs * 1.5f, heartY + hs * 0.2f,
                        heartX - hs * 1.2f, heartY - hs * 1.0f,
                        heartX, heartY - hs * 0.2f
                    )
                    cubicTo(
                        heartX + hs * 1.2f, heartY - hs * 1.0f,
                        heartX + hs * 1.5f, heartY + hs * 0.2f,
                        heartX, heartY + hs * 1.2f
                    )
                    close()
                }
                drawPath(heartPath, BimbaPinkDark)
                // Brillo del corazón
                drawCircle(Color.White.copy(alpha = 0.35f), hs * 0.3f,
                    Offset(heartX - hs * 0.4f, heartY - hs * 0.3f))
            }

            // ── Aura cálida rosada (siempre visible — Bimba es decoración) ───
            run {
                val auraPulse = (sin(t * 1.2f * PI.toFloat()) * 0.5f + 0.5f) * 0.12f + 0.06f
                drawCircle(
                    BimbaPink.copy(alpha = auraPulse),
                    bodyH * 1.8f,
                    Offset(x, y - bodyH * 0.1f + bodyBob)
                )
            }
        }
    }
}
