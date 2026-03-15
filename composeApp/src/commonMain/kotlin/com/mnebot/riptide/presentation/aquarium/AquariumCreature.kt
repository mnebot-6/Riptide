package com.mnebot.riptide.presentation.aquarium

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.draw.drawWithContent
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import kotlin.math.sin
import kotlin.math.max

val emojiToSpecies = mapOf(
    "🐟" to CreatureSpecies.CLOWNFISH,
    "🐠" to CreatureSpecies.ANGELFISH,
    "🐡" to CreatureSpecies.PUFFERFISH,
    "🪸" to CreatureSpecies.BRAIN_CORAL,
    "🌿" to CreatureSpecies.ANEMONE,
    "🎋" to CreatureSpecies.KELP,
    "🦞" to CreatureSpecies.LOBSTER,
    "🦀" to CreatureSpecies.HERMIT_CRAB,
    "🦐" to CreatureSpecies.SHRIMP,
    "🐚" to CreatureSpecies.SEA_URCHIN,
    "⭐" to CreatureSpecies.STARFISH,
    "🦪" to CreatureSpecies.OYSTER,
    "🦈" to CreatureSpecies.MANTA_RAY,
    "🪼" to CreatureSpecies.MOON_JELLYFISH,
    "🐋" to CreatureSpecies.WHALE_SHARK,
    "🐙" to CreatureSpecies.OCTOPUS,
    "🦑" to CreatureSpecies.SQUID,
    "🐢" to CreatureSpecies.SEA_TURTLE,
    "🐬" to CreatureSpecies.DOLPHIN,
    "🦭" to CreatureSpecies.SEAL,
    "🐳" to CreatureSpecies.BLUE_WHALE,
    "🪙" to CreatureSpecies.TREASURE_CHEST,
    "⚓" to CreatureSpecies.ANCHOR,
    "🚢" to CreatureSpecies.SUNKEN_SHIP,
)

data class CreatureSpec(
    val emoji: String,
    val species: CreatureSpecies,
    val category: MarineCategory,
    val unlockLevel: Int,
    val swimDuration: Int,        // ms; 0 = fija
    val wobbleAmplitude: Float,
    val speedScalePerLevel: Float // + más rápido, - más lento, 0 sin cambio
)

val allCreatures = listOf(
    // FISH — crecen más rápidos
    CreatureSpec("🐟", CreatureSpecies.CLOWNFISH,      MarineCategory.FISH,        2,  7000,  0.06f,  0.05f),
    CreatureSpec("🐠", CreatureSpecies.ANGELFISH,      MarineCategory.FISH,        4,  6000,  0.07f,  0.05f),
    CreatureSpec("🐡", CreatureSpecies.PUFFERFISH,     MarineCategory.FISH,        7,  9000,  0.04f,  0.03f),
    // FLORA — fijas, sin cambio de velocidad
    CreatureSpec("🪸", CreatureSpecies.BRAIN_CORAL,    MarineCategory.FLORA,       2,  0,     0.00f,  0.00f),
    CreatureSpec("🌿", CreatureSpecies.ANEMONE,        MarineCategory.FLORA,       4,  0,     0.00f,  0.00f),
    CreatureSpec("🎋", CreatureSpecies.KELP,           MarineCategory.FLORA,       6,  0,     0.00f,  0.00f),
    // CRUSTACEAN — crecen más rápidos
    CreatureSpec("🦞", CreatureSpecies.LOBSTER,        MarineCategory.CRUSTACEAN,  2,  9000,  0.03f,  0.04f),
    CreatureSpec("🦀", CreatureSpecies.HERMIT_CRAB,    MarineCategory.CRUSTACEAN,  4,  8000,  0.04f,  0.05f),
    CreatureSpec("🦐", CreatureSpecies.SHRIMP,         MarineCategory.CRUSTACEAN,  6,  6000,  0.05f,  0.06f),
    // MOLLUSK — fijas, sin cambio
    CreatureSpec("🐚", CreatureSpecies.SEA_URCHIN,     MarineCategory.MOLLUSK,     2,  0,     0.00f,  0.00f),
    CreatureSpec("⭐", CreatureSpecies.STARFISH,       MarineCategory.MOLLUSK,     4,  0,     0.00f,  0.00f),
    CreatureSpec("🦪", CreatureSpecies.OYSTER,         MarineCategory.MOLLUSK,     7,  0,     0.00f,  0.00f),
    // PELAGIC — grandes se vuelven más lentos
    CreatureSpec("🦈", CreatureSpecies.MANTA_RAY,      MarineCategory.PELAGIC,     2,  5000,  0.08f,  0.04f),
    CreatureSpec("🪼", CreatureSpecies.MOON_JELLYFISH, MarineCategory.PELAGIC,     4,  11000, 0.10f, -0.02f),
    CreatureSpec("🐋", CreatureSpecies.WHALE_SHARK,    MarineCategory.PELAGIC,     8,  14000, 0.05f, -0.04f),
    // CEPHALOPOD — rápidos
    CreatureSpec("🐙", CreatureSpecies.OCTOPUS,        MarineCategory.CEPHALOPOD,  2,  8000,  0.06f,  0.05f),
    CreatureSpec("🦑", CreatureSpecies.SQUID,          MarineCategory.CEPHALOPOD,  5,  6000,  0.08f,  0.06f),
    // REPTILE — lentos al crecer
    CreatureSpec("🐢", CreatureSpecies.SEA_TURTLE,     MarineCategory.REPTILE,     2,  12000, 0.04f, -0.03f),
    // MAMMAL — grandes más lentos
    CreatureSpec("🐬", CreatureSpecies.DOLPHIN,        MarineCategory.MAMMAL,      2,  5000,  0.10f,  0.03f),
    CreatureSpec("🦭", CreatureSpecies.SEAL,           MarineCategory.MAMMAL,      5,  10000, 0.05f, -0.02f),
    CreatureSpec("🐳", CreatureSpecies.BLUE_WHALE,     MarineCategory.MAMMAL,      8,  18000, 0.03f, -0.04f),
    // DECORATION — fijas, sin cambio
    CreatureSpec("🪙", CreatureSpecies.TREASURE_CHEST, MarineCategory.DECORATION,  1,  0,     0.00f,  0.00f),
    CreatureSpec("⚓", CreatureSpecies.ANCHOR,         MarineCategory.DECORATION,  1,  0,     0.00f,  0.00f),
    CreatureSpec("🚢", CreatureSpecies.SUNKEN_SHIP,    MarineCategory.DECORATION,  1,  0,     0.00f,  0.00f),
)

