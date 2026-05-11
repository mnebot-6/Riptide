package com.mnebot.riptide.data.remote

import android.util.Log
import com.mnebot.riptide.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * API service wrapping all HTTP calls to the Riptide backend.
 */
class RiptideApi(private val client: HttpClient) {

    /** Authenticate with Google ID token. Returns JWT pair + user info. */
    suspend fun authGoogle(idToken: String): TokenResponseDto {
        val response = client.post("/auth/google") {
            setBody(GoogleAuthRequestDto(idToken))
        }
        return response.body()
    }

    /** Batch sync: push dirty entities, pull server changes. */
    suspend fun sync(request: SyncRequest): SyncResponse {
        val response = client.post("/api/sync") {
            setBody(request)
        }
        if (response.status.value !in 200..299) {
            val errorBody = response.bodyAsText()
            Log.e("RiptideApi", "Sync failed: HTTP ${response.status.value} — $errorBody")
            throw RuntimeException("Sync failed: HTTP ${response.status.value} — $errorBody")
        }
        return response.body()
    }

    /** Hard-delete all user data on the server (for "keep local" sync choice). */
    suspend fun deleteUserData() {
        val response = client.delete("/api/user-data")
        if (response.status.value !in 200..299) {
            val errorBody = response.bodyAsText()
            throw RuntimeException("Delete user data failed: HTTP ${response.status.value} — $errorBody")
        }
    }

    /** Permanently delete the account, all server data, and revoke all sessions. */
    suspend fun deleteAccount() {
        val response = client.delete("/api/account")
        if (response.status.value !in 200..299) {
            val errorBody = response.bodyAsText()
            Log.e("RiptideApi", "Delete account failed: HTTP ${response.status.value} — $errorBody")
            throw RuntimeException("Delete account failed: HTTP ${response.status.value} — $errorBody")
        }
    }

    /** Revoke all refresh tokens for current user. */
    suspend fun logout() {
        client.post("/auth/logout")
    }
}
