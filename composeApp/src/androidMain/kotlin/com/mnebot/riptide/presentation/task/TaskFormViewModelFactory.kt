package com.mnebot.riptide.presentation.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.repository.DayTaskRepositoryImpl
import com.mnebot.riptide.data.repository.RecurringTaskDefRepositoryImpl

class TaskFormViewModelFactory(
    private val context: Context,
    private val existingTaskId: String? = null,
    private val existingRecurringDefId: String? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = DatabaseProvider.getDatabase(context)
        @Suppress("UNCHECKED_CAST")
        return TaskFormViewModel(
            dayTaskRepository = DayTaskRepositoryImpl(db.dayTaskDao()),
            recurringTaskDefRepository = RecurringTaskDefRepositoryImpl(db.recurringTaskDefDao()),
            existingTaskId = existingTaskId,
            existingRecurringDefId = existingRecurringDefId
        ) as T
    }
}