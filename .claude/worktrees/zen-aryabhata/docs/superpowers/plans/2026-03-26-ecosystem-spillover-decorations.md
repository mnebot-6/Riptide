# Ecosystem Spillover 80/20 + Decoration Unlock Conditions — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an 80/20 XP redistribution mechanic to balance weak marine categories, and unlock the three DECORATION species via specific in-app conditions.

**Architecture:** The XP spillover is a pure domain change in `EcosystemProcessor` (single file). The decoration system adds a new `DecorationUnlockChecker` domain class, extends `PendingLootbox` with a `directSpecies` field, and wires trigger points into existing entry paths (NightSummaryProcessor, WallpaperService, MainViewModel). UI in `EcosystemScreen` shows per-species condition hints.

**Tech Stack:** Kotlin Multiplatform, Room (DAOs in androidMain), DataStore (androidMain), Compose Multiplatform, kotlinx-coroutines-test (commonTest), kotlin.test.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `domain/EcosystemProcessor.kt` | Modify | Add `findSpilloverTarget()`, split XP 80/20 in `addXpForTask` + `addNightBonus` |
| `domain/model/PendingLootbox.kt` | Modify | Add `directSpecies: CreatureSpecies? = null` |
| `domain/LootboxResolver.kt` | Modify | Short-circuit to `directSpecies` when set |
| `data/repository/UserPreferencesRepositoryImpl.kt` | Modify | Extend lootbox serialization to 3-part format; add `wallpaper_activated` key |
| `domain/repository/UserPreferencesRepository.kt` | Modify | Add `isWallpaperActivated()` + `setWallpaperActivated()` |
| `domain/repository/DayTaskRepository.kt` | Modify | Add `countCompletedAllTime(): Int` |
| `domain/repository/DaySummaryRepository.kt` | Modify | Add `getLatestN(n: Int): List<DaySummary>` |
| `data/local/dao/DayTaskDao.kt` | Modify | Add `countCompleted()` query |
| `data/local/dao/DaySummaryDao.kt` | Modify | Add `getLatestN(n)` query |
| `data/repository/DayTaskRepositoryImpl.kt` | Modify | Implement `countCompletedAllTime()` |
| `data/repository/DaySummaryRepositoryImpl.kt` | Modify | Implement `getLatestN()` |
| `domain/model/DecorationProgress.kt` | Create | Data class for UI progress display |
| `domain/DecorationUnlockChecker.kt` | Create | Checks all 3 conditions, unlocks species, returns progress |
| `domain/NightSummaryProcessor.kt` | Modify | Accept `DecorationUnlockChecker?`, call `checkAll()` at end |
| `androidMain/.../RiptideWallpaperService.kt` | Modify | Call `setWallpaperActivated()` + `checkSunkenShip()` on visibility |
| `presentation/main/MainViewModel.kt` | Modify | Accept `DecorationUnlockChecker?`, call `checkAll()` in init |
| `presentation/aquarium/EcosystemScreen.kt` | Modify | `LockedCreatureCard` shows per-species condition hint + progress |
| `commonTest/.../fakes/FakeEcosystemStateRepository.kt` | Create | In-memory fake for tests |
| `commonTest/.../fakes/FakeMarineCreatureRepository.kt` | Create | In-memory fake for tests |
| `commonTest/.../fakes/FakeUserPreferencesRepository.kt` | Create | In-memory fake for tests |
| `commonTest/.../fakes/FakeDayTaskRepository.kt` | Modify | Add `countCompletedAllTime()` |
| `commonTest/.../fakes/FakeDaySummaryRepository.kt` | Modify | Add `getLatestN()` |
| `commonTest/.../domain/EcosystemProcessorSpilloverTest.kt` | Create | 7 tests for spillover logic |
| `commonTest/.../domain/DecorationUnlockCheckerTest.kt` | Create | 10 tests for decoration conditions |
| `presentation/aquarium/AquariumCreature.kt` | Modify | Update DECORATION comment |

---

## Task 1: Fakes for EcosystemProcessor tests

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeEcosystemStateRepository.kt`
- Create: `composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeMarineCreatureRepository.kt`

- [ ] **Step 1.1: Create `FakeEcosystemStateRepository`**

```kotlin
// composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeEcosystemStateRepository.kt
package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.repository.EcosystemStateRepository

class FakeEcosystemStateRepository : EcosystemStateRepository {
    private val states = mutableMapOf<MarineCategory, EcosystemState>()

    fun seed(state: EcosystemState) { states[state.category] = state }
    fun all(): List<EcosystemState> = states.values.toList()

    override suspend fun getByCategory(category: MarineCategory): EcosystemState? = states[category]
    override suspend fun getAll(): List<EcosystemState> = states.values.toList()
    override suspend fun getUnlocked(): List<EcosystemState> = states.values.filter { it.isUnlocked }
    override suspend fun insert(state: EcosystemState) { states[state.category] = state }
    override suspend fun update(state: EcosystemState) { states[state.category] = state }
}
```

- [ ] **Step 1.2: Create `FakeMarineCreatureRepository`**

```kotlin
// composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeMarineCreatureRepository.kt
package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.domain.repository.MarineCreatureRepository

class FakeMarineCreatureRepository : MarineCreatureRepository {
    private val creatures = mutableListOf<MarineCreature>()

    fun all(): List<MarineCreature> = creatures.toList()

    override suspend fun getByEcosystem(ecosystemId: String): List<MarineCreature> =
        creatures.filter { it.ecosystemId == ecosystemId }

    override suspend fun getByCategory(category: MarineCategory): List<MarineCreature> =
        creatures.filter { it.species.category == category }

    override suspend fun insert(creature: MarineCreature) { creatures.add(creature) }

    override suspend fun update(creature: MarineCreature) {
        val idx = creatures.indexOfFirst { it.id == creature.id }
        if (idx >= 0) creatures[idx] = creature
    }
}
```

- [ ] **Step 1.3: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeEcosystemStateRepository.kt
git add composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeMarineCreatureRepository.kt
git commit -m "test: add FakeEcosystemStateRepository and FakeMarineCreatureRepository"
```

---

