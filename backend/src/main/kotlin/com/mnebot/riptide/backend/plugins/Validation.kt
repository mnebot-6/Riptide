package com.mnebot.riptide.backend.plugins

/** Input validation utilities for API DTOs */
object Validation {
    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val ISO_DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val ISO_TIME_REGEX = Regex("^\\d{2}:\\d{2}(:\\d{2})?$")
    private val ISO_DATETIME_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")
    private val HEX_COLOR_REGEX = Regex("^#[0-9a-fA-F]{6}$")

    private val VALID_SCHEDULE_TYPES = setOf("ONE_TIME", "RECURRING")
    private val VALID_STATUSES = setOf("PENDING", "COMPLETED", "EXPIRED", "POSTPONED")
    private val VALID_CATEGORIES = setOf(
        "FISH", "FLORA", "CRUSTACEAN", "MOLLUSK", "PELAGIC",
        "CEPHALOPOD", "REPTILE", "MAMMAL", "DECORATION"
    )

    private const val MAX_TITLE_LENGTH = 500
    private const val MAX_NICKNAME_LENGTH = 100
    private const val MAX_NAME_LENGTH = 200
    private const val MAX_ICON_LENGTH = 50
    private const val MAX_JSON_LENGTH = 10_000
    private const val MAX_FEEDBACK_LENGTH = 2_000

    fun requireUuid(value: String, field: String) {
        if (!UUID_REGEX.matches(value)) {
            throw ValidationException("$field must be a valid UUID")
        }
    }

    fun requireUuidOrNull(value: String?, field: String) {
        if (value != null) requireUuid(value, field)
    }

    fun requireMaxLength(value: String, max: Int, field: String) {
        if (value.length > max) {
            throw ValidationException("$field exceeds maximum length of $max")
        }
    }

    fun requireNotBlank(value: String, field: String) {
        if (value.isBlank()) {
            throw ValidationException("$field must not be blank")
        }
    }

    fun requireIsoDate(value: String?, field: String) {
        if (value != null && !ISO_DATE_REGEX.matches(value)) {
            throw ValidationException("$field must be a valid ISO date (YYYY-MM-DD)")
        }
    }

    fun requireIsoTime(value: String?, field: String) {
        if (value != null && !ISO_TIME_REGEX.matches(value)) {
            throw ValidationException("$field must be a valid ISO time (HH:MM or HH:MM:SS)")
        }
    }

    fun requireIsoDateTime(value: String?, field: String) {
        if (value != null && !ISO_DATETIME_REGEX.matches(value)) {
            throw ValidationException("$field must be a valid ISO datetime")
        }
    }

    fun requireHexColor(value: String, field: String) {
        if (!HEX_COLOR_REGEX.matches(value)) {
            throw ValidationException("$field must be a hex color (#RRGGBB)")
        }
    }

    fun requireEnum(value: String, allowed: Set<String>, field: String) {
        if (value !in allowed) {
            throw ValidationException("$field must be one of: ${allowed.joinToString()}")
        }
    }

    // ── DTO validators ────────────────────────────────────────

    fun validateWorkBlock(id: String, name: String, color: String, icon: String, recurrenceJson: String) {
        requireUuid(id, "id")
        requireNotBlank(name, "name")
        requireMaxLength(name, MAX_NAME_LENGTH, "name")
        requireHexColor(color, "color")
        requireMaxLength(icon, MAX_ICON_LENGTH, "icon")
        requireMaxLength(recurrenceJson, MAX_JSON_LENGTH, "recurrenceJson")
    }

    fun validateDayTask(
        id: String, title: String, scheduleType: String, blockId: String?,
        date: String?, time: String?, status: String,
        completedAt: String?, postponedTo: String?, sourceTaskId: String?
    ) {
        requireUuid(id, "id")
        requireUuidOrNull(blockId, "blockId")
        requireNotBlank(title, "title")
        requireMaxLength(title, MAX_TITLE_LENGTH, "title")
        requireEnum(scheduleType, VALID_SCHEDULE_TYPES, "scheduleType")
        requireEnum(status, VALID_STATUSES, "status")
        requireIsoDate(date, "date")
        requireIsoTime(time, "time")
        requireIsoDateTime(completedAt, "completedAt")
        requireIsoDateTime(postponedTo, "postponedTo")
        requireUuidOrNull(sourceTaskId, "sourceTaskId")
    }

    fun validateRecurringTaskDef(id: String, blockId: String, title: String, time: String?, recurrence: String) {
        requireUuid(id, "id")
        requireUuid(blockId, "blockId")
        requireNotBlank(title, "title")
        requireMaxLength(title, MAX_TITLE_LENGTH, "title")
        requireIsoTime(time, "time")
        requireMaxLength(recurrence, MAX_JSON_LENGTH, "recurrence")
    }

    fun validateDaySummary(id: String, date: String, feedbackMessage: String) {
        requireUuid(id, "id")
        requireIsoDate(date, "date")
        requireMaxLength(feedbackMessage, MAX_FEEDBACK_LENGTH, "feedbackMessage")
    }

    fun validateBlockStreak(blockId: String, lastActiveDate: String) {
        requireUuid(blockId, "blockId")
        requireIsoDate(lastActiveDate, "lastActiveDate")
    }

    fun validateEcosystemState(id: String, category: String, lastUpdated: String) {
        requireUuid(id, "id")
        requireEnum(category, VALID_CATEGORIES, "category")
        requireIsoDateTime(lastUpdated, "lastUpdated")
    }

    fun validateMarineCreature(id: String, ecosystemId: String, category: String, species: String, nickname: String?, unlockedAt: String) {
        requireUuid(id, "id")
        requireUuid(ecosystemId, "ecosystemId")
        requireEnum(category, VALID_CATEGORIES, "category")
        requireNotBlank(species, "species")
        requireMaxLength(species, MAX_ICON_LENGTH, "species")
        if (nickname != null) requireMaxLength(nickname, MAX_NICKNAME_LENGTH, "nickname")
        requireIsoDateTime(unlockedAt, "unlockedAt")
    }

    fun validateBlockCategory(blockId: String, category: String) {
        requireUuid(blockId, "blockId")
        requireEnum(category, VALID_CATEGORIES, "category")
    }
}
