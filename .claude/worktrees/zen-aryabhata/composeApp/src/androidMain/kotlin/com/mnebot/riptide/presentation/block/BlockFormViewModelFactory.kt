package com.mnebot.riptide.presentation.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mnebot.riptide.data.local.db.RiptideDatabase
import com.mnebot.riptide.data.repository.BlockCategoryRepositoryImpl
import com.mnebot.riptide.data.repository.EcosystemStateRepositoryImpl
import com.mnebot.riptide.data.repository.WorkBlockRepositoryImpl
import com.mnebot.riptide.domain.MarineCategoryAssigner

class BlockFormViewModelFactory(
    private val database: RiptideDatabase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val workBlockRepository = WorkBlockRepositoryImpl(database.workBlockDao())
        val blockCategoryRepository = BlockCategoryRepositoryImpl(database.blockCategoryDao())
        val ecosystemStateRepository = EcosystemStateRepositoryImpl(database.ecosystemStateDao())
        val marineCategoryAssigner = MarineCategoryAssigner(
            workBlockRepository,
            blockCategoryRepository,
            ecosystemStateRepository
        )

        @Suppress("UNCHECKED_CAST")
        return BlockFormViewModel(
            workBlockRepository = workBlockRepository,
            marineCategoryAssigner = marineCategoryAssigner
        ) as T
    }
}