package com.mnebot.riptide.presentation.aquarium

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.draw.drawWithContent
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.abs

// ── Constantes ────────────────────────────────────────────────────────────────
private const val PI  = 3.14159f
private const val TAU = 6.28318f
private const val PHI = 1.61803f

// ── Curvas de aceleración horizontal ─────────────────────────────────────────
//
// SMOOTH — coseno estándar. Peces, tortugas, ballenas.
// BURST  — 80% del camino en el primer 30% del tiempo, luego planea.
//          Calamar, gamba, pulpo: propulsión a chorro.
// CRAWL  — 95% lineal. Crustáceos que caminan.
//
enum class EasingType { SMOOTH, BURST, CRAWL }

private fun applyEasing(t: Float, type: EasingType): Float {
    val tc = t.coerceIn(0f, 1f)
    return when (type) {
        EasingType.SMOOTH -> (1f - cos(tc * PI)) / 2f
        EasingType.BURST  -> if (tc < 0.30f) {
            ((1f - cos(tc / 0.30f * PI)) / 2f) * 0.80f
        } else {
            0.80f + ((1f - cos((tc - 0.30f) / 0.70f * PI)) / 2f) * 0.20f
        }
        EasingType.CRAWL  -> tc * 0.94f + ((1f - cos(tc * PI)) / 2f) * 0.06f
    }
}

// ── Zona de nado ──────────────────────────────────────────────────────────────
//
// bandFraction define la altura total disponible para la zona.
// Cada criatura tiene un personalYFraction [0..1] que distribuye su base Y
// dentro de la banda, de modo que criaturas de la misma zona no estén
// todas exactamente a la misma altura.
//
enum class SwimZone(val centerFraction: Float, val bandFraction: Float) {
    SURFACE(0.16f, 0.06f),
    UPPER  (0.30f, 0.09f),
    MID    (0.46f, 0.12f),
    LOWER  (0.63f, 0.09f),
    BOTTOM (0.81f, 0.05f),
}

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

// ── CreatureSpec ──────────────────────────────────────────────────────────────
//
// swimDuration       ms por ciclo completo. Menor = más rápido.
// wobbleAmplitude    amplitud onda Y primaria (fracción de banda).
// speedScalePerLevel ajuste velocidad por nivel.
// swimZone           banda vertical.
// personalYFraction  [0..1] posición base dentro de la banda.
//                    0 = parte alta de la banda. 1 = parte baja.
//                    Permite que criaturas de la misma zona estén a distinta altura.
// waveCount          entero → ondas Y primarias por ciclo (sin salto en loop).
// erraticness        [0..1] peso onda secundaria PHI·waveCount (aperiódica).
// driftSpeed         velocidad deriva lenta del eje Y (fraccionario → aperiódico).
// driftAmplitude     fracción de banda usada por deriva.
// pauseFraction      fracción del ciclo en pausa en cada extremo.
// easingType         curva de aceleración horizontal.
// verticalCoupling   [0..1] arco vertical acoplado a posición X.
//                    El máximo de Y ocurre en el centro del recorrido.
//                    Delfín sube al acelerar, baja al frenar — natural.
// microWobble        amplitud oscilación alta frecuencia (aleta/cola).
// xErraticness       [0..1] perturbación aperiódica de X (microaceleraciones).
//                    Solo para peces pequeños y camarones. 0 en el resto.
// fixedWobbleScale   para criaturas fijas: amplitud del ondeo de corriente.
//                    0 = totalmente inmóvil. 1 = ondeo estándar.
//
data class CreatureSpec(
    val emoji: String,
    val species: CreatureSpecies,
    val category: MarineCategory,
    val unlockLevel: Int,
    val swimDuration: Int,
    val wobbleAmplitude: Float,
    val speedScalePerLevel: Float,
    val swimZone: SwimZone,
    val personalYFraction: Float   = 0.5f,
    val waveCount: Int             = 2,
    val erraticness: Float         = 0.0f,
    val driftSpeed: Float          = 0.37f,
    val driftAmplitude: Float      = 0.30f,
    val pauseFraction: Float       = 0.06f,
    val easingType: EasingType     = EasingType.SMOOTH,
    val verticalCoupling: Float    = 0.00f,
    val microWobble: Float         = 0.015f,
    val xErraticness: Float        = 0.00f,
    val fixedWobbleScale: Float    = 1.0f
)

