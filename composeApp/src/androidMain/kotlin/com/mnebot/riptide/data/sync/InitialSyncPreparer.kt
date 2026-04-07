package com.mnebot.riptide.data.sync

import com.mnebot.riptide.data.local.db.RiptideDatabase
import com.mnebot.riptide.data.local.nowIso

/**
 * Stamps all pre-existing entities with a valid updatedAt timestamp
 * so they are included in the first sync push.
 */
class InitialSyncPreparer(private val db: RiptideDatabase) {

    suspend fun stampAllEntities() {
        val now = nowIso()
        db.workBlockDao().stampUpdatedAt(now)
        db.blockCategoryDao().stampUpdatedAt(now)
        db.dayTaskDao().stampUpdatedAt(now)
        db.recurringTaskDefDao().stampUpdatedAt(now)
        db.daySummaryDao().stampUpdatedAt(now)
        db.blockStreakDao().stampUpdatedAt(now)
        db.ecosystemStateDao().stampUpdatedAt(now)
        db.marineCreatureDao().stampUpdatedAt(now)
    }

    suspend fun hasLocalData(): Boolean =
        db.workBlockDao().getAll().isNotEmpty()

    suspend fun clearAllLocalData() {
        db.clearAllTables()
    }
}
