package com.mnebot.riptide.data.remote

import com.mnebot.riptide.BuildConfig
import com.mnebot.riptide.data.remote.dto.RefreshResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {

    fun create(tokenProvider: TokenProvider): HttpClient = HttpClient(OkHttp) {

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val access = tokenProvider.getAccessToken() ?: return@loadTokens null
                    val refresh = tokenProvider.getRefreshToken() ?: ""
                    BearerTokens(access, refresh)
                }
                refreshTokens {
                    val refreshToken = tokenProvider.getRefreshToken()
                        ?: return@refreshTokens null
                    try {
                        val response = client.post("${BuildConfig.API_BASE_URL}/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("refreshToken" to refreshToken))
                            markAsRefreshTokenRequest()
                        }
                        if (response.status == HttpStatusCode.OK) {
                            val body = response.body<RefreshResponseDto>()
                            tokenProvider.saveTokens(body.accessToken, body.refreshToken)
                            BearerTokens(body.accessToken, body.refreshToken)
                        } else {
                            tokenProvider.clearTokens()
                            null
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }

        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.HEADERS else LogLevel.NONE
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }

        defaultRequest {
            url(BuildConfig.API_BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }
}
