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
    val swimDuration: Int,   // ms; 0 = fija
    val wobbleAmplitude: Float
)

val allCreatures = listOf(
    // FISH
    CreatureSpec("🐟", CreatureSpecies.CLOWNFISH,     MarineCategory.FISH,        2,  7000,  0.06f),
    CreatureSpec("🐠", CreatureSpecies.ANGELFISH,     MarineCategory.FISH,        4,  6000,  0.07f),
    CreatureSpec("🐡", CreatureSpecies.PUFFERFISH,    MarineCategory.FISH,        7,  9000,  0.04f),
    // FLORA
    CreatureSpec("🪸", CreatureSpecies.BRAIN_CORAL,   MarineCategory.FLORA,       2,  0,     0.00f),
    CreatureSpec("🌿", CreatureSpecies.ANEMONE,       MarineCategory.FLORA,       4,  0,     0.00f),
    CreatureSpec("🎋", CreatureSpecies.KELP,          MarineCategory.FLORA,       6,  0,     0.00f),
    // CRUSTACEAN
    CreatureSpec("🦞", CreatureSpecies.LOBSTER,       MarineCategory.CRUSTACEAN,  2,  9000,  0.03f),
    CreatureSpec("🦀", CreatureSpecies.HERMIT_CRAB,   MarineCategory.CRUSTACEAN,  4,  8000,  0.04f),
    CreatureSpec("🦐", CreatureSpecies.SHRIMP,        MarineCategory.CRUSTACEAN,  6,  6000,  0.05f),
    // MOLLUSK
    CreatureSpec("🐚", CreatureSpecies.SEA_URCHIN,    MarineCategory.MOLLUSK,     2,  0,     0.00f),
    CreatureSpec("⭐", CreatureSpecies.STARFISH,      MarineCategory.MOLLUSK,     4,  0,     0.00f),
    CreatureSpec("🦪", CreatureSpecies.OYSTER,        MarineCategory.MOLLUSK,     7,  0,     0.00f),
    // PELAGIC
    CreatureSpec("🦈", CreatureSpecies.MANTA_RAY,     MarineCategory.PELAGIC,     2,  5000,  0.08f),
    CreatureSpec("🪼", CreatureSpecies.MOON_JELLYFISH,MarineCategory.PELAGIC,     4,  11000, 0.10f),
    CreatureSpec("🐋", CreatureSpecies.WHALE_SHARK,   MarineCategory.PELAGIC,     8,  14000, 0.05f),
    // CEPHALOPOD
    CreatureSpec("🐙", CreatureSpecies.OCTOPUS,       MarineCategory.CEPHALOPOD,  2,  8000,  0.06f),
    CreatureSpec("🦑", CreatureSpecies.SQUID,         MarineCategory.CEPHALOPOD,  5,  6000,  0.08f),
    // REPTILE
    CreatureSpec("🐢", CreatureSpecies.SEA_TURTLE,    MarineCategory.REPTILE,     2,  12000, 0.04f),
    // MAMMAL
    CreatureSpec("🐬", CreatureSpecies.DOLPHIN,       MarineCategory.MAMMAL,      2,  5000,  0.10f),
    CreatureSpec("🦭", CreatureSpecies.SEAL,          MarineCategory.MAMMAL,      5,  10000, 0.05f),
    CreatureSpec("🐳", CreatureSpecies.BLUE_WHALE,    MarineCategory.MAMMAL,      8,  18000, 0.03f),
    // DECORATION — nunca nadan, siempre fijas
    CreatureSpec("🪙", CreatureSpecies.TREASURE_CHEST,MarineCategory.DECORATION,  1,  0,     0.00f),
    CreatureSpec("⚓", CreatureSpecies.ANCHOR,        MarineCategory.DECORATION,  1,  0,     0.00f),
    CreatureSpec("🚢", CreatureSpecies.SUNKEN_SHIP,   MarineCategory.DECORATION,  1,  0,     0.00f),
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
                    val iconSize = 28f * (1 + spec.unlockLevel / 15)

                    if (spec.swimDuration == 0) {
                        val x = w * initialX(index)
                        val y = h * (0.80f + initialY(index) * 0.15f)
                        drawEmoji(spec.emoji, x, y, iconSize, mirrored = false)
                    } else {
                        val phase = phaseOffset(index)
                        val swimProgress = (timeMs + phase) % 1f
                        val goingRight = swimProgress < 0.5f

                        val x = if (goingRight) {
                            w * (swimProgress * 2f) * 0.9f + w * 0.05f
                        } else {
                            w * ((1f - swimProgress) * 2f) * 0.9f + w * 0.05f
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