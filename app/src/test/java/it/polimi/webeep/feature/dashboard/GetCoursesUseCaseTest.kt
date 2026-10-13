package it.polimi.webeep.feature.dashboard

import app.cash.turbine.test
import it.polimi.webeep.core.util.ConnectivityObserver
import it.polimi.webeep.core.util.DataError
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.dashboard.domain.GetCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.SyncCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.util.fakes.FakeDashboardRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
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

        GetCoursesUseCase(repository, connectivity(false))().test {
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

        GetCoursesUseCase(repository, connectivity(true))().test {
            awaitItem()
            assertTrue(repository.syncCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits typed network error when sync fails with empty cache`() = runTest {
        val repository = FakeDashboardRepository()
        val failure = IOException("Unable to resolve host")
        repository.syncResult = Result.failure(failure)

        GetCoursesUseCase(repository, connectivity(true))().test {
            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(DataError.Network, (error as Resource.Error).error)
            assertEquals(failure, error.throwable)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sync use case fails fast when offline`() = runTest {
        val repository = FakeDashboardRepository()

        val result = SyncCoursesUseCase(repository, connectivity(false))()

        assertEquals(Resource.Error(DataError.Network), result)
        assertFalse(repository.syncCalled)
    }

    @Test
    fun `sync use case maps failures to DataError`() = runTest {
        val repository = FakeDashboardRepository()
        repository.syncResult = Result.failure(RuntimeException("boom"))

        val result = SyncCoursesUseCase(repository, connectivity(true))()

        assertTrue(result is Resource.Error)
        assertEquals(DataError.Unknown, (result as Resource.Error).error)
    }

    @Test
    fun `sync use case returns synced courses`() = runTest {
        val repository = FakeDashboardRepository()
        repository.syncResult = Result.success(listOf(sampleCourse))

        val result = SyncCoursesUseCase(repository, connectivity(true))()

        assertEquals(Resource.Success(listOf(sampleCourse)), result)
    }

    private fun connectivity(online: Boolean) = object : ConnectivityObserver {
        override val isOnline: Flow<Boolean> = flowOf(online)
    }
}
