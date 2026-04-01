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
    CreatureSpecies.BUTTERFLYFISH -> Res.string.species_butterflyfish
    CreatureSpecies.SEAHORSE -> Res.string.species_seahorse
    CreatureSpecies.MORAY_EEL -> Res.string.species_moray_eel
    CreatureSpecies.TUBE_SPONGE -> Res.string.species_tube_sponge
    CreatureSpecies.SEA_GRASS -> Res.string.species_sea_grass
    CreatureSpecies.FIRE_CORAL -> Res.string.species_fire_coral
    CreatureSpecies.STAGHORN_CORAL -> Res.string.species_staghorn_coral
    CreatureSpecies.KRILL -> Res.string.species_krill
    CreatureSpecies.HORSESHOE_CRAB -> Res.string.species_horseshoe_crab
    CreatureSpecies.MANTIS_SHRIMP -> Res.string.species_mantis_shrimp
    CreatureSpecies.COCONUT_CRAB -> Res.string.species_coconut_crab
    CreatureSpecies.CONCH -> Res.string.species_conch
    CreatureSpecies.SCALLOP -> Res.string.species_scallop
    CreatureSpecies.SEA_SLUG -> Res.string.species_sea_slug
    CreatureSpecies.SEA_CUCUMBER -> Res.string.species_sea_cucumber
    CreatureSpecies.BLUEFIN_TUNA -> Res.string.species_bluefin_tuna
    CreatureSpecies.FLYING_FISH -> Res.string.species_flying_fish
    CreatureSpecies.LIONSMANE_JELLYFISH -> Res.string.species_lionsmane_jellyfish
    CreatureSpecies.SWORDFISH -> Res.string.species_swordfish
    CreatureSpecies.CHAMBERED_NAUTILUS -> Res.string.species_chambered_nautilus
    CreatureSpecies.GIANT_PACIFIC_OCTOPUS -> Res.string.species_giant_pacific_octopus
    CreatureSpecies.GREEN_SEA_TURTLE -> Res.string.species_green_sea_turtle
    CreatureSpecies.SEA_SNAKE -> Res.string.species_sea_snake
    CreatureSpecies.LEATHERBACK_TURTLE -> Res.string.species_leatherback_turtle
    CreatureSpecies.SALTWATER_CROCODILE -> Res.string.species_saltwater_crocodile
    CreatureSpecies.NARWHAL -> Res.string.species_narwhal
    CreatureSpecies.DIVING_HELMET -> Res.string.species_diving_helmet
    CreatureSpecies.CORAL_THRONE -> Res.string.species_coral_throne
    CreatureSpecies.GOLDEN_TRIDENT -> Res.string.species_golden_trident
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
