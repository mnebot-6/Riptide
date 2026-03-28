package com.mnebot.riptide

import platform.Foundation.NSUUID

actual fun generateUUID(): String = NSUUID().UUIDString()