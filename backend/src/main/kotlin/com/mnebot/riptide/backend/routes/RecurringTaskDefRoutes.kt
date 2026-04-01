package com.mnebot.riptide.backend.routes

import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.RecurringTaskDefsTable
import com.mnebot.riptide.backend.models.RecurringTaskDefDto
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

fun Route.recurringTaskDefRoutes() {
    authenticate("auth-jwt") {
        route("/api/recurring-defs") {

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val since = call.request.queryParameters["updatedSince"]
                val includeDeleted = call.request.queryParameters["includeDeleted"] == "true"

                val defs = dbQuery {
                    val query = RecurringTaskDefsTable.selectAll()
                        .where { RecurringTaskDefsTable.userId eq uid }

                    if (!includeDeleted) {
                        query.andWhere { RecurringTaskDefsTable.isDeleted eq false }
                    }
                    if (since != null) {
                        val sinceDate = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME)
                        query.andWhere { RecurringTaskDefsTable.updatedAt greater sinceDate }
                    }

                    query.map { it.toRecurringTaskDefDto() }
                }

                call.respond(defs)
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!

                val def = dbQuery {
                    RecurringTaskDefsTable.selectAll()
                        .where { (RecurringTaskDefsTable.id eq id) and (RecurringTaskDefsTable.userId eq uid) }
                        .singleOrNull()?.toRecurringTaskDefDto()
                } ?: throw NotFoundException("Recurring task def not found")

                call.respond(def)
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val dto = call.receive<RecurringTaskDefDto>()

                Validation.validateRecurringTaskDef(dto.id, dto.blockId, dto.title, dto.time, dto.recurrence)

                dbQuery {
                    RecurringTaskDefsTable.insert {
                        it[id] = dto.id
                        it[userId] = uid
                        it[blockId] = dto.blockId
                        it[title] = dto.title
                        it[time] = dto.time
                        it[recurrence] = dto.recurrence
                        it[isActive] = dto.isActive
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
                val dto = call.receive<RecurringTaskDefDto>()

                Validation.validateRecurringTaskDef(dto.id, dto.blockId, dto.title, dto.time, dto.recurrence)

                val updated = dbQuery {
                    RecurringTaskDefsTable.update({
                        (RecurringTaskDefsTable.id eq id) and (RecurringTaskDefsTable.userId eq uid)
                    }) {
                        it[blockId] = dto.blockId
                        it[title] = dto.title
                        it[time] = dto.time
                        it[recurrence] = dto.recurrence
                        it[isActive] = dto.isActive
                        it[notificationsEnabled] = dto.notificationsEnabled
                        it[updatedAt] = LocalDateTime.now()
                    }
                }

                if (updated == 0) throw NotFoundException("Recurring task def not found")
                call.respond(HttpStatusCode.OK, dto.copy(updatedAt = LocalDateTime.now().toString()))
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!

                val updated = dbQuery {
                    RecurringTaskDefsTable.update({
                        (RecurringTaskDefsTable.id eq id) and (RecurringTaskDefsTable.userId eq uid)
                    }) {
                        it[isDeleted] = true
                        it[updatedAt] = LocalDateTime.now()
                    }
                }

                if (updated == 0) throw NotFoundException("Recurring task def not found")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun ResultRow.toRecurringTaskDefDto() = RecurringTaskDefDto(
    id = this[RecurringTaskDefsTable.id],
    blockId = this[RecurringTaskDefsTable.blockId],
    title = this[RecurringTaskDefsTable.title],
    time = this[RecurringTaskDefsTable.time],
    recurrence = this[RecurringTaskDefsTable.recurrence],
    isActive = this[RecurringTaskDefsTable.isActive],
    notificationsEnabled = this[RecurringTaskDefsTable.notificationsEnabled],
    updatedAt = this[RecurringTaskDefsTable.updatedAt].toString(),
    isDeleted = this[RecurringTaskDefsTable.isDeleted]
)
