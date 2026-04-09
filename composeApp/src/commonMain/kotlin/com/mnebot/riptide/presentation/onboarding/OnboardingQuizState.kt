package com.mnebot.riptide.presentation.onboarding

enum class Occupation { STUDENT, WORKER, FREELANCER, OTHER }

enum class LifeArea { HEALTH, FITNESS, STUDY, WORK, PERSONAL, CREATIVE, MINDFULNESS }

enum class TaskStructure { FIXED, FLEXIBLE, MIX }

enum class DailyTaskCount { LIGHT, MODERATE, HEAVY }

data class OnboardingQuizState(
    val occupation: Occupation = Occupation.WORKER,
    val lifeAreas: Set<LifeArea> = emptySet(),
    val wakeHour: Int = 7,
    val bedHour: Int = 23,
    val workDays: Set<Int> = setOf(1, 2, 3, 4, 5),  // 1=Mon .. 7=Sun
    val structure: TaskStructure = TaskStructure.MIX,
    val taskCount: DailyTaskCount = DailyTaskCount.MODERATE
)
