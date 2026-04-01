package com.mnebot.riptide.backend.routes

import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.DayTasksTable
import com.mnebot.riptide.backend.models.DayTaskDto
import com.mnebot.riptide.backend.plugins.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun Route.dayTaskRoutes() {
    authenticate("auth-jwt") {
        route("/api/tasks") {

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val date = call.request.queryParameters["date"]
                val since = call.request.queryParameters["updatedSince"]
                val includeDeleted = call.request.queryParameters["includeDeleted"] == "true"

                val tasks = dbQuery {
                    val query = DayTasksTable.selectAll()
                        .where { DayTasksTable.userId eq uid }

                    if (!includeDeleted) {
                        query.andWhere { DayTasksTable.isDeleted eq false }
                    }
                    if (date != null) {
                        query.andWhere { DayTasksTable.date eq date }
                    }
                    if (since != null) {
                        val sinceDate = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME)
                        query.andWhere { DayTasksTable.updatedAt greater sinceDate }
                    }

                    query.map { it.toDayTaskDto() }
                }

                call.respond(tasks)
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!

                val task = dbQuery {
                    DayTasksTable.selectAll()
                        .where { (DayTasksTable.id eq id) and (DayTasksTable.userId eq uid) }
                        .singleOrNull()?.toDayTaskDto()
                } ?: throw NotFoundException("Task not found")

                call.respond(task)
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val dto = call.receive<DayTaskDto>()

                Validation.validateDayTask(
                    dto.id, dto.title, dto.scheduleType, dto.blockId,
                    dto.date, dto.time, dto.status,
                    dto.completedAt, dto.postponedTo, dto.sourceTaskId
                )

                dbQuery {
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
                        it[updatedAt] = LocalDateTime.now()
                        it[isDeleted] = false
                    }
                }

                call.respond(HttpStatusCode.Created, dto.copy(updatedAt = LocalDateTime.now().toString()))
            }

            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!
                val dto = call.receive<DayTaskDto>()

                Validation.validateDayTask(
                    dto.id, dto.title, dto.scheduleType, dto.blockId,
                    dto.date, dto.time, dto.status,
                    dto.completedAt, dto.postponedTo, dto.sourceTaskId
                )

                val updated = dbQuery {
                    DayTasksTable.update({
                        (DayTasksTable.id eq id) and (DayTasksTable.userId eq uid)
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
                        it[hasBeenRewarded] = dto.hasBeenRewarded
                        it[notificationsEnabled] = dto.notificationsEnabled
                        it[updatedAt] = LocalDateTime.now()
                    }
                }

                if (updated == 0) throw NotFoundException("Task not found")
                call.respond(HttpStatusCode.OK, dto.copy(updatedAt = LocalDateTime.now().toString()))
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!

                val updated = dbQuery {
                    DayTasksTable.update({
                        (DayTasksTable.id eq id) and (DayTasksTable.userId eq uid)
                    }) {
                        it[isDeleted] = true
                        it[updatedAt] = LocalDateTime.now()
                    }
                }

                if (updated == 0) throw NotFoundException("Task not found")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun ResultRow.toDayTaskDto() = DayTaskDto(
    id = this[DayTasksTable.id],
    blockId = this[DayTasksTable.blockId],
    title = this[DayTasksTable.title],
    scheduleType = this[DayTasksTable.scheduleType],
    date = this[DayTasksTable.date],
    time = this[DayTasksTable.time],
    recurrence = this[DayTasksTable.recurrence],
    status = this[DayTasksTable.status],
    completedAt = this[DayTasksTable.completedAt],
    postponedTo = this[DayTasksTable.postponedTo],
    sourceTaskId = this[DayTasksTable.sourceTaskId],
    hasBeenRewarded = this[DayTasksTable.hasBeenRewarded],
    notificationsEnabled = this[DayTasksTable.notificationsEnabled],
    updatedAt = this[DayTasksTable.updatedAt].toString(),
    isDeleted = this[DayTasksTable.isDeleted]
)
