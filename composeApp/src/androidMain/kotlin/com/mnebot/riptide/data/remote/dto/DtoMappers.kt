package com.mnebot.riptide.data.remote.dto

import com.mnebot.riptide.data.local.entity.*

// -- WorkBlock ---------------------------------------------------------------

fun WorkBlockEntity.toDto() = WorkBlockDto(
    id = id,
    name = name,
    color = color,
    icon = icon,
    recurrenceJson = recurrenceJson,
    isActive = isActive,
    updatedAt = updatedAt.ifEmpty { null },
    isDeleted = isDeleted
)

fun WorkBlockDto.toEntity() = WorkBlockEntity(
    id = id,
    name = name,
    color = color,
    icon = icon,
    recurrenceJson = recurrenceJson,
    isActive = isActive,
    updatedAt = updatedAt ?: "",
    isDeleted = isDeleted
)

// -- BlockCategory -----------------------------------------------------------

fun BlockCategoryEntity.toDto() = BlockCategoryDto(
    blockId = blockId,
    category = category,
    updatedAt = updatedAt.ifEmpty { null }
)

fun BlockCategoryDto.toEntity() = BlockCategoryEntity(
    blockId = blockId,
    category = category,
    updatedAt = updatedAt ?: ""
)

// -- DayTask -----------------------------------------------------------------

fun DayTaskEntity.toDto() = DayTaskDto(
    id = id,
    blockId = blockId,
    title = title,
    scheduleType = scheduleType,
    date = date,
    time = time,
    recurrence = recurrence,
    status = status,
    completedAt = completedAt,
    postponedTo = postponedTo,
    sourceTaskId = sourceTaskId,
    hasBeenRewarded = hasBeenRewarded,
    notificationsEnabled = notificationsEnabled,
    updatedAt = updatedAt.ifEmpty { null },
    isDeleted = isDeleted
)

fun DayTaskDto.toEntity() = DayTaskEntity(
    id = id,
    blockId = blockId,
    title = title,
    scheduleType = scheduleType,
    date = date,
    time = time,
    recurrence = recurrence,
    status = status,
    completedAt = completedAt,
    postponedTo = postponedTo,
    sourceTaskId = sourceTaskId,
    hasBeenRewarded = hasBeenRewarded,
    notificationsEnabled = notificationsEnabled,
    updatedAt = updatedAt ?: "",
    isDeleted = isDeleted
)

// -- RecurringTaskDef --------------------------------------------------------

fun RecurringTaskDefEntity.toDto() = RecurringTaskDefDto(
    id = id,
    blockId = blockId,
    title = title,
    time = time,
    recurrence = recurrence,
    isActive = isActive,
    notificationsEnabled = notificationsEnabled,
    updatedAt = updatedAt.ifEmpty { null },
    isDeleted = isDeleted
)

fun RecurringTaskDefDto.toEntity() = RecurringTaskDefEntity(
    id = id,
    blockId = blockId,
    title = title,
    time = time,
    recurrence = recurrence,
    isActive = isActive,
    notificationsEnabled = notificationsEnabled,
    updatedAt = updatedAt ?: "",
    isDeleted = isDeleted
)

// -- DaySummary --------------------------------------------------------------

fun DaySummaryEntity.toDto() = DaySummaryDto(
    id = id,
    date = date,
    score = score,
    tasksTotal = tasksTotal,
    tasksCompleted = tasksCompleted,
    streakDay = streakDay,
    feedbackMessage = feedbackMessage,
    updatedAt = updatedAt.ifEmpty { null }
)

fun DaySummaryDto.toEntity() = DaySummaryEntity(
    id = id,
    date = date,
    score = score,
    tasksTotal = tasksTotal,
    tasksCompleted = tasksCompleted,
    streakDay = streakDay,
    feedbackMessage = feedbackMessage,
    updatedAt = updatedAt ?: ""
)

// -- BlockStreak -------------------------------------------------------------

fun BlockStreakEntity.toDto() = BlockStreakDto(
    blockId = blockId,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastActiveDate = lastActiveDate,
    updatedAt = updatedAt.ifEmpty { null }
)

fun BlockStreakDto.toEntity() = BlockStreakEntity(
    blockId = blockId,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    lastActiveDate = lastActiveDate,
    updatedAt = updatedAt ?: ""
)

// -- EcosystemState ----------------------------------------------------------

fun EcosystemStateEntity.toDto() = EcosystemStateDto(
    id = id,
    category = category,
    totalExperience = totalExperience,
    currentLevel = currentLevel,
    isUnlocked = isUnlocked,
    lastUpdated = lastUpdated,
    updatedAt = updatedAt.ifEmpty { null }
)

fun EcosystemStateDto.toEntity() = EcosystemStateEntity(
    id = id,
    category = category,
    totalExperience = totalExperience,
    currentLevel = currentLevel,
    isUnlocked = isUnlocked,
    lastUpdated = lastUpdated,
    updatedAt = updatedAt ?: ""
)

// -- MarineCreature ----------------------------------------------------------

fun MarineCreatureEntity.toDto() = MarineCreatureDto(
    id = id,
    ecosystemId = ecosystemId,
    category = category,
    species = species,
    nickname = nickname,
    unlockedAtLevel = unlockedAtLevel,
    experience = experience,
    creatureLevel = creatureLevel,
    unlockedAt = unlockedAt,
    updatedAt = updatedAt.ifEmpty { null }
)

fun MarineCreatureDto.toEntity() = MarineCreatureEntity(
    id = id,
    ecosystemId = ecosystemId,
    category = category,
    species = species,
    nickname = nickname,
    unlockedAtLevel = unlockedAtLevel,
    experience = experience,
    creatureLevel = creatureLevel,
    unlockedAt = unlockedAt,
    updatedAt = updatedAt ?: ""
)
