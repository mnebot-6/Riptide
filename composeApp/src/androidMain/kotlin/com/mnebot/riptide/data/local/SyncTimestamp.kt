package com.mnebot.riptide.data.local

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Returns the current timestamp as an ISO-8601 string for sync tracking. */
fun nowIso(): String = Clock.System.now()
    .toLocalDateTime(TimeZone.currentSystemDefault())
    .toString()
