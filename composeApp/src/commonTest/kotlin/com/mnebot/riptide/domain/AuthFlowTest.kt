package com.mnebot.riptide.domain

import com.mnebot.riptide.domain.fakes.FakeUserPreferencesRepository
import com.mnebot.riptide.domain.model.LoggedInUser
import com.mnebot.riptide.domain.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for auth state management and sync status model.
 * These test the UserPreferencesRepository contract used by the sync layer.
 */
class AuthFlowTest {

    // ── Token management ──────────────────────────────────────────────────────

    @Test
    fun initialState_noTokens() = runTest {
        val repo = FakeUserPreferencesRepository()
        assertNull(repo.getAccessToken())
        assertNull(repo.getRefreshToken())
    }

    @Test
    fun saveTokens_retrievable() = runTest {
        val repo = FakeUserPreferencesRepository()
        repo.saveTokens("access123", "refresh456")

        assertEquals("access123", repo.getAccessToken())
        assertEquals("refresh456", repo.getRefreshToken())
    }

    @Test
    fun clearAuth_removesEverything() = runTest {
        val repo = FakeUserPreferencesRepository()
        repo.saveTokens("access", "refresh")
        repo.saveUser(LoggedInUser("u1", "test@mail.com", "Test", null))
        repo.setLastSyncTime("2026-04-01T12:00:00")

        repo.clearAuth()

        assertNull(repo.getAccessToken())
        assertNull(repo.getRefreshToken())
        assertNull(repo.getLoggedInUser())
        assertNull(repo.getLastSyncTime())
    }

    @Test
    fun isLoggedIn_reflectsTokenState() = runTest {
        val repo = FakeUserPreferencesRepository()
        assertTrue(!repo.isLoggedIn().first(), "Should not be logged in without tokens")

        repo.saveTokens("access", "refresh")
        assertTrue(repo.isLoggedIn().first(), "Should be logged in with tokens")
    }

    // ── User info ─────────────────────────────────────────────────────────────

    @Test
    fun initialState_noUser() = runTest {
        val repo = FakeUserPreferencesRepository()
        assertNull(repo.getLoggedInUser())
    }

    @Test
    fun saveUser_retrievable() = runTest {
        val repo = FakeUserPreferencesRepository()
        val user = LoggedInUser("id1", "user@email.com", "Display Name", "https://avatar.url")
        repo.saveUser(user)

        val saved = repo.getLoggedInUser()
        assertNotNull(saved)
        assertEquals("id1", saved.id)
        assertEquals("user@email.com", saved.email)
        assertEquals("Display Name", saved.displayName)
        assertEquals("https://avatar.url", saved.avatarUrl)
    }

    @Test
    fun saveUser_withNullOptionalFields() = runTest {
        val repo = FakeUserPreferencesRepository()
        val user = LoggedInUser("id2", "minimal@email.com", null, null)
        repo.saveUser(user)

        val saved = repo.getLoggedInUser()
        assertNotNull(saved)
        assertNull(saved.displayName)
        assertNull(saved.avatarUrl)
    }

    // ── Sync time ─────────────────────────────────────────────────────────────

    @Test
    fun initialState_noSyncTime() = runTest {
        val repo = FakeUserPreferencesRepository()
        assertNull(repo.getLastSyncTime())
    }

    @Test
    fun setLastSyncTime_retrievable() = runTest {
        val repo = FakeUserPreferencesRepository()
        repo.setLastSyncTime("2026-04-01T12:00:00")
        assertEquals("2026-04-01T12:00:00", repo.getLastSyncTime())
    }

    @Test
    fun setLastSyncTime_updatesOnSubsequentCalls() = runTest {
        val repo = FakeUserPreferencesRepository()
        repo.setLastSyncTime("2026-04-01T12:00:00")
        repo.setLastSyncTime("2026-04-01T13:00:00")
        assertEquals("2026-04-01T13:00:00", repo.getLastSyncTime())
    }

    // ── SyncStatus enum ───────────────────────────────────────────────────────

    @Test
    fun syncStatus_hasAllExpectedValues() {
        val values = SyncStatus.entries
        assertEquals(5, values.size)
        assertTrue(values.contains(SyncStatus.IDLE))
        assertTrue(values.contains(SyncStatus.SYNCING))
        assertTrue(values.contains(SyncStatus.SUCCESS))
        assertTrue(values.contains(SyncStatus.ERROR))
        assertTrue(values.contains(SyncStatus.OFFLINE))
    }

    // ── Full auth lifecycle ───────────────────────────────────────────────────

    @Test
    fun fullAuthLifecycle_loginSyncLogout() = runTest {
        val repo = FakeUserPreferencesRepository()

        // 1. Login
        repo.saveTokens("jwt-access", "jwt-refresh")
        repo.saveUser(LoggedInUser("u1", "user@email.com", "User", null))
        assertTrue(repo.isLoggedIn().first())
        assertNotNull(repo.getLoggedInUser())

        // 2. Sync
        repo.setLastSyncTime("2026-04-01T12:00:00")
        assertEquals("2026-04-01T12:00:00", repo.getLastSyncTime())

        // 3. Logout — clears everything
        repo.clearAuth()
        assertTrue(!repo.isLoggedIn().first())
        assertNull(repo.getAccessToken())
        assertNull(repo.getLoggedInUser())
        assertNull(repo.getLastSyncTime())
    }
}
