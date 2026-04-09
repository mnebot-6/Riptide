package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.graphics.Color
import com.mnebot.riptide.domain.model.MarineCategory

/**
 * Visual theme for a biome pond. Rather than replacing every color constant
 * in the aquarium renderer, BiomeTheme works as an overlay: it tints the
 * ocean gradient, sand, and rocks while preserving the time-of-day system.
 */
data class BiomeTheme(
    val oceanShallow: Color,
    val oceanMid: Color,
    val oceanDeep: Color,
    val sandLight: Color,
    val sandMid: Color,
    val sandDark: Color,
    val rockLight: Color,
    val rockMid: Color,
    val rockDark: Color,
    val waterTint: Color = Color.Transparent,
    val ambientOverlay: Color = Color.Transparent
) {
    companion object {
        /** Default theme — matches the existing hardcoded palette. */
        val DEFAULT = BiomeTheme(
            oceanShallow = Color(0xFF5ABCE8),
            oceanMid = Color(0xFF2880C8),
            oceanDeep = Color(0xFF0E4A90),
            sandLight = Color(0xFFE2C898),
            sandMid = Color(0xFFC8A872),
            sandDark = Color(0xFF9A7A50),
            rockLight = Color(0xFF7A8CA2),
            rockMid = Color(0xFF586070),
            rockDark = Color(0xFF3A4858)
        )
    }
}

/** Maps each MarineCategory to its biome visual theme. */
object BiomeThemes {

    private val CORAL_REEF = BiomeTheme(
        oceanShallow = Color(0xFF5ABCE8),
        oceanMid = Color(0xFF2880C8),
        oceanDeep = Color(0xFF0E4A90),
        sandLight = Color(0xFFE2C898),
        sandMid = Color(0xFFC8A872),
        sandDark = Color(0xFF9A7A50),
        rockLight = Color(0xFF7A8CA2),
        rockMid = Color(0xFF586070),
        rockDark = Color(0xFF3A4858)
    )

    private val KELP_FOREST = BiomeTheme(
        oceanShallow = Color(0xFF3AAA6A),
        oceanMid = Color(0xFF1A7848),
        oceanDeep = Color(0xFF0A4828),
        sandLight = Color(0xFFC0D0A0),
        sandMid = Color(0xFF98B070),
        sandDark = Color(0xFF688848),
        rockLight = Color(0xFF607848),
        rockMid = Color(0xFF405830),
        rockDark = Color(0xFF284020),
        waterTint = Color(0x1030A030)
    )

    private val MANGROVE = BiomeTheme(
        oceanShallow = Color(0xFF8A9060),
        oceanMid = Color(0xFF6A7040),
        oceanDeep = Color(0xFF3A4828),
        sandLight = Color(0xFFD0B080),
        sandMid = Color(0xFFB09060),
        sandDark = Color(0xFF887040),
        rockLight = Color(0xFF806838),
        rockMid = Color(0xFF604828),
        rockDark = Color(0xFF403018),
        waterTint = Color(0x10706020)
    )

    private val SANDY_FLOOR = BiomeTheme(
        oceanShallow = Color(0xFF70C8E0),
        oceanMid = Color(0xFF40A0C0),
        oceanDeep = Color(0xFF186898),
        sandLight = Color(0xFFF0E0C0),
        sandMid = Color(0xFFD8C8A0),
        sandDark = Color(0xFFB0A080),
        rockLight = Color(0xFFA0A898),
        rockMid = Color(0xFF788880),
        rockDark = Color(0xFF586868)
    )

    private val OPEN_OCEAN = BiomeTheme(
        oceanShallow = Color(0xFF3088D0),
        oceanMid = Color(0xFF1860A8),
        oceanDeep = Color(0xFF083870),
        sandLight = Color(0xFF607090),
        sandMid = Color(0xFF485868),
        sandDark = Color(0xFF304050),
        rockLight = Color(0xFF506080),
        rockMid = Color(0xFF384858),
        rockDark = Color(0xFF283040),
        waterTint = Color(0x08103080)
    )

    private val ABYSS = BiomeTheme(
        oceanShallow = Color(0xFF182040),
        oceanMid = Color(0xFF101830),
        oceanDeep = Color(0xFF080E20),
        sandLight = Color(0xFF383848),
        sandMid = Color(0xFF282838),
        sandDark = Color(0xFF181828),
        rockLight = Color(0xFF303048),
        rockMid = Color(0xFF202038),
        rockDark = Color(0xFF101028),
        waterTint = Color(0x18100820),
        ambientOverlay = Color(0x0840F0F0)  // bioluminescent hint
    )

    private val TROPICAL_LAGOON = BiomeTheme(
        oceanShallow = Color(0xFF40D8C8),
        oceanMid = Color(0xFF28B0A8),
        oceanDeep = Color(0xFF108878),
        sandLight = Color(0xFFF0E8D0),
        sandMid = Color(0xFFD0C8A8),
        sandDark = Color(0xFFA89878),
        rockLight = Color(0xFF80A0A0),
        rockMid = Color(0xFF607878),
        rockDark = Color(0xFF405858)
    )

    private val ARCTIC = BiomeTheme(
        oceanShallow = Color(0xFF90C8E0),
        oceanMid = Color(0xFF5898C0),
        oceanDeep = Color(0xFF2868A0),
        sandLight = Color(0xFFD8E0E8),
        sandMid = Color(0xFFB0C0D0),
        sandDark = Color(0xFF88A0B0),
        rockLight = Color(0xFFA0B0C0),
        rockMid = Color(0xFF7890A0),
        rockDark = Color(0xFF587080),
        waterTint = Color(0x08C0E0F0)
    )

    private val TREASURE_CAVE = BiomeTheme(
        oceanShallow = Color(0xFF384050),
        oceanMid = Color(0xFF283040),
        oceanDeep = Color(0xFF182028),
        sandLight = Color(0xFFC8A860),
        sandMid = Color(0xFFA08840),
        sandDark = Color(0xFF786828),
        rockLight = Color(0xFF585040),
        rockMid = Color(0xFF403830),
        rockDark = Color(0xFF282020),
        ambientOverlay = Color(0x08F0C030)  // golden shimmer
    )

    fun forCategory(category: MarineCategory): BiomeTheme = when (category) {
        MarineCategory.FISH -> CORAL_REEF
        MarineCategory.FLORA -> KELP_FOREST
        MarineCategory.CRUSTACEAN -> MANGROVE
        MarineCategory.MOLLUSK -> SANDY_FLOOR
        MarineCategory.PELAGIC -> OPEN_OCEAN
        MarineCategory.CEPHALOPOD -> ABYSS
        MarineCategory.REPTILE -> TROPICAL_LAGOON
        MarineCategory.MAMMAL -> ARCTIC
        MarineCategory.DECORATION -> TREASURE_CAVE
        MarineCategory.COMPANION -> BiomeTheme.DEFAULT  // follows selected pond
    }
}
