package com.mnebot.riptide.backend.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.response.*
import java.util.*

object JwtConfig {
    lateinit var secret: String
    lateinit var issuer: String
    lateinit var audience: String
    var expirationSeconds: Long = 86400          // 24h
    var refreshExpirationSeconds: Long = 2592000 // 30d

    fun init(config: ApplicationConfig) {
        secret = config.property("jwt.secret").getString()
        issuer = config.property("jwt.issuer").getString()
        audience = config.property("jwt.audience").getString()
        expirationSeconds = config.property("jwt.expirationSeconds").getString().toLong()
        refreshExpirationSeconds = config.property("jwt.refreshExpirationSeconds").getString().toLong()

        // Startup validation — refuse to start with weak secret
        require(secret.length >= 32) {
            "JWT_SECRET must be at least 32 characters. Current: ${secret.length} chars. " +
            "Set a strong secret via the JWT_SECRET environment variable."
        }
        require(secret != "riptide-dev-secret-change-in-production-placeholder") {
            "JWT_SECRET is still set to the placeholder value. Configure a real secret."
        }
    }

    fun generateAccessToken(userId: String, email: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("type", "access")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + expirationSeconds * 1000))
            .sign(Algorithm.HMAC256(secret))

    fun generateRefreshToken(userId: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("type", "refresh")
            .withJWTId(UUID.randomUUID().toString()) // Unique per token for future revocation
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + refreshExpirationSeconds * 1000))
            .sign(Algorithm.HMAC256(secret))

    /** Verify a refresh token — throws JWTVerificationException on failure */
    fun verifyRefreshToken(token: String) =
        JWT.require(Algorithm.HMAC256(secret))
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("type", "refresh")
            .build()
            .verify(token)
}

fun Application.configureSecurity() {
    val appConfig = environment.config
    JwtConfig.init(appConfig)

    val jwtRealm = appConfig.property("jwt.realm").getString()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(JwtConfig.secret))
                    .withIssuer(JwtConfig.issuer)
                    .withAudience(JwtConfig.audience)
                    .withClaim("type", "access")
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId")?.asString()
                if (!userId.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Token is invalid or expired", 401)
                )
            }
        }
    }
}

/** Extension to extract userId from JWT principal inside authenticated routes */
val JWTPrincipal.userId: String
    get() = payload.getClaim("userId").asString()
