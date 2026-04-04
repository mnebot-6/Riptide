package com.mnebot.riptide.backend.models

import kotlinx.serialization.Serializable

// ── Work Block ──────────────────────────────────────────────

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

// ── Block Category ──────────────────────────────────────────

@Serializable
data class BlockCategoryDto(
    val blockId: String,
    val category: String,
    val updatedAt: String? = null
)

// ── Day Task ────────────────────────────────────────────────

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
    val targetCount: Int? = null,
    val currentCount: Int = 0,
    val notes: String? = null,
    val timerDurationMinutes: Int? = null,
    val isPriority: Boolean = false,
    val updatedAt: String? = null,
    val isDeleted: Boolean = false
)

// ── Recurring Task Def ──────────────────────────────────────

@Serializable
data class RecurringTaskDefDto(
    val id: String,
    val blockId: String,
    val title: String,
    val time: String? = null,
    val recurrence: String,
    val isActive: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val targetCount: Int? = null,
    val noteTemplate: String? = null,
    val timerDurationMinutes: Int? = null,
    val isPriority: Boolean = false,
    val updatedAt: String? = null,
    val isDeleted: Boolean = false
)

// ── Day Summary ─────────────────────────────────────────────

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

// ── Block Streak ────────────────────────────────────────────

@Serializable
data class BlockStreakDto(
    val blockId: String,
    val currentStreak: Int,
    val longestStreak: Int = 0,
    val lastActiveDate: String,
    val updatedAt: String? = null
)

// ── Ecosystem State ─────────────────────────────────────────

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

// ── Marine Creature ─────────────────────────────────────────

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

// ── Sync (batch) ────────────────────────────────────────────

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
data class FullSyncResponse(
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

// ── Batch / Sync helpers ────────────────────────────────────

@Serializable
data class BatchUpsertRequest<T>(
    val items: List<T>
)

@Serializable
data class SyncResponse<T>(
    val items: List<T>,
    val serverTime: String
)
