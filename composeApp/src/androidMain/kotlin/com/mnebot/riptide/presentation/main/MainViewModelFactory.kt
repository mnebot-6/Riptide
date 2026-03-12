package com.mnebot.riptide.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.repository.*
import com.mnebot.riptide.domain.EcosystemProcessor
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.RecurringTaskGenerator
import com.mnebot.riptide.presentation.main.MainViewModel

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = DatabaseProvider.getDatabase(context)
        val workBlockRepository = WorkBlockRepositoryImpl(db.workBlockDao())
        val blockCategoryRepository = BlockCategoryRepositoryImpl(db.blockCategoryDao())
        val dayTaskRepository = DayTaskRepositoryImpl(db.dayTaskDao())
        val recurringTaskDefRepository = RecurringTaskDefRepositoryImpl(db.recurringTaskDefDao())
        val marineCategoryAssigner = MarineCategoryAssigner(
            workBlockRepository, blockCategoryRepository
        )
        val recurringTaskGenerator = RecurringTaskGenerator(
            recurringTaskDefRepository, dayTaskRepository
        )

        @Suppress("UNCHECKED_CAST")
        return MainViewModel(
            workBlockRepository = WorkBlockRepositoryImpl(db.workBlockDao()),
            blockCategoryRepository = BlockCategoryRepositoryImpl(db.blockCategoryDao()),
            dayTaskRepository = DayTaskRepositoryImpl(db.dayTaskDao()),
            recurringTaskDefRepository = RecurringTaskDefRepositoryImpl(db.recurringTaskDefDao()),
            recurringTaskGenerator = RecurringTaskGenerator(
                RecurringTaskDefRepositoryImpl(db.recurringTaskDefDao()),
                DayTaskRepositoryImpl(db.dayTaskDao())
            ),
            marineCategoryAssigner = MarineCategoryAssigner(
                WorkBlockRepositoryImpl(db.workBlockDao()),
                BlockCategoryRepositoryImpl(db.blockCategoryDao())
            ),
            blockStreakRepository = BlockStreakRepositoryImpl(db.blockStreakDao()),
            daySummaryRepository = DaySummaryRepositoryImpl(db.daySummaryDao()),
            ecosystemProcessor = EcosystemProcessor(
                EcosystemStateRepositoryImpl(db.ecosystemStateDao())
            ),
            ecosystemStateRepository = EcosystemStateRepositoryImpl(db.ecosystemStateDao())
        ) as T
    }
}