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

    if (allowedHosts.isEmpty()) {
        appEnv.log.warn("CORS: No allowedHosts configured — allowing all origins (dev mode)")
    }

    install(CORS) {
        if (allowedHosts.isEmpty()) {
            anyHost()
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
    }
}
