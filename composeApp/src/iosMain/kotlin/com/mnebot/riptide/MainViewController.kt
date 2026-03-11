package com.mnebot.riptide

import androidx.compose.ui.window.ComposeUIViewController
import com.mnebot.riptide.presentation.main.MainViewModel
import com.mnebot.riptide.domain.repository.DayTaskRepository
import com.mnebot.riptide.domain.repository.WorkBlockRepository

fun MainViewController(
    workBlockRepository: WorkBlockRepository,
    dayTaskRepository: DayTaskRepository
) = ComposeUIViewController {
    val viewModel = MainViewModel(
        workBlockRepository = workBlockRepository,
        dayTaskRepository = dayTaskRepository
    )
    App(viewModel = viewModel)
}