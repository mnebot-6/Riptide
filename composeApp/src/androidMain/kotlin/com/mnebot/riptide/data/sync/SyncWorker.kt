package com.mnebot.riptide.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.remote.ApiClient
import com.mnebot.riptide.data.remote.DataStoreTokenProvider
import com.mnebot.riptide.data.remote.RiptideApi
import com.mnebot.riptide.data.repository.UserPreferencesRepositoryImpl
import java.util.concurrent.TimeUnit

/**
 * Periodic background sync via WorkManager.
 * Runs every hour when the device has an internet connection.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userPrefs = UserPreferencesRepositoryImpl(applicationContext)

        // Skip if not logged in
        if (userPrefs.getAccessToken() == null) return Result.success()

        val tokenProvider = DataStoreTokenProvider(userPrefs)
        val client = ApiClient.create(tokenProvider)
        val api = RiptideApi(client)
        val db = DatabaseProvider.getDatabase(applicationContext)
        val syncManager = SyncManager(db, api, userPrefs)

        return when (syncManager.sync()) {
            is SyncManager.SyncResult.Success -> Result.success()
            is SyncManager.SyncResult.NotLoggedIn -> Result.success()
            is SyncManager.SyncResult.Error -> {
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }
    }

    companion object {
        private const val WORK_NAME = "riptide_periodic_sync"

        /** Schedule hourly background sync with network constraint. */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Cancel periodic sync (e.g., on logout). */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
