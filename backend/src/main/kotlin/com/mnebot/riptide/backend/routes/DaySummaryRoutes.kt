package com.mnebot.riptide.backend.routes

import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.DaySummariesTable
import com.mnebot.riptide.backend.models.DaySummaryDto
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

fun Route.daySummaryRoutes() {
    authenticate("auth-jwt") {
        route("/api/summaries") {

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val from = call.request.queryParameters["from"]
                val to = call.request.queryParameters["to"]
                val since = call.request.queryParameters["updatedSince"]

                val summaries = dbQuery {
                    val query = DaySummariesTable.selectAll()
                        .where { DaySummariesTable.userId eq uid }

                    if (from != null) {
                        query.andWhere { DaySummariesTable.date greaterEq from }
                    }
                    if (to != null) {
                        query.andWhere { DaySummariesTable.date lessEq to }
                    }
                    if (since != null) {
                        val sinceDate = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME)
                        query.andWhere { DaySummariesTable.updatedAt greater sinceDate }
                    }

                    query.map { it.toDaySummaryDto() }
                }

                call.respond(summaries)
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!

                val summary = dbQuery {
                    DaySummariesTable.selectAll()
                        .where { (DaySummariesTable.id eq id) and (DaySummariesTable.userId eq uid) }
                        .singleOrNull()?.toDaySummaryDto()
                } ?: throw NotFoundException("Summary not found")

                call.respond(summary)
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val dto = call.receive<DaySummaryDto>()

                Validation.validateDaySummary(dto.id, dto.date, dto.feedbackMessage)

                dbQuery {
                    DaySummariesTable.insert {
                        it[id] = dto.id
                        it[userId] = uid
                        it[date] = dto.date
                        it[score] = dto.score
                        it[tasksTotal] = dto.tasksTotal
                        it[tasksCompleted] = dto.tasksCompleted
                        it[streakDay] = dto.streakDay
                        it[feedbackMessage] = dto.feedbackMessage
                        it[updatedAt] = LocalDateTime.now()
                    }
                }

                call.respond(HttpStatusCode.Created, dto.copy(updatedAt = LocalDateTime.now().toString()))
            }

            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!
                val dto = call.receive<DaySummaryDto>()

                Validation.validateDaySummary(dto.id, dto.date, dto.feedbackMessage)

                val updated = dbQuery {
                    DaySummariesTable.update({
                        (DaySummariesTable.id eq id) and (DaySummariesTable.userId eq uid)
                    }) {
                        it[date] = dto.date
                        it[score] = dto.score
                        it[tasksTotal] = dto.tasksTotal
                        it[tasksCompleted] = dto.tasksCompleted
                        it[streakDay] = dto.streakDay
                        it[feedbackMessage] = dto.feedbackMessage
                        it[updatedAt] = LocalDateTime.now()
                    }
                }

                if (updated == 0) throw NotFoundException("Summary not found")
                call.respond(HttpStatusCode.OK, dto.copy(updatedAt = LocalDateTime.now().toString()))
            }
        }
    }
}

private fun ResultRow.toDaySummaryDto() = DaySummaryDto(
    id = this[DaySummariesTable.id],
    date = this[DaySummariesTable.date],
    score = this[DaySummariesTable.score],
    tasksTotal = this[DaySummariesTable.tasksTotal],
    tasksCompleted = this[DaySummariesTable.tasksCompleted],
    streakDay = this[DaySummariesTable.streakDay],
    feedbackMessage = this[DaySummariesTable.feedbackMessage],
    updatedAt = this[DaySummariesTable.updatedAt].toString()
)
