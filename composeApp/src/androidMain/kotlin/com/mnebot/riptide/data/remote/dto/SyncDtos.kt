package com.mnebot.riptide.data.remote.dto

import kotlinx.serialization.Serializable

// -- Auth DTOs ---------------------------------------------------------------

@Serializable
data class GoogleAuthRequestDto(
    val idToken: String
)

@Serializable
data class TokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponseDto
)

@Serializable
data class UserResponseDto(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class RefreshResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

// -- Sync DTOs ---------------------------------------------------------------

@Serializable
data class SyncRequest(
    val lastSyncTime: String? = null,
    val workBlocks: List<WorkBlockDto> = emptyList(),
    val blockCategories: List<BlockCategoryDto> = emptyList(),
    val dayTasks: List<DayTaskDto> = emptyList(),
    val recurringTaskDefs: List<RecurringTaskDefDto> = emptyList(),
    val daySummaries: List<DaySummaryDto> = emptyList(),
    val blockStreaks: List<BlockStreakDto> = emptyList(),
    val ecosystemStates: List<EcosystemStateDto> = emptyList(),
    val marineCreatures: List<MarineCreatureDto> = emptyList()
)

@Serializable
data class SyncResponse(
    val serverTime: String,
    val workBlocks: List<WorkBlockDto> = emptyList(),
    val blockCategories: List<BlockCategoryDto> = emptyList(),
    val dayTasks: List<DayTaskDto> = emptyList(),
    val recurringTaskDefs: List<RecurringTaskDefDto> = emptyList(),
    val daySummaries: List<DaySummaryDto> = emptyList(),
    val blockStreaks: List<BlockStreakDto> = emptyList(),
    val ecosystemStates: List<EcosystemStateDto> = emptyList(),
    val marineCreatures: List<MarineCreatureDto> = emptyList()
)

// -- Resource DTOs (mirror backend ApiModels.kt) -----------------------------

@Serializable
data class WorkBlockDto(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val recurrenceJson: String,
    val isActive: Boolean = true,
    val updatedAt: String? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class BlockCategoryDto(
    val blockId: String,
    val category: String,
    val updatedAt: String? = null
)

@Serializable
data class DayTaskDto(
    val id: String,
    val blockId: String? = null,
    val title: String,
    val scheduleType: String,
    val date: String? = null,
    val time: String? = null,
    val recurrence: String? = null,
    val status: String,
    val completedAt: String? = null,
    val postponedTo: String? = null,
    val sourceTaskId: String? = null,
    val hasBeenRewarded: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val updatedAt: String? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class RecurringTaskDefDto(
    val id: String,
    val blockId: String,
    val title: String,
    val time: String? = null,
    val recurrence: String,
    val isActive: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val updatedAt: String? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class DaySummaryDto(
    val id: String,
    val date: String,
    val score: Float,
    val tasksTotal: Int,
    val tasksCompleted: Int,
    val streakDay: Int,
    val feedbackMessage: String,
    val updatedAt: String? = null
)

@Serializable
data class BlockStreakDto(
    val blockId: String,
    val currentStreak: Int,
    val longestStreak: Int = 0,
    val lastActiveDate: String,
    val updatedAt: String? = null
)

@Serializable
data class EcosystemStateDto(
    val id: String,
    val category: String,
    val totalExperience: Int,
    val currentLevel: Int,
    val isUnlocked: Boolean,
    val lastUpdated: String,
    val updatedAt: String? = null
)

@Serializable
data class MarineCreatureDto(
    val id: String,
    val ecosystemId: String,
    val category: String,
    val species: String,
    val nickname: String? = null,
    val unlockedAtLevel: Int,
    val experience: Int,
    val creatureLevel: Int,
    val unlockedAt: String,
    val updatedAt: String? = null
)
