package com.mnebot.riptide.backend.routes

import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.WorkBlocksTable
import com.mnebot.riptide.backend.models.WorkBlockDto
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

fun Route.workBlockRoutes() {
    authenticate("auth-jwt") {
        route("/api/blocks") {

            // GET /api/blocks?updatedSince=ISO&includeDeleted=true
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val since = call.request.queryParameters["updatedSince"]
                val includeDeleted = call.request.queryParameters["includeDeleted"] == "true"

                val blocks = dbQuery {
                    val query = WorkBlocksTable.selectAll()
                        .where { WorkBlocksTable.userId eq uid }

                    if (!includeDeleted) {
                        query.andWhere { WorkBlocksTable.isDeleted eq false }
                    }
                    if (since != null) {
                        val sinceDate = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME)
                        query.andWhere { WorkBlocksTable.updatedAt greater sinceDate }
                    }

                    query.map { it.toWorkBlockDto() }
                }

                call.respond(blocks)
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!

                val block = dbQuery {
                    WorkBlocksTable.selectAll()
                        .where { (WorkBlocksTable.id eq id) and (WorkBlocksTable.userId eq uid) }
                        .singleOrNull()?.toWorkBlockDto()
                } ?: throw NotFoundException("Block not found")

                call.respond(block)
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val dto = call.receive<WorkBlockDto>()

                Validation.validateWorkBlock(dto.id, dto.name, dto.color, dto.icon, dto.recurrenceJson)

                dbQuery {
                    WorkBlocksTable.insert {
                        it[id] = dto.id
                        it[userId] = uid
                        it[name] = dto.name
                        it[color] = dto.color
                        it[icon] = dto.icon
                        it[recurrenceJson] = dto.recurrenceJson
                        it[isActive] = dto.isActive
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
                val dto = call.receive<WorkBlockDto>()

                Validation.validateWorkBlock(dto.id, dto.name, dto.color, dto.icon, dto.recurrenceJson)

                val updated = dbQuery {
                    WorkBlocksTable.update({
                        (WorkBlocksTable.id eq id) and (WorkBlocksTable.userId eq uid)
                    }) {
                        it[name] = dto.name
                        it[color] = dto.color
                        it[icon] = dto.icon
                        it[recurrenceJson] = dto.recurrenceJson
                        it[isActive] = dto.isActive
                        it[updatedAt] = LocalDateTime.now()
                    }
                }

                if (updated == 0) throw NotFoundException("Block not found")
                call.respond(HttpStatusCode.OK, dto.copy(updatedAt = LocalDateTime.now().toString()))
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!

                val updated = dbQuery {
                    WorkBlocksTable.update({
                        (WorkBlocksTable.id eq id) and (WorkBlocksTable.userId eq uid)
                    }) {
                        it[isDeleted] = true
                        it[updatedAt] = LocalDateTime.now()
                    }
                }

                if (updated == 0) throw NotFoundException("Block not found")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/** Rate limit helper for API routes */
internal fun rateLimitApi(userId: String) {
    if (!RateLimiters.api.tryAcquire(userId)) {
        throw RateLimitException()
    }
}

private fun ResultRow.toWorkBlockDto() = WorkBlockDto(
    id = this[WorkBlocksTable.id],
    name = this[WorkBlocksTable.name],
    color = this[WorkBlocksTable.color],
    icon = this[WorkBlocksTable.icon],
    recurrenceJson = this[WorkBlocksTable.recurrenceJson],
    isActive = this[WorkBlocksTable.isActive],
    updatedAt = this[WorkBlocksTable.updatedAt].toString(),
    isDeleted = this[WorkBlocksTable.isDeleted]
)
