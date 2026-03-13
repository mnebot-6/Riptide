package com.mnebot.riptide.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "riptide_prefs")

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    companion object {
        private val KEY_NIGHT_HOUR = intPreferencesKey("night_summary_hour")
        private val KEY_NIGHT_MINUTE = intPreferencesKey("night_summary_minute")
        private val KEY_PENDING_UNLOCKS = stringPreferencesKey("pending_unlocks")
        private const val DEFAULT_HOUR = 23
        private const val DEFAULT_MINUTE = 30
        private const val SEPARATOR = "|"
    }

    override fun getNightSummaryTime(): Flow<LocalTime> =
        context.dataStore.data.map { prefs ->
            val hour = prefs[KEY_NIGHT_HOUR] ?: DEFAULT_HOUR
            val minute = prefs[KEY_NIGHT_MINUTE] ?: DEFAULT_MINUTE
            LocalTime(hour, minute)
        }

    override suspend fun setNightSummaryTime(time: LocalTime) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NIGHT_HOUR] = time.hour
            prefs[KEY_NIGHT_MINUTE] = time.minute
        }
    }

    override suspend fun getPendingUnlocks(): List<String> {
        val raw = context.dataStore.data.first()[KEY_PENDING_UNLOCKS] ?: return emptyList()
        return if (raw.isBlank()) emptyList() else raw.split(SEPARATOR)
    }

    override suspend fun setPendingUnlocks(emojis: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PENDING_UNLOCKS] = emojis.joinToString(SEPARATOR)
        }
    }
}