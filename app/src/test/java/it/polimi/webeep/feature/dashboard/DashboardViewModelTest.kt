package it.polimi.webeep.feature.dashboard

import app.cash.turbine.test
import it.polimi.webeep.R
import it.polimi.webeep.core.datastore.MissingTokenException
import it.polimi.webeep.core.util.ConnectivityObserver
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.GetCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.SyncCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.feature.dashboard.presentation.DashboardUiState
import it.polimi.webeep.feature.dashboard.presentation.DashboardViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val testDispatcher: kotlinx.coroutines.test.TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getCoursesUseCase: GetCoursesUseCase
    private lateinit var syncCoursesUseCase: SyncCoursesUseCase
    private lateinit var fakeDashboardRepository: FakeDashboardRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeConnectivity: FakeConnectivityObserver
    private lateinit var viewModel: DashboardViewModel

    private val sampleCourses = listOf(
        Course(
            id = 1,
            shortname = "CS101",
            fullname = "Intro to CS",
            summary = "Introductory Course",
            courseImage = "image.png",
            progress = 0.5f,
            isFavourite = false,
            category = "Ingegneria",
            startDate = 1000L,
            endDate = 2000L
        )
    )

    @Before
    fun setup() {
        fakeDashboardRepository = FakeDashboardRepository()
        fakeAuthRepository = FakeAuthRepository()
        fakeConnectivity = FakeConnectivityObserver()

        getCoursesUseCase = GetCoursesUseCase(fakeDashboardRepository, fakeConnectivity)
        syncCoursesUseCase = SyncCoursesUseCase(fakeDashboardRepository, fakeConnectivity)

        fakeDashboardRepository.userInfoResult = Result.success("Alessio" to "avatar.png")
        fakeDashboardRepository.cachedCoursesFlow.value = sampleCourses
        fakeDashboardRepository.syncResult = Result.success(sampleCourses)
    }

    private fun initViewModel() {
        viewModel = DashboardViewModel(
            getCoursesUseCase,
            syncCoursesUseCase,
            fakeDashboardRepository,
            fakeAuthRepository,
            fakeConnectivity
        )
    }

    @Test
    fun `init emits Loading then Success with courses`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            val successState = expectMostRecentItem()
            assertTrue(successState is DashboardUiState.Success)
            val success = successState as DashboardUiState.Success
            assertEquals(1, success.courses.size)
            assertEquals("Intro to CS", success.courses.first().fullname)
            assertEquals("Alessio", success.userName)
        }
    }

    @Test
    fun `init emits Loading then Error on network failure`() = runTest {
        // Se la cache è vuota e il sync fallisce, emette Error
        fakeDashboardRepository.cachedCoursesFlow.value = emptyList()
        fakeDashboardRepository.syncResult = Result.failure(IOException("timeout"))

        initViewModel()

        viewModel.uiState.test {
            // skip initial Loading
            var state = awaitItem()
            if (state is DashboardUiState.Loading) state = awaitItem()

            // Should be Error now
            assertTrue(state is DashboardUiState.Error)
            assertEquals(R.string.error_data_network, (state as DashboardUiState.Error).messageRes)
        }
    }

    @Test
    fun `loadCourses with empty list emits Success with empty courses`() = runTest {
        fakeDashboardRepository.cachedCoursesFlow.value = emptyList()
        fakeDashboardRepository.syncResult = Result.success(emptyList())

        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DashboardUiState.Success)
            assertEquals(0, (state as DashboardUiState.Success).courses.size)
        }
    }

    @Test
    fun `refresh sets isRefreshing then loads data`() = runTest {
        initViewModel()

        viewModel.refresh()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DashboardUiState.Success)
            val success = state as DashboardUiState.Success
            assertEquals(false, success.isRefreshing)
            assertEquals(1, success.courses.size)
        }
    }

    @Test
    fun `refresh after error transitions correctly`() = runTest {
        // Errore iniziale
        fakeDashboardRepository.cachedCoursesFlow.value = emptyList()
        fakeDashboardRepository.syncResult = Result.failure(Exception("Sync fallito"))

        initViewModel()

        // Risolve l'errore per il refresh
        fakeDashboardRepository.cachedCoursesFlow.value = sampleCourses
        fakeDashboardRepository.syncResult = Result.success(sampleCourses)

        viewModel.refresh()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DashboardUiState.Success)
            assertEquals(1, (state as DashboardUiState.Success).courses.size)
        }
    }

    @Test
    fun `courses are correctly mapped to UI models`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DashboardUiState.Success)
            val uiModel = (state as DashboardUiState.Success).courses.first()
            assertEquals(1, uiModel.id)
            assertEquals("CS101", uiModel.shortname)
            assertEquals("Intro to CS", uiModel.fullname)
            assertEquals("image.png", uiModel.courseImage)
            assertEquals(0.5f, uiModel.progress)
            assertEquals(false, uiModel.isFavourite)
            assertEquals("Ingegneria", uiModel.category)
        }
    }

    @Test
    fun `logout clears state`() = runTest {
        initViewModel()
        viewModel.logout()
        assertTrue(fakeAuthRepository.logoutCalled)
    }

    @Test
    fun `refresh failure with existing data keeps data and resets refresh flag`() = runTest {
        initViewModel()

        fakeDashboardRepository.syncResult = Result.failure(Exception("Sync fallito"))
        viewModel.refresh()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DashboardUiState.Success)
            val success = state as DashboardUiState.Success
            assertEquals(false, success.isRefreshing)
            assertEquals(1, success.courses.size)
        }
    }

    @Test
    fun `missing token error maps to session expired message`() = runTest {
        fakeDashboardRepository.cachedCoursesFlow.value = emptyList()
        fakeDashboardRepository.syncResult =
            Result.failure(MissingTokenException())

        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DashboardUiState.Error)
            assertEquals(R.string.error_session_expired, (state as DashboardUiState.Error).messageRes)
        }
    }

    @Test
    fun `userName stays blank when profile cannot be loaded`() = runTest {
        fakeDashboardRepository.userInfoResult = Result.failure(IOException("offline"))

        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DashboardUiState.Success)
            assertEquals("", (state as DashboardUiState.Success).userName)
        }
    }

    @Test
    fun `refresh while offline with empty cache shows network error without syncing`() = runTest {
        fakeDashboardRepository.cachedCoursesFlow.value = emptyList()
        fakeDashboardRepository.syncResult = Result.failure(IOException("offline"))
        initViewModel()

        fakeConnectivity.online.value = false
        fakeDashboardRepository.syncCount = 0
        viewModel.refresh()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DashboardUiState.Error)
            assertEquals(R.string.error_data_network, (state as DashboardUiState.Error).messageRes)
        }
        assertEquals(0, fakeDashboardRepository.syncCount)
    }

    private class FakeConnectivityObserver : ConnectivityObserver {
        val online = MutableStateFlow(true)
        override val isOnline: Flow<Boolean> = online
    }

    private class FakeDashboardRepository : DashboardRepository {
        val cachedCoursesFlow = MutableStateFlow<List<Course>>(emptyList())
        var syncResult: Result<List<Course>> = Result.success(emptyList())
        var userInfoResult: Result<Pair<String, String>> = Result.success("" to "")

        override fun getCoursesFromCache(): Flow<List<Course>> = cachedCoursesFlow
        var syncCount = 0

        override suspend fun syncCourses(): Result<List<Course>> {
            syncCount++
            return syncResult
        }
        override suspend fun getUserInfo(): Result<Pair<String, String>> = userInfoResult
    }

    private class FakeAuthRepository : AuthRepository {
        var logoutCalled = false
        override suspend fun validateToken(token: String): Result<it.polimi.webeep.feature.auth.domain.UserSession> =
            Result.failure(Exception())
        override suspend fun saveAuthData(token: String, privateToken: String?, session: it.polimi.webeep.feature.auth.domain.UserSession) =
            Unit
        override suspend fun isLoggedIn(): Boolean = false
        override suspend fun logout() {
            logoutCalled = true
        }
    }
}
