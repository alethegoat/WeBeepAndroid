package it.polimi.webeep.core.datastore

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenManagerTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var application: Application

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        // Istanziazione del TokenManager con contesto Android simulato
        // Questo innescherà la creazione di EncryptedSharedPreferences e MasterKey (StrongBox o fallback TEE)
        tokenManager = TokenManager(application)
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
            privateToken = null,
            userId = userId,
            fullName = fullName
        )

        // Then
        assertEquals("Token sincrono aggiornato", dummyToken, tokenManager.getTokenSync())
        assertEquals("Token asincrono letto da preferenze", dummyToken, tokenManager.getToken())
        assertEquals("UserId letto da preferenze", userId, tokenManager.getUserId())
        assertEquals("FullName letto da preferenze", fullName, tokenManager.getUserFullName())
        assertTrue("isLoggedIn emit true", tokenManager.isLoggedIn.first())
        assertEquals("wsToken emit value", dummyToken, tokenManager.wsToken.first())
    }

    @Test
    fun `clearSessionSync clears all token data synchronously`() = runBlocking {
        // Given
        tokenManager.saveAuthData("token", null, 1, "Name")
        
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
        tokenManager.saveAuthData("token", null, 1, "Name")
        
        // When
        tokenManager.clearAll()
        
        // Then
        assertNull("Token must be null", tokenManager.getToken())
        assertNull("UserId must be null", tokenManager.getUserId())
        assertNull("FullName must be null", tokenManager.getUserFullName())
        assertFalse("State should be logged out", tokenManager.isLoggedIn.first())
    }
}
