package com.mnebot.riptide.presentation.navigation

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import riptide.composeapp.generated.resources.*

/**
 * Tabs displayed by [com.mnebot.riptide.presentation.main.MainShellScreen].
 *
 * Order matters: PROGRESS sits to the LEFT, TODAY is the centre (default landing
 * tab), POND to the RIGHT. Calendar was removed in the post-launch UX cleanup.
 */
enum class BottomNavTab(
    val icon: DrawableResource,
    val labelRes: StringResource
) {
    PROGRESS(Res.drawable.ic_bar_chart, Res.string.tab_progress),
    TODAY(Res.drawable.ic_check_square, Res.string.tab_today),
    POND(Res.drawable.ic_waves, Res.string.tab_pond)
}