## Task 2: XP Spillover 80/20 — tests first

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/EcosystemProcessorSpilloverTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/EcosystemProcessor.kt`

- [ ] **Step 2.1: Write failing tests for spillover**

```kotlin
// composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/EcosystemProcessorSpilloverTest.kt
package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeEcosystemStateRepository
import com.mnebot.riptide.domain.fakes.FakeMarineCreatureRepository
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EcosystemProcessorSpilloverTest {

    private val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    private fun state(
        category: MarineCategory,
        xp: Int = 0,
        level: Int = 1,
        unlocked: Boolean = true
    ) = EcosystemState(
        id = category.name,
        category = category,
        totalExperience = xp,
        currentLevel = level,
        isUnlocked = unlocked,
        lastUpdated = now
    )

    private fun makeProcessor(
        ecoRepo: FakeEcosystemStateRepository,
        creatureRepo: FakeMarineCreatureRepository
    ) = EcosystemProcessor(ecoRepo, creatureRepo)

    // ── Spillover applies when there is a weaker candidate ────────────────────

    @Test
    fun spillover_weakerCandidateExists_20percentGoesToWeakest() = runTest {
        val ecoRepo = FakeEcosystemStateRepository().also {
            it.seed(state(MarineCategory.FISH, xp = 100))       // target (has categories)
            it.seed(state(MarineCategory.FLORA, xp = 0))        // weakest → gets spillover
        }
        val creatureRepo = FakeMarineCreatureRepository()
        val processor = makeProcessor(ecoRepo, creatureRepo)

        // Task with FISH category: 10 XP split → FISH gets 8, FLORA gets 2
        processor.addXpForTask(listOf(MarineCategory.FISH))

        val fishXp = ecoRepo.all().first { it.category == MarineCategory.FISH }.totalExperience
        val floraXp = ecoRepo.all().first { it.category == MarineCategory.FLORA }.totalExperience
        assertEquals(108, fishXp, "FISH should receive 80% of 10 XP (8) + existing 100")
        assertEquals(2, floraXp,  "FLORA should receive 20% of 10 XP (2)")
    }

    // ── No spillover when only 1 category is unlocked ─────────────────────────

    @Test
    fun spillover_onlyOneCategory_noSpillover() = runTest {
        val ecoRepo = FakeEcosystemStateRepository().also {
            it.seed(state(MarineCategory.FISH, xp = 0))
        }
        val creatureRepo = FakeMarineCreatureRepository()
        val processor = makeProcessor(ecoRepo, creatureRepo)

        processor.addXpForTask(listOf(MarineCategory.FISH))

        val fishXp = ecoRepo.all().first { it.category == MarineCategory.FISH }.totalExperience
        assertEquals(10, fishXp, "FISH gets 100% of XP when it is the only category")
    }

    // ── No spillover when weakest is already among targets ────────────────────

    @Test
    fun spillover_weakestIsAlreadyTarget_noExtraSpillover() = runTest {
        val ecoRepo = FakeEcosystemStateRepository().also {
            it.seed(state(MarineCategory.FISH, xp = 0))    // target + weakest
            it.seed(state(MarineCategory.FLORA, xp = 100)) // higher xp
        }
        val creatureRepo = FakeMarineCreatureRepository()
        val processor = makeProcessor(ecoRepo, creatureRepo)

        // FISH is both the target and the weakest — no split
        processor.addXpForTask(listOf(MarineCategory.FISH))

        val fishXp = ecoRepo.all().first { it.category == MarineCategory.FISH }.totalExperience
        val floraXp = ecoRepo.all().first { it.category == MarineCategory.FLORA }.totalExperience
        assertEquals(10, fishXp, "FISH gets 100% since it is both target and weakest")
        assertEquals(100, floraXp, "FLORA is untouched")
    }

    // ── DECORATION never receives spillover ───────────────────────────────────

    @Test
    fun spillover_decorationNeverCandidate() = runTest {
        val ecoRepo = FakeEcosystemStateRepository().also {
            it.seed(state(MarineCategory.FISH, xp = 100))
            it.seed(state(MarineCategory.DECORATION, xp = 0, unlocked = true))
        }
        val creatureRepo = FakeMarineCreatureRepository()
        val processor = makeProcessor(ecoRepo, creatureRepo)

        processor.addXpForTask(listOf(MarineCategory.FISH))

        val decorationXp = ecoRepo.all()
            .first { it.category == MarineCategory.DECORATION }.totalExperience
        assertEquals(0, decorationXp, "DECORATION must never receive spillover XP")
    }

    // ── 80+20 always sums to 100% of XP_PER_TASK ─────────────────────────────

    @Test
    fun spillover_total_equals100percent() = runTest {
        val ecoRepo = FakeEcosystemStateRepository().also {
            it.seed(state(MarineCategory.FISH, xp = 0))
            it.seed(state(MarineCategory.FLORA, xp = 0))
        }
        val creatureRepo = FakeMarineCreatureRepository()
        val processor = makeProcessor(ecoRepo, creatureRepo)

        processor.addXpForTask(listOf(MarineCategory.FISH))

        val states = ecoRepo.all()
        val totalDistributed = states.sumOf { it.totalExperience }
        assertEquals(
            EcosystemLevelCalculator.XP_PER_TASK,
            totalDistributed,
            "Total XP distributed must equal XP_PER_TASK (80 + 20 = 100%)"
        )
    }

    // ── fromSpillover prevents cascading redistribution ───────────────────────

    @Test
    fun spillover_fromSpillover_doesNotTriggerAnotherSpillover() = runTest {
        // Two categories: FISH has 100 XP, FLORA has 0 XP.
        // When FLORA receives the spillover 2 XP, FISH should NOT receive another spillover
        val ecoRepo = FakeEcosystemStateRepository().also {
            it.seed(state(MarineCategory.FISH, xp = 100))
            it.seed(state(MarineCategory.FLORA, xp = 0))
            it.seed(state(MarineCategory.CRUSTACEAN, xp = 50))
        }
        val creatureRepo = FakeMarineCreatureRepository()
        val processor = makeProcessor(ecoRepo, creatureRepo)

        processor.addXpForTask(listOf(MarineCategory.FISH))

        // FISH: 100 + 8 = 108; FLORA: 0 + 2 = 2; CRUSTACEAN: 50 (untouched)
        val fishXp       = ecoRepo.all().first { it.category == MarineCategory.FISH }.totalExperience
        val floraXp      = ecoRepo.all().first { it.category == MarineCategory.FLORA }.totalExperience
        val crustaceanXp = ecoRepo.all().first { it.category == MarineCategory.CRUSTACEAN }.totalExperience
        assertEquals(108, fishXp,       "FISH: 100 + 8")
        assertEquals(2,   floraXp,      "FLORA: 0 + 2 (spillover, no cascade)")
        assertEquals(50,  crustaceanXp, "CRUSTACEAN untouched (no second spillover)")
    }

    // ── Tied weakest: one of the tied candidates receives spillover ───────────

    @Test
    fun spillover_tiedWeakest_oneCandidateChosen() = runTest {
        val ecoRepo = FakeEcosystemStateRepository().also {
            it.seed(state(MarineCategory.FISH, xp = 100))
            it.seed(state(MarineCategory.FLORA, xp = 0))
            it.seed(state(MarineCategory.CRUSTACEAN, xp = 0))
        }
        val creatureRepo = FakeMarineCreatureRepository()
        val processor = makeProcessor(ecoRepo, creatureRepo)

        processor.addXpForTask(listOf(MarineCategory.FISH))

        val floraXp      = ecoRepo.all().first { it.category == MarineCategory.FLORA }.totalExperience
        val crustaceanXp = ecoRepo.all().first { it.category == MarineCategory.CRUSTACEAN }.totalExperience
        val spilloverReceived = floraXp + crustaceanXp
        assertEquals(2, spilloverReceived, "Exactly 2 XP (20%) goes to one of the tied weakest")
        assertTrue(floraXp == 0 || floraXp == 2, "Either FLORA or CRUSTACEAN receives the spillover")
        assertTrue(crustaceanXp == 0 || crustaceanXp == 2)
    }
}
```

- [ ] **Step 2.2: Run tests — expect compile errors (spillover not implemented)**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.EcosystemProcessorSpilloverTest" 2>&1 | tail -30
```

