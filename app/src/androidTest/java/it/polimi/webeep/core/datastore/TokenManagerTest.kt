package it.polimi.webeep.core.datastore

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenManagerTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var application: Application

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        // The Tink keyset and the Keystore master key are created lazily on first access.
        tokenManager = TokenManager(application, Dispatchers.IO)
        runBlocking { tokenManager.clearAll() }
    }

    @Test
    fun `ensureLoaded restores a persisted session in a new instance`() = runBlocking {
        tokenManager.saveAuthData("persisted", 7, "Name")

        val restored = TokenManager(application, Dispatchers.IO)
        assertFalse("Not loaded before ensureLoaded", restored.isLoggedIn.first())

        restored.ensureLoaded()

        assertTrue(restored.isLoggedIn.first())
        assertEquals("persisted", restored.getTokenSync())
    }

    @Test
    fun `getToken works without ensureLoaded`() = runBlocking {
        tokenManager.saveAuthData("token", 1, "Name")

        val fresh = TokenManager(application, Dispatchers.IO)

        assertEquals("token", fresh.getToken())
        assertTrue(fresh.isLoggedIn.first())
    }

    @Test
    fun `pending passports are stored, listed and cleared`() {
        tokenManager.savePendingPassportSync("111")
        tokenManager.savePendingPassportSync("222")

        assertEquals(listOf("111", "222"), tokenManager.getPendingPassportsSync())
        assertEquals("222", tokenManager.getPendingPassportsSync().lastOrNull())

        tokenManager.clearPendingPassportSync()

        assertTrue(tokenManager.getPendingPassportsSync().isEmpty())
    }

    @Test
    fun `saving auth data correctly updates state and cache`() = runBlocking {
        // Given
        val dummyToken = "abc123securetoken"
        val userId = 999
        val fullName = "Mario Rossi"

        // When
        tokenManager.saveAuthData(
            token = dummyToken,
            userId = userId,
            fullName = fullName
        )

        // Then
        assertEquals("Token sincrono aggiornato", dummyToken, tokenManager.getTokenSync())
        assertEquals("Token asincrono letto da preferenze", dummyToken, tokenManager.getToken())
        assertEquals("UserId letto da preferenze", userId, tokenManager.getUserId())
        assertTrue("isLoggedIn emit true", tokenManager.isLoggedIn.first())
        assertEquals("wsToken emit value", dummyToken, tokenManager.wsToken.first())
    }

    @Test
    fun `clearSessionSync clears all token data synchronously`() = runBlocking {
        // Given
        tokenManager.saveAuthData("token", 1, "Name")

        // When
        tokenManager.clearSessionSync()

        // Then
        assertNull("Token sincrono deve essere null", tokenManager.getTokenSync())
        assertNull("Token asincrono deve essere null", tokenManager.getToken())
        assertFalse("isLoggedIn emit false", tokenManager.isLoggedIn.first())
        assertNull("wsToken emit null", tokenManager.wsToken.first())
    }

    @Test
    fun `clearAll clears all persisted data from DataStore`() = runBlocking {
        // Given
        tokenManager.saveAuthData("token", 1, "Name")

        // When
        tokenManager.clearAll()

        // Then
        assertNull("Token must be null", tokenManager.getToken())
        assertNull("UserId must be null", tokenManager.getUserId())
        assertFalse("State should be logged out", tokenManager.isLoggedIn.first())
    }

    @Test
    fun `persisted values are not stored in clear`() = runBlocking {
        tokenManager.saveAuthData("plaintext-token-marker", 3, "Plaintext Name Marker")

        val raw = String(File(application.noBackupFilesDir, "webeep_secure.preferences_pb").readBytes(), Charsets.ISO_8859_1)

        assertFalse(raw.contains("plaintext-token-marker"))
        assertFalse(raw.contains("Plaintext Name Marker"))
    }

    // Legacy migration, remove after next release.
    @Test
    fun `legacy encrypted prefs are migrated once and deleted`() = runBlocking {
        val masterKey = MasterKey.Builder(application)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            application,
            LEGACY_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).edit()
            .putString("ws_token", "legacy-token")
            .putInt("user_id", 5)
            .putString("full_name", "Legacy User")
            .putString("private_token", "never-migrated")
            .commit()

        val migrated = TokenManager(application, Dispatchers.IO)
        migrated.ensureLoaded()

        assertEquals("legacy-token", migrated.getTokenSync())
        assertEquals(5, migrated.getUserId())
        assertFalse("Legacy file deleted", legacyPrefsFile().exists())

        val reloaded = TokenManager(application, Dispatchers.IO)
        reloaded.ensureLoaded()
        assertEquals("legacy-token", reloaded.getTokenSync())
    }

    @Test
    fun `legacy prefs without a session are discarded`() = runBlocking {
        legacyPrefsFile().apply {
            parentFile?.mkdirs()
            writeText("<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map><string name=\"x\">garbage</string></map>")
        }

        val manager = TokenManager(application, Dispatchers.IO)
        manager.ensureLoaded()

        assertNull(manager.getTokenSync())
        assertFalse(manager.isLoggedIn.first())
        assertFalse("Legacy file deleted", legacyPrefsFile().exists())
    }

    private fun legacyPrefsFile() = File(application.dataDir, "shared_prefs/$LEGACY_PREFS_NAME.xml")

    private companion object {
        const val LEGACY_PREFS_NAME = "webeep_secure_prefs"
    }
}
