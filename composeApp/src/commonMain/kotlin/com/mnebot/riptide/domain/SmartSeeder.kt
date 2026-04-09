package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.TaskSchedule
import com.mnebot.riptide.domain.model.TaskStatus
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.generateUUID
import com.mnebot.riptide.presentation.onboarding.LifeArea
import com.mnebot.riptide.presentation.onboarding.OnboardingQuizState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.jetbrains.compose.resources.getString
import riptide.composeapp.generated.resources.Res
import riptide.composeapp.generated.resources.task_first_task

/**
 * Seeds the app with blocks and tasks based on the onboarding quiz answers.
 * Reuses [PackageInstaller] for each life area selected by the user.
 */
class SmartSeeder(
    private val packageInstaller: PackageInstaller,
    private val dayTaskRepository: DayTaskRepository
) {

    suspend fun seedFromQuiz(quiz: OnboardingQuizState) {
        // Map selected life areas to packages
        for (area in quiz.lifeAreas) {
            val pkg = areaToPackage(area) ?: continue
            packageInstaller.install(pkg)
        }

        // If no areas were selected, install Health as a default
        if (quiz.lifeAreas.isEmpty()) {
            packageInstaller.install(TaskPackages.HEALTH)
        }

        // Insert an immediate completable task for today
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val firstTaskTitle = getString(Res.string.task_first_task)
        dayTaskRepository.insert(
            DayTask(
                id = generateUUID(),
                blockId = null,
                title = firstTaskTitle,
                schedule = TaskSchedule.OneTime(date = today, time = null),
                status = TaskStatus.PENDING,
                completedAt = null,
                postponedTo = null,
                sourceTaskId = null,
                notificationsEnabled = false
            )
        )
    }

    private fun areaToPackage(area: LifeArea) = when (area) {
        LifeArea.HEALTH -> TaskPackages.HEALTH
        LifeArea.FITNESS -> TaskPackages.FITNESS
        LifeArea.STUDY -> TaskPackages.STUDY
        LifeArea.MINDFULNESS -> TaskPackages.MINDFULNESS
        LifeArea.WORK -> null       // no predefined work package yet
        LifeArea.PERSONAL -> TaskPackages.HOME
        LifeArea.CREATIVE -> null   // no predefined creative package yet
    }
}
