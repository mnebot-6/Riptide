package com.mnebot.riptide.presentation.aquarium.weather

/**
 * Represents the current weather conditions affecting the aquarium's visual appearance.
 * All values are normalized to [0, 1] unless otherwise noted.
 */
data class WeatherState(
    /** Sky cloud coverage: 0 = clear sky, 1 = fully overcast */
    val cloudCoverage: Float = 0f,
    /** Wind strength affecting cloud drift and current particles: 0 = calm, 1 = strong */
    val windStrength: Float = 0f,
    /** Rain intensity: 0 = no rain, 1 = heavy rain */
    val rainIntensity: Float = 0f,
    /** Multiplier for wave amplitude: 0.5 = very calm, 1.0 = normal, 2.0 = stormy */
    val waveAmplitudeMultiplier: Float = 1.0f
) {
    companion object {
        /** Default clear weather */
        val Clear = WeatherState(
            cloudCoverage = 0.05f,
            windStrength = 0.1f,
            rainIntensity = 0f,
            waveAmplitudeMultiplier = 0.8f
        )

        /** Light overcast with gentle breeze */
        val Cloudy = WeatherState(
            cloudCoverage = 0.6f,
            windStrength = 0.3f,
            rainIntensity = 0f,
            waveAmplitudeMultiplier = 1.0f
        )

        /** Light rain */
        val Rainy = WeatherState(
            cloudCoverage = 0.75f,
            windStrength = 0.5f,
            rainIntensity = 0.5f,
            waveAmplitudeMultiplier = 1.4f
        )

        /** Heavy storm */
        val Stormy = WeatherState(
            cloudCoverage = 0.9f,
            windStrength = 0.8f,
            rainIntensity = 0.85f,
            waveAmplitudeMultiplier = 1.8f
        )
    }
}

/** Linear interpolation between two weather states */
fun lerpWeather(a: WeatherState, b: WeatherState, t: Float): WeatherState {
    val f = t.coerceIn(0f, 1f)
    return WeatherState(
        cloudCoverage = a.cloudCoverage + (b.cloudCoverage - a.cloudCoverage) * f,
        windStrength = a.windStrength + (b.windStrength - a.windStrength) * f,
        rainIntensity = a.rainIntensity + (b.rainIntensity - a.rainIntensity) * f,
        waveAmplitudeMultiplier = a.waveAmplitudeMultiplier + (b.waveAmplitudeMultiplier - a.waveAmplitudeMultiplier) * f
    )
}
