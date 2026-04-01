package com.mnebot.riptide.data.remote

import android.util.Log
import com.mnebot.riptide.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

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

    /** Revoke all refresh tokens for current user. */
    suspend fun logout() {
        client.post("/auth/logout")
    }
}
