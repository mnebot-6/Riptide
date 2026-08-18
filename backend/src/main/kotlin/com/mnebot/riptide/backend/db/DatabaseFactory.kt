package com.mnebot.riptide.backend.db

import com.mnebot.riptide.backend.db.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    /**
     * Parse a Railway/Heroku-style database URL:
     *   postgresql://user:password@host:port/dbname
     * into JDBC format + separate user/password for HikariCP.
     */
    private data class ParsedDbUrl(val jdbcUrl: String, val user: String, val password: String)

    private fun parsePostgresUrl(url: String): ParsedDbUrl? {
        // Match: postgresql://user:pass@host:port/db or postgres://user:pass@host:port/db
        val regex = Regex("""postgres(?:ql)?://([^:]+):([^@]+)@([^/]+)/(.+)""")
        val match = regex.matchEntire(url) ?: return null
        val (user, password, hostPort, dbName) = match.destructured
        return ParsedDbUrl(
            jdbcUrl = "jdbc:postgresql://$hostPort/$dbName?sslmode=require",
            user = user,
            password = password
        )
    }

    fun init(config: ApplicationConfig) {
        val rawUrl = config.property("database.url").getString()
        val maxPoolSize = config.property("database.maxPoolSize").getString().toInt()

        // Support Railway-style URLs (postgresql://user:pass@host/db)
        // and traditional JDBC URLs (jdbc:postgresql://host/db)
        val parsed = parsePostgresUrl(rawUrl)
        val jdbcUrl: String
        val dbUser: String
        val dbPassword: String

        if (parsed != null) {
            jdbcUrl = parsed.jdbcUrl
            dbUser = parsed.user
            dbPassword = parsed.password
        } else {
            jdbcUrl = rawUrl
            dbUser = config.property("database.user").getString()
            dbPassword = config.property("database.password").getString()
        }

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            username = dbUser
            password = dbPassword
            maximumPoolSize = maxPoolSize
            isAutoCommit = false

            // PostgreSQL optimizations
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        // Create tables and add missing columns (e.g. enriched task fields)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                RefreshTokensTable,
                WorkBlocksTable,
                BlockCategoriesTable,
                DayTasksTable,
                RecurringTaskDefsTable,
                DaySummariesTable,
                EcosystemStatesTable,
                MarineCreaturesTable
            )
        }
    }

    /** Execute a database query on the IO dispatcher */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
