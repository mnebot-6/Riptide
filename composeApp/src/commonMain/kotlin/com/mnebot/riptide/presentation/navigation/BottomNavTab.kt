package com.mnebot.riptide.presentation.navigation

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import riptide.composeapp.generated.resources.*

enum class BottomNavTab(
    val icon: DrawableResource,
    val labelRes: StringResource
) {
    TODAY(Res.drawable.ic_check_square, Res.string.tab_today),
    POND(Res.drawable.ic_fish, Res.string.tab_pond),
    PROGRESS(Res.drawable.ic_bar_chart, Res.string.tab_progress)
}
