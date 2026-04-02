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

// ── Paleta ───────────────────────────────────────────────────────────────────
private val MantaDorsal    = Color(0xFF1A1A3E)
private val MantaWingOuter = Color(0xFF252550)
private val MantaWingInner = Color(0xFF303068)
private val MantaBelly     = Color(0xFFBBBBDD)
private val MantaSpot      = Color(0xFF8888BB)
private val MantaGill      = Color(0xFF404070)
private val MantaEdge      = Color(0xFF3A3A6E)

/**
 * Manta raya orientada HORIZONTALMENTE:
 *   - Cabeza a la izquierda (mirrored=false → nada hacia la izquierda)
 *   - Alas se extienden hacia arriba/abajo (envergadura vertical)
 *   - Cola a la derecha
 *   - Aleteo: las puntas de las alas oscilan en el eje Y
 *
 * Cuando mirrored=true la escala se invierte en X → nada hacia la derecha.
 */
object MantaRayRenderer : CreatureRenderer {
    private val paths = PathPool()

    override fun DrawScope.render(
        x: Float, y: Float, size: Float, level: Int,
        animTimeMs: Long, mirrored: Boolean
    ) {
        paths.begin()
        val s = size / 28f   // factor de escala base

        // ── Dimensiones ──
        val bodyLen  = (12f + level * 1.0f) * s    // largo del cuerpo (eje X)
        val bodyHalf = (4f  + level * 0.4f) * s    // medio-alto del cuerpo
        val wingSpan = (16f + level * 1.5f) * s    // distancia del cuerpo a punta del ala (eje Y)
        val tailLen  = (10f + level * 1.0f) * s    // largo de la cola
        val cephLen  = (2.5f + level * 0.25f) * s  // largo aletas cefálicas

        // ── Aleteo ──
        // Onda que recorre el ala desde la raíz hasta la punta
        val t = animTimeMs / 1000f
        val flapBase  = sin(t * 2.5f * PI.toFloat())                  // ~1.25 ciclos/s
        val flapMid   = sin(t * 2.5f * PI.toFloat() - 0.6f)          // retraso
        val flapTip   = sin(t * 2.5f * PI.toFloat() - 1.2f)          // mayor retraso → onda
        val flapAmp   = (3.5f + level * 0.4f) * s

        // Desplazamiento X de cada sección del ala (simula ondulación)
        val rootDx = flapBase * flapAmp * 0.15f
        val midDx  = flapMid  * flapAmp * 0.5f
        val tipDx  = flapTip  * flapAmp * 1.0f

        withTransform({
            if (mirrored) scale(-1f, 1f, pivot = Offset(x, y))
        }) {

            // ════════════════════════════════════════════════════════════════
            // ALA SUPERIOR (arriba del cuerpo, punta va hacia arriba)
            // ════════════════════════════════════════════════════════════════
            val topWing = paths.obtain().apply {
                // Raíz delantera (pegada al cuerpo, lado cabeza)
                moveTo(x - bodyLen * 0.5f, y - bodyHalf * 0.6f)
                // Borde delantero del ala → hacia la punta
                cubicTo(
                    x - bodyLen * 0.3f + rootDx, y - wingSpan * 0.35f,
                    x - bodyLen * 0.05f + midDx,  y - wingSpan * 0.7f,
                    x + bodyLen * 0.1f + tipDx,   y - wingSpan           // punta
                )
                // Borde trasero del ala ← vuelta desde la punta
                cubicTo(
                    x + bodyLen * 0.3f + midDx,  y - wingSpan * 0.65f,
                    x + bodyLen * 0.45f + rootDx, y - wingSpan * 0.25f,
                    x + bodyLen * 0.35f, y - bodyHalf * 0.4f             // raíz trasera
                )
                close()
            }
            drawPath(topWing, MantaWingOuter)
            // Detalle interior del ala
            val topInner = paths.obtain().apply {
                moveTo(x - bodyLen * 0.35f, y - bodyHalf * 0.7f)
                cubicTo(
                    x - bodyLen * 0.15f + rootDx * 0.7f, y - wingSpan * 0.28f,
                    x + bodyLen * 0.05f + midDx * 0.7f,  y - wingSpan * 0.55f,
                    x + bodyLen * 0.15f + tipDx * 0.7f,  y - wingSpan * 0.78f
                )
            }
            drawPath(topInner, MantaWingInner.copy(alpha = 0.5f),
                style = Stroke(width = (1.8f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round))

            // ════════════════════════════════════════════════════════════════
            // ALA INFERIOR (espejo del ala superior)
            // ════════════════════════════════════════════════════════════════
            val botWing = paths.obtain().apply {
                moveTo(x - bodyLen * 0.5f, y + bodyHalf * 0.6f)
                cubicTo(
                    x - bodyLen * 0.3f - rootDx, y + wingSpan * 0.35f,
                    x - bodyLen * 0.05f - midDx,  y + wingSpan * 0.7f,
                    x + bodyLen * 0.1f - tipDx,   y + wingSpan
                )
                cubicTo(
                    x + bodyLen * 0.3f - midDx,  y + wingSpan * 0.65f,
                    x + bodyLen * 0.45f - rootDx, y + wingSpan * 0.25f,
                    x + bodyLen * 0.35f, y + bodyHalf * 0.4f
                )
                close()
            }
            drawPath(botWing, MantaWingOuter)
            val botInner = paths.obtain().apply {
                moveTo(x - bodyLen * 0.35f, y + bodyHalf * 0.7f)
                cubicTo(
                    x - bodyLen * 0.15f - rootDx * 0.7f, y + wingSpan * 0.28f,
                    x + bodyLen * 0.05f - midDx * 0.7f,  y + wingSpan * 0.55f,
                    x + bodyLen * 0.15f - tipDx * 0.7f,  y + wingSpan * 0.78f
                )
            }
            drawPath(botInner, MantaWingInner.copy(alpha = 0.5f),
                style = Stroke(width = (1.8f * s).coerceAtLeast(0.8f), cap = StrokeCap.Round))

            // ════════════════════════════════════════════════════════════════
            // CUERPO CENTRAL (forma de torpedo aplanado, horizontal)
            // ════════════════════════════════════════════════════════════════
            val body = paths.obtain().apply {
                // Desde la cabeza (izq) hasta la cola (der)
                moveTo(x - bodyLen, y)                                // punta de la cabeza
                // Borde superior del cuerpo
                cubicTo(
                    x - bodyLen * 0.7f, y - bodyHalf,
                    x + bodyLen * 0.1f, y - bodyHalf * 0.9f,
                    x + bodyLen * 0.5f, y - bodyHalf * 0.3f          // estrechamiento hacia cola
                )
                // Punto donde empieza la cola
                lineTo(x + bodyLen * 0.55f, y)
                // Borde inferior del cuerpo (vuelta)
                lineTo(x + bodyLen * 0.5f, y + bodyHalf * 0.3f)
                cubicTo(
                    x + bodyLen * 0.1f, y + bodyHalf * 0.9f,
                    x - bodyLen * 0.7f, y + bodyHalf,
                    x - bodyLen, y
                )
                close()
            }
            drawPath(body, MantaDorsal)

            // Línea dorsal (brillo sutil)
            drawLine(
                MantaEdge.copy(alpha = 0.4f),
                Offset(x - bodyLen * 0.6f, y),
                Offset(x + bodyLen * 0.4f, y),
                strokeWidth = (1.5f * s).coerceAtLeast(0.7f)
            )

            // ════════════════════════════════════════════════════════════════
            // COLA (línea fina ondulante hacia la derecha)
            // ════════════════════════════════════════════════════════════════
            val tailSway = sin(t * 3f) * 2.5f * s
            val tail = paths.obtain().apply {
                moveTo(x + bodyLen * 0.5f, y)
                cubicTo(
                    x + bodyLen * 0.5f + tailLen * 0.3f, y + tailSway * 0.4f,
                    x + bodyLen * 0.5f + tailLen * 0.6f, y + tailSway,
                    x + bodyLen * 0.5f + tailLen, y + tailSway * 0.6f
                )
            }
            drawPath(tail, MantaDorsal,
                style = Stroke(
                    width = (2f * s).coerceAtLeast(1f),
                    cap = StrokeCap.Round
                ))

            // ════════════════════════════════════════════════════════════════
            // ALETAS CEFÁLICAS (dos cuernitos a los lados de la cabeza)
            // ════════════════════════════════════════════════════════════════
            val cephSway = sin(t * 2f) * 0.8f * s
            // Superior
            val cephTop = paths.obtain().apply {
                moveTo(x - bodyLen * 0.85f, y - bodyHalf * 0.35f)
                quadraticTo(
                    x - bodyLen - cephLen * 0.4f, y - bodyHalf * 0.6f + cephSway,
                    x - bodyLen - cephLen * 0.2f, y - bodyHalf - cephLen + cephSway
                )
            }
            // Inferior
            val cephBot = paths.obtain().apply {
                moveTo(x - bodyLen * 0.85f, y + bodyHalf * 0.35f)
                quadraticTo(
                    x - bodyLen - cephLen * 0.4f, y + bodyHalf * 0.6f - cephSway,
                    x - bodyLen - cephLen * 0.2f, y + bodyHalf + cephLen - cephSway
                )
            }
            val cephStroke = (2.2f * s).coerceAtLeast(1f)
            drawPath(cephTop, MantaDorsal, style = Stroke(width = cephStroke, cap = StrokeCap.Round))
            drawPath(cephBot, MantaDorsal, style = Stroke(width = cephStroke, cap = StrokeCap.Round))

            // ════════════════════════════════════════════════════════════════
            // OJOS (a los lados de la cabeza)
            // ════════════════════════════════════════════════════════════════
            val eyeR = (1.3f + level * 0.15f) * s
            val eyeX = x - bodyLen * 0.65f
            drawCircle(Color.White.copy(alpha = 0.85f), eyeR, Offset(eyeX, y - bodyHalf * 0.55f))
            drawCircle(Color.White.copy(alpha = 0.85f), eyeR, Offset(eyeX, y + bodyHalf * 0.55f))
            drawCircle(Color(0xFF111133), eyeR * 0.5f, Offset(eyeX, y - bodyHalf * 0.55f))
            drawCircle(Color(0xFF111133), eyeR * 0.5f, Offset(eyeX, y + bodyHalf * 0.55f))

            // ════════════════════════════════════════════════════════════════
            // MARCAS VENTRALES (nivel 3+) — manchas claras en el vientre
            // ════════════════════════════════════════════════════════════════
            if (level >= 3) {
                val spotR = (1.5f + level * 0.25f) * s
                drawCircle(MantaBelly.copy(alpha = 0.35f), spotR, Offset(x - bodyLen * 0.15f, y))
                drawCircle(MantaSpot.copy(alpha = 0.3f), spotR * 0.65f, Offset(x + bodyLen * 0.1f, y - bodyHalf * 0.25f))
                drawCircle(MantaSpot.copy(alpha = 0.3f), spotR * 0.65f, Offset(x + bodyLen * 0.1f, y + bodyHalf * 0.25f))
            }

            // ════════════════════════════════════════════════════════════════
            // RANURAS BRANQUIALES (nivel 5+)
            // ════════════════════════════════════════════════════════════════
            if (level >= 5) {
                val gStroke = (1f * s).coerceAtLeast(0.6f)
                for (g in 0 until 3) {
                    val gx = x - bodyLen * (0.3f - g * 0.15f)
                    // Superior
                    drawLine(
                        MantaGill.copy(alpha = 0.45f),
                        Offset(gx, y - bodyHalf * 0.35f),
                        Offset(gx + bodyLen * 0.05f, y - bodyHalf * 0.6f),
                        strokeWidth = gStroke
                    )
                    // Inferior
                    drawLine(
                        MantaGill.copy(alpha = 0.45f),
                        Offset(gx, y + bodyHalf * 0.35f),
                        Offset(gx + bodyLen * 0.05f, y + bodyHalf * 0.6f),
                        strokeWidth = gStroke
                    )
                }
            }

            // ════════════════════════════════════════════════════════════════
            // BORDE DE LAS ALAS (contorno sutil para definir silueta)
            // ════════════════════════════════════════════════════════════════
            val edgeStroke = (0.8f * s).coerceAtLeast(0.5f)
            drawPath(topWing, MantaEdge.copy(alpha = 0.3f), style = Stroke(width = edgeStroke))
            drawPath(botWing, MantaEdge.copy(alpha = 0.3f), style = Stroke(width = edgeStroke))
        }
    }
}
