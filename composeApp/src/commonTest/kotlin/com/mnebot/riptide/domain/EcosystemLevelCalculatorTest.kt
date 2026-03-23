package com.mnebot.riptide.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcosystemLevelCalculatorTest {

    // ── xpForLevel ────────────────────────────────────────────────────────────

    @Test
    fun xpForLevel_level1_returns0() {
        assertEquals(0, EcosystemLevelCalculator.xpForLevel(1))
    }

    @Test
    fun xpForLevel_level2_returns1() {
        assertEquals(1, EcosystemLevelCalculator.xpForLevel(2))
    }

    @Test
    fun xpForLevel_level3_returns21() {
        // 1 (level 2) + 20 (first cost) = 21
        assertEquals(21, EcosystemLevelCalculator.xpForLevel(3))
    }

    @Test
    fun xpForLevel_isStrictlyIncreasing() {
        for (lv in 1..15) {
            assertTrue(
                EcosystemLevelCalculator.xpForLevel(lv + 1) > EcosystemLevelCalculator.xpForLevel(lv),
                "xpForLevel should be strictly increasing at level $lv"
            )
        }
    }

    // ── levelForXp ────────────────────────────────────────────────────────────

    @Test
    fun levelForXp_0xp_returns1() {
        assertEquals(1, EcosystemLevelCalculator.levelForXp(0))
    }

    @Test
    fun levelForXp_isInverseOfXpForLevel() {
        for (lv in 1..12) {
            val xp = EcosystemLevelCalculator.xpForLevel(lv)
            assertEquals(lv, EcosystemLevelCalculator.levelForXp(xp),
                "levelForXp(xpForLevel($lv)) should return $lv")
        }
    }

    @Test
    fun levelForXp_justBelowThreshold_staysAtLowerLevel() {
        val xpForLevel3 = EcosystemLevelCalculator.xpForLevel(3)
        assertEquals(2, EcosystemLevelCalculator.levelForXp(xpForLevel3 - 1))
    }

    @Test
    fun levelForXp_exactlyAtThreshold_returnsNewLevel() {
        val xpForLevel3 = EcosystemLevelCalculator.xpForLevel(3)
        assertEquals(3, EcosystemLevelCalculator.levelForXp(xpForLevel3))
    }

    // ── nightBonus ────────────────────────────────────────────────────────────

    @Test
    fun nightBonus_score0_noStreak_returns0() {
        assertEquals(0, EcosystemLevelCalculator.nightBonus(0f, 0))
    }

    @Test
    fun nightBonus_scoreBelow04_noStreak_returns0() {
        assertEquals(0, EcosystemLevelCalculator.nightBonus(0.39f, 0))
    }

    @Test
    fun nightBonus_score04_noStreak_returns10() {
        assertEquals(10, EcosystemLevelCalculator.nightBonus(0.40f, 0))
    }

    @Test
    fun nightBonus_score07_noStreak_returns25() {
        assertEquals(25, EcosystemLevelCalculator.nightBonus(0.70f, 0))
    }

    @Test
    fun nightBonus_score1_noStreak_returns50() {
        assertEquals(50, EcosystemLevelCalculator.nightBonus(1.0f, 0))
    }

    @Test
    fun nightBonus_streak3_adds15() {
        // 50 (score) + 3 * 5 (streak) = 65
        assertEquals(65, EcosystemLevelCalculator.nightBonus(1.0f, 3))
    }

    @Test
    fun nightBonus_score0_streak5_returns25() {
        // 0 (score) + 5 * 5 = 25
        assertEquals(25, EcosystemLevelCalculator.nightBonus(0f, 5))
    }

    // ── XP_PER_TASK ───────────────────────────────────────────────────────────

    @Test
    fun xpPerTask_is10() {
        assertEquals(10, EcosystemLevelCalculator.XP_PER_TASK)
    }
}
