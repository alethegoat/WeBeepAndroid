package it.polimi.webeep.core.session

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SessionExpiryHandlerTest {

    private val loggedIn = MutableStateFlow(false)
    private val tokenProvider: TokenProvider = mockk()
    private val authRepository: AuthRepository = mockk()
    private lateinit var handler: SessionExpiryHandler

    @Before
    fun setup() {
        every { tokenProvider.isLoggedIn } returns loggedIn
        // Mirrors the real logout: clearing the prefs leaves isLoggedIn false.
        coEvery { authRepository.logout() } coAnswers { loggedIn.value = false }
        handler = SessionExpiryHandler(tokenProvider, authRepository)
    }

    @Test
    fun `session loss triggers exactly one logout`() = runTest {
        loggedIn.value = true

        handler.sessionLost.test {
            loggedIn.value = false
            awaitItem()
            expectNoEvents()
        }

        coVerify(exactly = 1) { authRepository.logout() }
    }

    @Test
    fun `starting logged out or logging in does not trigger logout`() = runTest {
        handler.sessionLost.test {
            loggedIn.value = true
            expectNoEvents()
        }

        coVerify(exactly = 0) { authRepository.logout() }
    }
}
