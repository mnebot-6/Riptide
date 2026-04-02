package com.mnebot.riptide.presentation.aquarium

import androidx.compose.ui.graphics.Path

/**
 * Reusable pool of [Path] objects to avoid per-frame allocations in DrawScope.
 *
 * Usage in a renderer:
 * ```
 * object FooRenderer : CreatureRenderer {
 *     private val paths = PathPool()
 *
 *     override fun DrawScope.render(...) {
 *         paths.begin()
 *         val body = paths.obtain().apply { moveTo(...); ... }
 *         drawPath(body, ...)
 *     }
 * }
 * ```
 *
 * On the first frame, Path objects are allocated as needed.
 * On subsequent frames, they are reused (reset + refilled) — zero allocations.
 */
class PathPool {
    private val pool = mutableListOf<Path>()
    private var cursor = 0

    /** Call at the start of each render() to reset the cursor. */
    fun begin() {
        cursor = 0
    }

    /** Obtain a reset Path from the pool, growing the pool if needed. */
    fun obtain(): Path {
        if (cursor >= pool.size) pool.add(Path())
        return pool[cursor++].also { it.reset() }
    }
}
