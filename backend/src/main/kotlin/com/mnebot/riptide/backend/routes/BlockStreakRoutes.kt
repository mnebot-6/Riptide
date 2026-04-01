package com.mnebot.riptide.backend.routes

import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.BlockStreaksTable
import com.mnebot.riptide.backend.models.BlockStreakDto
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

fun Route.blockStreakRoutes() {
    authenticate("auth-jwt") {
        route("/api/streaks") {

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val since = call.request.queryParameters["updatedSince"]

                val streaks = dbQuery {
                    val query = BlockStreaksTable.selectAll()
                        .where { BlockStreaksTable.userId eq uid }

                    if (since != null) {
                        val sinceDate = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME)
                        query.andWhere { BlockStreaksTable.updatedAt greater sinceDate }
                    }

                    query.map { it.toBlockStreakDto() }
                }

                call.respond(streaks)
            }

            get("/{blockId}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val blockId = call.parameters["blockId"]!!

                val streak = dbQuery {
                    BlockStreaksTable.selectAll()
                        .where { (BlockStreaksTable.blockId eq blockId) and (BlockStreaksTable.userId eq uid) }
                        .singleOrNull()?.toBlockStreakDto()
                } ?: throw NotFoundException("Streak not found")

                call.respond(streak)
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val dto = call.receive<BlockStreakDto>()

                Validation.validateBlockStreak(dto.blockId, dto.lastActiveDate)

                dbQuery {
                    val exists = BlockStreaksTable.selectAll()
                        .where { (BlockStreaksTable.blockId eq dto.blockId) and (BlockStreaksTable.userId eq uid) }
                        .count() > 0

                    if (exists) {
                        BlockStreaksTable.update({
                            (BlockStreaksTable.blockId eq dto.blockId) and (BlockStreaksTable.userId eq uid)
                        }) {
                            it[currentStreak] = dto.currentStreak
                            it[longestStreak] = dto.longestStreak
                            it[lastActiveDate] = dto.lastActiveDate
                            it[updatedAt] = LocalDateTime.now()
                        }
                    } else {
                        BlockStreaksTable.insert {
                            it[blockId] = dto.blockId
                            it[userId] = uid
                            it[currentStreak] = dto.currentStreak
                            it[longestStreak] = dto.longestStreak
                            it[lastActiveDate] = dto.lastActiveDate
                            it[updatedAt] = LocalDateTime.now()
                        }
                    }
                }

                call.respond(HttpStatusCode.OK, dto.copy(updatedAt = LocalDateTime.now().toString()))
            }
        }
    }
}

private fun ResultRow.toBlockStreakDto() = BlockStreakDto(
    blockId = this[BlockStreaksTable.blockId],
    currentStreak = this[BlockStreaksTable.currentStreak],
    longestStreak = this[BlockStreaksTable.longestStreak],
    lastActiveDate = this[BlockStreaksTable.lastActiveDate],
    updatedAt = this[BlockStreaksTable.updatedAt].toString()
)
