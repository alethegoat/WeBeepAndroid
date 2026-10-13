package it.polimi.webeep.core.common

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class CancellationTest {

    @Test
    fun `runCatchingCancellable returns the value on success`() {
        assertEquals(3, runCatchingCancellable { 3 }.getOrNull())
    }

    @Test
    fun `runCatchingCancellable captures ordinary failures`() {
        val failure = IOException("offline")

        assertSame(failure, runCatchingCancellable { throw failure }.exceptionOrNull())
    }

    @Test
    fun `runCatchingCancellable rethrows CancellationException`() {
        val cancellation = CancellationException("cancelled")

        val thrown = assertThrows(CancellationException::class.java) { runCatchingCancellable { throw cancellation } }

        assertSame(cancellation, thrown)
    }
}
