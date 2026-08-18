package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.DecorationProgress
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.MarineCreatureRepository
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import com.mnebot.riptide.presentation.aquarium.allCreatures
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class DecorationUnlockChecker(
    private val daySummaryRepository: DaySummaryRepository,
    private val dayTaskRepository: DayTaskRepository,
    private val marineCreatureRepository: MarineCreatureRepository,
    private val ecosystemProcessor: EcosystemProcessor,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    /** Checks all 6 decoration conditions. Returns list of newly unlocked species (may be empty). */
    suspend fun checkAll(): List<CreatureSpecies> = listOfNotNull(
        checkTreasureChest(),
        checkAnchor(),
        checkSunkenShip(),
        checkDivingHelmet(),
        checkCoralThrone(),
        checkGoldenTrident()
    )

    /** TREASURE_CHEST: 7 días de racha. */
    suspend fun checkTreasureChest(): CreatureSpecies? {
        if (isAlreadyUnlocked(CreatureSpecies.TREASURE_CHEST)) return null
        if (currentStreak() < 7) return null
        return doUnlock(CreatureSpecies.TREASURE_CHEST)
    }

    /** ANCHOR: 100 or more tasks with status COMPLETED accumulated across all time. */
    suspend fun checkAnchor(): CreatureSpecies? {
        if (isAlreadyUnlocked(CreatureSpecies.ANCHOR)) return null
        if (dayTaskRepository.countCompletedAllTime() < 100) return null
        return doUnlock(CreatureSpecies.ANCHOR)
    }

    /** SUNKEN_SHIP: live wallpaper has been activated at least once. */
    suspend fun checkSunkenShip(): CreatureSpecies? {
        if (isAlreadyUnlocked(CreatureSpecies.SUNKEN_SHIP)) return null
        if (!userPreferencesRepository.isWallpaperActivated()) return null
        return doUnlock(CreatureSpecies.SUNKEN_SHIP)
    }

    /**
     * BIMBA 🐾: easter egg — se desbloquea al completar cualquier tarea cuyo título
     * contenga "premio" o "treat" (sin distinguir mayúsculas/minúsculas).
     * Solo puede desbloquearse una vez; después de eso la llamada es no-op.
     */
    suspend fun checkBimba(taskTitle: String): CreatureSpecies? {
        if (isCompanionAlreadyUnlocked(CreatureSpecies.BIMBA)) return null
        val lower = taskTitle.lowercase()
        if ("premio" !in lower && "treat" !in lower) return null
        return doUnlockCompanion(CreatureSpecies.BIMBA)
    }

    /** DIVING_HELMET: sign in with Google. */
    suspend fun checkDivingHelmet(): CreatureSpecies? {
        if (isAlreadyUnlocked(CreatureSpecies.DIVING_HELMET)) return null
        val user = userPreferencesRepository.getLoggedInUser() ?: return null
        return doUnlock(CreatureSpecies.DIVING_HELMET)
    }

    /** CORAL_THRONE: 14 días de racha. */
    suspend fun checkCoralThrone(): CreatureSpecies? {
        if (isAlreadyUnlocked(CreatureSpecies.CORAL_THRONE)) return null
        if (currentStreak() < 14) return null
        return doUnlock(CreatureSpecies.CORAL_THRONE)
    }

    /** GOLDEN_TRIDENT: unlock every species in any single category. */
    suspend fun checkGoldenTrident(): CreatureSpecies? {
        if (isAlreadyUnlocked(CreatureSpecies.GOLDEN_TRIDENT)) return null
        val lootboxCategories = listOf(
            MarineCategory.FISH, MarineCategory.FLORA, MarineCategory.CRUSTACEAN,
            MarineCategory.MOLLUSK, MarineCategory.PELAGIC, MarineCategory.CEPHALOPOD,
            MarineCategory.REPTILE, MarineCategory.MAMMAL
        )
        val completed = lootboxCategories.any { cat ->
            val totalInCat = allCreatures.count { it.category == cat }
            val unlockedInCat = marineCreatureRepository.getByCategory(cat).size
            unlockedInCat >= totalInCat
        }
        if (!completed) return null
        return doUnlock(CreatureSpecies.GOLDEN_TRIDENT)
    }

    /** Returns decoration unlock progress for UI display. */
    suspend fun getProgress(): DecorationProgress {
        val streak = currentStreak()
        val totalCompleted = dayTaskRepository.countCompletedAllTime()
        val wallpaperActive = userPreferencesRepository.isWallpaperActivated()
        val googleSignedIn = userPreferencesRepository.getLoggedInUser() != null
        val lootboxCategories = listOf(
            MarineCategory.FISH, MarineCategory.FLORA, MarineCategory.CRUSTACEAN,
            MarineCategory.MOLLUSK, MarineCategory.PELAGIC, MarineCategory.CEPHALOPOD,
            MarineCategory.REPTILE, MarineCategory.MAMMAL
        )
        val hasCompletedCat = lootboxCategories.any { cat ->
            val totalInCat = allCreatures.count { it.category == cat }
            val unlockedInCat = marineCreatureRepository.getByCategory(cat).size
            unlockedInCat >= totalInCat
        }
        return DecorationProgress(
            perfectDaysStreak = streak,
            completedTasksTotal = totalCompleted,
            wallpaperActivated = wallpaperActive,
            googleSignedIn = googleSignedIn,
            longestPerfectStreak = streak,
            hasCompletedAnyCategory = hasCompletedCat
        )
    }

    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun isAlreadyUnlocked(species: CreatureSpecies): Boolean =
        marineCreatureRepository.getByCategory(MarineCategory.DECORATION)
            .any { it.species == species }

    private suspend fun isCompanionAlreadyUnlocked(species: CreatureSpecies): Boolean =
        marineCreatureRepository.getByCategory(MarineCategory.COMPANION)
            .any { it.species == species }

    private suspend fun doUnlockCompanion(species: CreatureSpecies): CreatureSpecies {
        ecosystemProcessor.unlockCategory(MarineCategory.COMPANION)
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        marineCreatureRepository.insert(
            MarineCreature(
                id = generateUUID(),
                ecosystemId = MarineCategory.COMPANION.name,
                species = species,
                nickname = null,
                unlockedAtLevel = 1,
                experience = 0,
                creatureLevel = 1,
                unlockedAt = now
            )
        )
        val existing = userPreferencesRepository.getPendingLootboxes()
        userPreferencesRepository.setPendingLootboxes(
            existing + PendingLootbox(
                category = MarineCategory.COMPANION,
                categoryLevel = 0,
                directSpecies = species
            )
        )
        return species
    }

    private suspend fun doUnlock(species: CreatureSpecies): CreatureSpecies {
        // 1. Unlock DECORATION category (idempotent)
        ecosystemProcessor.unlockCategory(MarineCategory.DECORATION)

        // 2. Create the MarineCreature in DB
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        marineCreatureRepository.insert(
            MarineCreature(
                id = generateUUID(),
                ecosystemId = MarineCategory.DECORATION.name,
                species = species,
                nickname = null,
                unlockedAtLevel = 1,
                experience = 0,
                creatureLevel = 1,
                unlockedAt = now
            )
        )

        // 3. Queue PendingLootbox with directSpecies so the celebration UI fires
        val existing = userPreferencesRepository.getPendingLootboxes()
        userPreferencesRepository.setPendingLootboxes(
            existing + PendingLootbox(
                category = MarineCategory.DECORATION,
                categoryLevel = 0,
                directSpecies = species
            )
        )

        return species
    }

    /** La misma racha que ve el usuario, sin criterio propio. */
    private suspend fun currentStreak(): Int {
        val recent = daySummaryRepository.getLatestN(STREAK_LOOKBACK_DAYS)
        val mostRecent = recent.maxByOrNull { it.date }?.date ?: return 0
        return DayStreak.currentFrom(recent, mostRecent)
    }

    private companion object {
        const val STREAK_LOOKBACK_DAYS = 60
    }
}
