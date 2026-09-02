package it.polimi.webeep.feature.dashboard

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import it.polimi.webeep.core.util.NetworkMonitor
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.GetCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetCoursesUseCaseTest {

    private val sampleCourse = Course(
        id = 1,
        shortname = "CS101",
        fullname = "Intro",
        summary = null,
        courseImage = null,
        progress = null,
        isFavourite = false,
        category = null,
        startDate = 0L,
        endDate = 0L
    )

    @Test
    fun `skips network sync when offline and emits cache`() = runTest {
        val repository = FakeDashboardRepository()
        repository.cachedCoursesFlow.value = listOf(sampleCourse)
        repository.syncResult = Result.success(emptyList())

        val networkMonitor = mockk<NetworkMonitor> {
            every { isOnline } returns flowOf(false)
        }

        GetCoursesUseCase(repository, networkMonitor)().test {
            val success = awaitItem()
            assertTrue(success is Resource.Success)
            assertEquals(1, (success as Resource.Success).data.size)
            assertFalse(repository.syncCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `syncs when online`() = runTest {
        val repository = FakeDashboardRepository()
        repository.cachedCoursesFlow.value = listOf(sampleCourse)
        repository.syncResult = Result.success(listOf(sampleCourse))

        val networkMonitor = mockk<NetworkMonitor> {
            every { isOnline } returns flowOf(true)
        }

        GetCoursesUseCase(repository, networkMonitor)().test {
            awaitItem()
            assertTrue(repository.syncCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeDashboardRepository : DashboardRepository {
        val cachedCoursesFlow = MutableStateFlow<List<Course>>(emptyList())
        var syncResult: Result<List<Course>> = Result.success(emptyList())
        var syncCalled = false

        override fun getCoursesFromCache(): Flow<List<Course>> = cachedCoursesFlow
        override suspend fun syncCourses(): Result<List<Course>> {
            syncCalled = true
            return syncResult
        }
        override suspend fun getUserInfo(): Result<Pair<String, String>> = Result.success("" to "")
    }
}