val allCreatures = listOf(

    // ── FISH ──────────────────────────────────────────────────────────────────
    //
    // Clownfish — muy activo, zig-zag nervioso, nada en torno a la anemona.
    //   4 ondas + alta erraticidad + xErraticness = nunca hace el mismo patrón dos veces.
    //   personalYFraction 0.3 = tiende a la parte alta de su zona.
    CreatureSpec("🐟", CreatureSpecies.CLOWNFISH,
        MarineCategory.FISH,      2,  4800, 0.88f, 0.05f, SwimZone.UPPER,
        personalYFraction = 0.30f, waveCount = 4,  erraticness = 0.72f,
        driftSpeed = 0.71f, driftAmplitude = 0.50f, pauseFraction = 0.02f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.042f, xErraticness = 0.18f),

    // Angelfish — elegante, barridos suaves, tiende al centro de su zona.
    CreatureSpec("🐠", CreatureSpecies.ANGELFISH,
        MarineCategory.FISH,      4,  9000, 0.55f, 0.04f, SwimZone.MID,
        personalYFraction = 0.50f, waveCount = 2,  erraticness = 0.20f,
        driftSpeed = 0.29f, driftAmplitude = 0.36f, pauseFraction = 0.06f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.018f, xErraticness = 0.05f),

    // Pufferfish — torpe, se propulsa en arco único, largas pausas. Parte baja de zona.
    CreatureSpec("🐡", CreatureSpecies.PUFFERFISH,
        MarineCategory.FISH,      7, 13000, 0.20f, 0.02f, SwimZone.MID,
        personalYFraction = 0.70f, waveCount = 1,  erraticness = 0.08f,
        driftSpeed = 0.17f, driftAmplitude = 0.16f, pauseFraction = 0.22f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.007f, xErraticness = 0.00f),

    // ── FLORA ─────────────────────────────────────────────────────────────────
    //
    // Brain Coral — completamente rígido. Sin ondeo.
    CreatureSpec("🪸", CreatureSpecies.BRAIN_CORAL,
        MarineCategory.FLORA,     2,  0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 0.00f),

    // Anemone — ondeante. Se mueve con la corriente. Alto wobble.
    CreatureSpec("🌿", CreatureSpecies.ANEMONE,
        MarineCategory.FLORA,     4,  0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 1.80f),

    // Kelp — meceo largo y lento, amplitud media.
    CreatureSpec("🎋", CreatureSpecies.KELP,
        MarineCategory.FLORA,     6,  0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 1.20f),

    // ── CRUSTACEAN ────────────────────────────────────────────────────────────
    //
    // Clave: personalYFraction distintos → cada uno en su "cota" del fondo.
    // CRAWL easing + pausa larga + amplitud Y mínima → caminan, no nadan.
    //
    // Lobster — en el fondo más bajo. Muy lento, pausa larga.
    CreatureSpec("🦞", CreatureSpecies.LOBSTER,
        MarineCategory.CRUSTACEAN, 2, 16000, 0.07f, 0.03f, SwimZone.LOWER,
        personalYFraction = 0.85f, waveCount = 1,  erraticness = 0.05f,
        driftSpeed = 0.11f, driftAmplitude = 0.06f, pauseFraction = 0.30f,
        easingType = EasingType.CRAWL, verticalCoupling = 0.00f,
        microWobble = 0.004f, xErraticness = 0.00f),

    // Hermit Crab — algo más arriba que la langosta. Más errático, paradas frecuentes.
    CreatureSpec("🦀", CreatureSpecies.HERMIT_CRAB,
        MarineCategory.CRUSTACEAN, 4, 13000, 0.12f, 0.04f, SwimZone.LOWER,
        personalYFraction = 0.55f, waveCount = 1,  erraticness = 0.42f,
        driftSpeed = 0.19f, driftAmplitude = 0.10f, pauseFraction = 0.35f,
        easingType = EasingType.CRAWL, verticalCoupling = 0.00f,
        microWobble = 0.006f, xErraticness = 0.00f),

    // Shrimp — más arriba, nada en diagonal. BURST puro, alta erraticidad.
    CreatureSpec("🦐", CreatureSpecies.SHRIMP,
        MarineCategory.CRUSTACEAN, 6,  3200, 0.68f, 0.05f, SwimZone.LOWER,
        personalYFraction = 0.20f, waveCount = 3,  erraticness = 0.70f,
        driftSpeed = 0.41f, driftAmplitude = 0.50f, pauseFraction = 0.10f,
        easingType = EasingType.BURST, verticalCoupling = 0.00f,
        microWobble = 0.030f, xErraticness = 0.12f),

    // ── MOLLUSK (fijos) ───────────────────────────────────────────────────────
    CreatureSpec("🐚", CreatureSpecies.SEA_URCHIN,
        MarineCategory.MOLLUSK,   2,  0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 0.00f),

    CreatureSpec("⭐", CreatureSpecies.STARFISH,
        MarineCategory.MOLLUSK,   4,  0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 0.00f),

    CreatureSpec("🦪", CreatureSpecies.OYSTER,
        MarineCategory.MOLLUSK,   7,  0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 0.00f),

    // ── PELAGIC ───────────────────────────────────────────────────────────────
    //
    // Manta Ray — planeo grácil con coupling suave. Barridos amplios.
    //   En una pecera de su tamaño: ocupa la capa media con movimientos lentos.
    CreatureSpec("🦈", CreatureSpecies.MANTA_RAY,
        MarineCategory.PELAGIC,   2,  8500, 0.35f, 0.03f, SwimZone.MID,
        personalYFraction = 0.45f, waveCount = 1,  erraticness = 0.06f,
        driftSpeed = 0.23f, driftAmplitude = 0.26f, pauseFraction = 0.03f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.30f,
        microWobble = 0.008f, xErraticness = 0.00f),

    // Moon Jellyfish — deriva completamente pasiva. X también tiene componente aleatoria.
    //   No "nada": es arrastrada por corrientes simuladas.
    //   driftAmplitude muy alto = ocupa casi toda su banda.
    CreatureSpec("🪼", CreatureSpecies.MOON_JELLYFISH,
        MarineCategory.PELAGIC,   4, 17000, 0.95f,-0.01f, SwimZone.UPPER,
        personalYFraction = 0.50f, waveCount = 2,  erraticness = 0.90f,
        driftSpeed = 0.61f, driftAmplitude = 0.95f, pauseFraction = 0.00f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.002f, xErraticness = 0.22f),

    // Whale Shark — majestuosa, casi plana, predecible. La mayor del acuario.
    CreatureSpec("🐋", CreatureSpecies.WHALE_SHARK,
        MarineCategory.PELAGIC,   8, 20000, 0.08f,-0.03f, SwimZone.MID,
        personalYFraction = 0.50f, waveCount = 1,  erraticness = 0.04f,
        driftSpeed = 0.13f, driftAmplitude = 0.10f, pauseFraction = 0.04f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.003f, xErraticness = 0.00f),

    // ── CEPHALOPOD ────────────────────────────────────────────────────────────
    //
    // Octopus — BURST + pausa muy larga. Se propulsa en ráfaga, para, observa.
    //   En pecera: suele estar quieto en una esquina, luego se mueve de golpe.
    CreatureSpec("🐙", CreatureSpecies.OCTOPUS,
        MarineCategory.CEPHALOPOD, 2,  8000, 0.72f, 0.04f, SwimZone.LOWER,
        personalYFraction = 0.35f, waveCount = 2,  erraticness = 0.62f,
        driftSpeed = 0.43f, driftAmplitude = 0.55f, pauseFraction = 0.28f,
        easingType = EasingType.BURST, verticalCoupling = 0.00f,
        microWobble = 0.020f, xErraticness = 0.00f),

    // Squid — el más rápido. BURST extremo. Ráfagas en diagonal muy marcadas.
    CreatureSpec("🦑", CreatureSpecies.SQUID,
        MarineCategory.CEPHALOPOD, 5,  3000, 0.80f, 0.05f, SwimZone.MID,
        personalYFraction = 0.40f, waveCount = 3,  erraticness = 0.72f,
        driftSpeed = 0.47f, driftAmplitude = 0.60f, pauseFraction = 0.10f,
        easingType = EasingType.BURST, verticalCoupling = 0.00f,
        microWobble = 0.026f, xErraticness = 0.08f),

    // ── REPTILE ───────────────────────────────────────────────────────────────
    //
    // Sea Turtle — nado sereno con aletas grandes. Un arco limpio.
    //   En pecera: nada de forma regular y predecible, ligeramente inclinada.
    CreatureSpec("🐢", CreatureSpecies.SEA_TURTLE,
        MarineCategory.REPTILE,   2, 14000, 0.22f,-0.02f, SwimZone.MID,
        personalYFraction = 0.60f, waveCount = 1,  erraticness = 0.10f,
        driftSpeed = 0.31f, driftAmplitude = 0.26f, pauseFraction = 0.10f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.011f, xErraticness = 0.00f),

    // ── MAMMAL ────────────────────────────────────────────────────────────────
    //
    // Dolphin — el más expresivo. coupling 0.88 = sube fuerte en el centro.
    //   En pecera: traza arcos visibles, acelera al saltar, frena al sumergirse.
    //   xErraticness pequeño = microaceleraciones que rompen la monotonía.
    CreatureSpec("🐬", CreatureSpecies.DOLPHIN,
        MarineCategory.MAMMAL,    2,  3800, 0.75f, 0.03f, SwimZone.SURFACE,
        personalYFraction = 0.50f, waveCount = 3,  erraticness = 0.30f,
        driftSpeed = 0.59f, driftAmplitude = 0.60f, pauseFraction = 0.02f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.88f,
        microWobble = 0.022f, xErraticness = 0.06f),

    // Seal — ondulante de todo el cuerpo. Más tranquilo que el delfín.
    //   Coupling medio = arcos moderados.
    CreatureSpec("🦭", CreatureSpecies.SEAL,
        MarineCategory.MAMMAL,    5, 10500, 0.55f,-0.02f, SwimZone.UPPER,
        personalYFraction = 0.50f, waveCount = 2,  erraticness = 0.20f,
        driftSpeed = 0.37f, driftAmplitude = 0.38f, pauseFraction = 0.08f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.44f,
        microWobble = 0.015f, xErraticness = 0.03f),

    // Blue Whale — enorme, lentísima, casi plana. Ciclo de 22s.
    //   En una pecera imaginaria a su escala sería un movimiento muy pausado.
    CreatureSpec("🐳", CreatureSpecies.BLUE_WHALE,
        MarineCategory.MAMMAL,    8, 22000, 0.07f,-0.03f, SwimZone.SURFACE,
        personalYFraction = 0.50f, waveCount = 1,  erraticness = 0.03f,
        driftSpeed = 0.11f, driftAmplitude = 0.09f, pauseFraction = 0.04f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.003f, xErraticness = 0.00f),

    // ── DECORATION (fijos) ────────────────────────────────────────────────────
    CreatureSpec("🪙", CreatureSpecies.TREASURE_CHEST,
        MarineCategory.DECORATION, 1, 0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 0.00f),

    CreatureSpec("⚓", CreatureSpecies.ANCHOR,
        MarineCategory.DECORATION, 1, 0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 0.00f),

    CreatureSpec("🚢", CreatureSpecies.SUNKEN_SHIP,
        MarineCategory.DECORATION, 1, 0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 0.00f),
)

