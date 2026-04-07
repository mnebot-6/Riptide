package com.mnebot.riptide.data.remote

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mnebot.riptide.BuildConfig
import com.mnebot.riptide.domain.model.LoggedInUser
import com.mnebot.riptide.domain.repository.UserPreferencesRepository

data class SignInResult(
    val user: LoggedInUser,
    val hasExistingServerData: Boolean
)

/**
 * Manages Google Sign-In flow and JWT token lifecycle.
 */
class AuthManager(
    private val api: RiptideApi,
    private val userPrefs: UserPreferencesRepository
) {

    /** Returns the Intent to launch the Google Sign-In chooser. */
    fun getSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        // Force account chooser every time (in case user wants a different account)
        client.signOut()
        return client.signInIntent
    }

    /**
     * Process the result from the Google Sign-In Activity.
     * Sends the ID token to the backend, stores JWT pair + user info.
     */
    suspend fun handleSignInResult(data: Intent?): Result<SignInResult> = runCatching {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.getResult(ApiException::class.java)
        val idToken = account.idToken
            ?: throw IllegalStateException("Google Sign-In succeeded but no ID token received")

        val response = api.authGoogle(idToken)

        userPrefs.saveTokens(response.accessToken, response.refreshToken)

        val user = LoggedInUser(
            id = response.user.id,
            email = response.user.email,
            displayName = response.user.displayName,
            avatarUrl = response.user.avatarUrl
        )
        userPrefs.saveUser(user)
        SignInResult(user = user, hasExistingServerData = response.hasExistingData)
    }

    /** Sign out: revoke server tokens + clear local storage. */
    suspend fun logout(): Result<Unit> = runCatching {
        try { api.logout() } catch (_: Exception) { /* Best effort -- continue even if server unreachable */ }
        userPrefs.clearAuth()
    }
}
