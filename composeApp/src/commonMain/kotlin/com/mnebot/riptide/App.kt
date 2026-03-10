package com.mnebot.riptide

import androidx.compose.runtime.Composable
import com.mnebot.riptide.presentation.main.MainScreen
import com.mnebot.riptide.presentation.main.MainViewModel

@Composable
fun App(viewModel: MainViewModel) {
    MainScreen(viewModel = viewModel)
}