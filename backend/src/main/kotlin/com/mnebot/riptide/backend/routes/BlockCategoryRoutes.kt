package com.mnebot.riptide.backend.routes

import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.BlockCategoriesTable
import com.mnebot.riptide.backend.models.BlockCategoryDto
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

fun Route.blockCategoryRoutes() {
    authenticate("auth-jwt") {
        route("/api/block-categories") {

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val blockId = call.request.queryParameters["blockId"]
                val since = call.request.queryParameters["updatedSince"]

                val categories = dbQuery {
                    val query = BlockCategoriesTable.selectAll()
                        .where { BlockCategoriesTable.userId eq uid }

                    if (blockId != null) {
                        query.andWhere { BlockCategoriesTable.blockId eq blockId }
                    }
                    if (since != null) {
                        val sinceDate = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME)
                        query.andWhere { BlockCategoriesTable.updatedAt greater sinceDate }
                    }

                    query.map { it.toBlockCategoryDto() }
                }

                call.respond(categories)
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId
                rateLimitApi(uid)
                val dtos = call.receive<List<BlockCategoryDto>>()

                if (dtos.isEmpty()) {
                    throw ValidationException("List must not be empty")
                }

                // Validate all DTOs have the same blockId
                val blockId = dtos.first().blockId
                if (dtos.any { it.blockId != blockId }) {
                    throw ValidationException("All categories in a batch must have the same blockId")
                }

                // Validate each entry
                dtos.forEach { Validation.validateBlockCategory(it.blockId, it.category) }

                dbQuery {
                    // Delete existing categories for this block
                    BlockCategoriesTable.deleteWhere {
                        (BlockCategoriesTable.blockId eq blockId) and (userId eq uid)
                    }

                    // Insert new ones
                    for (dto in dtos) {
                        BlockCategoriesTable.insert {
                            it[BlockCategoriesTable.blockId] = dto.blockId
                            it[category] = dto.category
                            it[userId] = uid
                            it[updatedAt] = LocalDateTime.now()
                        }
                    }
                }

                call.respond(HttpStatusCode.Created, dtos)
            }
        }
    }
}

private fun ResultRow.toBlockCategoryDto() = BlockCategoryDto(
    blockId = this[BlockCategoriesTable.blockId],
    category = this[BlockCategoriesTable.category],
    updatedAt = this[BlockCategoriesTable.updatedAt].toString()
)
