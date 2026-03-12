package com.mnebot.riptide.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "riptide_prefs")

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    companion object {
        private val KEY_NIGHT_HOUR = intPreferencesKey("night_summary_hour")
        private val KEY_NIGHT_MINUTE = intPreferencesKey("night_summary_minute")
        private val DEFAULT_HOUR = 23
        private val DEFAULT_MINUTE = 30
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
}