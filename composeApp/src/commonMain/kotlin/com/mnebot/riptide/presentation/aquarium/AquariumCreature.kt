package com.mnebot.riptide.presentation.aquarium

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
private const val PI  = 3.14159265f
private const val TAU = 6.28318530f
private const val PHI = 1.61803398f   // número áureo — frecuencia secundaria irracional

// ── Oscilador base ────────────────────────────────────────────────────────────
//
// La clave matemática de que no haya teleportación jamás:
//
//   phase = (elapsedMs % periodMs).toFloat() / periodMs.toFloat()
//
// Esto significa:
//   • El módulo se aplica en aritmética Long (sin pérdida de precisión, nunca).
//   • phase ∈ [0, 1), su argumento a sin() ∈ [0, 2π). Float es exacto aquí.
//   • En el wrap: sin(0 + offset·TAU) = sin(TAU + offset·TAU) por periodicidad del seno.
//     → valor IDÉNTICO antes y después del wrap. Cero salto. Siempre.
//   • Esto es cierto para CUALQUIER periodMs, incluso irracionales respecto a la pantalla.
//
// Cada oscilador tiene su propio período independiente.
// No comparten ningún reloj global que reinicie.
//
private fun osc(elapsedMs: Long, periodMs: Long, phaseOffset: Float = 0f): Float {
    if (periodMs <= 0L) return 0f
    val phase = (elapsedMs % periodMs).toFloat() / periodMs.toFloat()
    return sin(phase * TAU + phaseOffset * TAU)
}

// ── Curvas de aceleración horizontal ─────────────────────────────────────────
//
// SMOOTH — coseno. Desacelera suavemente en los extremos.
//          Peces, tortugas, mamíferos, manta ray.
//
// BURST  — propulsión a chorro: 75% de la distancia en el primer 25% del tiempo,
//          luego planeo decelerado. Calamar, gamba, pulpo.
//
// CRAWL  — 93% lineal + 7% coseno. Movimiento de caminata.
//          Langosta, cangrejo ermitaño.
//
enum class EasingType { SMOOTH, BURST, CRAWL }

private fun applyEasing(t: Float, type: EasingType): Float {
    val tc = t.coerceIn(0f, 1f)
    return when (type) {
        EasingType.SMOOTH -> (1f - cos(tc * PI)) / 2f
        EasingType.BURST  -> if (tc < 0.25f) {
            ((1f - cos(tc / 0.25f * PI)) / 2f) * 0.75f
        } else {
            0.75f + ((1f - cos((tc - 0.25f) / 0.75f * PI)) / 2f) * 0.25f
        }
        EasingType.CRAWL  -> tc * 0.93f + ((1f - cos(tc * PI)) / 2f) * 0.07f
    }
}

