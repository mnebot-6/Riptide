package com.mnebot.riptide.presentation.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnebot.riptide.domain.MarineCategoryAssigner
import com.mnebot.riptide.domain.model.WorkBlock
import com.mnebot.riptide.domain.repository.WorkBlockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BlockFormResult {
    object Idle : BlockFormResult()
    object Saved : BlockFormResult()
    object Deleted : BlockFormResult()
    data class Error(val message: String) : BlockFormResult()
}

class BlockFormViewModel(
    private val workBlockRepository: WorkBlockRepository,
    private val marineCategoryAssigner: MarineCategoryAssigner
) : ViewModel() {

    private val _result = MutableStateFlow<BlockFormResult>(BlockFormResult.Idle)
    val result: StateFlow<BlockFormResult> = _result.asStateFlow()

    fun saveBlock(block: WorkBlock) {
        viewModelScope.launch {
            try {
                val existing = workBlockRepository.getById(block.id)
                if (existing == null) {
                    workBlockRepository.insert(block)
                } else {
                    workBlockRepository.update(block)
                }
                marineCategoryAssigner.reassign()
                _result.value = BlockFormResult.Saved
            } catch (e: Exception) {
                _result.value = BlockFormResult.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun deleteBlock(blockId: String) {
        viewModelScope.launch {
            try {
                workBlockRepository.delete(blockId)
                marineCategoryAssigner.reassign()
                _result.value = BlockFormResult.Deleted
            } catch (e: Exception) {
                _result.value = BlockFormResult.Error(e.message ?: "Error desconocido")
            }
        }
    }
}