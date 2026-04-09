package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.model.RecurringTaskDef
import com.mnebot.riptide.domain.model.TaskPackage
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.domain.repository.RecurringTaskDefRepository
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import com.mnebot.riptide.domain.repository.WorkBlockRepository
import com.mnebot.riptide.generateUUID
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.jetbrains.compose.resources.getString

class PackageInstaller(
    private val workBlockRepository: WorkBlockRepository,
    private val recurringTaskDefRepository: RecurringTaskDefRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val marineCategoryAssigner: MarineCategoryAssigner,
    private val recurringTaskGenerator: RecurringTaskGenerator
) {

    suspend fun install(pkg: TaskPackage) {
        if (isInstalled(pkg.id)) return

        val blockId = generateUUID()
        val blockName = getString(pkg.nameRes)

        workBlockRepository.insert(
            WorkBlock(
                id = blockId,
                name = blockName,
                marineCategories = emptyList(),
                color = pkg.color,
                icon = pkg.icon,
                recurrence = pkg.tasks.firstOrNull()?.recurrence
                    ?: com.mnebot.riptide.domain.model.Recurrence.None,
                isActive = true
            )
        )

        for (task in pkg.tasks) {
            val title = getString(task.titleRes)
            recurringTaskDefRepository.insert(
                RecurringTaskDef(
                    id = generateUUID(),
                    blockId = blockId,
                    title = title,
                    time = task.time,
                    recurrence = task.recurrence,
                    isActive = true,
                    notificationsEnabled = task.time != null,
                    targetCount = task.targetCount,
                    noteTemplate = null,
                    timerDurationMinutes = task.timerDurationMinutes,
                    isPriority = task.isPriority
                )
            )
        }

        marineCategoryAssigner.reassign()

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        recurringTaskGenerator.generateUpTo(today, daysAhead = 7)

        userPreferencesRepository.addInstalledPackageId(pkg.id)
    }

    suspend fun uninstall(pkg: TaskPackage) {
        if (!isInstalled(pkg.id)) return

        val blockName = getString(pkg.nameRes)
        val blocks = workBlockRepository.getAll()
        val block = blocks.find { it.name == blockName && it.color == pkg.color }

        if (block != null) {
            val defs = recurringTaskDefRepository.getByBlock(block.id)
            for (def in defs) {
                recurringTaskDefRepository.delete(def.id)
            }
            workBlockRepository.delete(block.id)
            marineCategoryAssigner.reassign()
        }

        userPreferencesRepository.removeInstalledPackageId(pkg.id)
    }

    suspend fun isInstalled(packageId: String): Boolean =
        packageId in userPreferencesRepository.getInstalledPackageIds()
}
