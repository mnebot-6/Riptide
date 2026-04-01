package com.mnebot.riptide.backend.plugins

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Simple in-memory rate limiter using sliding window.
 * For production with multiple instances, replace with Redis-based limiter.
 */
class RateLimiter(
    private val maxRequests: Int,
    private val windowMs: Long
) {
    private data class Window(
        val count: AtomicInteger = AtomicInteger(0),
        val windowStart: AtomicLong = AtomicLong(System.currentTimeMillis())
    )

    private val windows = ConcurrentHashMap<String, Window>()

    /** Returns true if the request is allowed, false if rate limited */
    fun tryAcquire(key: String): Boolean {
        val now = System.currentTimeMillis()
        val window = windows.getOrPut(key) { Window() }

        // Reset window if expired
        if (now - window.windowStart.get() > windowMs) {
            window.count.set(0)
            window.windowStart.set(now)
        }

        return window.count.incrementAndGet() <= maxRequests
    }

    /** Periodically clean up stale entries (call from a coroutine) */
    fun cleanup() {
        val now = System.currentTimeMillis()
        windows.entries.removeIf { now - it.value.windowStart.get() > windowMs * 2 }
    }
}

/** Global rate limiters */
object RateLimiters {
    /** Auth endpoints: 10 requests per minute per IP */
    val auth = RateLimiter(maxRequests = 10, windowMs = 60_000)

    /** API endpoints: 120 requests per minute per user */
    val api = RateLimiter(maxRequests = 120, windowMs = 60_000)
}
