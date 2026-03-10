package com.mnebot.riptide.presentation.main

import com.mnebot.riptide.domain.model.DayTask
import com.mnebot.riptide.domain.model.WorkBlock
import kotlinx.datetime.LocalDate

data class MainUiState(
    val selectedDate: LocalDate,
    val blocks: List<WorkBlock> = emptyList(),
    val tasksByBlock: Map<String, List<DayTask>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)