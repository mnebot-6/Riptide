package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeBlockCategoryRepository
import com.mnebot.riptide.domain.fakes.FakeEcosystemStateRepository
import com.mnebot.riptide.domain.fakes.FakeWorkBlockRepository
import com.mnebot.riptide.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarineCategoryAssignerTest {

    private fun block(id: String) = WorkBlock(
        id = id, name = "Block $id",
        marineCategories = emptyList(),
        color = "#1565C0", icon = "waves",
        recurrence = Recurrence.None, isActive = true
    )

    private fun ecosystemState(category: MarineCategory, unlocked: Boolean = true) = EcosystemState(
        id = "es-${category.name}",
        category = category,
        totalExperience = 0,
        currentLevel = 1,
        isUnlocked = unlocked,
        lastUpdated = LocalDateTime(2024, 6, 15, 12, 0)
    )

    private fun assigner(
        blockRepo: FakeWorkBlockRepository = FakeWorkBlockRepository(),
        catRepo: FakeBlockCategoryRepository = FakeBlockCategoryRepository(),
        ecoRepo: FakeEcosystemStateRepository = FakeEcosystemStateRepository()
    ) = MarineCategoryAssigner(blockRepo, catRepo, ecoRepo) to catRepo

    // ── Caso base ────────────────────────────────────────────────────────────

    @Test
    fun assigns_categories_to_single_block() = runTest {
        val blockRepo = FakeWorkBlockRepository()
        blockRepo.insert(block("b1"))

        val ecoRepo = FakeEcosystemStateRepository()
        ecoRepo.seed(ecosystemState(MarineCategory.FISH))
        ecoRepo.seed(ecosystemState(MarineCategory.FLORA))
        ecoRepo.seed(ecosystemState(MarineCategory.CRUSTACEAN))

        val (assigner, catRepo) = assigner(blockRepo, ecoRepo = ecoRepo)
        assigner.assignMissing()

        val cats = catRepo.categoriesFor("b1")
        assertEquals(3, cats.size, "Single block gets all unlocked non-DECORATION categories")
        assertTrue(cats.contains(MarineCategory.FISH))
        assertTrue(cats.contains(MarineCategory.FLORA))
        assertTrue(cats.contains(MarineCategory.CRUSTACEAN))
    }

    @Test
    fun distributes_categories_across_multiple_blocks() = runTest {
        val blockRepo = FakeWorkBlockRepository()
        blockRepo.insert(block("b1"))
        blockRepo.insert(block("b2"))

        val ecoRepo = FakeEcosystemStateRepository()
        ecoRepo.seed(ecosystemState(MarineCategory.FISH))
        ecoRepo.seed(ecosystemState(MarineCategory.FLORA))
        ecoRepo.seed(ecosystemState(MarineCategory.CRUSTACEAN))
        ecoRepo.seed(ecosystemState(MarineCategory.MOLLUSK))

        val (assigner, catRepo) = assigner(blockRepo, ecoRepo = ecoRepo)
        assigner.assignMissing()

        val cats1 = catRepo.categoriesFor("b1")
        val cats2 = catRepo.categoriesFor("b2")

        // 4 categorias / 2 bloques = 2 cada uno
        assertEquals(2, cats1.size)
        assertEquals(2, cats2.size)

        // Todas las categorias deben estar asignadas
        val allAssigned = (cats1 + cats2).toSet()
        assertEquals(4, allAssigned.size)
    }

    // ── Exclusion DECORATION ─────────────────────────────────────────────────

    @Test
    fun excludes_decoration_category() = runTest {
        val blockRepo = FakeWorkBlockRepository()
        blockRepo.insert(block("b1"))

        val ecoRepo = FakeEcosystemStateRepository()
        ecoRepo.seed(ecosystemState(MarineCategory.FISH))
        ecoRepo.seed(ecosystemState(MarineCategory.DECORATION, unlocked = true))

        val (assigner, catRepo) = assigner(blockRepo, ecoRepo = ecoRepo)
        assigner.assignMissing()

        val cats = catRepo.categoriesFor("b1")
        assertTrue(MarineCategory.DECORATION !in cats, "DECORATION should not be assigned")
        assertTrue(MarineCategory.FISH in cats)
    }

    // ── Locked categories ────────────────────────────────────────────────────

    @Test
    fun ignores_locked_categories() = runTest {
        val blockRepo = FakeWorkBlockRepository()
        blockRepo.insert(block("b1"))

        val ecoRepo = FakeEcosystemStateRepository()
        ecoRepo.seed(ecosystemState(MarineCategory.FISH, unlocked = true))
        ecoRepo.seed(ecosystemState(MarineCategory.CEPHALOPOD, unlocked = false))
        ecoRepo.seed(ecosystemState(MarineCategory.MAMMAL, unlocked = false))

        val (assigner, catRepo) = assigner(blockRepo, ecoRepo = ecoRepo)
        assigner.assignMissing()

        val cats = catRepo.categoriesFor("b1")
        assertEquals(1, cats.size)
        assertEquals(MarineCategory.FISH, cats[0])
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    fun no_blocks_does_nothing() = runTest {
        val ecoRepo = FakeEcosystemStateRepository()
        ecoRepo.seed(ecosystemState(MarineCategory.FISH))

        val (assigner, catRepo) = assigner(ecoRepo = ecoRepo)
        assigner.assignMissing()

        // No crash, no assignments
        assertTrue(catRepo.categoriesFor("anything").isEmpty())
    }

    @Test
    fun no_unlocked_categories_does_nothing() = runTest {
        val blockRepo = FakeWorkBlockRepository()
        blockRepo.insert(block("b1"))

        val ecoRepo = FakeEcosystemStateRepository()
        ecoRepo.seed(ecosystemState(MarineCategory.CEPHALOPOD, unlocked = false))

        val (assigner, catRepo) = assigner(blockRepo, ecoRepo = ecoRepo)
        assigner.assignMissing()

        assertTrue(catRepo.categoriesFor("b1").isEmpty())
    }

    @Test
    fun more_blocks_than_categories_each_block_gets_at_least_one() = runTest {
        val blockRepo = FakeWorkBlockRepository()
        blockRepo.insert(block("b1"))
        blockRepo.insert(block("b2"))
        blockRepo.insert(block("b3"))
        blockRepo.insert(block("b4"))

        val ecoRepo = FakeEcosystemStateRepository()
        ecoRepo.seed(ecosystemState(MarineCategory.FISH))
        ecoRepo.seed(ecosystemState(MarineCategory.FLORA))

        val (assigner, catRepo) = assigner(blockRepo, ecoRepo = ecoRepo)
        assigner.assignMissing()

        // Cada bloque debe tener al menos 1 categoria
        for (id in listOf("b1", "b2", "b3", "b4")) {
            assertTrue(catRepo.categoriesFor(id).isNotEmpty(), "Block $id should have at least 1 category")
        }
    }

    @Test
    fun no_duplicate_categories_per_block() = runTest {
        val blockRepo = FakeWorkBlockRepository()
        blockRepo.insert(block("b1"))

        val ecoRepo = FakeEcosystemStateRepository()
        // Muchas categorias desbloqueadas
        listOf(MarineCategory.FISH, MarineCategory.FLORA, MarineCategory.CRUSTACEAN,
            MarineCategory.MOLLUSK, MarineCategory.PELAGIC).forEach {
            ecoRepo.seed(ecosystemState(it))
        }

        val (assigner, catRepo) = assigner(blockRepo, ecoRepo = ecoRepo)
        assigner.assignMissing()

        val cats = catRepo.categoriesFor("b1")
        assertEquals(cats.size, cats.toSet().size, "No duplicate categories in a single block")
    }
}