// ── Zona de nado ──────────────────────────────────────────────────────────────
//
// Cada especie tiene una banda vertical asignada (swimZone).
// Dentro de esa banda, personalYFraction define su cota exacta.
// Esto separa criaturas de la misma zona en alturas distintas.
//
enum class SwimZone(val centerFraction: Float, val bandFraction: Float) {
    SURFACE(0.16f, 0.07f),   // superficie — delfín, ballena azul
    UPPER  (0.30f, 0.10f),   // zona alta — clownfish, medusa, foca
    MID    (0.47f, 0.13f),   // zona media — mayoría
    LOWER  (0.64f, 0.09f),   // zona baja — crustáceos, pulpo
    BOTTOM (0.82f, 0.05f),   // fondo — flora, moluscos, decoración
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
// swimDuration       ms por ciclo completo (izq→der→izq). Menor = más rápido.
// wobbleAmplitude    amplitud onda Y primaria (fracción de banda).
// speedScalePerLevel ajuste de velocidad al subir de nivel.
// swimZone           banda vertical asignada a la especie.
// personalYFraction  [0..1] cota personal dentro de la banda.
//                    0 = parte alta. 1 = parte baja.
//                    Separa especies en la misma zona en alturas distintas.
// waveCount          Entero → primaryPeriod = cycleDuration / waveCount (ms).
//                    Determina cuántas ondas Y completas por ciclo.
//                    1 = arco suave. 4 = cuatro ondulaciones por cruce.
// erraticness        [0..1] peso de la onda secundaria (periodo irracional PHI).
//                    0 = movimiento periódico y predecible.
//                    1 = la onda secundaria domina, nunca repite exactamente.
// driftSpeed         Velocidad de la deriva lenta del eje Y.
//                    Unidad: ciclos cada 12 segundos.
//                    0.11 → un ciclo de deriva cada ~109s. Muy lento.
//                    0.71 → un ciclo cada ~17s. Más activo.
// driftAmplitude     Cuánto se desplaza el eje Y (fracción de banda).
// pauseFraction      Fracción del ciclo en pausa en cada extremo.
//                    0.00 → nado continuo. 0.30 → langosta (30% parada por lado).
// easingType         Curva de aceleración horizontal.
// verticalCoupling   [0..1] arco vertical acoplado a posición X.
//                    El máximo de Y (sube en pantalla) ocurre en el centro del cruce.
//                    Delfín (0.88): salta visiblemente al ir más rápido.
// microWobble        Amplitud del movimiento de aleta/cola (~1.5Hz).
//                    Sutil. Da sensación de vida incluso durante pausas.
// xErraticness       [0..1] amplitud de microaceleraciones horizontales aperiódicas.
//                    Solo para especies nerviosas: clownfish, gamba, medusa.
// fixedWobbleScale   Solo criaturas fijas. 0=totalmente rígida. 1.8=anémona ondeante.
// tempoVariation     [0..1) modulación de velocidad a lo largo del tiempo.
//                    0 = velocidad constante (ballena, medusa).
//                    0.3 = variación moderada (pez ángel).
//                    0.6 = muy variable (cangrejo, pulpo).
//                    Implementado como tiempo deformado (integral de 1+k·sin).
//                    Siempre continuo y monotónico (k<1 → derivada > 0).
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
    val fixedWobbleScale: Float    = 1.0f,
    val tempoVariation: Float      = 0.0f,
    // sizeMultiplier: ajuste manual de tamaño relativo al cálculo base
    val sizeMultiplier: Float      = 1.0f,
    // instanceCount: criaturas fijas con Canvas se renderizan este número de veces
    val instanceCount: Int         = 1,
    // emojiRotation: rotación en grados para emojis orientados verticalmente (ej. langosta -90°)
    val emojiRotation: Float       = 0f
)

