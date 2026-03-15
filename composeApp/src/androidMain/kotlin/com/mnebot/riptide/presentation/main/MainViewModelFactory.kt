package com.mnebot.riptide.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.repository.*
import com.mnebot.riptide.domain.EcosystemProcessor
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.RecurringTaskGenerator

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = DatabaseProvider.getDatabase(context)
        val workBlockRepo = WorkBlockRepositoryImpl(db.workBlockDao())
        val blockCategoryRepo = BlockCategoryRepositoryImpl(db.blockCategoryDao())
        val dayTaskRepo = DayTaskRepositoryImpl(db.dayTaskDao())
        val recurringTaskDefRepo = RecurringTaskDefRepositoryImpl(db.recurringTaskDefDao())
        val ecosystemStateRepo = EcosystemStateRepositoryImpl(db.ecosystemStateDao())
        val userPreferencesRepo = UserPreferencesRepositoryImpl(context)

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
            blockStreakRepository = BlockStreakRepositoryImpl(db.blockStreakDao()),
            daySummaryRepository = DaySummaryRepositoryImpl(db.daySummaryDao()),
            ecosystemProcessor = EcosystemProcessor(ecosystemStateRepo),
            ecosystemStateRepository = ecosystemStateRepo,
            userPreferencesRepository = userPreferencesRepo,
            marineCreatureRepository = MarineCreatureRepositoryImpl(db.marineCreatureDao()),
        ) as T
    }
}