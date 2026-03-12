package com.mnebot.riptide.domain

object EcosystemLevelCalculator {

    fun xpForLevel(level: Int): Int {
        if (level <= 1) return 0
        var total = 0
        var cost = 100
        for (i in 2..level) {
            total += cost
            cost += 50
        }
        return total
    }

    fun levelForXp(xp: Int): Int {
        var level = 1
        while (xp >= xpForLevel(level + 1)) level++
        return level
    }

    fun xpInCurrentLevel(xp: Int): Int =
        xp - xpForLevel(levelForXp(xp))

    fun xpForNextLevel(currentLevel: Int): Int =
        xpForLevel(currentLevel + 1) - xpForLevel(currentLevel)

    fun nightBonus(score: Float, bestStreak: Int): Int {
        val scoreBonus = when {
            score >= 1.0f -> 50
            score >= 0.7f -> 25
            score >= 0.4f -> 10
            else -> 0
        }
        return scoreBonus + (bestStreak * 5)
    }

    const val XP_PER_TASK = 10
}