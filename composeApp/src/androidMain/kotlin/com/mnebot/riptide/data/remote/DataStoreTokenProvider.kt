package com.mnebot.riptide.data.remote

import com.mnebot.riptide.domain.repository.UserPreferencesRepository

/**
 * TokenProvider backed by DataStore preferences.
 */
class DataStoreTokenProvider(
    private val prefs: UserPreferencesRepository
) : TokenProvider {
    override suspend fun getAccessToken(): String? = prefs.getAccessToken()
    override suspend fun getRefreshToken(): String? = prefs.getRefreshToken()
    override suspend fun saveTokens(accessToken: String, refreshToken: String) =
        prefs.saveTokens(accessToken, refreshToken)
    override suspend fun clearTokens() = prefs.clearAuth()
}
