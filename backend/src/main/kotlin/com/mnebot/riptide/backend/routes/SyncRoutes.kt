package com.mnebot.riptide.backend.routes

import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.*
import com.mnebot.riptide.backend.models.*
import com.mnebot.riptide.backend.plugins.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Todo el control de sincronización vive en UTC.
 *
 * El cliente manda marcas ISO con sufijo 'Z' y el servidor las guarda tal cual, de modo
 * que `clientUpdated.isAfter(serverUpdated)` compara dos puntos de la misma línea de
 * tiempo. Comparar relojes de pared de zonas distintas hacía que el servidor descartara
 * en silencio borrados y ediciones del movil y luego se los devolviera resucitados.
 */
private fun parseClientTimestamp(raw: String?): LocalDateTime? {
    if (raw.isNullOrBlank()) return null
    return runCatching { Instant.parse(raw).atZone(ZoneOffset.UTC).toLocalDateTime() }
        .recoverCatching { LocalDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME) }
        .getOrNull()
}

private fun nowUtc(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

/** Se devuelve con 'Z' para que el cliente compare contra sus propias marcas UTC. */
private fun LocalDateTime.toUtcIso(): String = this.toInstant(ZoneOffset.UTC).toString()

fun Route.syncRoutes() {
    authenticate("auth-jwt") {
        post("/api/sync") {
            val principal = call.principal<JWTPrincipal>()!!
            val uid = principal.userId
            rateLimitApi(uid)

            val request = call.receive<SyncRequest>()

            // ── Validate batch sizes ──
            val maxBatch = 500
            if (request.workBlocks.size > maxBatch) throw ValidationException("workBlocks exceeds max batch size of $maxBatch")
            if (request.blockCategories.size > maxBatch) throw ValidationException("blockCategories exceeds max batch size of $maxBatch")
            if (request.dayTasks.size > maxBatch) throw ValidationException("dayTasks exceeds max batch size of $maxBatch")
            if (request.recurringTaskDefs.size > maxBatch) throw ValidationException("recurringTaskDefs exceeds max batch size of $maxBatch")
            if (request.daySummaries.size > maxBatch) throw ValidationException("daySummaries exceeds max batch size of $maxBatch")
            if (request.ecosystemStates.size > maxBatch) throw ValidationException("ecosystemStates exceeds max batch size of $maxBatch")
            if (request.marineCreatures.size > maxBatch) throw ValidationException("marineCreatures exceeds max batch size of $maxBatch")

            // ── Validate all items before any DB writes ──
            request.workBlocks.forEach { dto ->
                Validation.validateWorkBlock(dto.id, dto.name, dto.color, dto.icon, dto.recurrenceJson)
            }
            request.blockCategories.forEach { dto ->
                Validation.validateBlockCategory(dto.blockId, dto.category)
            }
            request.recurringTaskDefs.forEach { dto ->
                Validation.validateRecurringTaskDef(
                    dto.id, dto.blockId, dto.title, dto.time, dto.recurrence,
                    noteTemplate = dto.noteTemplate, targetCount = dto.targetCount,
                    timerDurationMinutes = dto.timerDurationMinutes
                )
            }
            request.dayTasks.forEach { dto ->
                Validation.validateDayTask(
                    dto.id, dto.title, dto.scheduleType, dto.blockId,
                    dto.date, dto.time, dto.status,
                    dto.completedAt, dto.postponedTo, dto.sourceTaskId,
                    recurrence = dto.recurrence, notes = dto.notes,
                    targetCount = dto.targetCount, currentCount = dto.currentCount,
                    timerDurationMinutes = dto.timerDurationMinutes
                )
            }
            request.daySummaries.forEach { dto ->
                Validation.validateDaySummary(dto.id, dto.date, dto.feedbackMessage)
            }
            request.ecosystemStates.forEach { dto ->
                Validation.validateEcosystemState(
                    dto.id, dto.category, dto.lastUpdated,
                    totalExperience = dto.totalExperience, currentLevel = dto.currentLevel
                )
            }
            request.marineCreatures.forEach { dto ->
                Validation.validateMarineCreature(
                    dto.id, dto.ecosystemId, dto.category, dto.species,
                    dto.nickname, dto.unlockedAt,
                    unlockedAtLevel = dto.unlockedAtLevel, experience = dto.experience,
                    creatureLevel = dto.creatureLevel
                )
            }

            val since = parseClientTimestamp(request.lastSyncTime)
            val now = nowUtc()

            val response = dbQuery {
                // ── PUSH: Upsert client changes (FK order) ──
                upsertWorkBlocks(request.workBlocks, uid, now)
                upsertBlockCategories(request.blockCategories, uid, now)
                upsertRecurringTaskDefs(request.recurringTaskDefs, uid, now)
                upsertDayTasks(request.dayTasks, uid, now)
                upsertDaySummaries(request.daySummaries, uid, now)
                upsertEcosystemStates(request.ecosystemStates, uid, now)
                upsertMarineCreatures(request.marineCreatures, uid, now)

                // ── PULL: Gather server changes ──
                FullSyncResponse(
                    serverTime = now.toUtcIso(),
                    workBlocks = pullWorkBlocks(uid, since),
                    blockCategories = pullBlockCategories(uid, since),
                    dayTasks = pullDayTasks(uid, since),
                    recurringTaskDefs = pullRecurringTaskDefs(uid, since),
                    daySummaries = pullDaySummaries(uid, since),
                    ecosystemStates = pullEcosystemStates(uid, since),
                    marineCreatures = pullMarineCreatures(uid, since)
                )
            }

            call.respond(HttpStatusCode.OK, response)
        }

        // DELETE /api/user-data — Hard-delete all user data (for "keep local" sync choice)
        delete("/api/user-data") {
            val uid = call.principal<JWTPrincipal>()!!.userId
            rateLimitApi(uid)

            dbQuery {
                // FK order: dependents first
                MarineCreaturesTable.deleteWhere { userId eq uid }
                EcosystemStatesTable.deleteWhere { userId eq uid }
                DaySummariesTable.deleteWhere { userId eq uid }
                DayTasksTable.deleteWhere { userId eq uid }
                RecurringTaskDefsTable.deleteWhere { userId eq uid }
                BlockCategoriesTable.deleteWhere { userId eq uid }
                WorkBlocksTable.deleteWhere { userId eq uid }
            }

            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }
    }
}

// ── PUSH helpers ────────────────────────────────────────────

private fun upsertWorkBlocks(items: List<WorkBlockDto>, uid: String, now: LocalDateTime) {
    for (dto in items) {
        val existing = WorkBlocksTable.selectAll()
            .where { (WorkBlocksTable.id eq dto.id) and (WorkBlocksTable.userId eq uid) }
            .singleOrNull()

        if (existing == null) {
            WorkBlocksTable.insert {
                it[id] = dto.id
                it[userId] = uid
                it[name] = dto.name
                it[color] = dto.color
                it[icon] = dto.icon
                it[recurrenceJson] = dto.recurrenceJson
                it[isActive] = dto.isActive
                it[isDeleted] = dto.isDeleted
                it[updatedAt] = now
            }
        } else {
            // Server deletion is authoritative
            if (existing[WorkBlocksTable.isDeleted] && !dto.isDeleted) continue

            val serverUpdated = existing[WorkBlocksTable.updatedAt]
            val clientUpdated = parseClientTimestamp(dto.updatedAt)

            if (clientUpdated != null && clientUpdated.isAfter(serverUpdated)) {
                WorkBlocksTable.update({
                    (WorkBlocksTable.id eq dto.id) and (WorkBlocksTable.userId eq uid)
                }) {
                    it[name] = dto.name
                    it[color] = dto.color
                    it[icon] = dto.icon
                    it[recurrenceJson] = dto.recurrenceJson
                    it[isActive] = dto.isActive
                    it[isDeleted] = dto.isDeleted
                    it[updatedAt] = now
                }
            }
        }
    }
}

private fun upsertBlockCategories(items: List<BlockCategoryDto>, uid: String, now: LocalDateTime) {
    for (dto in items) {
        val existing = BlockCategoriesTable.selectAll()
            .where {
                (BlockCategoriesTable.blockId eq dto.blockId) and
                (BlockCategoriesTable.category eq dto.category) and
                (BlockCategoriesTable.userId eq uid)
            }
            .singleOrNull()

        if (existing == null) {
            BlockCategoriesTable.insert {
                it[blockId] = dto.blockId
                it[category] = dto.category
                it[userId] = uid
                it[updatedAt] = now
            }
        } else {
            val serverUpdated = existing[BlockCategoriesTable.updatedAt]
            val clientUpdated = parseClientTimestamp(dto.updatedAt)

            if (clientUpdated != null && clientUpdated.isAfter(serverUpdated)) {
                BlockCategoriesTable.update({
                    (BlockCategoriesTable.blockId eq dto.blockId) and
                    (BlockCategoriesTable.category eq dto.category) and
                    (BlockCategoriesTable.userId eq uid)
                }) {
                    it[updatedAt] = now
                }
            }
        }
    }
}

private fun upsertRecurringTaskDefs(items: List<RecurringTaskDefDto>, uid: String, now: LocalDateTime) {
    for (dto in items) {
        val existing = RecurringTaskDefsTable.selectAll()
            .where { (RecurringTaskDefsTable.id eq dto.id) and (RecurringTaskDefsTable.userId eq uid) }
            .singleOrNull()

        if (existing == null) {
            RecurringTaskDefsTable.insert {
                it[id] = dto.id
                it[userId] = uid
                it[blockId] = dto.blockId
                it[title] = dto.title
                it[time] = dto.time
                it[recurrence] = dto.recurrence
                it[isActive] = dto.isActive
                it[notificationsEnabled] = dto.notificationsEnabled
                it[targetCount] = dto.targetCount
                it[noteTemplate] = dto.noteTemplate
                it[timerDurationMinutes] = dto.timerDurationMinutes
                it[isPriority] = dto.isPriority
                it[isDeleted] = dto.isDeleted
                it[updatedAt] = now
            }
        } else {
            if (existing[RecurringTaskDefsTable.isDeleted] && !dto.isDeleted) continue

            val serverUpdated = existing[RecurringTaskDefsTable.updatedAt]
            val clientUpdated = parseClientTimestamp(dto.updatedAt)

            if (clientUpdated != null && clientUpdated.isAfter(serverUpdated)) {
                RecurringTaskDefsTable.update({
                    (RecurringTaskDefsTable.id eq dto.id) and (RecurringTaskDefsTable.userId eq uid)
                }) {
                    it[blockId] = dto.blockId
                    it[title] = dto.title
                    it[time] = dto.time
                    it[recurrence] = dto.recurrence
                    it[isActive] = dto.isActive
                    it[notificationsEnabled] = dto.notificationsEnabled
                    it[targetCount] = dto.targetCount
                    it[noteTemplate] = dto.noteTemplate
                    it[timerDurationMinutes] = dto.timerDurationMinutes
                    it[isPriority] = dto.isPriority
                    it[isDeleted] = dto.isDeleted
                    it[updatedAt] = now
                }
            }
        }
    }
}

private fun upsertDayTasks(items: List<DayTaskDto>, uid: String, now: LocalDateTime) {
    for (dto in items) {
        val existing = DayTasksTable.selectAll()
            .where { (DayTasksTable.id eq dto.id) and (DayTasksTable.userId eq uid) }
            .singleOrNull()

        if (existing == null) {
            DayTasksTable.insert {
                it[id] = dto.id
                it[userId] = uid
                it[blockId] = dto.blockId
                it[title] = dto.title
                it[scheduleType] = dto.scheduleType
                it[date] = dto.date
                it[time] = dto.time
                it[recurrence] = dto.recurrence
                it[status] = dto.status
                it[completedAt] = dto.completedAt
                it[postponedTo] = dto.postponedTo
                it[sourceTaskId] = dto.sourceTaskId
                it[hasBeenRewarded] = dto.hasBeenRewarded
                it[notificationsEnabled] = dto.notificationsEnabled
                it[targetCount] = dto.targetCount
                it[currentCount] = dto.currentCount
                it[notes] = dto.notes
                it[timerDurationMinutes] = dto.timerDurationMinutes
                it[isPriority] = dto.isPriority
                it[isDeleted] = dto.isDeleted
                it[updatedAt] = now
            }
        } else {
            // Server deletion is authoritative
            if (existing[DayTasksTable.isDeleted] && !dto.isDeleted) continue

            val serverUpdated = existing[DayTasksTable.updatedAt]
            val clientUpdated = parseClientTimestamp(dto.updatedAt)

            if (clientUpdated != null && clientUpdated.isAfter(serverUpdated)) {
                // hasBeenRewarded uses OR logic: once rewarded, always rewarded
                val mergedRewarded = dto.hasBeenRewarded || existing[DayTasksTable.hasBeenRewarded]

                DayTasksTable.update({
                    (DayTasksTable.id eq dto.id) and (DayTasksTable.userId eq uid)
                }) {
                    it[blockId] = dto.blockId
                    it[title] = dto.title
                    it[scheduleType] = dto.scheduleType
                    it[date] = dto.date
                    it[time] = dto.time
                    it[recurrence] = dto.recurrence
                    it[status] = dto.status
                    it[completedAt] = dto.completedAt
                    it[postponedTo] = dto.postponedTo
                    it[sourceTaskId] = dto.sourceTaskId
                    it[hasBeenRewarded] = mergedRewarded
                    it[notificationsEnabled] = dto.notificationsEnabled
                    it[targetCount] = dto.targetCount
                    it[currentCount] = dto.currentCount
                    it[notes] = dto.notes
                    it[timerDurationMinutes] = dto.timerDurationMinutes
                    it[isPriority] = dto.isPriority
                    it[isDeleted] = dto.isDeleted
                    it[updatedAt] = now
                }
            }
        }
    }
}

private fun upsertDaySummaries(items: List<DaySummaryDto>, uid: String, now: LocalDateTime) {
    for (dto in items) {
        val existing = DaySummariesTable.selectAll()
            .where { (DaySummariesTable.id eq dto.id) and (DaySummariesTable.userId eq uid) }
            .singleOrNull()

        if (existing == null) {
            DaySummariesTable.insert {
                it[id] = dto.id
                it[userId] = uid
                it[date] = dto.date
                it[score] = dto.score
                it[tasksTotal] = dto.tasksTotal
                it[tasksCompleted] = dto.tasksCompleted
                it[streakDay] = dto.streakDay
                it[feedbackMessage] = dto.feedbackMessage
                it[updatedAt] = now
            }
        } else {
            val serverUpdated = existing[DaySummariesTable.updatedAt]
            val clientUpdated = parseClientTimestamp(dto.updatedAt)

            if (clientUpdated != null && clientUpdated.isAfter(serverUpdated)) {
                DaySummariesTable.update({
                    (DaySummariesTable.id eq dto.id) and (DaySummariesTable.userId eq uid)
                }) {
                    it[date] = dto.date
                    it[score] = dto.score
                    it[tasksTotal] = dto.tasksTotal
                    it[tasksCompleted] = dto.tasksCompleted
                    it[streakDay] = dto.streakDay
                    it[feedbackMessage] = dto.feedbackMessage
                    it[updatedAt] = now
                }
            }
        }
    }
}

private fun upsertEcosystemStates(items: List<EcosystemStateDto>, uid: String, now: LocalDateTime) {
    for (dto in items) {
        val existing = EcosystemStatesTable.selectAll()
            .where { (EcosystemStatesTable.id eq dto.id) and (EcosystemStatesTable.userId eq uid) }
            .singleOrNull()

        if (existing == null) {
            EcosystemStatesTable.insert {
                it[id] = dto.id
                it[userId] = uid
                it[category] = dto.category
                it[totalExperience] = dto.totalExperience
                it[currentLevel] = dto.currentLevel
                it[isUnlocked] = dto.isUnlocked
                it[lastUpdated] = dto.lastUpdated
                it[updatedAt] = now
            }
        } else {
            val serverUpdated = existing[EcosystemStatesTable.updatedAt]
            val clientUpdated = parseClientTimestamp(dto.updatedAt)

            if (clientUpdated != null && clientUpdated.isAfter(serverUpdated)) {
                EcosystemStatesTable.update({
                    (EcosystemStatesTable.id eq dto.id) and (EcosystemStatesTable.userId eq uid)
                }) {
                    it[category] = dto.category
                    it[totalExperience] = dto.totalExperience
                    it[currentLevel] = dto.currentLevel
                    it[isUnlocked] = dto.isUnlocked
                    it[lastUpdated] = dto.lastUpdated
                    it[updatedAt] = now
                }
            }
        }
    }
}

private fun upsertMarineCreatures(items: List<MarineCreatureDto>, uid: String, now: LocalDateTime) {
    for (dto in items) {
        val existing = MarineCreaturesTable.selectAll()
            .where { (MarineCreaturesTable.id eq dto.id) and (MarineCreaturesTable.userId eq uid) }
            .singleOrNull()

        if (existing == null) {
            MarineCreaturesTable.insert {
                it[id] = dto.id
                it[userId] = uid
                it[ecosystemId] = dto.ecosystemId
                it[category] = dto.category
                it[species] = dto.species
                it[nickname] = dto.nickname
                it[unlockedAtLevel] = dto.unlockedAtLevel
                it[experience] = dto.experience
                it[creatureLevel] = dto.creatureLevel
                it[unlockedAt] = dto.unlockedAt
                it[updatedAt] = now
            }
        } else {
            val serverUpdated = existing[MarineCreaturesTable.updatedAt]
            val clientUpdated = parseClientTimestamp(dto.updatedAt)

            if (clientUpdated != null && clientUpdated.isAfter(serverUpdated)) {
                MarineCreaturesTable.update({
                    (MarineCreaturesTable.id eq dto.id) and (MarineCreaturesTable.userId eq uid)
                }) {
                    it[ecosystemId] = dto.ecosystemId
                    it[category] = dto.category
                    it[species] = dto.species
                    it[nickname] = dto.nickname
                    it[unlockedAtLevel] = dto.unlockedAtLevel
                    it[experience] = dto.experience
                    it[creatureLevel] = dto.creatureLevel
                    it[unlockedAt] = dto.unlockedAt
                    it[updatedAt] = now
                }
            }
        }
    }
}

// ── PULL helpers ────────────────────────────────────────────

private fun pullWorkBlocks(uid: String, since: LocalDateTime?): List<WorkBlockDto> {
    val query = WorkBlocksTable.selectAll()
        .where { WorkBlocksTable.userId eq uid }
    if (since != null) {
        query.andWhere { WorkBlocksTable.updatedAt greater since }
    }
    return query.map { row ->
        WorkBlockDto(
            id = row[WorkBlocksTable.id],
            name = row[WorkBlocksTable.name],
            color = row[WorkBlocksTable.color],
            icon = row[WorkBlocksTable.icon],
            recurrenceJson = row[WorkBlocksTable.recurrenceJson],
            isActive = row[WorkBlocksTable.isActive],
            updatedAt = row[WorkBlocksTable.updatedAt].toUtcIso(),
            isDeleted = row[WorkBlocksTable.isDeleted]
        )
    }
}

private fun pullBlockCategories(uid: String, since: LocalDateTime?): List<BlockCategoryDto> {
    val query = BlockCategoriesTable.selectAll()
        .where { BlockCategoriesTable.userId eq uid }
    if (since != null) {
        query.andWhere { BlockCategoriesTable.updatedAt greater since }
    }
    return query.map { row ->
        BlockCategoryDto(
            blockId = row[BlockCategoriesTable.blockId],
            category = row[BlockCategoriesTable.category],
            updatedAt = row[BlockCategoriesTable.updatedAt].toUtcIso()
        )
    }
}

private fun pullDayTasks(uid: String, since: LocalDateTime?): List<DayTaskDto> {
    val query = DayTasksTable.selectAll()
        .where { DayTasksTable.userId eq uid }
    if (since != null) {
        query.andWhere { DayTasksTable.updatedAt greater since }
    }
    return query.map { row ->
        DayTaskDto(
            id = row[DayTasksTable.id],
            blockId = row[DayTasksTable.blockId],
            title = row[DayTasksTable.title],
            scheduleType = row[DayTasksTable.scheduleType],
            date = row[DayTasksTable.date],
            time = row[DayTasksTable.time],
            recurrence = row[DayTasksTable.recurrence],
            status = row[DayTasksTable.status],
            completedAt = row[DayTasksTable.completedAt],
            postponedTo = row[DayTasksTable.postponedTo],
            sourceTaskId = row[DayTasksTable.sourceTaskId],
            hasBeenRewarded = row[DayTasksTable.hasBeenRewarded],
            notificationsEnabled = row[DayTasksTable.notificationsEnabled],
            targetCount = row[DayTasksTable.targetCount],
            currentCount = row[DayTasksTable.currentCount],
            notes = row[DayTasksTable.notes],
            timerDurationMinutes = row[DayTasksTable.timerDurationMinutes],
            isPriority = row[DayTasksTable.isPriority],
            updatedAt = row[DayTasksTable.updatedAt].toUtcIso(),
            isDeleted = row[DayTasksTable.isDeleted]
        )
    }
}

private fun pullRecurringTaskDefs(uid: String, since: LocalDateTime?): List<RecurringTaskDefDto> {
    val query = RecurringTaskDefsTable.selectAll()
        .where { RecurringTaskDefsTable.userId eq uid }
    if (since != null) {
        query.andWhere { RecurringTaskDefsTable.updatedAt greater since }
    }
    return query.map { row ->
        RecurringTaskDefDto(
            id = row[RecurringTaskDefsTable.id],
            blockId = row[RecurringTaskDefsTable.blockId],
            title = row[RecurringTaskDefsTable.title],
            time = row[RecurringTaskDefsTable.time],
            recurrence = row[RecurringTaskDefsTable.recurrence],
            isActive = row[RecurringTaskDefsTable.isActive],
            notificationsEnabled = row[RecurringTaskDefsTable.notificationsEnabled],
            targetCount = row[RecurringTaskDefsTable.targetCount],
            noteTemplate = row[RecurringTaskDefsTable.noteTemplate],
            timerDurationMinutes = row[RecurringTaskDefsTable.timerDurationMinutes],
            isPriority = row[RecurringTaskDefsTable.isPriority],
            updatedAt = row[RecurringTaskDefsTable.updatedAt].toUtcIso(),
            isDeleted = row[RecurringTaskDefsTable.isDeleted]
        )
    }
}

private fun pullDaySummaries(uid: String, since: LocalDateTime?): List<DaySummaryDto> {
    val query = DaySummariesTable.selectAll()
        .where { DaySummariesTable.userId eq uid }
    if (since != null) {
        query.andWhere { DaySummariesTable.updatedAt greater since }
    }
    return query.map { row ->
        DaySummaryDto(
            id = row[DaySummariesTable.id],
            date = row[DaySummariesTable.date],
            score = row[DaySummariesTable.score],
            tasksTotal = row[DaySummariesTable.tasksTotal],
            tasksCompleted = row[DaySummariesTable.tasksCompleted],
            streakDay = row[DaySummariesTable.streakDay],
            feedbackMessage = row[DaySummariesTable.feedbackMessage],
            updatedAt = row[DaySummariesTable.updatedAt].toUtcIso()
        )
    }
}

private fun pullEcosystemStates(uid: String, since: LocalDateTime?): List<EcosystemStateDto> {
    val query = EcosystemStatesTable.selectAll()
        .where { EcosystemStatesTable.userId eq uid }
    if (since != null) {
        query.andWhere { EcosystemStatesTable.updatedAt greater since }
    }
    return query.map { row ->
        EcosystemStateDto(
            id = row[EcosystemStatesTable.id],
            category = row[EcosystemStatesTable.category],
            totalExperience = row[EcosystemStatesTable.totalExperience],
            currentLevel = row[EcosystemStatesTable.currentLevel],
            isUnlocked = row[EcosystemStatesTable.isUnlocked],
            lastUpdated = row[EcosystemStatesTable.lastUpdated],
            updatedAt = row[EcosystemStatesTable.updatedAt].toUtcIso()
        )
    }
}

private fun pullMarineCreatures(uid: String, since: LocalDateTime?): List<MarineCreatureDto> {
    val query = MarineCreaturesTable.selectAll()
        .where { MarineCreaturesTable.userId eq uid }
    if (since != null) {
        query.andWhere { MarineCreaturesTable.updatedAt greater since }
    }
    return query.map { row ->
        MarineCreatureDto(
            id = row[MarineCreaturesTable.id],
            ecosystemId = row[MarineCreaturesTable.ecosystemId],
            category = row[MarineCreaturesTable.category],
            species = row[MarineCreaturesTable.species],
            nickname = row[MarineCreaturesTable.nickname],
            unlockedAtLevel = row[MarineCreaturesTable.unlockedAtLevel],
            experience = row[MarineCreaturesTable.experience],
            creatureLevel = row[MarineCreaturesTable.creatureLevel],
            unlockedAt = row[MarineCreaturesTable.unlockedAt],
            updatedAt = row[MarineCreaturesTable.updatedAt].toUtcIso()
        )
    }
}
