package it.polimi.webeep.feature.dashboard

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import it.polimi.webeep.R
import it.polimi.webeep.core.common.ConnectivityObserver
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.MissingTokenException
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.dashboard.domain.GetCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.SyncCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.feature.dashboard.domain.model.UserProfile
import it.polimi.webeep.feature.dashboard.presentation.CourseFilter
import it.polimi.webeep.feature.dashboard.presentation.DashboardAction
import it.polimi.webeep.feature.dashboard.presentation.DashboardContent
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

    private fun initViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) {
        viewModel = DashboardViewModel(
            getCoursesUseCase,
            syncCoursesUseCase,
            fakeDashboardRepository,
            fakeConnectivity,
            savedStateHandle
        )
    }

    @Test
    fun `init emits Loading then Success with courses`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            val successState = expectMostRecentItem()
            assertTrue(successState.content is DashboardContent.Success)
            val success = successState.content as DashboardContent.Success
            assertEquals(1, success.courses.size)
            assertEquals("Intro to CS", success.courses.first().fullname)
            assertEquals("Alessio", successState.userName)
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
            if (state.content is DashboardContent.Loading) state = awaitItem()

            // Should be Error now
            assertTrue(state.content is DashboardContent.Error)
            assertEquals(R.string.error_data_network, (state.content as DashboardContent.Error).messageRes)
        }
    }

    @Test
    fun `loadCourses with empty list emits Success with empty courses`() = runTest {
        fakeDashboardRepository.cachedCoursesFlow.value = emptyList()
        fakeDashboardRepository.syncResult = Resource.Success(emptyList())

        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.content is DashboardContent.Success)
            assertEquals(0, (state.content as DashboardContent.Success).courses.size)
        }
    }

    @Test
    fun `refresh sets isRefreshing then loads data`() = runTest {
        initViewModel()

        viewModel.onAction(DashboardAction.Refresh)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.content is DashboardContent.Success)
            val success = state.content as DashboardContent.Success
            assertEquals(false, state.isRefreshing)
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

        viewModel.onAction(DashboardAction.Refresh)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.content is DashboardContent.Success)
            assertEquals(1, (state.content as DashboardContent.Success).courses.size)
        }
    }

    @Test
    fun `courses are correctly mapped to UI models`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.content is DashboardContent.Success)
            val uiModel = (state.content as DashboardContent.Success).courses.first()
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
        viewModel.onAction(DashboardAction.Refresh)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.content is DashboardContent.Success)
            val success = state.content as DashboardContent.Success
            assertEquals(false, state.isRefreshing)
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
            assertTrue(state.content is DashboardContent.Error)
            assertEquals(R.string.error_session_expired, (state.content as DashboardContent.Error).messageRes)
        }
    }

    @Test
    fun `userName stays blank when profile cannot be loaded`() = runTest {
        fakeDashboardRepository.userInfoResult = Resource.Error(DataError.Network, IOException("offline"))

        initViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.content is DashboardContent.Success)
            assertEquals("", state.userName)
        }
    }

    @Test
    fun `refresh while offline with empty cache shows network error without syncing`() = runTest {
        fakeDashboardRepository.cachedCoursesFlow.value = emptyList()
        fakeDashboardRepository.syncResult = Resource.Error(DataError.Network, IOException("offline"))
        initViewModel()

        fakeConnectivity.online.value = false
        fakeDashboardRepository.syncCount = 0
        viewModel.onAction(DashboardAction.Refresh)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.content is DashboardContent.Error)
            assertEquals(R.string.error_data_network, (state.content as DashboardContent.Error).messageRes)
        }
        assertEquals(0, fakeDashboardRepository.syncCount)
    }

    @Test
    fun `search query updates uiState and filters visible courses`() = runTest {
        fakeDashboardRepository.cachedCoursesFlow.value = sampleCourses + sampleCourses.first().copy(
            id = 2,
            shortname = "MA202",
            fullname = "Linear Algebra"
        )
        initViewModel()
        // The default ACTIVE filter keeps both (the end dates are in the past, so widen it)
        viewModel.onAction(DashboardAction.FilterChange(CourseFilter.ALL))

        viewModel.onAction(DashboardAction.SearchQueryChange("algebra"))

        assertEquals("algebra", viewModel.searchQuery)
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("algebra", state.searchQuery)
            val content = state.content as DashboardContent.Success
            assertEquals(2, content.courses.size)
            assertEquals(listOf(2), content.visibleCourses.map { it.id })
        }
    }

    @Test
    fun `filter change updates uiState and filters visible courses`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            val initial = expectMostRecentItem()
            assertEquals(CourseFilter.ACTIVE, initial.filter)
            // endDate 2000 is long past, so ACTIVE hides the course
            assertEquals(0, (initial.content as DashboardContent.Success).visibleCourses.size)

            viewModel.onAction(DashboardAction.FilterChange(CourseFilter.PAST))

            val past = expectMostRecentItem()
            assertEquals(CourseFilter.PAST, past.filter)
            assertEquals(1, (past.content as DashboardContent.Success).visibleCourses.size)
        }
    }

    @Test
    fun `reset filters restores the defaults`() = runTest {
        initViewModel()
        viewModel.onAction(DashboardAction.SearchQueryChange("zzz"))
        viewModel.onAction(DashboardAction.FilterChange(CourseFilter.PAST))

        viewModel.onAction(DashboardAction.ResetFilters)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("", state.searchQuery)
            assertEquals(CourseFilter.ACTIVE, state.filter)
            assertEquals(false, state.filtersActive)
        }
    }

    @Test
    fun `search and filter are written to and restored from SavedStateHandle`() = runTest {
        val handle = SavedStateHandle()
        initViewModel(handle)
        viewModel.onAction(DashboardAction.SearchQueryChange("intro"))
        viewModel.onAction(DashboardAction.FilterChange(CourseFilter.PAST))

        // A new ViewModel with the same handle simulates process-death restoration.
        initViewModel(handle)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("intro", state.searchQuery)
            assertEquals(CourseFilter.PAST, state.filter)
            assertEquals(1, (state.content as DashboardContent.Success).visibleCourses.size)
        }
    }

    @Test
    fun `isOnline follows connectivity in uiState`() = runTest {
        initViewModel()

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem().isOnline)

            fakeConnectivity.online.value = false

            assertEquals(false, expectMostRecentItem().isOnline)
        }
    }

    private class FakeConnectivityObserver : ConnectivityObserver {
        val online = MutableStateFlow(true)
        override val isOnline: Flow<Boolean> = online
    }
}
