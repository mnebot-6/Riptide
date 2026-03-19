package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.graphics.drawscope.DrawScope
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
