package com.mnebot.riptide.domain

object EcosystemLevelCalculator {

    fun xpForLevel(level: Int): Int {
        if (level <= 1) return 0
        if (level == 2) return 1
        var total = 1
        var cost = 20
        for (i in 3..level) {
            total += cost
            cost = (cost * 1.5f).toInt()
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