package com.mnebot.riptide.data.sync

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "RiptideSync"

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
            Log.i(TAG, "Trigger: debounced sync firing")
            syncManager.sync()
        }
    }

    /** Force immediate sync (e.g., user taps "Sync Now"). */
    fun syncNow() {
        debounceJob?.cancel()
        Log.i(TAG, "Trigger: syncNow requested")
        scope.launch { syncManager.sync(force = true) }
    }
}
