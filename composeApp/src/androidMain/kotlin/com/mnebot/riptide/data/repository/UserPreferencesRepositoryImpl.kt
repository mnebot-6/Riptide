package com.mnebot.riptide.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mnebot.riptide.domain.model.MarineCategory
import com.mnebot.riptide.domain.model.PendingLootbox
import com.mnebot.riptide.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "riptide_prefs")

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    companion object {
        private val KEY_NIGHT_HOUR = intPreferencesKey("night_summary_hour")
        private val KEY_NIGHT_MINUTE = intPreferencesKey("night_summary_minute")
        private val KEY_PENDING_UNLOCKS = stringPreferencesKey("pending_unlocks")
        private val KEY_PENDING_LOOTBOXES = stringPreferencesKey("pending_lootboxes")
        private val KEY_DISMISSED_SUMMARY_DATE = stringPreferencesKey("dismissed_summary_date")
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

    // Formato: "FISH:4|CRUSTACEAN:6"
    override suspend fun getPendingLootboxes(): List<PendingLootbox> {
        val raw = context.dataStore.data.first()[KEY_PENDING_LOOTBOXES] ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEPARATOR).mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val category = runCatching { MarineCategory.valueOf(parts[0]) }.getOrNull()
                val level = parts[1].toIntOrNull()
                if (category != null && level != null) PendingLootbox(category, level) else null
            } else null
        }
    }

    override suspend fun setPendingLootboxes(lootboxes: List<PendingLootbox>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PENDING_LOOTBOXES] = lootboxes.joinToString(SEPARATOR) {
                "${it.category.name}:${it.categoryLevel}"
            }
        }
    }

    override suspend fun getLastDismissedSummaryDate(): LocalDate? {
        val raw = context.dataStore.data.first()[KEY_DISMISSED_SUMMARY_DATE] ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    override suspend fun setLastDismissedSummaryDate(date: LocalDate) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DISMISSED_SUMMARY_DATE] = date.toString()
        }
    }
}