Expected: compilation failure — `EcosystemProcessor.addXpForTask` exists but doesn't have spillover logic yet.

- [ ] **Step 2.3: Implement spillover in `EcosystemProcessor`**

Replace `EcosystemProcessor.kt` entirely with the following (adds `fromSpillover` param to `addXp`, adds `findSpilloverTarget`, modifies `addXpForTask` and `addNightBonus`):

```kotlin
// composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/EcosystemProcessor.kt
package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.EcosystemState
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.repository.EcosystemStateRepository
import com.mnebot.riptide.domain.repository.MarineCreatureRepository
import com.mnebot.riptide.presentation.aquarium.CATEGORY_UNLOCK_LEVELS
import com.mnebot.riptide.presentation.aquarium.allCreatures
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Clock

class EcosystemProcessor(
    private val ecosystemStateRepository: EcosystemStateRepository,
    private val marineCreatureRepository: MarineCreatureRepository
) {
    suspend fun addXpForTask(categories: List<MarineCategory>): List<PendingLootbox> {
        val targets = if (categories.isEmpty()) {
            ecosystemStateRepository.getUnlocked()
                .map { it.category }
                .filter { it != MarineCategory.DECORATION }
        } else {
            categories
        }
        if (targets.isEmpty()) return emptyList()

        val xpEach = EcosystemLevelCalculator.XP_PER_TASK / targets.size
        val spilloverTarget = findSpilloverTarget(excludes = targets)

        return targets.flatMap { target ->
            if (spilloverTarget != null) {
                val actualXp    = (xpEach * 0.80).toInt()
                val spilloverXp = xpEach - actualXp
                val main = addXp(target, actualXp)
                val spill = addXp(spilloverTarget, spilloverXp, fromSpillover = true)
                main + spill
            } else {
                addXp(target, xpEach)
            }
        }.distinctBy { it.category to it.categoryLevel }  // deduplicate if spilloverTarget == target
    }

    suspend fun addNightBonus(
        score: Float,
        bestStreak: Int,
        categories: List<MarineCategory>
    ): List<PendingLootbox> {
        val targets = if (categories.isEmpty()) {
            ecosystemStateRepository.getUnlocked()
                .map { it.category }
                .filter { it != MarineCategory.DECORATION }
        } else {
            categories
        }
        if (targets.isEmpty()) return emptyList()

        val bonus = EcosystemLevelCalculator.nightBonus(score, bestStreak)
        if (bonus == 0) return emptyList()

        val xpEach = bonus / targets.size
        val spilloverTarget = findSpilloverTarget(excludes = targets)

        return targets.flatMap { target ->
            if (spilloverTarget != null) {
                val actualXp    = (xpEach * 0.80).toInt()
                val spilloverXp = xpEach - actualXp
                val main = addXp(target, actualXp)
                val spill = addXp(spilloverTarget, spilloverXp, fromSpillover = true)
                main + spill
            } else {
                addXp(target, xpEach)
            }
        }.distinctBy { it.category to it.categoryLevel }
    }

    /**
     * Returns the unlocked non-DECORATION category with least totalExperience,
     * excluding any categories already in [excludes].
     * Returns null when no valid candidate exists.
     * On tie, picks randomly among tied candidates.
     */
    private suspend fun findSpilloverTarget(excludes: List<MarineCategory>): MarineCategory? {
        val candidates = ecosystemStateRepository.getUnlocked()
            .filter { it.category != MarineCategory.DECORATION }
            .filter { it.category !in excludes }
        if (candidates.isEmpty()) return null
        val minXp = candidates.minOf { it.totalExperience }
        val tied  = candidates.filter { it.totalExperience == minXp }
        return tied[Random.nextInt(tied.size)].category
    }

    private suspend fun addXp(
        category: MarineCategory,
        xp: Int,
        fromOverflow: Boolean = false,
        fromSpillover: Boolean = false
    ): List<PendingLootbox> {
        if (xp <= 0) return emptyList()

        // ── XP overflow: si todas las especies de la categoría están desbloqueadas,
        //    50% del XP va a la categoría con menor nivel (catch-up) ──
        val allSpecsInCategory = allCreatures.filter { it.category == category }
        val unlockedCreatures = marineCreatureRepository.getByCategory(category)
        val allUnlocked = unlockedCreatures.size >= allSpecsInCategory.size

        val actualXp: Int
        var overflowLootboxes: List<PendingLootbox> = emptyList()

        // Neither overflow nor spillover propagate further when already redistributed
        val isRedistributed = fromOverflow || fromSpillover
        if (allUnlocked && !isRedistributed) {
            actualXp = xp / 2
            val overflow = xp - actualXp
            if (overflow > 0) {
                overflowLootboxes = redistributeOverflow(category, overflow)
            }
        } else {
            actualXp = xp
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = ecosystemStateRepository.getByCategory(category)

        val oldLevel = if (existing == null) 1 else existing.currentLevel
        val newXp    = (existing?.totalExperience ?: 0) + actualXp
        val newLevel = EcosystemLevelCalculator.levelForXp(newXp)

        if (existing == null) {
            ecosystemStateRepository.insert(
                EcosystemState(
                    id = generateUUID(),
                    category = category,
                    totalExperience = newXp,
                    currentLevel = newLevel,
                    isUnlocked = category.isUnlockedByDefault,
                    lastUpdated = now
                )
            )
        } else {
            ecosystemStateRepository.update(
                existing.copy(
                    totalExperience = newXp,
                    currentLevel = newLevel,
                    lastUpdated = now
                )
            )
        }

        // Repartir XP entre criaturas desbloqueadas de esta categoría
        val creatures = marineCreatureRepository.getByCategory(category)
        if (creatures.isNotEmpty()) {
            val xpPerCreature = actualXp / creatures.size
            if (xpPerCreature > 0) {
                creatures.forEach { creature ->
                    val newCreatureXp    = creature.experience + xpPerCreature
                    val newCreatureLevel = EcosystemLevelCalculator.levelForXp(newCreatureXp)
                    marineCreatureRepository.update(
                        creature.copy(
                            experience = newCreatureXp,
                            creatureLevel = newCreatureLevel
                        )
                    )
                }
            }
        }

        // ── Detección de lootbox ──
        val unlockLevels = CATEGORY_UNLOCK_LEVELS[category] ?: emptyList()
        val triggeredLevels = unlockLevels.filter { it in (oldLevel + 1)..newLevel }

        val unlockedCount  = marineCreatureRepository.getByCategory(category).size
        val totalSpecies   = allSpecsInCategory.size
        val availableSlots = (totalSpecies - unlockedCount).coerceAtLeast(0)
        val lootboxes = triggeredLevels.take(availableSlots).map { level ->
            PendingLootbox(category = category, categoryLevel = level)
        }

        return lootboxes + overflowLootboxes
    }

    private suspend fun redistributeOverflow(
        sourceCategory: MarineCategory,
        overflowXp: Int
    ): List<PendingLootbox> {
        val allStates = ecosystemStateRepository.getUnlocked()
        val candidates = allStates
            .filter { it.category != MarineCategory.DECORATION }
            .filter { it.category != sourceCategory }
            .filter { state ->
                val specsInCat    = allCreatures.count { it.category == state.category }
                val unlockedInCat = marineCreatureRepository.getByCategory(state.category).size
                unlockedInCat < specsInCat
            }
        if (candidates.isEmpty()) return emptyList()
        val minLevel = candidates.minOf { it.currentLevel }
        val tied     = candidates.filter { it.currentLevel == minLevel }
        val target   = tied[Random.nextInt(tied.size)]
        return addXp(target.category, overflowXp, fromOverflow = true)
    }

    suspend fun unlockCategory(category: MarineCategory) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val existing = ecosystemStateRepository.getByCategory(category)
        if (existing == null) {
            ecosystemStateRepository.insert(
                EcosystemState(
                    id = generateUUID(),
                    category = category,
                    totalExperience = 0,
                    currentLevel = 1,
                    isUnlocked = true,
                    lastUpdated = now
                )
            )
        } else {
            ecosystemStateRepository.update(existing.copy(isUnlocked = true))
        }
    }
}
```

