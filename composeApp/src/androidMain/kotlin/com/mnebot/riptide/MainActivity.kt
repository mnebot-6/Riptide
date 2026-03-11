package com.mnebot.riptide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.mnebot.riptide.data.local.db.DatabaseProvider
import com.mnebot.riptide.data.repository.BlockCategoryRepositoryImpl
import com.mnebot.riptide.data.repository.WorkBlockRepositoryImpl
import com.mnebot.riptide.presentation.main.MainViewModel
import com.mnebot.riptide.presentation.main.MainViewModelFactory
import com.mnebot.riptide.domain.MarineCategoryAssigner
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val database by lazy {
        DatabaseProvider.getDatabase(applicationContext)
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val assigner = MarineCategoryAssigner(
                WorkBlockRepositoryImpl(database.workBlockDao()),
                BlockCategoryRepositoryImpl(database.blockCategoryDao())
            )
            DataSeeder.seedIfEmpty(database, assigner)
            viewModel.reload()
        }
        setContent {
            App(viewModel = viewModel)
        }
    }
}