package com.mnebot.riptide.backend

import com.mnebot.riptide.backend.db.DatabaseFactory
import com.mnebot.riptide.backend.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init(environment.config)
    configureSecurityHeaders()
    configureSerialization()
    configureSecurity()
    configureStatusPages()
    configureRouting()
    configureCleanupTasks()
}