private fun fixedX(fixedIndex: Int, totalFixed: Int): Float {
    val step = 0.80f / (totalFixed + 1).toFloat()
    return 0.10f + step * (fixedIndex + 1)
}

// Distribución Fibonacci para desfasar fases entre criaturas
private fun phaseOffset(index: Int): Float = (index * 0.618f) % 1f

expect fun DrawScope.drawEmoji(
    emoji: String,
    x: Float,
    y: Float,
    sizeSp: Float,
    mirrored: Boolean
)

data class CreaturePosition(
    val species: CreatureSpecies,
    val x: Float,
    val y: Float,
    val hitRadius: Float
)

class CreatureFreezeState {
    private val frozen = mutableStateMapOf<CreatureSpecies, Boolean>()
    fun isFrozen(species: CreatureSpecies) = frozen[species] == true
    fun freeze(species: CreatureSpecies) { frozen[species] = true }
    fun unfreeze(species: CreatureSpecies) { frozen[species] = false }
}

@Composable
fun rememberCreatureFreezeState() = remember { CreatureFreezeState() }

private fun findHitCreature(
    offset: Offset,
    positions: List<CreaturePosition>
): CreatureSpecies? = positions
    .filter { pos ->
        val dx = offset.x - pos.x
        val dy = offset.y - pos.y
        abs(dx) <= pos.hitRadius && abs(dy) <= pos.hitRadius
    }
    .minByOrNull { pos ->
        val dx = offset.x - pos.x
        val dy = offset.y - pos.y
        dx * dx + dy * dy
    }
    ?.species

