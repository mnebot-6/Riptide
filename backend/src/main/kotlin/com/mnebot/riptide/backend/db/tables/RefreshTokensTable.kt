package com.mnebot.riptide.backend.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Stores refresh token metadata for rotation and revocation.
 * The actual JWT is never stored — only its unique JTI (JWT ID).
 *
 * Flow:
 * 1. On login → insert new row (jti, userId, expiresAt)
 * 2. On refresh → verify JTI exists and is not revoked → revoke old → issue new → insert new row
 * 3. On logout → revoke all tokens for userId
 * 4. If a revoked token is used → revoke ALL tokens for that user (token theft detection)
 */
object RefreshTokensTable : Table("refresh_tokens") {
    val jti = varchar("jti", 36)   // UUID — the JWT ID claim
    val userId = varchar("user_id", 36).references(UsersTable.id).index()
    val isRevoked = bool("is_revoked").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val expiresAt = datetime("expires_at")

    override val primaryKey = PrimaryKey(jti)
}
