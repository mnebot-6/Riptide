package com.mnebot.riptide.backend.plugins

import com.mnebot.riptide.backend.db.tables.RefreshTokensTable
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * Periodically cleans up expired refresh tokens and stale rate limiter entries.
 * Runs every hour.
 */
fun Application.configureCleanupTasks() {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Suppress("DEPRECATION")
    environment.monitor.subscribe(ApplicationStopping) {
        scope.cancel()
    }

    scope.launch {
        while (isActive) {
            delay(3_600_000) // 1 hour

            try {
                // Delete expired refresh tokens
                val deleted = transaction {
                    RefreshTokensTable.deleteWhere {
                        expiresAt less LocalDateTime.now()
                    }
                }
                if (deleted > 0) {
                    environment.log.info("Token cleanup: removed $deleted expired refresh tokens")
                }

                // Cleanup rate limiter stale entries
                RateLimiters.auth.cleanup()
                RateLimiters.api.cleanup()
            } catch (e: Exception) {
                environment.log.error("Token cleanup failed", e)
            }
        }
    }
}
