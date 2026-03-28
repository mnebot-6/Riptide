package com.mnebot.riptide.presentation.aquarium.weather

/**
 * Provides weather state for the aquarium.
 * Currently only [RandomWeatherProvider] exists (random weather per session).
 * Future: [LocationWeatherProvider] will fetch real weather from an API.
 */
interface WeatherProvider {
    /** Get the current weather state. May evolve over time. */
    fun currentWeather(elapsedMs: Long): WeatherState
}
