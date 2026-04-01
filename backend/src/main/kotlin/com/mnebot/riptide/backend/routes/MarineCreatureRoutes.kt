package com.mnebot.riptide.backend.routes

import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.MarineCreaturesTable
import com.mnebot.riptide.backend.models.MarineCreatureDto
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

fun Route.marineCreatureRoutes() {
    authenticate("auth-jwt") {
        route("/api/creatures") {

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val category = call.request.queryParameters["category"]
                val since = call.request.queryParameters["updatedSince"]

                val creatures = dbQuery {
                    val query = MarineCreaturesTable.selectAll()
                        .where { MarineCreaturesTable.userId eq uid }

                    if (category != null) {
                        query.andWhere { MarineCreaturesTable.category eq category }
                    }
                    if (since != null) {
                        val sinceDate = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME)
                        query.andWhere { MarineCreaturesTable.updatedAt greater sinceDate }
                    }

                    query.map { it.toMarineCreatureDto() }
                }

                call.respond(creatures)
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!

                val creature = dbQuery {
                    MarineCreaturesTable.selectAll()
                        .where { (MarineCreaturesTable.id eq id) and (MarineCreaturesTable.userId eq uid) }
                        .singleOrNull()?.toMarineCreatureDto()
                } ?: throw NotFoundException("Creature not found")

                call.respond(creature)
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val dto = call.receive<MarineCreatureDto>()

                Validation.validateMarineCreature(
                    dto.id, dto.ecosystemId, dto.category,
                    dto.species, dto.nickname, dto.unlockedAt
                )

                dbQuery {
                    val exists = MarineCreaturesTable.selectAll()
                        .where { (MarineCreaturesTable.id eq dto.id) and (MarineCreaturesTable.userId eq uid) }
                        .count() > 0

                    if (exists) {
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
                            it[updatedAt] = LocalDateTime.now()
                        }
                    } else {
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
                            it[updatedAt] = LocalDateTime.now()
                        }
                    }
                }

                call.respond(HttpStatusCode.OK, dto.copy(updatedAt = LocalDateTime.now().toString()))
            }
        }
    }
}

private fun ResultRow.toMarineCreatureDto() = MarineCreatureDto(
    id = this[MarineCreaturesTable.id],
    ecosystemId = this[MarineCreaturesTable.ecosystemId],
    category = this[MarineCreaturesTable.category],
    species = this[MarineCreaturesTable.species],
    nickname = this[MarineCreaturesTable.nickname],
    unlockedAtLevel = this[MarineCreaturesTable.unlockedAtLevel],
    experience = this[MarineCreaturesTable.experience],
    creatureLevel = this[MarineCreaturesTable.creatureLevel],
    unlockedAt = this[MarineCreaturesTable.unlockedAt],
    updatedAt = this[MarineCreaturesTable.updatedAt].toString()
)
