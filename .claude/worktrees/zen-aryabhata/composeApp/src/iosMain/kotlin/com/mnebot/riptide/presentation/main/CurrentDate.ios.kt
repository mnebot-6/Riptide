package com.mnebot.riptide.presentation.main

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

actual fun currentDate(): LocalDate =
    kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())