fun initialX(index: Int): Float = ((index * 137 + 50) % 80 + 10) / 100f
fun initialY(index: Int): Float = ((index * 97 + 30) % 60 + 20) / 100f
fun phaseOffset(index: Int): Float = (index * 0.618f) % 1f

expect fun DrawScope.drawEmoji(
    emoji: String,
    x: Float,
    y: Float,
    sizeSp: Float,
    mirrored: Boolean
)

@Composable
fun AquariumCreatures(
    ecosystemByCategory: Map<MarineCategory, EcosystemState>,
    creatureLevelBySpecies: Map<CreatureSpecies, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val unlockedCreatures = remember(ecosystemByCategory) {
        allCreatures.filter { spec ->
            val state = ecosystemByCategory[spec.category] ?: return@filter false
            if (!state.isUnlocked) return@filter false
            state.currentLevel >= spec.unlockLevel
        }
    }

    if (unlockedCreatures.isEmpty()) return

    val infiniteTransition = rememberInfiniteTransition(label = "creatures")
    val timeMs by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "creatureTime"
    )

    Layout(
        content = {},
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                unlockedCreatures.forEachIndexed { index, spec ->
                    val w = size.width
                    val h = size.height

                    // Nivel individual de la criatura (1 si no existe aún)
                    val creatureLevel = creatureLevelBySpecies[spec.species] ?: 1

                    // Tamaño: parte de 80% en nivel 1, +10% por nivel
                    val sizeScale = 0.8f + (creatureLevel - 1) * 0.10f
                    val baseSize = 28f * (1 + spec.unlockLevel / 15f)
                    val iconSize = baseSize * sizeScale

                    if (spec.swimDuration == 0) {
                        val x = w * initialX(index)
                        val y = h * (0.80f + initialY(index) * 0.15f)
                        drawEmoji(spec.emoji, x, y, iconSize, mirrored = false)
                    } else {
                        // Velocidad: ajustada por nivel individual
                        val speedMultiplier = max(
                            0.3f,
                            1f + (creatureLevel - 1) * spec.speedScalePerLevel
                        )
                        val adjustedDuration = (spec.swimDuration / speedMultiplier).toInt()
                        val cycleDuration = adjustedDuration.coerceAtLeast(2000)

                        val phase = phaseOffset(index)
                        // Normalizamos timeMs (0-1 en 12s) al ciclo de esta criatura
                        val cycleProgress = ((timeMs * 12000f / cycleDuration) + phase) % 1f
                        val goingRight = cycleProgress < 0.5f

                        val x = if (goingRight) {
                            w * (cycleProgress * 2f) * 0.9f + w * 0.05f
                        } else {
                            w * ((1f - cycleProgress) * 2f) * 0.9f + w * 0.05f
                        }

                        val baseY = h * initialY(index)
                        val wobble = sin(timeMs * 6.28f * 3f + phase * 6.28f) * h * spec.wobbleAmplitude
                        val y = baseY + wobble

                        drawEmoji(spec.emoji, x, y, iconSize, mirrored = goingRight)
                    }
                }
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}