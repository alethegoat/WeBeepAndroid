package it.polimi.webeep.core.datastore

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import it.polimi.webeep.util.fakes.FakeCredentialStore
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TokenManagerTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val dispatcher = UnconfinedTestDispatcher()
    private val legacyPrefs = File(application.dataDir, "shared_prefs/webeep_secure_prefs.xml")

    @Before
    fun cleanLegacy() {
        application.deleteSharedPreferences("webeep_secure_prefs")
    }

    private fun manager(store: FakeCredentialStore) = TokenManager(application, { store }, dispatcher)

    private fun writeLegacyFile() {
        legacyPrefs.parentFile?.mkdirs()
        legacyPrefs.writeText("<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><string name=\"x\">garbage</string></map>")
    }

    @Test
    fun `nothing is logged in before ensureLoaded and the store is not opened`() = runTest {
        val store = FakeCredentialStore(mapOf("ws_token" to "t"))
        val tokens = manager(store)

        assertFalse(tokens.isLoggedIn.first())
        assertFalse(tokens.isLoaded)
        assertNull(tokens.getTokenSync())
        assertEquals(0, store.reads)
    }

    @Test
    fun `ensureLoaded restores a persisted session in a new instance`() = runTest {
        val store = FakeCredentialStore()
        manager(store).saveAuthData("persisted", 7)

        val restored = manager(store)
        restored.ensureLoaded()

        assertTrue(restored.isLoaded)
        assertTrue(restored.isLoggedIn.first())
        assertEquals("persisted", restored.wsToken.first())
        assertEquals("persisted", restored.getTokenSync())
        assertEquals(7, restored.getUserId())
    }

    @Test
    fun `ensureLoaded reads the store only once`() = runTest {
        val store = FakeCredentialStore(mapOf("ws_token" to "t"))
        val tokens = manager(store)

        tokens.ensureLoaded()
        tokens.ensureLoaded()
        tokens.getToken()

        assertEquals(1, store.reads)
    }

    @Test
    fun `getToken works without ensureLoaded and publishes the session`() = runTest {
        val store = FakeCredentialStore(mapOf("ws_token" to "token", "user_id" to "1"))

        val fresh = manager(store)

        assertEquals("token", fresh.getToken())
        assertEquals("token", fresh.getTokenSync())
        assertTrue(fresh.isLoggedIn.first())
    }

    @Test
    fun `a legacy minus one user id reads as unknown`() = runTest {
        val tokens = manager(FakeCredentialStore(mapOf("ws_token" to "t", "user_id" to "-1")))

        assertNull(tokens.getUserId())
    }

    @Test
    fun `saveAuthData updates flows, sync cache and the store`() = runTest {
        val store = FakeCredentialStore()
        val tokens = manager(store)

        tokens.saveAuthData("abc123", 999)

        assertEquals("abc123", tokens.getTokenSync())
        assertEquals("abc123", tokens.getToken())
        assertEquals(999, tokens.getUserId())
        assertTrue(tokens.isLoggedIn.first())
        assertEquals("abc123", tokens.wsToken.first())
        assertEquals(mapOf("ws_token" to "abc123", "user_id" to "999"), store.content)
    }

    @Test
    fun `saveAuthData throws and leaves the state unchanged when the store cannot persist`() = runTest {
        val store = FakeCredentialStore()
        val tokens = manager(store)
        tokens.saveAuthData("old", 1)
        store.failWrites = true

        assertThrows(SecurityException::class.java) { kotlinx.coroutines.runBlocking { tokens.saveAuthData("new", 2) } }

        assertEquals("old", tokens.getTokenSync())
        assertEquals("old", tokens.wsToken.first())
        assertEquals(1, tokens.getUserId())
        assertEquals(mapOf("ws_token" to "old", "user_id" to "1"), store.content)
    }

    @Test
    fun `updateUserId keeps the token and persists the new id`() = runTest {
        val store = FakeCredentialStore()
        val tokens = manager(store)
        tokens.saveAuthData("tok", 1)

        tokens.updateUserId(42)

        assertEquals(42, tokens.getUserId())
        assertEquals("tok", tokens.getToken())
        assertEquals("42", store.content["user_id"])
    }

    @Test
    fun `updateUserId throws when the store cannot persist and keeps the old id`() = runTest {
        val store = FakeCredentialStore()
        val tokens = manager(store)
        tokens.saveAuthData("tok", 1)
        store.failWrites = true

        assertThrows(SecurityException::class.java) { kotlinx.coroutines.runBlocking { tokens.updateUserId(42) } }

        assertEquals(1, tokens.getUserId())
    }

    @Test
    fun `clearAll empties memory and store and flips isLoggedIn`() = runTest {
        val store = FakeCredentialStore()
        val tokens = manager(store)
        tokens.saveAuthData("token", 1)
        tokens.savePendingPassport("p")

        tokens.clearAll()

        assertNull(tokens.getToken())
        assertNull(tokens.getUserId())
        assertNull(tokens.getTokenSync())
        assertNull(tokens.wsToken.first())
        assertFalse(tokens.isLoggedIn.first())
        assertTrue(tokens.getPendingPassports().isEmpty())
        assertTrue(store.content.isEmpty())
    }

    @Test
    fun `clearSessionSync empties everything synchronously even before loading`() = runTest {
        val store = FakeCredentialStore(mapOf("ws_token" to "t", "user_id" to "3"))
        val tokens = manager(store)

        tokens.clearSessionSync()

        assertTrue(tokens.isLoaded)
        assertNull(tokens.getTokenSync())
        assertFalse(tokens.isLoggedIn.first())
        assertTrue(store.content.isEmpty())
    }

    @Test
    fun `clearSessionSync after login flips isLoggedIn back`() = runTest {
        val tokens = manager(FakeCredentialStore())
        tokens.saveAuthData("token", 1)
        assertTrue(tokens.isLoggedIn.first())

        tokens.clearSessionSync()

        assertFalse(tokens.isLoggedIn.first())
        assertNull(tokens.wsToken.first())
    }

    @Test
    fun `pending passports are stored, listed oldest first and cleared`() = runTest {
        val store = FakeCredentialStore()
        val tokens = manager(store)
        tokens.savePendingPassport("111")
        tokens.savePendingPassport("222")

        assertEquals(listOf("111", "222"), tokens.getPendingPassports())
        assertTrue(store.content.getValue("pending_sso_passports").startsWith("111|"))

        tokens.clearPendingPassport()

        assertTrue(tokens.getPendingPassports().isEmpty())
        assertFalse(store.content.containsKey("pending_sso_passports"))
    }

    @Test
    fun `saving the same passport again does not duplicate it`() = runTest {
        val tokens = manager(FakeCredentialStore())

        tokens.savePendingPassport("111")
        tokens.savePendingPassport("222")
        tokens.savePendingPassport("111")

        assertEquals(listOf("222", "111"), tokens.getPendingPassports())
    }

    @Test
    fun `expired passports are not returned`() = runTest {
        val now = System.currentTimeMillis()
        val stale = now - PendingPassports.TTL_MS - 1_000
        val store = FakeCredentialStore(mapOf("pending_sso_passports" to "old|$stale\nfresh|$now"))
        val tokens = manager(store)

        assertEquals(listOf("fresh"), tokens.getPendingPassports())
    }

    @Test
    fun `passports survive in a new instance and do not log the user in`() = runTest {
        val store = FakeCredentialStore()
        manager(store).savePendingPassport("abc")

        val other = manager(store)

        assertEquals(listOf("abc"), other.getPendingPassports())
        assertFalse(other.isLoggedIn.first())
    }

    @Test
    fun `saveAuthData drops passports pending from an earlier login`() = runTest {
        val store = FakeCredentialStore()
        val tokens = manager(store)
        tokens.savePendingPassport("abc")

        tokens.saveAuthData("t", 1)

        assertTrue(tokens.getPendingPassports().isEmpty())
    }

    @Test
    fun `an unreadable legacy file is deleted and the current session kept`() = runTest {
        writeLegacyFile()
        val tokens = manager(FakeCredentialStore(mapOf("ws_token" to "current", "user_id" to "2")))

        tokens.ensureLoaded()

        assertEquals("current", tokens.getTokenSync())
        assertFalse(legacyPrefs.exists())
    }

    @Test
    fun `an unreadable legacy file is deleted and a fresh install stays logged out`() = runTest {
        writeLegacyFile()
        val store = FakeCredentialStore()
        val tokens = manager(store)

        tokens.ensureLoaded()

        assertFalse(tokens.isLoggedIn.first())
        assertTrue(store.content.isEmpty())
        assertFalse(legacyPrefs.exists())
    }

    @Test
    fun `clearAll also removes a leftover legacy file`() = runTest {
        writeLegacyFile()
        val tokens = manager(FakeCredentialStore())

        tokens.clearAll()

        assertFalse(legacyPrefs.exists())
    }

    @Test
    fun `a store that cannot persist does not stop clearAll from logging out in memory`() = runTest {
        val store = FakeCredentialStore()
        val tokens = manager(store)
        tokens.saveAuthData("t", 1)
        store.failWrites = true

        tokens.clearAll()

        assertFalse(tokens.isLoggedIn.first())
        assertNull(tokens.getTokenSync())
    }
}
