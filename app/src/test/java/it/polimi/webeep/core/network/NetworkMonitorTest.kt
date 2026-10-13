package it.polimi.webeep.core.network

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric only provides a working [NetworkRequest.Builder]; the system service is a mock. */
@RunWith(RobolectricTestRunner::class)
class NetworkMonitorTest {

    private val manager = mockk<ConnectivityManager>(relaxed = true)
    private val application = mockk<Application> {
        every { getSystemService(ConnectivityManager::class.java) } returns manager
    }
    private val callback = slot<ConnectivityManager.NetworkCallback>()
    private val network = mockk<Network>()

    private fun currentlyOnline(online: Boolean) {
        every { manager.activeNetwork } returns if (online) network else null
        every { manager.getNetworkCapabilities(network) } returns if (online) capabilities(internet = true, validated = true) else null
        every { manager.getNetworkCapabilities(null) } returns null
        every { manager.registerNetworkCallback(any<NetworkRequest>(), capture(callback)) } returns Unit
    }

    private fun capabilities(internet: Boolean, validated: Boolean) = mockk<NetworkCapabilities> {
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns internet
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns validated
    }

    @Test
    fun `emits the current connectivity on subscription`() = runTest {
        currentlyOnline(true)

        NetworkMonitor(application).isOnline.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits false on subscription when there is no active network`() = runTest {
        currentlyOnline(false)

        NetworkMonitor(application).isOnline.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `capability changes toggle the state and duplicates are dropped`() = runTest {
        currentlyOnline(false)

        NetworkMonitor(application).isOnline.test {
            assertFalse(awaitItem())

            callback.captured.onCapabilitiesChanged(network, capabilities(internet = true, validated = true))
            assertTrue(awaitItem())

            callback.captured.onCapabilitiesChanged(network, capabilities(internet = true, validated = true))
            callback.captured.onCapabilitiesChanged(network, capabilities(internet = true, validated = false))
            assertFalse(awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAvailable reports online`() = runTest {
        currentlyOnline(false)

        NetworkMonitor(application).isOnline.test {
            assertFalse(awaitItem())

            callback.captured.onAvailable(network)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onLost re-checks the active network`() = runTest {
        currentlyOnline(true)

        NetworkMonitor(application).isOnline.test {
            assertTrue(awaitItem())

            every { manager.activeNetwork } returns null
            callback.captured.onLost(network)

            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancelling the collection unregisters the callback`() = runTest {
        currentlyOnline(true)

        NetworkMonitor(application).isOnline.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify { manager.unregisterNetworkCallback(callback.captured) }
    }
}
