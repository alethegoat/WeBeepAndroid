package it.polimi.webeep.feature.auth

import app.cash.turbine.test
import it.polimi.webeep.feature.auth.domain.SsoCallbackRelay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SsoCallbackRelayTest {

    @Test
    fun `token offered before collection is delivered once`() = runTest {
        val relay = SsoCallbackRelay()
        relay.offer("token-1")
        assertTrue(relay.hasPending)

        relay.tokens.test {
            assertEquals("token-1", awaitItem())
            expectNoEvents()
        }
        assertFalse(relay.hasPending)

        relay.tokens.test {
            expectNoEvents()
        }
    }

    @Test
    fun `only the latest pending token is kept`() = runTest {
        val relay = SsoCallbackRelay()
        relay.offer("token-1")
        relay.offer("token-2")

        relay.tokens.test {
            assertEquals("token-2", awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `blank tokens are ignored`() = runTest {
        val relay = SsoCallbackRelay()
        relay.offer("  ")

        assertFalse(relay.hasPending)
        relay.tokens.test {
            expectNoEvents()
        }
    }
}