- [ ] **Step 2.4: Run spillover tests — expect all PASS**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.EcosystemProcessorSpilloverTest" 2>&1 | tail -20
```

Expected: `7 tests completed, 0 failed`

- [ ] **Step 2.5: Run full test suite to check no regressions**

```bash
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -20
```

Expected: all previously passing tests still pass.

- [ ] **Step 2.6: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/EcosystemProcessorSpilloverTest.kt
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/EcosystemProcessor.kt
git commit -m "feat: add XP spillover 80/20 — weakest unlocked category receives 20% of each task XP"
```

---

## Task 3: PendingLootbox.directSpecies + LootboxResolver

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/model/PendingLootbox.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/LootboxResolver.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/repository/UserPreferencesRepositoryImpl.kt`

- [ ] **Step 3.1: Extend `PendingLootbox`**

```kotlin
// composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/model/PendingLootbox.kt
package com.mnebot.riptide.domain.model

data class PendingLootbox(
    val category: MarineCategory,
    val categoryLevel: Int,
    val directSpecies: CreatureSpecies? = null
)
```

- [ ] **Step 3.2: Update `LootboxResolver` to short-circuit when `directSpecies != null`**

```kotlin
// composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/LootboxResolver.kt
package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.repository.MarineCreatureRepository
import com.mnebot.riptide.presentation.aquarium.CreatureSpec
import com.mnebot.riptide.presentation.aquarium.allCreatures
import kotlin.random.Random

class LootboxResolver(
    private val marineCreatureRepository: MarineCreatureRepository
) {
    /**
     * Resuelve una lootbox:
     * - Si [PendingLootbox.directSpecies] != null → devuelve esa especie directamente.
     * - Si null → elige aleatoriamente (ponderado por rareza) entre las no desbloqueadas.
     */
    suspend fun resolve(lootbox: PendingLootbox): CreatureSpec {
        // Direct unlock (e.g. decoration conditions)
        if (lootbox.directSpecies != null) {
            return allCreatures.first { it.species == lootbox.directSpecies }
        }

        val allSpecs = allCreatures.filter { it.category == lootbox.category }
        val unlockedSpecies = marineCreatureRepository.getByCategory(lootbox.category)
            .map { it.species }
            .toSet()
        val candidates = allSpecs.filter { it.species !in unlockedSpecies }

        if (candidates.isEmpty()) {
            return allSpecs.first()
        }
        return weightedRandom(candidates)
    }

    private fun weightedRandom(candidates: List<CreatureSpec>): CreatureSpec {
        val totalWeight = candidates.sumOf { it.rarity.weight.toDouble() }
        var roll = Random.nextDouble() * totalWeight
        for (spec in candidates) {
            roll -= spec.rarity.weight
            if (roll <= 0.0) return spec
        }
        return candidates.last()
    }
}
```

- [ ] **Step 3.3: Update `UserPreferencesRepositoryImpl` serialization**

The current format is `"FISH:4|CRUSTACEAN:6"`. The new format adds an optional third part: `"DECORATION:0:TREASURE_CHEST"`. Both 2-part (legacy) and 3-part (new) must be parsed.

Replace only the `getPendingLootboxes` and `setPendingLootboxes` methods:

```kotlin
// In UserPreferencesRepositoryImpl.kt — replace getPendingLootboxes and setPendingLootboxes

// Formato: "FISH:4" (legacy) o "DECORATION:0:TREASURE_CHEST" (con directSpecies)
override suspend fun getPendingLootboxes(): List<PendingLootbox> {
    val raw = context.dataStore.data.first()[KEY_PENDING_LOOTBOXES] ?: return emptyList()
    if (raw.isBlank()) return emptyList()
    return raw.split(SEPARATOR).mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size < 2) return@mapNotNull null
        val category = runCatching { MarineCategory.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
        val level    = parts[1].toIntOrNull() ?: return@mapNotNull null
        val direct   = if (parts.size >= 3)
            runCatching { com.mnebot.riptide.domain.model.CreatureSpecies.valueOf(parts[2]) }.getOrNull()
        else null
        PendingLootbox(category, level, direct)
    }
}

