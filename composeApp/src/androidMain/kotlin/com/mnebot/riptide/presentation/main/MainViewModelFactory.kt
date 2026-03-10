package com.mnebot.riptide.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mnebot.riptide.data.local.db.RiptideDatabase
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.data.repository.WorkBlockRepositoryImpl

class MainViewModelFactory(
    private val database: RiptideDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(
            workBlockRepository = WorkBlockRepositoryImpl(database.workBlockDao()),
            dayTaskRepository = DayTaskRepositoryImpl(database.dayTaskDao())
        ) as T
    }
}