val allCreatures = listOf(

    // ── FISH ──────────────────────────────────────────────────────────────────
    //
    // Clownfish — nervioso y errático en X, pero contenido en Y.
    // El zig-zag es horizontal (xErraticness + 3 waveCount cortas),
    // no saltos verticales gigantes. tempoVar da variación de ritmo.
    CreatureSpec("🐟", CreatureSpecies.CLOWNFISH,
        MarineCategory.FISH,      2,  7500, 0.40f, 0.05f, SwimZone.UPPER,
        personalYFraction = 0.30f, waveCount = 3,  erraticness = 0.65f,
        driftSpeed = 0.61f, driftAmplitude = 0.40f, pauseFraction = 0.02f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.035f, xErraticness = 0.20f,
        tempoVariation = 0.25f, sizeMultiplier = 0.72f),

    // Angelfish — elegante, barridos suaves. Referencia de "movimiento correcto".
    CreatureSpec("🐠", CreatureSpecies.ANGELFISH,
        MarineCategory.FISH,      4,  9000, 0.55f, 0.04f, SwimZone.MID,
        personalYFraction = 0.50f, waveCount = 2,  erraticness = 0.20f,
        driftSpeed = 0.29f, driftAmplitude = 0.36f, pauseFraction = 0.06f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.018f, xErraticness = 0.05f,
        tempoVariation = 0.15f, sizeMultiplier = 0.74f),

    // Pufferfish — torpe, lentísimo, un arco único, pausas largas.
    CreatureSpec("🐡", CreatureSpecies.PUFFERFISH,
        MarineCategory.FISH,      7, 13000, 0.20f, 0.02f, SwimZone.MID,
        personalYFraction = 0.70f, waveCount = 1,  erraticness = 0.08f,
        driftSpeed = 0.17f, driftAmplitude = 0.16f, pauseFraction = 0.22f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.007f, xErraticness = 0.00f,
        tempoVariation = 0.10f),

    // ── FLORA ─────────────────────────────────────────────────────────────────
    //
    // Brain Coral — absolutamente rígido. fixedWobbleScale=0. 4 instancias en el suelo.
    CreatureSpec("🪸", CreatureSpecies.BRAIN_CORAL,
        MarineCategory.FLORA,     2,  0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 0.00f, instanceCount = 4),

    // Anemone — se mece con la corriente internamente vía animTimeMs.
    CreatureSpec("🌿", CreatureSpecies.ANEMONE,
        MarineCategory.FLORA,     4,  0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 0.00f, instanceCount = 3),

    // Kelp — meceo largo y suave. Bosque de tallos.
    CreatureSpec("🎋", CreatureSpecies.KELP,
        MarineCategory.FLORA,     6,  0, 0f, 0f, SwimZone.BOTTOM,
        fixedWobbleScale = 0.00f, instanceCount = 4),

    // ── CRUSTACEAN ────────────────────────────────────────────────────────────
    //
    // CLAVE: tempoVariation alta → a veces avanzan decididamente, a veces tantean.
    // verticalCoupling simula paso sobre irregularidades del fondo.
    // CRAWL easing se mantiene — solo ganan dimensionalidad vertical y ritmo variable.
    //
    // Lobster — cota más baja. Lento con ritmo variable.
    // verticalCoupling 0.15 = sube ligeramente al pasar "rocas" del fondo.
    // tempoVar 0.55 = a veces camina decidido, a veces casi para.
    // Lobster — pegado al suelo (personalYFraction=0.95). Se arrastra lentamente.
    // verticalCoupling 0.25 = sube ligeramente al pasar sobre rocas del fondo.
    CreatureSpec("🦞", CreatureSpecies.LOBSTER,
        MarineCategory.CRUSTACEAN, 2, 16000, 0.18f, 0.03f, SwimZone.BOTTOM,
        personalYFraction = 0.95f, waveCount = 1,  erraticness = 0.12f,
        driftSpeed = 0.13f, driftAmplitude = 0.14f, pauseFraction = 0.28f,
        easingType = EasingType.CRAWL, verticalCoupling = 0.25f,
        microWobble = 0.012f, xErraticness = 0.00f,
        tempoVariation = 0.55f, emojiRotation = -90f),

    // Hermit Crab — explorador errático. Algo más alto que la langosta (personalYFraction=0.30).
    // Avanza a trompicones y escala pequeñas rocas (verticalCoupling más alto).
    CreatureSpec("🦀", CreatureSpecies.HERMIT_CRAB,
        MarineCategory.CRUSTACEAN, 4, 13000, 0.22f, 0.04f, SwimZone.BOTTOM,
        personalYFraction = 0.30f, waveCount = 1,  erraticness = 0.55f,
        driftSpeed = 0.21f, driftAmplitude = 0.18f, pauseFraction = 0.32f,
        easingType = EasingType.CRAWL, verticalCoupling = 0.35f,
        microWobble = 0.014f, xErraticness = 0.06f,
        tempoVariation = 0.60f),

    // Shrimp — BURST puro: propulsión a cola. Más pausada que antes
    // pero mantiene su personalidad nerviosa y diagonal.
    CreatureSpec("🦐", CreatureSpecies.SHRIMP,
        MarineCategory.CRUSTACEAN, 6,  5500, 0.55f, 0.05f, SwimZone.LOWER,
        personalYFraction = 0.20f, waveCount = 3,  erraticness = 0.70f,
        driftSpeed = 0.41f, driftAmplitude = 0.45f, pauseFraction = 0.10f,
        easingType = EasingType.BURST, verticalCoupling = 0.00f,
        microWobble = 0.028f, xErraticness = 0.12f,
        tempoVariation = 0.30f, sizeMultiplier = 0.72f),

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
    // Manta Ray — planeo grácil. verticalCoupling=0.30: sube ligeramente
    // al cruzar el centro, como si planeara sobre una corriente ascendente.
    CreatureSpec("🦈", CreatureSpecies.MANTA_RAY,
        MarineCategory.PELAGIC,   2,  8500, 0.35f, 0.03f, SwimZone.MID,
        personalYFraction = 0.45f, waveCount = 1,  erraticness = 0.06f,
        driftSpeed = 0.23f, driftAmplitude = 0.26f, pauseFraction = 0.03f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.30f,
        microWobble = 0.008f, xErraticness = 0.00f,
        tempoVariation = 0.12f, sizeMultiplier = 2.4f),

    // Moon Jellyfish — deriva completamente pasiva. erraticness=0.90 + xErraticness=0.22.
    // No "nada": es arrastrada por corrientes simuladas. Nunca repite el mismo camino.
    CreatureSpec("🪼", CreatureSpecies.MOON_JELLYFISH,
        MarineCategory.PELAGIC,   4, 17000, 0.95f,-0.01f, SwimZone.UPPER,
        personalYFraction = 0.50f, waveCount = 2,  erraticness = 0.90f,
        driftSpeed = 0.61f, driftAmplitude = 0.95f, pauseFraction = 0.00f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.002f, xErraticness = 0.22f,
        tempoVariation = 0.08f),

    // Whale Shark — majestuosa. Ciclo de 20s, casi plana, predecible.
    CreatureSpec("🐋", CreatureSpecies.WHALE_SHARK,
        MarineCategory.PELAGIC,   8, 20000, 0.08f,-0.03f, SwimZone.MID,
        personalYFraction = 0.50f, waveCount = 1,  erraticness = 0.04f,
        driftSpeed = 0.13f, driftAmplitude = 0.10f, pauseFraction = 0.04f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.003f, xErraticness = 0.00f,
        tempoVariation = 0.06f),

    // ── CEPHALOPOD ────────────────────────────────────────────────────────────
    //
    // Octopus — BURST + pausa 28%. Se propulsa en chorro, para, observa.
    // tempoVar 0.30 = ritmo variable entre propulsiones.
    CreatureSpec("🐙", CreatureSpecies.OCTOPUS,
        MarineCategory.CEPHALOPOD, 2,  8000, 0.72f, 0.04f, SwimZone.LOWER,
        personalYFraction = 0.35f, waveCount = 2,  erraticness = 0.62f,
        driftSpeed = 0.43f, driftAmplitude = 0.55f, pauseFraction = 0.28f,
        easingType = EasingType.BURST, verticalCoupling = 0.00f,
        microWobble = 0.020f, xErraticness = 0.00f,
        tempoVariation = 0.30f),

    // Squid — BURST rápido. Ráfagas en diagonal. Más pausado que antes
    // pero tempoVar amplifica el contraste: a veces dispara, a veces planea.
    CreatureSpec("🦑", CreatureSpecies.SQUID,
        MarineCategory.CEPHALOPOD, 5,  5000, 0.65f, 0.05f, SwimZone.MID,
        personalYFraction = 0.40f, waveCount = 3,  erraticness = 0.72f,
        driftSpeed = 0.47f, driftAmplitude = 0.55f, pauseFraction = 0.10f,
        easingType = EasingType.BURST, verticalCoupling = 0.00f,
        microWobble = 0.024f, xErraticness = 0.08f,
        tempoVariation = 0.25f),

    // ── REPTILE ───────────────────────────────────────────────────────────────
    //
    // Sea Turtle — nado sereno con aletas grandes. Un arco limpio y predecible.
    CreatureSpec("🐢", CreatureSpecies.SEA_TURTLE,
        MarineCategory.REPTILE,   2, 14000, 0.22f,-0.02f, SwimZone.MID,
        personalYFraction = 0.60f, waveCount = 1,  erraticness = 0.10f,
        driftSpeed = 0.31f, driftAmplitude = 0.26f, pauseFraction = 0.10f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.011f, xErraticness = 0.00f,
        tempoVariation = 0.12f),

    // ── MAMMAL ────────────────────────────────────────────────────────────────
    //
    // Dolphin — el más expresivo. verticalCoupling=0.80.
    // Más pausado que antes (6500ms) → los arcos de salto se aprecian mejor.
    // tempoVar 0.20 = a veces salta vivo, a veces planea relajado.
    CreatureSpec("🐬", CreatureSpecies.DOLPHIN,
        MarineCategory.MAMMAL,    2,  6500, 0.60f, 0.03f, SwimZone.SURFACE,
        personalYFraction = 0.50f, waveCount = 2,  erraticness = 0.25f,
        driftSpeed = 0.51f, driftAmplitude = 0.50f, pauseFraction = 0.03f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.80f,
        microWobble = 0.020f, xErraticness = 0.06f,
        tempoVariation = 0.20f),

    // Seal — ondulante de todo el cuerpo. verticalCoupling=0.44: arcos moderados.
    CreatureSpec("🦭", CreatureSpecies.SEAL,
        MarineCategory.MAMMAL,    5, 10500, 0.55f,-0.02f, SwimZone.UPPER,
        personalYFraction = 0.50f, waveCount = 2,  erraticness = 0.20f,
        driftSpeed = 0.37f, driftAmplitude = 0.38f, pauseFraction = 0.08f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.44f,
        microWobble = 0.015f, xErraticness = 0.03f,
        tempoVariation = 0.15f),

    // Blue Whale — enorme, lentísima, casi plana. Ciclo de 22s.
    CreatureSpec("🐳", CreatureSpecies.BLUE_WHALE,
        MarineCategory.MAMMAL,    8, 22000, 0.07f,-0.03f, SwimZone.SURFACE,
        personalYFraction = 0.50f, waveCount = 1,  erraticness = 0.03f,
        driftSpeed = 0.11f, driftAmplitude = 0.09f, pauseFraction = 0.04f,
        easingType = EasingType.SMOOTH, verticalCoupling = 0.00f,
        microWobble = 0.003f, xErraticness = 0.00f,
        tempoVariation = 0.05f),

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

// ── Posición X de criaturas fijas ─────────────────────────────────────────────
private fun fixedX(fixedIndex: Int, totalFixed: Int): Float {
    val step = 0.80f / (totalFixed + 1).toFloat()
    return 0.10f + step * (fixedIndex + 1)
}

// Distribución Fibonacci → fases bien separadas, sin agrupamiento visible
private fun phaseOffset(index: Int): Float = (index * 0.618f) % 1f

// ── Variación por instancia ──────────────────────────────────────────────────
//
// Función determinista: dado un index y un seed, devuelve un valor en [-1, 1].
// Cada (index, seed) produce un valor distinto pero estable entre frames.
// Esto hace que dos criaturas de la misma especie nunca sean idénticas.
//
private fun instanceNoise(index: Int, seed: Int): Float {
    return ((index * 7919 + seed * 104729) % 1000) / 500f - 1f
}

// Aplica variación ±pct sobre un valor base.
// instanceNoise ∈ [-1,1], pct = 0.12 → resultado ∈ [base×0.88, base×1.12]
private fun vary(base: Float, index: Int, seed: Int, pct: Float = 0.12f): Float {
    return base * (1f + instanceNoise(index, seed) * pct)
}

// ── Tempo warping ────────────────────────────────────────────────────────────
//
// Deforma el tiempo para que la criatura nade a velocidad variable.
//
// Matemática:
//   velocidad instantánea = 1 + k · sin(TAU · t / P + φ)
//   tiempo deformado t' = ∫₀ᵗ velocidad(s) ds
//                       = t + (k·P/TAU) · [cos(φ) − cos(TAU·t/P + φ)]
//
// Propiedades:
//   • Continua: sin() y cos() son continuas → t' es continua.
//   • Monotónica: k < 1 → derivada = 1 + k·sin(...) > 0 siempre.
//   • Sin saltos en wrap: al usarla con osc(), el módulo opera sobre Long
//     y sin() es periódica → continuidad garantizada.
//
// pacePeriodMs: período del modulador (~37s base, variado por phase).
//   Largo para que el cambio de ritmo sea gradual e imperceptible como ciclo.
//
private fun warpTime(
    t: Long,
    tempoVar: Float,
    pacePeriodMs: Long,
    phaseOffset: Float
): Long {
    if (tempoVar <= 0f) return t
    val k = tempoVar.coerceIn(0f, 0.95f)

    // Módulo Long para precisión, luego Float para trigonometría
    val phaseFrac = (t % pacePeriodMs).toFloat() / pacePeriodMs.toFloat()
    val phi = phaseOffset * TAU
    val cosInit = cos(phi)
    val cosNow = cos(phaseFrac * TAU + phi)

    val offset = (k * pacePeriodMs.toFloat() / TAU) * (cosInit - cosNow)
    return t + offset.toLong()
}

expect fun DrawScope.drawEmoji(
    emoji: String,
    x: Float,
    y: Float,
    sizeSp: Float,
    mirrored: Boolean,
    rotation: Float = 0f
)

// ── Hit-testing ───────────────────────────────────────────────────────────────

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
        abs(offset.x - pos.x) <= pos.hitRadius && abs(offset.y - pos.y) <= pos.hitRadius
    }
    .minByOrNull { pos ->
        val dx = offset.x - pos.x; val dy = offset.y - pos.y
        dx * dx + dy * dy
    }
    ?.species