override suspend fun setPendingLootboxes(lootboxes: List<PendingLootbox>) {
    context.dataStore.edit { prefs ->
        prefs[KEY_PENDING_LOOTBOXES] = lootboxes.joinToString(SEPARATOR) {
            if (it.directSpecies != null)
                "${it.category.name}:${it.categoryLevel}:${it.directSpecies.name}"
            else
                "${it.category.name}:${it.categoryLevel}"
        }
    }
}
```

- [ ] **Step 3.4: Verify compilation**

```bash
./gradlew :composeApp:compileDebugKotlin 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL (no errors)

- [ ] **Step 3.5: Run full test suite**

```bash
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -10
```

Expected: all tests still pass.

- [ ] **Step 3.6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/model/PendingLootbox.kt
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/LootboxResolver.kt
git add composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/repository/UserPreferencesRepositoryImpl.kt
git commit -m "feat: extend PendingLootbox with directSpecies for decoration unlocks"
```

---

## Task 4: Repository extensions (DayTask count + DaySummary latest N + wallpaper flag)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/repository/DayTaskRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/repository/DaySummaryRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/repository/UserPreferencesRepository.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/local/dao/DayTaskDao.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/local/dao/DaySummaryDao.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/repository/DayTaskRepositoryImpl.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/repository/DaySummaryRepositoryImpl.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/repository/UserPreferencesRepositoryImpl.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeDayTaskRepository.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeDaySummaryRepository.kt`

- [ ] **Step 4.1: Add `countCompletedAllTime()` to `DayTaskRepository` interface**

```kotlin
// Add to DayTaskRepository.kt interface body
suspend fun countCompletedAllTime(): Int
```

- [ ] **Step 4.2: Add query to `DayTaskDao`**

```kotlin
// Add to DayTaskDao.kt
@Query("SELECT COUNT(*) FROM day_tasks WHERE status = 'COMPLETED'")
suspend fun countCompleted(): Int
```

- [ ] **Step 4.3: Implement in `DayTaskRepositoryImpl`**

```kotlin
// Add to DayTaskRepositoryImpl.kt
override suspend fun countCompletedAllTime(): Int = dao.countCompleted()
```

- [ ] **Step 4.4: Implement in `FakeDayTaskRepository`**

```kotlin
// Add to FakeDayTaskRepository.kt
override suspend fun countCompletedAllTime(): Int =
    tasks.count { it.status == com.mnebot.riptide.domain.model.TaskStatus.COMPLETED }
```

- [ ] **Step 4.5: Add `getLatestN()` to `DaySummaryRepository` interface**

```kotlin
// Add to DaySummaryRepository.kt interface body
suspend fun getLatestN(n: Int): List<DaySummary>
```

- [ ] **Step 4.6: Add query to `DaySummaryDao`**

First locate the DAO file:
```bash
find composeApp/src/androidMain -name "DaySummaryDao.kt"
```

Then add:
```kotlin
// Add to DaySummaryDao.kt
@Query("SELECT * FROM day_summaries ORDER BY date DESC LIMIT :n")
suspend fun getLatestN(n: Int): List<DaySummaryEntity>
```

- [ ] **Step 4.7: Implement in `DaySummaryRepositoryImpl`**

```kotlin
// Add to DaySummaryRepositoryImpl.kt
override suspend fun getLatestN(n: Int): List<DaySummary> =
    dao.getLatestN(n).map { it.toDomain() }
```

- [ ] **Step 4.8: Implement in `FakeDaySummaryRepository`**

```kotlin
// Add to FakeDaySummaryRepository.kt
override suspend fun getLatestN(n: Int): List<DaySummary> =
    summaries.values.sortedByDescending { it.date }.take(n)
```

- [ ] **Step 4.9: Add wallpaper keys to `UserPreferencesRepository` interface**

```kotlin
// Add to UserPreferencesRepository.kt interface body
suspend fun isWallpaperActivated(): Boolean
suspend fun setWallpaperActivated()
```

- [ ] **Step 4.10: Implement in `UserPreferencesRepositoryImpl`**

Add constant and two methods:
```kotlin
// In companion object of UserPreferencesRepositoryImpl:
private val KEY_WALLPAPER_ACTIVATED = booleanPreferencesKey("wallpaper_activated")

// New methods:
override suspend fun isWallpaperActivated(): Boolean =
    context.dataStore.data.first()[KEY_WALLPAPER_ACTIVATED] ?: false

override suspend fun setWallpaperActivated() {
    context.dataStore.edit { prefs -> prefs[KEY_WALLPAPER_ACTIVATED] = true }
}
```

- [ ] **Step 4.11: Verify compilation**

```bash
./gradlew :composeApp:compileDebugKotlin 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4.12: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/repository/DayTaskRepository.kt
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/repository/DaySummaryRepository.kt
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/repository/UserPreferencesRepository.kt
git add composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/local/dao/DayTaskDao.kt
git add composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/local/dao/DaySummaryDao.kt
git add composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/repository/DayTaskRepositoryImpl.kt
git add composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/repository/DaySummaryRepositoryImpl.kt
git add composeApp/src/androidMain/kotlin/com/mnebot/riptide/data/repository/UserPreferencesRepositoryImpl.kt
git add composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeDayTaskRepository.kt
git add composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeDaySummaryRepository.kt
git commit -m "feat: add countCompletedAllTime, getLatestN and wallpaper_activated to repositories"
```

---

## Task 5: DecorationProgress + DecorationUnlockChecker — tests first

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/model/DecorationProgress.kt`
- Create: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/DecorationUnlockChecker.kt`
- Create: `composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeUserPreferencesRepository.kt`
- Create: `composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/DecorationUnlockCheckerTest.kt`

- [ ] **Step 5.1: Create `DecorationProgress`**

```kotlin
// composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/model/DecorationProgress.kt
package com.mnebot.riptide.domain.model

data class DecorationProgress(
    val perfectDaysStreak: Int,       // días consecutivos actuales con score 1.0f
    val completedTasksTotal: Int,     // acumulado histórico de tareas COMPLETED
    val wallpaperActivated: Boolean   // true si el live wallpaper fue activado alguna vez
)
```

- [ ] **Step 5.2: Create `FakeUserPreferencesRepository`**