@Composable
fun AquariumCreatures(
    ecosystemByCategory: Map<MarineCategory, EcosystemState>,
    creatureLevelBySpecies: Map<CreatureSpecies, Int> = emptyMap(),
    creaturesData: List<MarineCreature> = emptyList(),
    freezeState: CreatureFreezeState = rememberCreatureFreezeState(),
    onCreatureLongPress: (MarineCreature, CreatureSpec) -> Unit = { _, _ -> },
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

    val fixedCreatures = remember(unlockedCreatures) {
        unlockedCreatures.filter { it.swimDuration == 0 }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "creatures")
    // Reloj global 0→1 en 12000ms, loop continuo
    val timeMs by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "creatureTime"
    )

    val creaturePositions = remember { mutableStateOf<List<CreaturePosition>>(emptyList()) }
    val frozenTimeMap = remember { mutableStateMapOf<CreatureSpecies, Float>() }
    val currentTimeMs by rememberUpdatedState(timeMs)

    Layout(
        content = {},
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val hit = findHitCreature(offset, creaturePositions.value) ?: return@detectTapGestures
                        val spec = unlockedCreatures.find { it.species == hit } ?: return@detectTapGestures
                        val creature = creaturesData.find { it.species == hit } ?: return@detectTapGestures
                        freezeState.freeze(hit)
                        frozenTimeMap[hit] = currentTimeMs
                        onCreatureLongPress(creature, spec)
                    }
                )
            }
            .drawWithContent {
                drawContent()
                val positions = mutableListOf<CreaturePosition>()
                var fixedIndex = 0

                unlockedCreatures.forEachIndexed { index, spec ->
                    val w = size.width
                    val h = size.height

                    val creatureLevel = creatureLevelBySpecies[spec.species] ?: 1
                    val sizeScale = 0.8f + (creatureLevel - 1) * 0.10f
                    val baseSize = 28f * (1 + spec.unlockLevel / 15f)
                    val iconSize = baseSize * sizeScale

                    val isFrozen = freezeState.isFrozen(spec.species)
                    val t = if (isFrozen) frozenTimeMap[spec.species] ?: timeMs else timeMs

                    // phase: desfasa ondas Y entre criaturas. NUNCA afecta X.
                    val phase = phaseOffset(index)

                    // ── Base Y personal ──────────────────────────────────────
                    // Cada criatura tiene su "cota" dentro de la banda, derivada de
                    // personalYFraction. La distribución es determinista (no aleatoria),
                    // por lo que no cambia entre recomposiciones.
                    // personalYFraction 0 = parte alta de la banda, 1 = parte baja.
                    val zoneCenter  = h * spec.swimZone.centerFraction
                    val zoneBand    = h * spec.swimZone.bandFraction
                    val personalY   = zoneCenter + (spec.personalYFraction - 0.5f) * zoneBand

                    val x: Float
                    val y: Float

                    if (spec.swimDuration == 0) {
                        // ── FIJA ────────────────────────────────────────────────
                        x = w * fixedX(fixedIndex, fixedCreatures.size)
                        fixedIndex++

                        // Ondeo de corriente: 1 ciclo exacto en 12s → sin salto en loop.
                        // fixedWobbleScale permite ajustar por especie:
                        //   0 = completamente inmóvil (coral, erizo, ostra)
                        //   1.8 = anemona muy ondeante
                        val wobble = sin(t * TAU + phase * TAU) * zoneBand * 0.18f * spec.fixedWobbleScale
                        y = personalY + wobble
                        drawEmoji(spec.emoji, x, y, iconSize, mirrored = false)

                    } else {
                        // ── NADADORA ────────────────────────────────────────────
                        val speedMult = max(0.3f, 1f + (creatureLevel - 1) * spec.speedScalePerLevel)
                        val cycleDuration = (spec.swimDuration / speedMult).toInt().coerceAtLeast(2000)

                        // rawProgress SIN phase → X siempre continua, sin salto.
                        val rawProgress = (t * 12000f / cycleDuration) % 1f

                        // ── Posición X ──────────────────────────────────────────
                        val pf = spec.pauseFraction
                        val swimFraction = (0.5f - pf).coerceAtLeast(0.01f)

                        val goingRight: Boolean
                        val swimProgress: Float

                        when {
                            rawProgress < pf -> {
                                goingRight = true
                                swimProgress = 0f
                            }
                            rawProgress < 0.5f -> {
                                goingRight = true
                                val localT = ((rawProgress - pf) / swimFraction).coerceIn(0f, 1f)
                                swimProgress = applyEasing(localT, spec.easingType)
                            }
                            rawProgress < 0.5f + pf -> {
                                goingRight = false
                                swimProgress = 1f
                            }
                            else -> {
                                goingRight = false
                                val localT = ((rawProgress - (0.5f + pf)) / swimFraction).coerceIn(0f, 1f)
                                swimProgress = 1f - applyEasing(localT, spec.easingType)
                            }
                        }

                        // Perturbación aperiódica de X para peces nerviosos y medusas.
                        // Frecuencia PHI·5 ≈ irracional → microaceleraciones impredecibles.
                        // Amplitud pequeña para no distorsionar el recorrido principal.
                        val xPerturbation = if (spec.xErraticness > 0f)
                            sin(rawProgress * TAU * 5f * PHI + phase * TAU) *
                                    spec.xErraticness * 0.04f
                        else 0f

                        x = (w * 0.05f + w * 0.90f * swimProgress + w * xPerturbation)
                            .coerceIn(w * 0.02f, w * 0.98f)

                        // ── Posición Y: cinco capas ────────────────────────────
                        //
                        // Todas las oscilaciones usan `personalY` como base,
                        // no `zoneCenter`. Así cada criatura gravita hacia su
                        // cota personal, y criaturas de la misma zona están
                        // naturalmente separadas en altura.
                        //
                        // 1. ONDA PRIMARIA — entero → sin salto en loop
                        val primaryWave = sin(rawProgress * TAU * spec.waveCount + phase * TAU)

                        // 2. ONDA SECUNDARIA — PHI·waveCount (irracional, aperiódica)
                        val secondaryWave = sin(rawProgress * TAU * spec.waveCount * PHI + phase * TAU * PHI)

                        val waveY = (primaryWave * (1f - spec.erraticness) +
                                secondaryWave * spec.erraticness) *
                                zoneBand * spec.wobbleAmplitude

                        // 3. DERIVA LENTA — driftSpeed fraccionario → aperiódico vs ciclo 12s
                        //    La criatura nunca está en la misma altura en dos ciclos seguidos.
                        val drift = sin(t * TAU * spec.driftSpeed + phase * TAU) *
                                zoneBand * spec.driftAmplitude

                        // 4. ACOPLAMIENTO VERTICAL — arco ligado a posición X.
                        //    La criatura sube al centro del recorrido (máxima velocidad)
                        //    y baja en los extremos. Delfín: arcos de salto realistas.
                        //    Signo negativo: "arriba" en pantalla es Y menor.
                        val coupledArc = -sin(swimProgress * PI) * zoneBand * spec.verticalCoupling

                        // 5. MICROWOBBLE — alta frecuencia (~1.5Hz). Aleta/cola.
                        val micro = sin(t * TAU * 18f + phase * TAU) * zoneBand * spec.microWobble

                        y = (personalY + waveY + drift + coupledArc + micro).coerceIn(
                            personalY - zoneBand * 1.10f,
                            personalY + zoneBand * 1.10f
                        )

                        drawEmoji(spec.emoji, x, y, iconSize, mirrored = goingRight)
                    }

                    positions.add(CreaturePosition(spec.species, x, y, maxOf(iconSize * 2f, 60f)))
                }

                creaturePositions.value = positions
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}