// ── Composable principal ──────────────────────────────────────────────────────

@Composable
fun AquariumCreatures(
    ecosystemByCategory: Map<MarineCategory, EcosystemState>,
    creatureLevelBySpecies: Map<CreatureSpecies, Int> = emptyMap(),
    creaturesData: List<MarineCreature> = emptyList(),
    freezeState: CreatureFreezeState = rememberCreatureFreezeState(),
    onCreatureTap: (MarineCreature, CreatureSpec) -> Unit = { _, _ -> },
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

    // ── Reloj monotónico ──────────────────────────────────────────────────────
    //
    // `elapsedMs` es tiempo transcurrido en ms desde que el composable entró en
    // composición. Monotónicamente creciente. NUNCA reinicia.
    //
    // Leer este state en drawWithContent crea observación solo en la fase de draw:
    // los cambios de estado solo invalidan el canvas, NO provocan recomposición.
    // Resultado: ~60 redraws/s en sincronía con el vsync. Sin overhead de composición.
    //
    val elapsedMs = remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        var startNs = 0L
        withFrameNanos { startNs = it }
        while (true) {
            withFrameNanos { frameNs ->
                elapsedMs.value = (frameNs - startNs) / 1_000_000L
            }
        }
    }

    // frozenTimeMap: guarda elapsedMs en Long en el momento del freeze.
    // La criatura congelada usa este tiempo fijo → inmóvil hasta que se descongele.
    val frozenTimeMap = remember { mutableStateMapOf<CreatureSpecies, Long>() }
    val creaturePositions = remember { mutableStateOf<List<CreaturePosition>>(emptyList()) }

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
                        frozenTimeMap[hit] = elapsedMs.value
                        onCreatureTap(creature, spec)
                    }
                )
            }
            .drawWithContent {
                drawContent()
                val positions = mutableListOf<CreaturePosition>()

                // Leer elapsedMs una sola vez por frame (state observation aquí)
                val nowMs = elapsedMs.value

                // Pre-calcular distribución de flora Canvas (múltiples instancias, interleaved)
                val fixedEmojiCreatures = fixedCreatures.filter { rendererFor(it.species) == null }
                val fixedCanvasCreatures = fixedCreatures.filter { rendererFor(it.species) != null }
                val totalFloraSlots = fixedCanvasCreatures.sumOf { it.instanceCount }

                // Round-robin: intercalar especies en vez de agruparlas por secciones
                // Ej: BC#0, AN#0, KE#0, BC#1, AN#1, KE#1, BC#2, KE#2, BC#3, KE#3
                val floraSlotMap = mutableMapOf<CreatureSpecies, List<Int>>()
                run {
                    var slotIdx = 0
                    val maxInstances = fixedCanvasCreatures.maxOfOrNull { it.instanceCount } ?: 0
                    val tempMap = mutableMapOf<CreatureSpecies, MutableList<Int>>()
                    fixedCanvasCreatures.forEach { tempMap[it.species] = mutableListOf() }
                    for (round in 0 until maxInstances) {
                        for (spec in fixedCanvasCreatures) {
                            if (round < spec.instanceCount) {
                                tempMap[spec.species]!!.add(slotIdx)
                                slotIdx++
                            }
                        }
                    }
                    tempMap.forEach { (k, v) -> floraSlotMap[k] = v }
                }

                unlockedCreatures.forEachIndexed { index, spec ->
                    val w = size.width
                    val h = size.height

                    val creatureLevel = creatureLevelBySpecies[spec.species] ?: 1
                    val sizeScale = 0.8f + (creatureLevel - 1) * 0.10f
                    val baseSize = 28f * (1 + spec.unlockLevel / 15f)
                    val iconSize = baseSize * sizeScale * spec.sizeMultiplier

                    // Criatura congelada usa su tiempo fijo; si no, usa el reloj global
                    val tRaw = if (freezeState.isFrozen(spec.species))
                        frozenTimeMap[spec.species] ?: nowMs
                    else nowMs

                    val phase = phaseOffset(index)
                    val zoneCenter = h * spec.swimZone.centerFraction
                    val zoneBand  = h * spec.swimZone.bandFraction

                    // Cota personal dentro de la banda. Determinista, no aleatoria.
                    val personalY = zoneCenter + (spec.personalYFraction - 0.5f) * zoneBand

                    var x: Float = 0f
                    var y: Float = 0f
                    var hitboxAdded = false

                    if (spec.swimDuration == 0) {
                        val floorY = AquariumBounds.floorY(h)
                        val renderer = rendererFor(spec.species)

                        if (renderer != null) {
                            // ── FLORA CANVAS — múltiples instancias interleaved ──
                            val slots = floraSlotMap[spec.species] ?: emptyList()
                            val renderSize = iconSize * 3.2f

                            for (i in 0 until spec.instanceCount) {
                                val slotIndex = slots.getOrElse(i) { i }
                                val instanceX = w * (0.05f + 0.90f * (slotIndex + 0.5f) / totalFloraSlots)
                                val instanceAnim = tRaw + i * 5000L
                                with(renderer) { render(instanceX, floorY, renderSize, creatureLevel, instanceAnim, false) }
                                // Hitbox en todas las instancias
                                positions.add(CreaturePosition(spec.species, instanceX, floorY - renderSize * 0.4f, maxOf(renderSize, 75f)))
                            }
                            hitboxAdded = true

                        } else {
                            // ── CRIATURA FIJA EMOJI — posición en eje X distribuido ──
                            val emojiIdx = fixedEmojiCreatures.indexOf(spec)
                            x = w * fixedX(emojiIdx, fixedEmojiCreatures.size)
                            val wobble = osc(tRaw, 12000L, phase) * zoneBand * 0.20f * spec.fixedWobbleScale
                            y = floorY - iconSize * 0.3f + wobble
                            drawEmoji(spec.emoji, x, y, iconSize, mirrored = false, rotation = spec.emojiRotation)
                        }

                    } else {
                        // ── CRIATURA NADADORA ─────────────────────────────────────

                        // Variación por instancia: ±12% sobre parámetros clave.
                        // Cada criatura del mismo tipo tiene ritmo y amplitud ligeramente distintos.
                        val variedDuration = vary(spec.swimDuration.toFloat(), index, 1).toLong()
                        val variedWobble = vary(spec.wobbleAmplitude, index, 2)
                        val variedDriftSpeed = vary(spec.driftSpeed, index, 3)
                        val variedTempoVar = vary(spec.tempoVariation, index, 4, pct = 0.15f)

                        val speedMult = max(0.3f, 1f + (creatureLevel - 1) * spec.speedScalePerLevel)
                        val cycleDuration = (variedDuration / speedMult).toLong().coerceAtLeast(2000L)

                        // ── Tempo warping ─────────────────────────────────────────
                        // Período del modulador: ~37s base, variado por phase para
                        // que cada criatura tenga su propio ciclo de aceleración.
                        // Largo → el cambio de ritmo es gradual, nunca se percibe como repetición.
                        val pacePeriodMs = (37000f * (1f + phase * 0.4f)).toLong()
                        val tSwim = warpTime(tRaw, variedTempoVar, pacePeriodMs, phase)

                        // Phase offset en tiempo: desplaza el inicio del ciclo.
                        val phaseMs = (phase * cycleDuration).toLong()

                        // rawProgress: [0, 1) dentro del ciclo propio.
                        // Modulo Long → sin pérdida de precisión. Continuo en wrap. ✓
                        val rawProgress = ((tSwim + phaseMs) % cycleDuration).toFloat() / cycleDuration.toFloat()

                        // ── Posición X ────────────────────────────────────────────
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

                        // Microaceleración horizontal aperiódica.
                        // Usa tSwim → las perturbaciones también se modulan con el tempo.
                        val xPert = if (spec.xErraticness > 0f) {
                            val xPertPeriod = (cycleDuration.toFloat() / (5f * PHI)).toLong().coerceAtLeast(200L)
                            osc(tSwim + phaseMs, xPertPeriod, phase * PHI) * spec.xErraticness * 0.05f
                        } else 0f

                        // ── Márgenes simétricos basados en tamaño del emoji ──────
                        // El centro del emoji nunca se acerca a menos de halfIcon del borde.
                        // Resultado: el emoji siempre está completamente visible en ambos extremos.
                        val halfIcon = iconSize / 2f
                        val xMin = halfIcon + w * 0.01f
                        val xMax = w - halfIcon - w * 0.01f
                        x = (xMin + (xMax - xMin) * swimProgress + w * xPert)
                            .coerceIn(xMin, xMax)

                        // ── Posición Y: cinco capas continuas ─────────────────────
                        //
                        // Capas 1-2 (ondas de nado) y 4 (coupling) usan tSwim → se modulan
                        // con el tempo. Capas 3 (drift) y 5 (microwobble) usan tRaw →
                        // son independientes del ritmo de nado (corriente ambiental y biología).
                        //
                        // 1. ONDA PRIMARIA
                        val primaryPeriod = (cycleDuration / spec.waveCount.toLong().coerceAtLeast(1L))
                            .coerceAtLeast(200L)
                        val primaryWave = osc(tSwim + phaseMs, primaryPeriod, phase)

                        // 2. ONDA SECUNDARIA (irracional respecto a la primaria)
                        val secPeriod = (cycleDuration.toFloat() / (spec.waveCount.toFloat() * PHI))
                            .toLong().coerceAtLeast(200L)
                        val secondaryWave = osc(tSwim + phaseMs, secPeriod, phase * PHI)

                        val waveY = (primaryWave * (1f - spec.erraticness) +
                                secondaryWave * spec.erraticness) *
                                zoneBand * variedWobble

                        // 3. DERIVA LENTA DEL EJE Y (corriente ambiental — usa tRaw)
                        val driftPeriod = (12000f / variedDriftSpeed.coerceAtLeast(0.01f))
                            .toLong().coerceAtLeast(1000L)
                        val drift = osc(tRaw, driftPeriod, phase) * zoneBand * spec.driftAmplitude

                        // 4. ACOPLAMIENTO VERTICAL (usa swimProgress de tSwim)
                        val coupledArc = -sin(swimProgress * PI) * zoneBand * spec.verticalCoupling

                        // 5. MICROWOBBLE (biológico — usa tRaw)
                        val micro = osc(tRaw, 667L, phase) * zoneBand * spec.microWobble

                        // Y base según zona
                        val rawY = if (spec.swimZone == SwimZone.BOTTOM) {
                            // Crustáceos en el suelo: anclar a floorY con offset vertical por especie.
                            // personalYFraction 0.95=pegado al suelo, 0.30=algo más arriba (cangrejo).
                            val floorY = AquariumBounds.floorY(h)
                            val bottomCoupling = h * 0.10f  // banda efectiva más grande para coupling
                            val heightOffset = (1f - spec.personalYFraction) * h * 0.06f
                            val coupledArcBottom = -sin(swimProgress * PI) * bottomCoupling * spec.verticalCoupling
                            floorY - iconSize * 0.3f - heightOffset + coupledArcBottom + micro
                        } else {
                            (personalY + waveY + drift + coupledArc + micro).coerceIn(
                                personalY - zoneBand * 1.15f,
                                personalY + zoneBand * 1.15f
                            )
                        }

                        // Y clamping global: no atravesar superficie ni suelo
                        val surfaceLimit = if (spec.swimZone == SwimZone.SURFACE) {
                            halfIcon  // delfín/ballena pueden saltar fuera del agua
                        } else {
                            AquariumBounds.surfaceY(h) + halfIcon
                        }
                        val floorLimit = AquariumBounds.floorY(h) - halfIcon
                        y = rawY.coerceIn(surfaceLimit, floorLimit)

                        val renderer = rendererFor(spec.species)
                        if (renderer != null) {
                            with(renderer) { render(x, y, iconSize, creatureLevel, tRaw, goingRight) }
                        } else {
                            drawEmoji(spec.emoji, x, y, iconSize, mirrored = goingRight, rotation = spec.emojiRotation)
                        }
                    }

                    // Hitbox (solo si no se añadió ya en el bloque de flora Canvas)
                    if (!hitboxAdded) {
                        positions.add(CreaturePosition(spec.species, x, y, maxOf(iconSize * 2.5f, 75f)))
                    }
                }

                creaturePositions.value = positions
            }
    ) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}