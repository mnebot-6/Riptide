package com.mnebot.riptide.presentation.aquarium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.presentation.aquarium.flora.AnemoneRenderer
import com.mnebot.riptide.presentation.aquarium.flora.BrainCoralRenderer
import com.mnebot.riptide.presentation.aquarium.flora.KelpRenderer

interface CreatureRenderer {
    fun DrawScope.render(
        x: Float,
        y: Float,
        size: Float,
        level: Int,
        animTimeMs: Long,
        mirrored: Boolean
    )
}

private val renderers = mapOf<CreatureSpecies, CreatureRenderer>(
    CreatureSpecies.BRAIN_CORAL to BrainCoralRenderer,
    CreatureSpecies.ANEMONE to AnemoneRenderer,
    CreatureSpecies.KELP to KelpRenderer,
)

fun rendererFor(species: CreatureSpecies): CreatureRenderer? = renderers[species]

// ── Composable de criatura estática (para EcosystemScreen y CreatureDetailDialog) ──
//
// Si la especie tiene CreatureRenderer: dibuja Canvas animado (flora viva).
// Si no: muestra el emoji con texto, ajustando el tamaño a la caja disponible.
//
@Composable
fun CreatureIcon(
    spec: CreatureSpec,
    level: Int,
    modifier: Modifier = Modifier
) {
    val renderer = rendererFor(spec.species)
    if (renderer != null) {
        var animTimeMs by remember { mutableLongStateOf(0L) }
        LaunchedEffect(Unit) {
            var startNs = 0L
            withFrameNanos { startNs = it }
            while (true) {
                withFrameNanos { nanos ->
                    animTimeMs = (nanos - startNs) / 1_000_000L
                }
            }
        }
        Canvas(modifier = modifier.fillMaxSize()) {
            with(renderer) {
                render(
                    x = size.width / 2f,
                    y = size.height,
                    size = size.height,
                    level = level,
                    animTimeMs = animTimeMs,
                    mirrored = false
                )
            }
        }
    } else {
        BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val sizeDp = with(density) { constraints.maxHeight.toDp() }.value
            Text(text = spec.emoji, fontSize = (sizeDp * 0.80f).coerceAtLeast(8f).sp)
        }
    }
}
