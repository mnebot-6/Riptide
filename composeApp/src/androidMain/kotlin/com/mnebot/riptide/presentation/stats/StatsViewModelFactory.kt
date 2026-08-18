package com.mnebot.riptide.presentation.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.repository.*

class StatsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = DatabaseProvider.getDatabase(context)
        @Suppress("UNCHECKED_CAST")
        return StatsViewModel(
            daySummaryRepository = DaySummaryRepositoryImpl(db.daySummaryDao())
        ) as T
    }
}
