package it.polimi.webeep.feature.dashboard

import app.cash.turbine.test
import it.polimi.webeep.R
import it.polimi.webeep.core.datastore.MissingTokenException
import it.polimi.webeep.core.util.ConnectivityObserver
import it.polimi.webeep.core.util.DataError
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.dashboard.domain.GetCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.SyncCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.feature.dashboard.domain.model.UserProfile
import it.polimi.webeep.feature.dashboard.presentation.DashboardUiState
import it.polimi.webeep.feature.dashboard.presentation.DashboardViewModel
import it.polimi.webeep.util.MainDispatcherRule
import it.polimi.webeep.util.fakes.FakeDashboardRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getCoursesUseCase: GetCoursesUseCase
    private lateinit var syncCoursesUseCase: SyncCoursesUseCase
    private lateinit var fakeDashboardRepository: FakeDashboardRepository
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
        fakeConnectivity = FakeConnectivityObserver()

        getCoursesUseCase = GetCoursesUseCase(fakeDashboardRepository, fakeConnectivity)
        syncCoursesUseCase = SyncCoursesUseCase(fakeDashboardRepository, fakeConnectivity)

        fakeDashboardRepository.userInfoResult = Resource.Success(UserProfile("Alessio", "avatar.png"))
        fakeDashboardRepository.cachedCoursesFlow.value = sampleCourses
        fakeDashboardRepository.syncResult = Resource.Success(sampleCourses)
    }

    private fun initViewModel() {
        viewModel = DashboardViewModel(
            getCoursesUseCase,
            syncCoursesUseCase,
            fakeDashboardRepository,
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
        // If the cache is empty and the sync fails, emit Error
        fakeDashboardRepository.cachedCoursesFlow.value = emptyList()
        fakeDashboardRepository.syncResult = Resource.Error(DataError.Network, IOException("timeout"))

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
        fakeDashboardRepository.syncResult = Resource.Success(emptyList())

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
        // Initial error
        fakeDashboardRepository.cachedCoursesFlow.value = emptyList()
        fakeDashboardRepository.syncResult = Resource.Error(DataError.Unknown, Exception("Sync fallito"))

        initViewModel()

        // Resolve the error for the refresh
        fakeDashboardRepository.cachedCoursesFlow.value = sampleCourses
        fakeDashboardRepository.syncResult = Resource.Success(sampleCourses)

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
    fun `refresh failure with existing data keeps data and resets refresh flag`() = runTest {
        initViewModel()

        fakeDashboardRepository.syncResult = Resource.Error(DataError.Unknown, Exception("Sync fallito"))
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
            Resource.Error(DataError.Unauthorized, MissingTokenException())

        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DashboardUiState.Error)
            assertEquals(R.string.error_session_expired, (state as DashboardUiState.Error).messageRes)
        }
    }

    @Test
    fun `userName stays blank when profile cannot be loaded`() = runTest {
        fakeDashboardRepository.userInfoResult = Resource.Error(DataError.Network, IOException("offline"))

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
        fakeDashboardRepository.syncResult = Resource.Error(DataError.Network, IOException("offline"))
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
}