```kotlin
// composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeUserPreferencesRepository.kt
package com.mnebot.riptide.domain.fakes

import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private var pendingLootboxes = mutableListOf<PendingLootbox>()
    private var wallpaperActivated = false

    override fun getNightSummaryTime(): Flow<LocalTime> = flowOf(LocalTime(23, 30))
    override suspend fun setNightSummaryTime(time: LocalTime) {}
    override suspend fun getPendingUnlocks(): List<String> = emptyList()
    override suspend fun setPendingUnlocks(emojis: List<String>) {}
    override suspend fun getPendingLootboxes(): List<PendingLootbox> = pendingLootboxes.toList()
    override suspend fun setPendingLootboxes(lootboxes: List<PendingLootbox>) {
        pendingLootboxes = lootboxes.toMutableList()
    }
    override suspend fun getLastDismissedSummaryDate(): LocalDate? = null
    override suspend fun setLastDismissedSummaryDate(date: LocalDate) {}
    override fun getMorningReminderTime(): Flow<LocalTime?> = flowOf(null)
    override suspend fun setMorningReminderTime(time: LocalTime?) {}
    override fun hasCompletedOnboarding(): Flow<Boolean> = flowOf(true)
    override suspend fun setOnboardingCompleted() {}
    override suspend fun isWallpaperActivated(): Boolean = wallpaperActivated
    override suspend fun setWallpaperActivated() { wallpaperActivated = true }

    fun activateWallpaper() { wallpaperActivated = true }
}
```

- [ ] **Step 5.3: Write failing tests for `DecorationUnlockChecker`**

```kotlin
// composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/DecorationUnlockCheckerTest.kt
package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeDaySummaryRepository
import com.mnebot.riptide.domain.fakes.FakeDayTaskRepository
import com.mnebot.riptide.domain.fakes.FakeEcosystemStateRepository
import com.mnebot.riptide.domain.fakes.FakeMarineCreatureRepository
import com.mnebot.riptide.domain.fakes.FakeUserPreferencesRepository
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.DaySummary
import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.TaskSchedule
import com.mnebot.riptide.domain.model.TaskStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecorationUnlockCheckerTest {

    private val baseDate = LocalDate(2026, 3, 26)

    private fun summary(date: LocalDate, score: Float) = DaySummary(
        id = date.toString(), date = date, score = score,
        tasksTotal = 2, tasksCompleted = if (score == 1f) 2 else 1,
        streakDay = 0, feedbackMessage = ""
    )

    private fun completedTask(id: String) = DayTask(
        id = id, blockId = null, title = "T$id",
        schedule = TaskSchedule.OneTime(baseDate, null),
        status = TaskStatus.COMPLETED,
        completedAt = null, postponedTo = null, sourceTaskId = null
    )

    private fun makeChecker(
        summaryRepo: FakeDaySummaryRepository = FakeDaySummaryRepository(),
        taskRepo: FakeDayTaskRepository = FakeDayTaskRepository(),
        creatureRepo: FakeMarineCreatureRepository = FakeMarineCreatureRepository(),
        prefsRepo: FakeUserPreferencesRepository = FakeUserPreferencesRepository()
    ): DecorationUnlockChecker {
        val ecoRepo = FakeEcosystemStateRepository()
        val ecoProcessor = EcosystemProcessor(ecoRepo, creatureRepo)
        return DecorationUnlockChecker(summaryRepo, taskRepo, creatureRepo, ecoProcessor, prefsRepo)
    }

    // ── TREASURE_CHEST: 7 consecutive perfect days ────────────────────────────

    @Test
    fun treasureChest_sevenPerfectConsecutiveDays_unlocks() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        repeat(7) { i -> summaryRepo.insert(summary(baseDate.minus(i, kotlinx.datetime.DateTimeUnit.DAY), 1f)) }

        val checker = makeChecker(summaryRepo = summaryRepo)
        val result = checker.checkTreasureChest()

        assertEquals(CreatureSpecies.TREASURE_CHEST, result)
    }

    @Test
    fun treasureChest_sixPerfectDays_doesNotUnlock() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        repeat(6) { i -> summaryRepo.insert(summary(baseDate.minus(i, kotlinx.datetime.DateTimeUnit.DAY), 1f)) }

        val checker = makeChecker(summaryRepo = summaryRepo)
        val result = checker.checkTreasureChest()

        assertNull(result, "6 perfect days should not unlock TREASURE_CHEST")
    }

    @Test
    fun treasureChest_sevenDaysWithGap_doesNotUnlock() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        // Days: today, yesterday, 2 ago, 4 ago (gap on day 3!), 5, 6, 7 ago
        listOf(0, 1, 2, 4, 5, 6, 7).forEach { i ->
            summaryRepo.insert(summary(baseDate.minus(i, kotlinx.datetime.DateTimeUnit.DAY), 1f))
        }

        val checker = makeChecker(summaryRepo = summaryRepo)
        val result = checker.checkTreasureChest()

        assertNull(result, "Gap in 7-day window should not unlock TREASURE_CHEST")
    }

    @Test
    fun treasureChest_alreadyUnlocked_idempotent() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        repeat(7) { i -> summaryRepo.insert(summary(baseDate.minus(i, kotlinx.datetime.DateTimeUnit.DAY), 1f)) }

        val creatureRepo = FakeMarineCreatureRepository()
        // Pre-seed: TREASURE_CHEST already unlocked
        creatureRepo.insert(com.mnebot.riptide.domain.model.MarineCreature(
            id = "existing", ecosystemId = "eco",
            species = CreatureSpecies.TREASURE_CHEST, nickname = null,
            unlockedAtLevel = 1, experience = 0, creatureLevel = 1,
            unlockedAt = LocalDateTime(2026, 1, 1, 0, 0)
        ))

        val checker = makeChecker(summaryRepo = summaryRepo, creatureRepo = creatureRepo)
        val result = checker.checkTreasureChest()

        assertNull(result, "Already unlocked TREASURE_CHEST must return null (idempotent)")
    }

    // ── ANCHOR: 100 total completed tasks ─────────────────────────────────────

    @Test
    fun anchor_100CompletedTasks_unlocks() = runTest {
        val taskRepo = FakeDayTaskRepository()
        repeat(100) { i -> taskRepo.addTask(completedTask(i.toString())) }

        val checker = makeChecker(taskRepo = taskRepo)
        val result = checker.checkAnchor()

        assertEquals(CreatureSpecies.ANCHOR, result)
    }

    @Test
    fun anchor_99Tasks_doesNotUnlock() = runTest {
        val taskRepo = FakeDayTaskRepository()
        repeat(99) { i -> taskRepo.addTask(completedTask(i.toString())) }

        val checker = makeChecker(taskRepo = taskRepo)
        val result = checker.checkAnchor()

        assertNull(result, "99 tasks should not unlock ANCHOR")
    }

    @Test
    fun anchor_alreadyUnlocked_idempotent() = runTest {
        val taskRepo = FakeDayTaskRepository()
        repeat(100) { i -> taskRepo.addTask(completedTask(i.toString())) }

        val creatureRepo = FakeMarineCreatureRepository()
        creatureRepo.insert(com.mnebot.riptide.domain.model.MarineCreature(
            id = "anchor", ecosystemId = "eco",
            species = CreatureSpecies.ANCHOR, nickname = null,
            unlockedAtLevel = 1, experience = 0, creatureLevel = 1,
            unlockedAt = LocalDateTime(2026, 1, 1, 0, 0)
        ))

        val checker = makeChecker(taskRepo = taskRepo, creatureRepo = creatureRepo)
        val result = checker.checkAnchor()

        assertNull(result, "Already unlocked ANCHOR must return null")
    }

    // ── SUNKEN_SHIP: wallpaper activated ──────────────────────────────────────

    @Test
    fun sunkenShip_wallpaperActivated_unlocks() = runTest {
        val prefsRepo = FakeUserPreferencesRepository().also { it.activateWallpaper() }

        val checker = makeChecker(prefsRepo = prefsRepo)
        val result = checker.checkSunkenShip()

        assertEquals(CreatureSpecies.SUNKEN_SHIP, result)
    }

    @Test
    fun sunkenShip_wallpaperNotActivated_doesNotUnlock() = runTest {
        val checker = makeChecker() // wallpaperActivated = false

        val result = checker.checkSunkenShip()

        assertNull(result, "Wallpaper not activated should not unlock SUNKEN_SHIP")
    }

    // ── getProgress ───────────────────────────────────────────────────────────

    @Test
    fun getProgress_returnsCorrectCounts() = runTest {
        val summaryRepo = FakeDaySummaryRepository()
        repeat(3) { i -> summaryRepo.insert(summary(baseDate.minus(i, kotlinx.datetime.DateTimeUnit.DAY), 1f)) }
        val taskRepo = FakeDayTaskRepository()
        repeat(42) { i -> taskRepo.addTask(completedTask(i.toString())) }
        val prefsRepo = FakeUserPreferencesRepository().also { it.activateWallpaper() }

        val checker = makeChecker(summaryRepo = summaryRepo, taskRepo = taskRepo, prefsRepo = prefsRepo)
        val progress = checker.getProgress()

        assertEquals(3, progress.perfectDaysStreak,    "Should count 3 consecutive perfect days")
        assertEquals(42, progress.completedTasksTotal, "Should count 42 completed tasks")
        assertTrue(progress.wallpaperActivated,        "Wallpaper should be marked activated")
    }
}
```

