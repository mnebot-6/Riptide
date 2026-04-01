package com.mnebot.riptide.backend.routes

import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.EcosystemStatesTable
import com.mnebot.riptide.backend.models.EcosystemStateDto
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

fun Route.ecosystemStateRoutes() {
    authenticate("auth-jwt") {
        route("/api/ecosystem-states") {

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val since = call.request.queryParameters["updatedSince"]

                val states = dbQuery {
                    val query = EcosystemStatesTable.selectAll()
                        .where { EcosystemStatesTable.userId eq uid }

                    if (since != null) {
                        val sinceDate = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME)
                        query.andWhere { EcosystemStatesTable.updatedAt greater sinceDate }
                    }

                    query.map { it.toEcosystemStateDto() }
                }

                call.respond(states)
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val id = call.parameters["id"]!!

                val state = dbQuery {
                    EcosystemStatesTable.selectAll()
                        .where { (EcosystemStatesTable.id eq id) and (EcosystemStatesTable.userId eq uid) }
                        .singleOrNull()?.toEcosystemStateDto()
                } ?: throw NotFoundException("Ecosystem state not found")

                call.respond(state)
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val dto = call.receive<EcosystemStateDto>()

                Validation.validateEcosystemState(dto.id, dto.category, dto.lastUpdated)

                dbQuery {
                    val exists = EcosystemStatesTable.selectAll()
                        .where { (EcosystemStatesTable.id eq dto.id) and (EcosystemStatesTable.userId eq uid) }
                        .count() > 0

                    if (exists) {
                        EcosystemStatesTable.update({
                            (EcosystemStatesTable.id eq dto.id) and (EcosystemStatesTable.userId eq uid)
                        }) {
                            it[category] = dto.category
                            it[totalExperience] = dto.totalExperience
                            it[currentLevel] = dto.currentLevel
                            it[isUnlocked] = dto.isUnlocked
                            it[lastUpdated] = dto.lastUpdated
                            it[updatedAt] = LocalDateTime.now()
                        }
                    } else {
                        EcosystemStatesTable.insert {
                            it[id] = dto.id
                            it[userId] = uid
                            it[category] = dto.category
                            it[totalExperience] = dto.totalExperience
                            it[currentLevel] = dto.currentLevel
                            it[isUnlocked] = dto.isUnlocked
                            it[lastUpdated] = dto.lastUpdated
                            it[updatedAt] = LocalDateTime.now()
                        }
                    }
                }

                call.respond(HttpStatusCode.OK, dto.copy(updatedAt = LocalDateTime.now().toString()))
            }
        }
    }
}

private fun ResultRow.toEcosystemStateDto() = EcosystemStateDto(
    id = this[EcosystemStatesTable.id],
    category = this[EcosystemStatesTable.category],
    totalExperience = this[EcosystemStatesTable.totalExperience],
    currentLevel = this[EcosystemStatesTable.currentLevel],
    isUnlocked = this[EcosystemStatesTable.isUnlocked],
    lastUpdated = this[EcosystemStatesTable.lastUpdated],
    updatedAt = this[EcosystemStatesTable.updatedAt].toString()
)
