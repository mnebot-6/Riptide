package com.mnebot.riptide.backend.routes

import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.*
import com.mnebot.riptide.backend.plugins.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere

/**
 * Account lifecycle endpoints. Required by Google Play (since 2024) and GDPR Art. 17
 * ("right to erasure"): an authenticated user must be able to delete their account
 * and all associated data from a single in-app action.
 */
fun Route.accountRoutes() {
    authenticate("auth-jwt") {

        // DELETE /api/account — Erase user + all data + revoke refresh tokens.
        // Idempotent: returns 200 even if the user record is already gone.
        delete("/api/account") {
            val uid = call.principal<JWTPrincipal>()!!.userId

            dbQuery {
                // FK-safe order: dependents first, then refresh tokens, then user row.
                MarineCreaturesTable.deleteWhere { userId eq uid }
                EcosystemStatesTable.deleteWhere { userId eq uid }
                BlockStreaksTable.deleteWhere { userId eq uid }
                DaySummariesTable.deleteWhere { userId eq uid }
                DayTasksTable.deleteWhere { userId eq uid }
                RecurringTaskDefsTable.deleteWhere { userId eq uid }
                BlockCategoriesTable.deleteWhere { userId eq uid }
                WorkBlocksTable.deleteWhere { userId eq uid }
                RefreshTokensTable.deleteWhere { userId eq uid }
                UsersTable.deleteWhere { id eq uid }
            }

            call.application.environment.log.info("Account deleted: $uid")
            call.respond(HttpStatusCode.OK, mapOf("status" to "deleted"))
        }
    }
}
