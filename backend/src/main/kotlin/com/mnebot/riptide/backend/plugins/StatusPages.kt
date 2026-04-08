package com.mnebot.riptide.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String, val status: Int)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.application.environment.log.warn("Validation error [${call.request.local.method.value} ${call.request.local.uri}]: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(cause.message ?: "Validation error", 400)
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.application.environment.log.warn("Bad request (IllegalArgument) [${call.request.local.method.value} ${call.request.local.uri}]: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Invalid request", 400)
            )
        }
        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(cause.message ?: "Not found", 404)
            )
        }
        exception<UnauthorizedException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(cause.message ?: "Unauthorized", 401)
            )
        }
        exception<RateLimitException> { call, _ ->
            call.respond(
                HttpStatusCode.TooManyRequests,
                ErrorResponse("Too many requests. Please try again later.", 429)
            )
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Internal server error", 500)
            )
        }
    }
}

class NotFoundException(message: String) : RuntimeException(message)
class UnauthorizedException(message: String) : RuntimeException(message)
class ValidationException(message: String) : RuntimeException(message)
class RateLimitException : RuntimeException("Rate limit exceeded")
