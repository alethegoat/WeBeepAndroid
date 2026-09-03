package it.polimi.webeep.feature.dashboard

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.GetCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.SyncCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.feature.dashboard.presentation.DashboardUiState
import it.polimi.webeep.feature.dashboard.presentation.DashboardViewModel
import it.polimi.webeep.core.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: kotlinx.coroutines.test.TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
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
    private lateinit var fakeNetworkMonitor: NetworkMonitor
    private lateinit var fakeApplication: android.app.Application
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
        fakeNetworkMonitor = mockk {
            every { isOnline } returns flowOf(true)
        }
        
        getCoursesUseCase = GetCoursesUseCase(fakeDashboardRepository, fakeNetworkMonitor)
        syncCoursesUseCase = SyncCoursesUseCase(fakeDashboardRepository)
        fakeApplication = mockk(relaxed = true)
        
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
            fakeNetworkMonitor,
            fakeApplication
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
        fakeDashboardRepository.syncResult = Result.failure(Exception("Errore di rete"))
        
        initViewModel()

        viewModel.uiState.test {
            // skip initial Loading
            var state = awaitItem()
            if (state is DashboardUiState.Loading) state = awaitItem()
            
            // Should be Error now
            assertTrue(state is DashboardUiState.Error)
            assertTrue((state as DashboardUiState.Error).message.contains("Errore di rete"))
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

    private class FakeDashboardRepository : DashboardRepository {
        val cachedCoursesFlow = MutableStateFlow<List<Course>>(emptyList())
        var syncResult: Result<List<Course>> = Result.success(emptyList())
        var userInfoResult: Result<Pair<String, String>> = Result.success("" to "")

        override fun getCoursesFromCache(): Flow<List<Course>> = cachedCoursesFlow
        override suspend fun syncCourses(): Result<List<Course>> = syncResult
        override suspend fun getUserInfo(): Result<Pair<String, String>> = userInfoResult
    }

    private class FakeAuthRepository : AuthRepository {
        var logoutCalled = false
        override suspend fun validateToken(token: String): Result<it.polimi.webeep.feature.auth.domain.UserSession> = Result.failure(Exception())
        override suspend fun saveAuthData(token: String, privateToken: String?, session: it.polimi.webeep.feature.auth.domain.UserSession) {}
        override suspend fun isLoggedIn(): Boolean = false
        override suspend fun logout() { logoutCalled = true }
    }
}
