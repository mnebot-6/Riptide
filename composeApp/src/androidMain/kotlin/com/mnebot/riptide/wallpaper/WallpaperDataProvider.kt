package com.mnebot.riptide.wallpaper

import android.content.Context
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import com.mnebot.riptide.presentation.aquarium.allCreatures

/**
 * Provides creature data from Room DB for the live wallpaper.
 * Reads all unlocked marine creatures and maps them to CreatureSpec for rendering.
 */
class WallpaperDataProvider(context: Context) {
    private val db = DatabaseProvider.getDatabase(context)

    suspend fun loadCreatureData(): WallpaperCreatureData {
        val entities = db.marineCreatureDao().getAll()

        // Map entity species name strings to CreatureSpecies enum
        val unlockedSpecies = entities.mapNotNull { entity ->
            try { CreatureSpecies.valueOf(entity.species) } catch (_: Exception) { null }
        }.toSet()

        val unlockedCreatures = allCreatures.filter { it.species in unlockedSpecies }
        val fixedCreatures = unlockedCreatures.filter { it.swimDuration == 0 }

        // El nivel visual se deriva del nivel de la categoría, igual que en la app
        val levelByCategory = db.ecosystemStateDao().getAll()
            .associate { it.category to it.currentLevel }

        val creatureLevelBySpecies = entities.mapNotNull { entity ->
            try {
                val species = CreatureSpecies.valueOf(entity.species)
                val spec = allCreatures.firstOrNull { it.species == species } ?: return@mapNotNull null
                val categoryLevel = levelByCategory[spec.category.name] ?: 1
                val visualLevel = (1 + (categoryLevel - entity.unlockedAtLevel) / 2).coerceIn(1, 5)
                species to visualLevel
            } catch (_: Exception) { null }
        }.toMap()

        return WallpaperCreatureData(unlockedCreatures, fixedCreatures, creatureLevelBySpecies)
    }
}

data class WallpaperCreatureData(
    val unlockedCreatures: List<CreatureSpec>,
    val fixedCreatures: List<CreatureSpec>,
    val creatureLevelBySpecies: Map<CreatureSpecies, Int>
)
