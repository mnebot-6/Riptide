package com.mnebot.riptide.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.TaskReminderSchedulerImpl
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.remote.ApiClient
import com.mnebot.riptide.data.remote.DataStoreTokenProvider
import com.mnebot.riptide.data.remote.RiptideApi
import com.mnebot.riptide.data.repository.*
import com.mnebot.riptide.data.sync.SyncManager
import com.mnebot.riptide.data.sync.SyncTrigger
import com.mnebot.riptide.domain.DecorationUnlockChecker
import com.mnebot.riptide.domain.EcosystemProcessor
import com.mnebot.riptide.domain.LootboxResolver
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.RecurringTaskGenerator
import com.mnebot.riptide.widget.WidgetUpdater

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = DatabaseProvider.getDatabase(context)
        val workBlockRepo = WorkBlockRepositoryImpl(db.workBlockDao())
        val blockCategoryRepo = BlockCategoryRepositoryImpl(db.blockCategoryDao())
        val dayTaskRepo    = DayTaskRepositoryImpl(db.dayTaskDao())
        val daySummaryRepo = DaySummaryRepositoryImpl(db.daySummaryDao())
        val recurringTaskDefRepo = RecurringTaskDefRepositoryImpl(db.recurringTaskDefDao())
        val ecosystemStateRepo = EcosystemStateRepositoryImpl(db.ecosystemStateDao())
        val marineCreatureRepo = MarineCreatureRepositoryImpl(db.marineCreatureDao())
        val userPreferencesRepo = UserPreferencesRepositoryImpl(context)

        // Sync infrastructure
        val tokenProvider = DataStoreTokenProvider(userPreferencesRepo)
        val httpClient = ApiClient.create(tokenProvider)
        val api = RiptideApi(httpClient)
        val syncManager = SyncManager(db, api, userPreferencesRepo)
        // SyncTrigger needs a CoroutineScope — created lazily in the ViewModel via lambdas
        var syncTrigger: SyncTrigger? = null

        @Suppress("UNCHECKED_CAST")
        return MainViewModel(
            workBlockRepository = workBlockRepo,
            blockCategoryRepository = blockCategoryRepo,
            dayTaskRepository = dayTaskRepo,
            recurringTaskDefRepository = recurringTaskDefRepo,
            recurringTaskGenerator = RecurringTaskGenerator(recurringTaskDefRepo, dayTaskRepo),
            marineCategoryAssigner = MarineCategoryAssigner(
                workBlockRepo,
                blockCategoryRepo,
                ecosystemStateRepo
            ),
            daySummaryRepository = daySummaryRepo,
            ecosystemProcessor = EcosystemProcessor(ecosystemStateRepo, marineCreatureRepo),
            lootboxResolver = LootboxResolver(marineCreatureRepo),
            ecosystemStateRepository = ecosystemStateRepo,
            userPreferencesRepository = userPreferencesRepo,
            marineCreatureRepository = marineCreatureRepo,
            taskReminderScheduler = TaskReminderSchedulerImpl(context.applicationContext),
            decorationUnlockChecker = DecorationUnlockChecker(
                daySummaryRepository      = daySummaryRepo,
                dayTaskRepository         = dayTaskRepo,
                marineCreatureRepository  = marineCreatureRepo,
                ecosystemProcessor        = EcosystemProcessor(ecosystemStateRepo, marineCreatureRepo),
                userPreferencesRepository = userPreferencesRepo
            ),
            onTaskMutated = { WidgetUpdater.refreshAll(context.applicationContext) },
            syncStatusFlow = syncManager.status,
            syncErrorFlow = syncManager.lastError,
            syncLastMillisFlow = syncManager.lastSyncMillis,
            onSyncMutation = {
                // Lazily initialize SyncTrigger on first mutation
                // (it needs the ViewModel's scope, but we approximate with the existing scope)
                syncTrigger?.notifyMutation()
            },
            onSyncNow = {
                syncTrigger?.syncNow()
            }
        ).also { vm ->
            // Now that the ViewModel is created, we can use its viewModelScope
            // to initialize the SyncTrigger
            syncTrigger = SyncTrigger(syncManager, vm.viewModelScope)
        } as T
    }
}