- [ ] **Step 5.4: Run tests — expect compile errors (DecorationUnlockChecker doesn't exist yet)**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.DecorationUnlockCheckerTest" 2>&1 | tail -15
```

Expected: compilation failure.

- [ ] **Step 5.5: Create `DecorationUnlockChecker`**

```kotlin
// composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/DecorationUnlockChecker.kt
package com.mnebot.riptide.domain

import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.domain.model.CreatureSpecies
import com.mnebot.riptide.domain.model.DecorationProgress
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.MarineCreature
import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.repository.DaySummaryRepository
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.MarineCreatureRepository
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
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
     * no gaps between dates.
     */
    suspend fun checkTreasureChest(): CreatureSpecies? {
        if (isAlreadyUnlocked(CreatureSpecies.TREASURE_CHEST)) return null
        val latest = daySummaryRepository.getLatestN(7)
        if (latest.size < 7) return null
        val sorted = latest.sortedByDescending { it.date }
        // All must be perfect
        if (sorted.any { it.score < 1.0f }) return null
        // Dates must be consecutive (no gaps)
        val dates = sorted.map { it.date }
        for (i in 0 until dates.size - 1) {
            if (dates[i].toEpochDays() - dates[i + 1].toEpochDays() != 1) return null
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
     * Counts how many of the provided summaries (sorted desc) form a consecutive
     * streak of perfect days (score == 1.0f, no date gaps).
     */
    private fun countConsecutivePerfectDays(summaries: List<com.mnebot.riptide.domain.model.DaySummary>): Int {
        if (summaries.isEmpty()) return 0
        val sorted = summaries.sortedByDescending { it.date }
        var streak = 0
        for (i in sorted.indices) {
            if (sorted[i].score < 1.0f) break
            if (i > 0 && sorted[i - 1].date.toEpochDays() - sorted[i].date.toEpochDays() != 1) break
            streak++
        }
        return streak
    }
}
```

- [ ] **Step 5.6: Run decoration tests — expect all PASS**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.DecorationUnlockCheckerTest" 2>&1 | tail -20
```

Expected: `10 tests completed, 0 failed`

- [ ] **Step 5.7: Run full test suite**

```bash
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -10
```

Expected: all tests pass.

- [ ] **Step 5.8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/model/DecorationProgress.kt
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/DecorationUnlockChecker.kt
git add composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/fakes/FakeUserPreferencesRepository.kt
git add composeApp/src/commonTest/kotlin/com/mnebot/riptide/domain/DecorationUnlockCheckerTest.kt
git commit -m "feat: add DecorationUnlockChecker with conditions for TREASURE_CHEST, ANCHOR, SUNKEN_SHIP"
```

---

## Task 6: Wire trigger points

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/NightSummaryProcessor.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/mnebot/riptide/wallpaper/RiptideWallpaperService.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt`

- [ ] **Step 6.1: Add `DecorationUnlockChecker?` to `NightSummaryProcessor` and call `checkAll()` at end of `processDay()`**

In `NightSummaryProcessor.kt`, add the parameter to the constructor and call at the end of `processDay()`:

```kotlin
// Add to constructor parameters:
private val decorationUnlockChecker: DecorationUnlockChecker? = null,

// At the end of processDay(), after the existing newLootboxes block:
val decorationUnlocks = decorationUnlockChecker?.checkAll() ?: emptyList()
// decorationUnlocks are already persisted inside checkAll() via setPendingLootboxes,
// no further action needed here.
```

The full constructor signature becomes:
```kotlin
class NightSummaryProcessor(
    private val dayTaskRepository: DayTaskRepository,
    private val daySummaryRepository: DaySummaryRepository,
    private val blockStreakProcessor: BlockStreakProcessor? = null,
    private val ecosystemProcessor: EcosystemProcessor? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val decorationUnlockChecker: DecorationUnlockChecker? = null,
)
```

And at the very end of `processDay()` (after the `newLootboxes` block):
```kotlin
decorationUnlockChecker?.checkAll()
```

- [ ] **Step 6.2: Add wallpaper trigger in `RiptideWallpaperService`**

In `RiptideWallpaperService.kt`, inside the `Engine` inner class, find `onVisibilityChanged` and add:

```kotlin
override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible) {
        // existing visibility-on code (frame scheduling, data refresh) stays here

        // New: mark wallpaper as activated and check SUNKEN_SHIP unlock
        serviceScope.launch {
            userPreferencesRepository.setWallpaperActivated()
            decorationUnlockChecker?.checkSunkenShip()
        }
    }
    // existing visibility-off code stays here
}
```

`userPreferencesRepository` and `decorationUnlockChecker` must be passed into or accessible by the `Engine`. The service already has access to the Room DB and UserPreferences via the Application's dependency graph — follow the same pattern used for `WallpaperDataProvider`. Inject `DecorationUnlockChecker` at the service level and pass it to the Engine.

- [ ] **Step 6.3: Add `DecorationUnlockChecker?` to `MainViewModel` and call `checkAll()` in `init`**

```kotlin
// Add to MainViewModel constructor (with default null, after taskReminderScheduler):
private val decorationUnlockChecker: DecorationUnlockChecker? = null,

// Add to the viewModelScope.launch block in init:
decorationUnlockChecker?.checkAll()
```

The full `init` block becomes:
```kotlin
init {
    viewModelScope.launch {
        recurringTaskGenerator.generateUpTo(currentDate(), daysAhead = 7)
        loadDay(_uiState.value.selectedDate)
        loadPendingLootboxIfAny()
        decorationUnlockChecker?.checkAll()
    }
}
```

- [ ] **Step 6.4: Verify compilation**

```bash
./gradlew :composeApp:compileDebugKotlin 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6.5: Run full test suite**

```bash
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -10
```

Expected: all tests pass (NightSummaryProcessorTest still pass since new param is nullable with default null).

- [ ] **Step 6.6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/domain/NightSummaryProcessor.kt
git add composeApp/src/androidMain/kotlin/com/mnebot/riptide/wallpaper/RiptideWallpaperService.kt
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/main/MainViewModel.kt
git commit -m "feat: wire DecorationUnlockChecker into NightSummaryProcessor, WallpaperService and MainViewModel"
```

---

## Task 7: UI — per-species condition hints in EcosystemScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/aquarium/EcosystemScreen.kt`
- Modify: `composeApp/src/androidMain/res/values/strings.xml`
- Modify: `composeApp/src/androidMain/res/values-es/strings.xml`
- Modify: `composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/aquarium/AquariumCreature.kt`

- [ ] **Step 7.1: Add new strings for decoration hints**

In `values/strings.xml` add:
```xml
<string name="decoration_hint_treasure_chest">7 perfect days in a row (%1$d/7)</string>
<string name="decoration_hint_anchor">Complete 100 tasks (%1$d/100)</string>
<string name="decoration_hint_sunken_ship">Activate Riptide wallpaper</string>
```

In `values-es/strings.xml` add:
```xml
<string name="decoration_hint_treasure_chest">7 días perfectos seguidos (%1$d/7)</string>
<string name="decoration_hint_anchor">Completa 100 tareas (%1$d/100)</string>
<string name="decoration_hint_sunken_ship">Activa el wallpaper de Riptide</string>
```

- [ ] **Step 7.2: Update `EcosystemScreen` signature to accept `DecorationProgress`**

Change the composable signature:
```kotlin
@Composable
fun EcosystemScreen(
    ecosystemByCategory: Map<MarineCategory, EcosystemState>,
    creaturesData: List<MarineCreature>,
    decorationProgress: com.mnebot.riptide.domain.model.DecorationProgress =
        com.mnebot.riptide.domain.model.DecorationProgress(0, 0, false),
    onCreatureNicknameChanged: (String, String) -> Unit,
    onNavigateBack: () -> Unit = {}
)
```

- [ ] **Step 7.3: Update `LockedCreatureCard` to show per-species condition hint**

Replace the existing `LockedCreatureCard` composable:

```kotlin
@Composable
private fun LockedCreatureCard(
    spec: CreatureSpec,
    decorationProgress: com.mnebot.riptide.domain.model.DecorationProgress
) {
    val hintText: String = when (spec.species) {
        CreatureSpecies.TREASURE_CHEST ->
            stringResource(Res.string.decoration_hint_treasure_chest, decorationProgress.perfectDaysStreak)
        CreatureSpecies.ANCHOR ->
            stringResource(Res.string.decoration_hint_anchor, decorationProgress.completedTasksTotal)
        CreatureSpecies.SUNKEN_SHIP ->
            stringResource(Res.string.decoration_hint_sunken_ship)
        else ->
            stringResource(Res.string.msg_unlock_hint)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x11FFFFFF))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = spec.emoji,
            fontSize = 32.sp,
            color = Color.White.copy(alpha = 0.25f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Rarity chip tenue
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(rarityColor(spec.rarity).copy(alpha = 0.12f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = stringResource(spec.rarity.displayNameRes()).uppercase(),
                color = rarityColor(spec.rarity).copy(alpha = 0.5f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = hintText,
            color = SectionLabel,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}
```

- [ ] **Step 7.4: Pass `decorationProgress` through to `LockedCreatureCard` call sites**

In the grid rendering loop in `EcosystemScreen`, find where `LockedCreatureCard(spec = spec)` is called and change to:
```kotlin
LockedCreatureCard(spec = spec, decorationProgress = decorationProgress)
```

- [ ] **Step 7.5: Update `AquariumCreature.kt` DECORATION comment**

Find the comment `// DECORATION: sin lootbox, se desbloquean automáticamente` and replace:
```kotlin
// DECORATION: sin niveles de lootbox — se desbloquean por condiciones específicas
// (DecorationUnlockChecker): TREASURE_CHEST=7 días perfectos, ANCHOR=100 tareas, SUNKEN_SHIP=wallpaper
```

- [ ] **Step 7.6: Verify compilation**

```bash
./gradlew :composeApp:compileDebugKotlin 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 7.7: Run full test suite**

```bash
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -10
```

Expected: all tests pass.

- [ ] **Step 7.8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/aquarium/EcosystemScreen.kt
git add composeApp/src/androidMain/res/values/strings.xml
git add composeApp/src/androidMain/res/values-es/strings.xml
git add composeApp/src/commonMain/kotlin/com/mnebot/riptide/presentation/aquarium/AquariumCreature.kt
git commit -m "feat: show per-species decoration condition hints in LockedCreatureCard"
```

---

## Final verification

- [ ] **Build debug APK**

```bash
./gradlew assembleDebug 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL — APK generated in `composeApp/build/outputs/apk/debug/`

- [ ] **Full test run**

```bash
./gradlew testDebugUnitTest 2>&1 | tail -10
```

Expected: all tests pass (≥47 tests: existing 37 + 7 spillover + 10 decoration + 3 LootboxResolver)
