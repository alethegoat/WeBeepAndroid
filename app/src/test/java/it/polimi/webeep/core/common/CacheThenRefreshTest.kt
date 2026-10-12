package it.polimi.webeep.core.common

import app.cash.turbine.test
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheThenRefreshTest {

    private val networkError = Resource.Error(DataError.Network, IOException("down"))

    private fun connectivity(online: Boolean) = object : ConnectivityObserver {
        override val isOnline: Flow<Boolean> = flowOf(online)
    }

    @Test
    fun `emits the cache before the refresh result matters`() = runTest {
        val cache = MutableStateFlow(listOf(1, 2))

        cacheThenRefresh(cache, connectivity(true), { Resource.Success(Unit) }, emitEmptyAfterError = false).test {
            assertEquals(Resource.Success(listOf(1, 2)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh error with a non-empty cache is silent`() = runTest {
        val cache = MutableStateFlow(listOf(1))

        cacheThenRefresh(cache, connectivity(true), { networkError }, emitEmptyAfterError = false).test {
            assertEquals(Resource.Success(listOf(1)), awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh error with an empty cache is emitted alone`() = runTest {
        val cache = MutableStateFlow(emptyList<Int>())

        cacheThenRefresh(cache, connectivity(true), { networkError }, emitEmptyAfterError = false).test {
            assertEquals(networkError, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emitEmptyAfterError also reports the empty list after the error`() = runTest {
        val cache = MutableStateFlow(emptyList<Int>())

        cacheThenRefresh(cache, connectivity(true), { networkError }, emitEmptyAfterError = true).test {
            assertEquals(networkError, awaitItem())
            assertEquals(Resource.Success(emptyList<Int>()), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful refresh with an empty cache reports an empty list`() = runTest {
        val cache = MutableStateFlow(emptyList<Int>())

        cacheThenRefresh(cache, connectivity(true), { Resource.Success(Unit) }, emitEmptyAfterError = false).test {
            assertEquals(Resource.Success(emptyList<Int>()), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `offline with an empty cache reports an empty list without refreshing`() = runTest {
        val cache = MutableStateFlow(emptyList<Int>())
        var refreshed = false

        cacheThenRefresh(cache, connectivity(false), {
            refreshed = true
            networkError
        }, emitEmptyAfterError = true).test {
            assertEquals(Resource.Success(emptyList<Int>()), awaitItem())
            assertTrue(!refreshed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `later cache updates keep flowing and cancellation propagates`() = runTest {
        val cache = MutableStateFlow(listOf(1))

        cacheThenRefresh(cache, connectivity(true), { Resource.Success(Unit) }, emitEmptyAfterError = false).test {
            assertEquals(Resource.Success(listOf(1)), awaitItem())
            cache.value = listOf(1, 2)
            assertEquals(Resource.Success(listOf(1, 2)), awaitItem())
            cancel()
        }
    }

    @Test
    fun `cancellation during refresh is not swallowed`() = runTest {
        val cache = MutableStateFlow(listOf(1))

        cacheThenRefresh(cache, connectivity(true), { throw CancellationException("stop") }, emitEmptyAfterError = false).test {
            assertEquals(Resource.Success(listOf(1)), awaitItem())
            awaitError()
        }
    }
}
