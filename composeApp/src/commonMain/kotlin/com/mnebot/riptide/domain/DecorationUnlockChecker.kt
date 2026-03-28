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
    /** Checks all 3 conditions. Returns list of newly unlocked species (may be empty). */
    suspend fun checkAll(): List<CreatureSpecies> = listOfNotNull(
        checkTreasureChest(),
        checkAnchor(),
        checkSunkenShip()
    )

    /**
     * TREASURE_CHEST: 7 consecutive calendar days each with DaySummary.score == 1.0f,
     * no date gaps allowed.
     */
    suspend fun checkTreasureChest(): CreatureSpecies? {
        if (isAlreadyUnlocked(CreatureSpecies.TREASURE_CHEST)) return null
        val latest = daySummaryRepository.getLatestN(7)
        if (latest.size < 7) return null
        val sorted = latest.sortedByDescending { it.date }
        if (sorted.any { it.score < 1.0f }) return null
        // Verify no gaps: each consecutive pair must differ by exactly 1 day
        for (i in 0 until sorted.size - 1) {
            if (sorted[i].date.toEpochDays() - sorted[i + 1].date.toEpochDays() != 1L) return null
        }
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

    /** Returns decoration unlock progress for UI display. */
    suspend fun getProgress(): DecorationProgress {
        val latest = daySummaryRepository.getLatestN(7)
        val streak = countConsecutivePerfectDays(latest)
        val totalCompleted = dayTaskRepository.countCompletedAllTime()
        val wallpaperActive = userPreferencesRepository.isWallpaperActivated()
        return DecorationProgress(streak, totalCompleted, wallpaperActive)
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

    /**
     * Counts how many of the most recent summaries form a consecutive streak of perfect days
     * (score == 1.0f, no date gaps). Summaries need not already be sorted.
     */
    private fun countConsecutivePerfectDays(summaries: List<DaySummary>): Int {
        if (summaries.isEmpty()) return 0
        val sorted = summaries.sortedByDescending { it.date }
        var streak = 0
        for (i in sorted.indices) {
            if (sorted[i].score < 1.0f) break
            if (i > 0 && sorted[i - 1].date.toEpochDays() - sorted[i].date.toEpochDays() != 1L) break
            streak++
        }
        return streak
    }
}
