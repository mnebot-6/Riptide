package com.mnebot.riptide.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.mnebot.riptide.presentation.aquarium.getCurrentHourFraction

private val AdaptiveBase = Color(0xFF0D1E38)

internal fun adaptiveCardAlpha(hourFraction: Float): Float {
    return when {
        hourFraction < 5f  -> 0.45f
        hourFraction < 8f  -> 0.45f + (hourFraction - 5f) / 3f * 0.25f  // 0.45→0.70
        hourFraction < 17f -> 0.70f
        hourFraction < 21f -> 0.70f - (hourFraction - 17f) / 4f * 0.25f  // 0.70→0.45
        else               -> 0.45f
    }
}

fun adaptiveCardColor(hourFraction: Float): Color =
    AdaptiveBase.copy(alpha = adaptiveCardAlpha(hourFraction))

@Composable
fun rememberAdaptiveCardColor(): Color {
    val hourFraction = remember { getCurrentHourFraction() }
    return remember(hourFraction) { adaptiveCardColor(hourFraction) }
}
