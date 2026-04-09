package com.mnebot.riptide.domain.model

import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource

data class TaskPackage(
    val id: String,
    val nameRes: StringResource,
    val descriptionRes: StringResource,
    val icon: String,
    val color: String,
    val tasks: List<PackageTask>
)

data class PackageTask(
    val titleRes: StringResource,
    val time: LocalTime? = null,
    val recurrence: Recurrence = Recurrence.None,
    val targetCount: Int? = null,
    val timerDurationMinutes: Int? = null,
    val isPriority: Boolean = false
)
