package it.polimi.webeep.core.util

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceTest {

    @Test
    fun `resourceOf wraps a returned value in Success`() {
        assertEquals(Resource.Success(42), resourceOf { 42 })
    }

    @Test
    fun `resourceOf maps a failure to DataError and keeps the throwable`() {
        val failure = IOException("offline")

        val result = resourceOf { throw failure }

        assertTrue(result is Resource.Error)
        assertEquals(DataError.Network, (result as Resource.Error).error)
        assertSame(failure, result.throwable)
    }

    @Test
    fun `resourceOf rethrows CancellationException instead of wrapping it`() {
        val cancellation = CancellationException("cancelled")

        val thrown = assertThrows(CancellationException::class.java) { resourceOf { throw cancellation } }

        assertSame(cancellation, thrown)
    }

    @Test
    fun `resourceOf lets a cancelled coroutine stop at its suspension point`() = runTest {
        var result: Resource<Unit>? = null
        val job = launch { result = resourceOf { awaitCancellation() } }
        runCurrent()

        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertNull(result)
    }

    @Test
    fun `resourceOf can call suspend functions`() = runTest {
        val deferred = CompletableDeferred("ready")

        assertEquals(Resource.Success("ready"), resourceOf { deferred.await() })
    }

    @Test
    fun `getOrThrow rethrows the original failure`() {
        val failure = IOException("offline")

        val thrown = assertThrows(IOException::class.java) { Resource.Error(DataError.Network, failure).getOrThrow() }

        assertSame(failure, thrown)
        assertEquals(7, Resource.Success(7).getOrThrow())
    }

    @Test
    fun `getOrThrow without a kept throwable fails with the data error`() {
        val thrown = assertThrows(IllegalStateException::class.java) { Resource.Error(DataError.Storage).getOrThrow() }

        assertTrue(thrown.message.orEmpty().contains("Storage"))
    }

    @Test
    fun `onSuccess runs only for a success`() {
        var seen: Int? = null

        Resource.Error(DataError.Unknown).onSuccess { seen = 1 }
        assertNull(seen)

        Resource.Success(5).onSuccess { seen = it }
        assertEquals(5, seen)
    }
}
