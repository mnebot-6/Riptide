package com.mnebot.riptide.presentation.aquarium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.presentation.aquarium.fauna.AnchorRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.BimbaRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.AngelfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.BarracudaRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.BarnacleRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.BlueRingedOctopusRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.BlueWhaleRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.ClownfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.CuttlefishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.DolphinRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.GiantClamRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.HammerheadRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.HermitCrabRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.LionfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.LobsterRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.ManateeRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.MantaRayRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.MarineIguanaRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.MoonJellyfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.NautilusRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.OctopusRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.OysterRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.PufferfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SeaOtterRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SeaTurtleRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SeaUrchinRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SealRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.ShrimpRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SpiderCrabRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SquidRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.StarfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SunfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SunkenShipRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SurgeonfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.TreasureChestRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.WhaleSharkRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.ButterflyfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SeahorseRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.MorayEelRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.KrillRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.HorseshoeCrabRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.MantisShrimpRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.CoconutCrabRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.ConchRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.ScallopRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SeaSlugRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SeaCucumberRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.BluefinTunaRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.FlyingFishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.LionsmaneJellyfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SwordfishRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.ChamberedNautilusRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.GiantPacificOctopusRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.GreenSeaTurtleRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SeaSnakeRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.LeatherbackTurtleRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.SaltwaterCrocodileRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.NarwhalRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.DivingHelmetRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.CoralThroneRenderer
import com.mnebot.riptide.presentation.aquarium.fauna.GoldenTridentRenderer
import com.mnebot.riptide.presentation.aquarium.flora.AnemoneRenderer
import com.mnebot.riptide.presentation.aquarium.flora.BrainCoralRenderer
import com.mnebot.riptide.presentation.aquarium.flora.FanCoralRenderer
import com.mnebot.riptide.presentation.aquarium.flora.KelpRenderer
import com.mnebot.riptide.presentation.aquarium.flora.PosidoniaRenderer
import com.mnebot.riptide.presentation.aquarium.flora.TubeSpongeRenderer
import com.mnebot.riptide.presentation.aquarium.flora.SeaGrassRenderer
import com.mnebot.riptide.presentation.aquarium.flora.FireCoralRenderer
import com.mnebot.riptide.presentation.aquarium.flora.StaghornCoralRenderer

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
    // ── FLORA ─────────────────────────────────────────────────────────────────
    CreatureSpecies.BRAIN_CORAL         to BrainCoralRenderer,
    CreatureSpecies.ANEMONE             to AnemoneRenderer,
    CreatureSpecies.KELP                to KelpRenderer,
    CreatureSpecies.POSIDONIA           to PosidoniaRenderer,
    CreatureSpecies.FAN_CORAL           to FanCoralRenderer,
    CreatureSpecies.TUBE_SPONGE         to TubeSpongeRenderer,
    CreatureSpecies.SEA_GRASS           to SeaGrassRenderer,
    CreatureSpecies.FIRE_CORAL          to FireCoralRenderer,
    CreatureSpecies.STAGHORN_CORAL      to StaghornCoralRenderer,
    // ── FISH ──────────────────────────────────────────────────────────────────
    CreatureSpecies.CLOWNFISH           to ClownfishRenderer,
    CreatureSpecies.ANGELFISH           to AngelfishRenderer,
    CreatureSpecies.PUFFERFISH          to PufferfishRenderer,
    CreatureSpecies.SURGEONFISH         to SurgeonfishRenderer,
    CreatureSpecies.LIONFISH            to LionfishRenderer,
    CreatureSpecies.SUNFISH             to SunfishRenderer,
    CreatureSpecies.BUTTERFLYFISH       to ButterflyfishRenderer,
    CreatureSpecies.SEAHORSE            to SeahorseRenderer,
    CreatureSpecies.MORAY_EEL           to MorayEelRenderer,
    // ── CRUSTACEAN ────────────────────────────────────────────────────────────
    CreatureSpecies.LOBSTER             to LobsterRenderer,
    CreatureSpecies.HERMIT_CRAB         to HermitCrabRenderer,
    CreatureSpecies.SHRIMP              to ShrimpRenderer,
    CreatureSpecies.SPIDER_CRAB         to SpiderCrabRenderer,
    CreatureSpecies.BARNACLE            to BarnacleRenderer,
    CreatureSpecies.KRILL               to KrillRenderer,
    CreatureSpecies.HORSESHOE_CRAB      to HorseshoeCrabRenderer,
    CreatureSpecies.MANTIS_SHRIMP       to MantisShrimpRenderer,
    CreatureSpecies.COCONUT_CRAB        to CoconutCrabRenderer,
    // ── MOLLUSK ───────────────────────────────────────────────────────────────
    CreatureSpecies.SEA_URCHIN          to SeaUrchinRenderer,
    CreatureSpecies.STARFISH            to StarfishRenderer,
    CreatureSpecies.OYSTER              to OysterRenderer,
    CreatureSpecies.NAUTILUS            to NautilusRenderer,
    CreatureSpecies.GIANT_CLAM          to GiantClamRenderer,
    CreatureSpecies.CONCH               to ConchRenderer,
    CreatureSpecies.SCALLOP             to ScallopRenderer,
    CreatureSpecies.SEA_SLUG            to SeaSlugRenderer,
    CreatureSpecies.SEA_CUCUMBER        to SeaCucumberRenderer,
    // ── PELAGIC ───────────────────────────────────────────────────────────────
    CreatureSpecies.MANTA_RAY           to MantaRayRenderer,
    CreatureSpecies.MOON_JELLYFISH      to MoonJellyfishRenderer,
    CreatureSpecies.WHALE_SHARK         to WhaleSharkRenderer,
    CreatureSpecies.HAMMERHEAD          to HammerheadRenderer,
    CreatureSpecies.BARRACUDA           to BarracudaRenderer,
    CreatureSpecies.BLUEFIN_TUNA        to BluefinTunaRenderer,
    CreatureSpecies.FLYING_FISH         to FlyingFishRenderer,
    CreatureSpecies.LIONSMANE_JELLYFISH to LionsmaneJellyfishRenderer,
    CreatureSpecies.SWORDFISH           to SwordfishRenderer,
    // ── CEPHALOPOD ────────────────────────────────────────────────────────────
    CreatureSpecies.OCTOPUS             to OctopusRenderer,
    CreatureSpecies.SQUID               to SquidRenderer,
    CreatureSpecies.CUTTLEFISH          to CuttlefishRenderer,
    CreatureSpecies.BLUE_RINGED_OCTOPUS to BlueRingedOctopusRenderer,
    CreatureSpecies.CHAMBERED_NAUTILUS  to ChamberedNautilusRenderer,
    CreatureSpecies.GIANT_PACIFIC_OCTOPUS to GiantPacificOctopusRenderer,
    // ── REPTILE ───────────────────────────────────────────────────────────────
    CreatureSpecies.SEA_TURTLE          to SeaTurtleRenderer,
    CreatureSpecies.MARINE_IGUANA       to MarineIguanaRenderer,
    CreatureSpecies.GREEN_SEA_TURTLE    to GreenSeaTurtleRenderer,
    CreatureSpecies.SEA_SNAKE           to SeaSnakeRenderer,
    CreatureSpecies.LEATHERBACK_TURTLE  to LeatherbackTurtleRenderer,
    CreatureSpecies.SALTWATER_CROCODILE to SaltwaterCrocodileRenderer,
    // ── MAMMAL ────────────────────────────────────────────────────────────────
    CreatureSpecies.DOLPHIN             to DolphinRenderer,
    CreatureSpecies.SEAL                to SealRenderer,
    CreatureSpecies.BLUE_WHALE          to BlueWhaleRenderer,
    CreatureSpecies.SEA_OTTER           to SeaOtterRenderer,
    CreatureSpecies.MANATEE             to ManateeRenderer,
    CreatureSpecies.NARWHAL             to NarwhalRenderer,
    // ── DECORATION ────────────────────────────────────────────────────────────
    CreatureSpecies.TREASURE_CHEST      to TreasureChestRenderer,
    CreatureSpecies.ANCHOR              to AnchorRenderer,
    CreatureSpecies.SUNKEN_SHIP         to SunkenShipRenderer,
    CreatureSpecies.DIVING_HELMET       to DivingHelmetRenderer,
    CreatureSpecies.CORAL_THRONE        to CoralThroneRenderer,
    CreatureSpecies.GOLDEN_TRIDENT      to GoldenTridentRenderer,
    // ── COMPANION (easter egg) ────────────────────────────────────────────────
    CreatureSpecies.BIMBA               to BimbaRenderer,
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
        val isSwimmer = spec.swimDuration > 0
        val isFlora = !isSwimmer
        Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
            // Flora: escalar para que quepa sin recorte (base en el fondo, crece hacia arriba)
            // Nadadores: centrado, usar min(width, height) para no desbordar
            val renderSize = if (isFlora) size.height * 0.55f
                             else minOf(size.width, size.height) * 0.8f
            with(renderer) {
                render(
                    x = size.width / 2f,
                    y = if (isFlora) size.height else size.height * 0.55f,
                    size = renderSize,
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
