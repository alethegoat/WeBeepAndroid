package it.polimi.webeep.feature.auth.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.feature.auth.domain.UserSession
import it.polimi.webeep.util.fakes.FakeSessionDataCleaner
import it.polimi.webeep.util.fakes.FakeSyncScheduling
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private val apiService: MoodleApiService = mockk()
    private val tokenProvider: TokenProvider = mockk(relaxed = true)
    private val syncScheduler = FakeSyncScheduling()
    private val cleaner = FakeSessionDataCleaner()
    private lateinit var repository: AuthRepositoryImpl

    private val siteInfo = SiteInfoDto(
        username = "mariorossi",
        fullname = "Mario Rossi",
        userid = 12345,
        siteurl = "https://webeep.polimi.it",
        userpictureurl = "https://webeep.polimi.it/pluginfile.php/1/user/icon/f1?token=secret"
    )

    @Before
    fun setup() {
        repository = AuthRepositoryImpl(apiService, tokenProvider, syncScheduler, cleaner)
    }

    @Test
    fun `validateToken maps site info to a session without auth query params`() = runTest {
        coEvery { apiService.getSiteInfo(wsToken = "tok") } returns siteInfo

        val result = repository.validateToken("tok")

        val session = (result as Resource.Success).data
        assertEquals(12345, session.userId)
        assertEquals("mariorossi", session.userName)
        assertTrue(!session.avatarUrl.contains("token="))
    }

    @Test
    fun `validateToken maps failures to a typed error`() = runTest {
        coEvery { apiService.getSiteInfo(wsToken = "tok") } throws IOException("offline")

        val result = repository.validateToken("tok")

        assertEquals(DataError.Network, (result as Resource.Error).error)
    }

    @Test
    fun `saveAuthData persists only token and user id`() = runTest {
        val session = UserSession(12345, "Mario Rossi", "mariorossi", "https://webeep.polimi.it", "")

        repository.saveAuthData("tok", session)

        coVerify(exactly = 1) { tokenProvider.saveAuthData("tok", 12345) }
    }

    @Test
    fun `logout stops sync and clears credentials before wiping local data`() = runTest {
        repository.logout()

        assertEquals(1, syncScheduler.cancelCount)
        assertEquals(1, cleaner.wipeCount)
        coVerify(exactly = 1) { tokenProvider.clearAll() }
    }

    @Test
    fun `clearLocalData keeps credentials`() = runTest {
        repository.clearLocalData()

        assertEquals(1, cleaner.wipeCount)
        coVerify(exactly = 0) { tokenProvider.clearAll() }
    }

    @Test
    fun `concurrent logouts never run the wipe at the same time`() = runTest {
        val gate = CompletableDeferred<Unit>()
        cleaner.gate = gate

        val first = launch { repository.logout() }
        val second = launch { repository.logout() }
        advanceUntilIdle()
        assertEquals("Second logout waits for the mutex", 1, cleaner.wipeCount)

        gate.complete(Unit)
        advanceUntilIdle()

        first.join()
        second.join()
        assertEquals(2, cleaner.wipeCount)
        assertEquals(1, cleaner.maxConcurrent)
    }

    @Test
    fun `cancellation from the wipe propagates and releases the mutex`() = runTest {
        cleaner.failure = CancellationException("cancelled")

        try {
            repository.logout()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            // expected
        }

        cleaner.failure = null
        repository.logout()
        assertEquals(2, cleaner.wipeCount)
    }

    @Test
    fun `isLoggedIn loads the store first`() = runTest {
        every { tokenProvider.isLoggedIn } returns flowOf(true)

        assertTrue(repository.isLoggedIn())

        coVerify(exactly = 1) { tokenProvider.ensureLoaded() }
    }
}
