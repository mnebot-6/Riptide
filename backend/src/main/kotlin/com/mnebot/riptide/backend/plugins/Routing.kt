package com.mnebot.riptide.backend.plugins

import com.mnebot.riptide.backend.routes.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(CallLogging)

    // ── CORS — restricted by environment ────────────────────
    val appEnv = environment
    val allowedHosts = appEnv.config
        .propertyOrNull("cors.allowedHosts")?.getString()
        ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        ?: emptyList()

    val isDev = appEnv.config.propertyOrNull("ktor.deployment.environment")?.getString() == "development"
    if (allowedHosts.isEmpty() && isDev) {
        appEnv.log.warn("CORS: No allowedHosts configured — allowing all origins (dev mode)")
    } else if (allowedHosts.isEmpty()) {
        appEnv.log.warn("CORS: No allowedHosts configured in production — CORS will reject cross-origin requests")
    }

    install(CORS) {
        if (allowedHosts.isEmpty() && isDev) {
            anyHost()
        } else if (allowedHosts.isEmpty()) {
            // Production with no config: no cross-origin allowed (mobile app doesn't need CORS)
        } else {
            allowedHosts.forEach { host ->
                allowHost(host, schemes = listOf("https"))
            }
        }
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    routing {
        // Health check (no version leak)
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        // Auth (public)
        authRoutes()

        // Protected CRUD routes
        workBlockRoutes()
        blockCategoryRoutes()
        dayTaskRoutes()
        recurringTaskDefRoutes()
        daySummaryRoutes()
        blockStreakRoutes()
        ecosystemStateRoutes()
        marineCreatureRoutes()

        // Batch sync
        syncRoutes()

        // Account lifecycle (delete account)
        accountRoutes()
    }
}
