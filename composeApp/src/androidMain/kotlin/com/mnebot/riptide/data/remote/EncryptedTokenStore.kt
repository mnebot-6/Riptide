package com.mnebot.riptide.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists JWT tokens in EncryptedSharedPreferences backed by Android Keystore.
 * File: shared_prefs/riptide_secure_prefs.xml — excluded from Auto Backup and D2D
 * transfer via res/xml/{data_extraction_rules,backup_rules}.xml.
 */
class EncryptedTokenStore(private val context: Context) {

    private val prefs: SharedPreferences by lazy { openOrRecover() }

    private fun openOrRecover(): SharedPreferences = try {
        create()
    } catch (e: Exception) {
        Log.w(TAG, "EncryptedSharedPreferences init failed, recreating", e)
        context.deleteSharedPreferences(FILE_NAME)
        create()
    }

    private fun create(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun saveTokens(access: String, refresh: String) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val FILE_NAME = "riptide_secure_prefs"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val TAG = "EncryptedTokenStore"
    }
}
