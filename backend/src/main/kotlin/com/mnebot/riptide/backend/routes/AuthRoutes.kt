package com.mnebot.riptide.backend.routes

import com.auth0.jwt.exceptions.JWTVerificationException
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.mnebot.riptide.backend.db.DatabaseFactory.dbQuery
import com.mnebot.riptide.backend.db.tables.RefreshTokensTable
import com.mnebot.riptide.backend.db.tables.UsersTable
import com.mnebot.riptide.backend.db.tables.WorkBlocksTable
import com.mnebot.riptide.backend.models.*
import com.mnebot.riptide.backend.plugins.JwtConfig
import com.mnebot.riptide.backend.plugins.RateLimitException
import com.mnebot.riptide.backend.plugins.RateLimiters
import com.mnebot.riptide.backend.plugins.UnauthorizedException
import com.mnebot.riptide.backend.plugins.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

fun Route.authRoutes() {
    route("/auth") {

        // POST /auth/google — Verify Google ID token, create/get user, return JWT pair
        post("/google") {
            val clientIp = call.request.local.remoteAddress
            if (!RateLimiters.auth.tryAcquire(clientIp)) throw RateLimitException()

            val request = call.receive<GoogleAuthRequest>()

            if (request.idToken.isBlank() || request.idToken.length > 4096) {
                throw UnauthorizedException("Invalid token format")
            }

            val googleClientId = call.application.environment.config
                .property("google.clientId").getString()

            val verifier = GoogleIdTokenVerifier.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance()
            )
                .setAudience(listOf(googleClientId))
                .build()

            val googleToken = verifier.verify(request.idToken)
                ?: throw UnauthorizedException("Invalid Google ID token")

            val payload = googleToken.payload
            val googleId = payload.subject
                ?: throw UnauthorizedException("Missing subject in token")
            val email = payload.email
                ?: throw UnauthorizedException("Missing email in token")
            val name = payload["name"] as? String
            val pictureUrl = payload["picture"] as? String

            // Find or create user
            val user = dbQuery {
                val existing = UsersTable
                    .selectAll()
                    .where { UsersTable.googleId eq googleId }
                    .singleOrNull()

                if (existing != null) {
                    UsersTable.update({ UsersTable.googleId eq googleId }) {
                        if (name != null) it[displayName] = name
                        if (pictureUrl != null) it[avatarUrl] = pictureUrl
                        it[updatedAt] = LocalDateTime.now()
                    }
                    UserResponse(
                        id = existing[UsersTable.id],
                        email = existing[UsersTable.email],
                        displayName = name ?: existing[UsersTable.displayName],
                        avatarUrl = pictureUrl ?: existing[UsersTable.avatarUrl]
                    )
                } else {
                    val userId = UUID.randomUUID().toString()
                    UsersTable.insert {
                        it[id] = userId
                        it[UsersTable.email] = email
                        it[displayName] = name
                        it[UsersTable.googleId] = googleId
                        it[avatarUrl] = pictureUrl
                        it[createdAt] = LocalDateTime.now()
                        it[updatedAt] = LocalDateTime.now()
                    }
                    UserResponse(
                        id = userId,
                        email = email,
                        displayName = name,
                        avatarUrl = pictureUrl
                    )
                }
            }

            // Check if user already has data on the server
            val hasData = dbQuery {
                WorkBlocksTable.selectAll()
                    .where { (WorkBlocksTable.userId eq user.id) and (WorkBlocksTable.isDeleted eq false) }
                    .limit(1).count() > 0
            }

            val accessToken = JwtConfig.generateAccessToken(user.id, user.email)
            val refreshToken = JwtConfig.generateRefreshToken(user.id)

            // Store refresh token JTI in DB
            storeRefreshToken(refreshToken, user.id)

            call.respond(TokenResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = JwtConfig.expirationSeconds,
                user = user,
                hasExistingData = hasData
            ))
        }

        // POST /auth/refresh — Rotate refresh token: verify → revoke old → issue new pair
        post("/refresh") {
            val clientIp = call.request.local.remoteAddress
            if (!RateLimiters.auth.tryAcquire(clientIp)) throw RateLimitException()

            val request = call.receive<RefreshRequest>()

            if (request.refreshToken.isBlank() || request.refreshToken.length > 4096) {
                throw UnauthorizedException("Invalid token format")
            }

            val decoded = try {
                JwtConfig.verifyRefreshToken(request.refreshToken)
            } catch (e: JWTVerificationException) {
                throw UnauthorizedException("Invalid or expired refresh token")
            }

            val userId = decoded.getClaim("userId").asString()
                ?: throw UnauthorizedException("Malformed refresh token")
            val jti = decoded.id
                ?: throw UnauthorizedException("Malformed refresh token")

            // Check if token exists and is not revoked
            val tokenStatus = dbQuery {
                RefreshTokensTable.selectAll()
                    .where { RefreshTokensTable.jti eq jti }
                    .singleOrNull()
            }

            if (tokenStatus == null) {
                // Token not in DB — possibly crafted
                throw UnauthorizedException("Unknown refresh token")
            }

            if (tokenStatus[RefreshTokensTable.isRevoked]) {
                // THEFT DETECTION: a revoked token was reused
                // Revoke ALL tokens for this user as a safety measure
                call.application.environment.log.warn(
                    "Refresh token reuse detected for user $userId (jti=$jti). Revoking all tokens."
                )
                dbQuery {
                    RefreshTokensTable.update({ RefreshTokensTable.userId eq userId }) {
                        it[isRevoked] = true
                    }
                }
                throw UnauthorizedException("Security alert: all sessions revoked. Please sign in again.")
            }

            // Verify user still exists
            val email = dbQuery {
                UsersTable.selectAll()
                    .where { UsersTable.id eq userId }
                    .singleOrNull()
                    ?.get(UsersTable.email)
            } ?: throw UnauthorizedException("User not found")

            // Revoke the old token
            dbQuery {
                RefreshTokensTable.update({ RefreshTokensTable.jti eq jti }) {
                    it[isRevoked] = true
                }
            }

            // Issue new pair
            val newAccessToken = JwtConfig.generateAccessToken(userId, email)
            val newRefreshToken = JwtConfig.generateRefreshToken(userId)

            storeRefreshToken(newRefreshToken, userId)

            call.respond(RefreshResponse(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                expiresIn = JwtConfig.expirationSeconds
            ))
        }

        // POST /auth/logout — Revoke all refresh tokens for the user
        authenticate("auth-jwt") {
            post("/logout") {
                val principal = call.principal<JWTPrincipal>()!!
                val uid = principal.userId

                val revoked = dbQuery {
                    RefreshTokensTable.update({ RefreshTokensTable.userId eq uid }) {
                        it[isRevoked] = true
                    }
                }

                call.application.environment.log.info("Logout: revoked $revoked refresh tokens for user $uid")
                call.respond(mapOf("status" to "ok", "revokedTokens" to revoked))
            }
        }
    }
}

/** Store a refresh token's JTI in the database */
private suspend fun storeRefreshToken(refreshToken: String, userId: String) {
    val decoded = JwtConfig.verifyRefreshToken(refreshToken)
    val jti = decoded.id
    val expiresAt = LocalDateTime.now().plusSeconds(JwtConfig.refreshExpirationSeconds)

    dbQuery {
        RefreshTokensTable.insert {
            it[RefreshTokensTable.jti] = jti
            it[RefreshTokensTable.userId] = userId
            it[isRevoked] = false
            it[createdAt] = LocalDateTime.now()
            it[RefreshTokensTable.expiresAt] = expiresAt
        }
    }
}
