package com.mnebot.riptide.presentation.aquarium.weather

import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates random weather that transitions smoothly over time.
 *
 * On creation, picks a random weather "seed". The weather evolves slowly
 * using sine-based oscillation with irrational period ratios to avoid
 * obvious repetition. Transitions between weather presets every ~30 minutes.
 */
class RandomWeatherProvider(seed: Long = System.currentTimeMillis()) : WeatherProvider {

    private val random = Random(seed)

    // Random offsets for each parameter — creates unique per-session weather personality
    private val cloudPhase = random.nextFloat() * 6.2832f    // 0..2π
    private val windPhase = random.nextFloat() * 6.2832f
    private val rainPhase = random.nextFloat() * 6.2832f

    // Base values — biased towards pleasant weather (clear/cloudy more likely than stormy)
    private val baseCloud = random.nextFloat() * 0.5f       // 0..0.5 base coverage
    private val baseWind = random.nextFloat() * 0.3f + 0.05f // 0.05..0.35 base wind
    private val rainThreshold = 0.55f + random.nextFloat() * 0.2f  // rain only above this

    companion object {
        // Oscillation periods in milliseconds (irrational ratios avoid repetition)
        private const val CLOUD_PERIOD = 1_800_000.0  // 30 min
        private const val WIND_PERIOD = 1_113_000.0    // ~18.5 min (incommensurate)
        private const val RAIN_PERIOD = 2_471_000.0    // ~41 min (incommensurate)
        private const val WAVE_PERIOD = 1_517_000.0    // ~25 min
    }

    override fun currentWeather(elapsedMs: Long): WeatherState {
        val t = elapsedMs.toDouble()

        // Smooth oscillation for each parameter
        val cloudOsc = sin(t / CLOUD_PERIOD * 6.2832 + cloudPhase).toFloat()
        val windOsc = sin(t / WIND_PERIOD * 6.2832 + windPhase).toFloat()
        val rainOsc = sin(t / RAIN_PERIOD * 6.2832 + rainPhase).toFloat()
        val waveOsc = sin(t / WAVE_PERIOD * 6.2832 + cloudPhase * 0.7f).toFloat()

        // Cloud coverage: base ± 0.35 oscillation, clamped to [0, 1]
        val cloud = (baseCloud + cloudOsc * 0.35f).coerceIn(0f, 1f)

        // Wind: base ± 0.25, clamped to [0, 1]
        val wind = (baseWind + windOsc * 0.25f).coerceIn(0f, 1f)

        // Rain: only when clouds are heavy enough
        val rawRain = (baseCloud + rainOsc * 0.4f)
        val rain = if (rawRain > rainThreshold) {
            ((rawRain - rainThreshold) / (1f - rainThreshold)).coerceIn(0f, 1f)
        } else 0f

        // Wave amplitude: tied to wind + subtle oscillation
        val waveMultiplier = (0.7f + wind * 0.8f + waveOsc * 0.15f).coerceIn(0.5f, 2.0f)

        return WeatherState(
            cloudCoverage = cloud,
            windStrength = wind,
            rainIntensity = rain,
            waveAmplitudeMultiplier = waveMultiplier
        )
    }
}
