package com.mnebot.riptide

import java.util.UUID

actual fun generateUUID(): String = UUID.randomUUID().toString()