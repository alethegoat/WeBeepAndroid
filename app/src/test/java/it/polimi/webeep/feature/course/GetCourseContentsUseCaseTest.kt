package it.polimi.webeep.feature.course

import app.cash.turbine.test
import it.polimi.webeep.core.common.ConnectivityObserver
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.GetCourseContentsUseCase
import it.polimi.webeep.feature.course.domain.model.Section
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetCourseContentsUseCaseTest {

    private class FakeCourseRepository : CourseRepository {
        val cache = MutableStateFlow<List<Section>>(emptyList())
        var refreshResult: Resource<List<Section>> = Resource.Success(emptyList())
        val refreshedIds = mutableListOf<Int>()

        override suspend fun getCourseContents(courseId: Int): Resource<List<Section>> {
            refreshedIds += courseId
            return refreshResult
        }

        override fun getCachedCourseContents(courseId: Int): Flow<List<Section>> = cache

        override suspend fun getCourseName(courseId: Int): String? = null
    }

    private val section = Section(id = 1, name = "Week 1", summary = null, sectionNumber = 1, modules = emptyList())
    private val repository = FakeCourseRepository()

    private fun online(value: Boolean) = object : ConnectivityObserver {
        override val isOnline: Flow<Boolean> = flowOf(value)
    }

    @Test
    fun `offline emits the cache and never refreshes`() = runTest {
        repository.cache.value = listOf(section)

        GetCourseContentsUseCase(repository, online(false))(5).test {
            assertEquals(Resource.Success(listOf(section)), awaitItem())
            assertTrue(repository.refreshedIds.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `online refreshes the requested course`() = runTest {
        repository.cache.value = listOf(section)

        GetCourseContentsUseCase(repository, online(true))(5).test {
            awaitItem()
            assertEquals(listOf(5), repository.refreshedIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a refresh error with an empty cache is emitted followed by the empty list`() = runTest {
        repository.refreshResult = Resource.Error(DataError.Network)

        GetCourseContentsUseCase(repository, online(true))(5).test {
            assertEquals(Resource.Error(DataError.Network), awaitItem())
            assertEquals(Resource.Success(emptyList<Section>()), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a refresh error is hidden when the cache has content`() = runTest {
        repository.cache.value = listOf(section)
        repository.refreshResult = Resource.Error(DataError.Network)

        GetCourseContentsUseCase(repository, online(true))(5).test {
            assertEquals(Resource.Success(listOf(section)), awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an empty cache after a successful refresh reports an empty list`() = runTest {
        GetCourseContentsUseCase(repository, online(true))(5).test {
            assertEquals(Resource.Success(emptyList<Section>()), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
