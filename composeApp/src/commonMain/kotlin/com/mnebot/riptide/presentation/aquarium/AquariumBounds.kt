package com.mnebot.riptide.presentation.aquarium

object AquariumBounds {
    const val SURFACE_FRACTION = 0.08f
    const val FLOOR_FRACTION = 0.88f

    fun surfaceY(h: Float): Float = h * SURFACE_FRACTION
    fun floorY(h: Float): Float = h * FLOOR_FRACTION
}
