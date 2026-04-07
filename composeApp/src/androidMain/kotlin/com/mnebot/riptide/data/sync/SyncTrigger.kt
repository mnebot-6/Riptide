package com.mnebot.riptide.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounced sync trigger. Waits 5 seconds after the last mutation before syncing.
 * Calling [notifyMutation] restarts the debounce timer.
 */
class SyncTrigger(
    private val syncManager: SyncManager,
    private val scope: CoroutineScope
) {
    private var debounceJob: Job? = null

    /** Called after any local data mutation. Debounces for 5 seconds. */
    fun notifyMutation() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(5_000)
            syncManager.sync()
        }
    }

    /** Force immediate sync (e.g., user taps "Sync Now"). */
    fun syncNow() {
        debounceJob?.cancel()
        scope.launch { syncManager.sync(force = true) }
    }
}
