package com.mnebot.riptide.presentation

import androidx.compose.runtime.Composable
import com.mnebot.riptide.domain.model.CreatureRarity
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.MarineCategory
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import riptide.composeapp.generated.resources.*

@Composable
fun localizedDays(): List<Pair<Int, String>> = listOf(
    1 to stringResource(Res.string.day_mon),
    2 to stringResource(Res.string.day_tue),
    3 to stringResource(Res.string.day_wed),
    4 to stringResource(Res.string.day_thu),
    5 to stringResource(Res.string.day_fri),
    6 to stringResource(Res.string.day_sat),
    7 to stringResource(Res.string.day_sun)
)

fun CreatureSpecies.displayNameRes(): StringResource = when (this) {
    CreatureSpecies.CLOWNFISH -> Res.string.species_clownfish
    CreatureSpecies.ANGELFISH -> Res.string.species_angelfish
    CreatureSpecies.PUFFERFISH -> Res.string.species_pufferfish
    CreatureSpecies.SURGEONFISH -> Res.string.species_surgeonfish
    CreatureSpecies.LIONFISH -> Res.string.species_lionfish
    CreatureSpecies.SUNFISH -> Res.string.species_sunfish
    CreatureSpecies.BRAIN_CORAL -> Res.string.species_brain_coral
    CreatureSpecies.ANEMONE -> Res.string.species_anemone
    CreatureSpecies.KELP -> Res.string.species_kelp
    CreatureSpecies.POSIDONIA -> Res.string.species_posidonia
    CreatureSpecies.FAN_CORAL -> Res.string.species_fan_coral
    CreatureSpecies.LOBSTER -> Res.string.species_lobster
    CreatureSpecies.HERMIT_CRAB -> Res.string.species_hermit_crab
    CreatureSpecies.SHRIMP -> Res.string.species_shrimp
    CreatureSpecies.SPIDER_CRAB -> Res.string.species_spider_crab
    CreatureSpecies.BARNACLE -> Res.string.species_barnacle
    CreatureSpecies.SEA_URCHIN -> Res.string.species_sea_urchin
    CreatureSpecies.STARFISH -> Res.string.species_starfish
    CreatureSpecies.OYSTER -> Res.string.species_oyster
    CreatureSpecies.NAUTILUS -> Res.string.species_nautilus
    CreatureSpecies.GIANT_CLAM -> Res.string.species_giant_clam
    CreatureSpecies.MANTA_RAY -> Res.string.species_manta_ray
    CreatureSpecies.MOON_JELLYFISH -> Res.string.species_moon_jellyfish
    CreatureSpecies.WHALE_SHARK -> Res.string.species_whale_shark
    CreatureSpecies.HAMMERHEAD -> Res.string.species_hammerhead
    CreatureSpecies.BARRACUDA -> Res.string.species_barracuda
    CreatureSpecies.OCTOPUS -> Res.string.species_octopus
    CreatureSpecies.SQUID -> Res.string.species_squid
    CreatureSpecies.CUTTLEFISH -> Res.string.species_cuttlefish
    CreatureSpecies.BLUE_RINGED_OCTOPUS -> Res.string.species_blue_ringed_octopus
    CreatureSpecies.SEA_TURTLE -> Res.string.species_sea_turtle
    CreatureSpecies.MARINE_IGUANA -> Res.string.species_marine_iguana
    CreatureSpecies.DOLPHIN -> Res.string.species_dolphin
    CreatureSpecies.SEAL -> Res.string.species_seal
    CreatureSpecies.BLUE_WHALE -> Res.string.species_blue_whale
    CreatureSpecies.SEA_OTTER -> Res.string.species_sea_otter
    CreatureSpecies.MANATEE -> Res.string.species_manatee
    CreatureSpecies.TREASURE_CHEST -> Res.string.species_treasure_chest
    CreatureSpecies.ANCHOR -> Res.string.species_anchor
    CreatureSpecies.SUNKEN_SHIP -> Res.string.species_sunken_ship
    CreatureSpecies.BIMBA -> Res.string.species_bimba
}

fun MarineCategory.displayNameRes(): StringResource = when (this) {
    MarineCategory.FISH -> Res.string.category_fish
    MarineCategory.FLORA -> Res.string.category_flora
    MarineCategory.CRUSTACEAN -> Res.string.category_crustacean
    MarineCategory.MOLLUSK -> Res.string.category_mollusk
    MarineCategory.PELAGIC -> Res.string.category_pelagic
    MarineCategory.CEPHALOPOD -> Res.string.category_cephalopod
    MarineCategory.REPTILE -> Res.string.category_reptile
    MarineCategory.MAMMAL -> Res.string.category_mammal
    MarineCategory.DECORATION -> Res.string.category_decoration
    MarineCategory.COMPANION -> Res.string.category_companion
}

fun CreatureRarity.displayNameRes(): StringResource = when (this) {
    CreatureRarity.COMMON -> Res.string.rarity_common
    CreatureRarity.UNCOMMON -> Res.string.rarity_uncommon
    CreatureRarity.RARE -> Res.string.rarity_rare
    CreatureRarity.EPIC -> Res.string.rarity_epic
    CreatureRarity.LEGENDARY -> Res.string.rarity_legendary
}
