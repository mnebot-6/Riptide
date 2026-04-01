package com.mnebot.riptide.data.remote

/**
 * Abstraction for JWT token storage, decoupled from DataStore.
 * Used by ApiClient for automatic Bearer token injection and refresh.
 */
interface TokenProvider {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clearTokens()
}
