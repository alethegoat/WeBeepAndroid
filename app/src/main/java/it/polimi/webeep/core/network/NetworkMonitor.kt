package it.polimi.webeep.core.network

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import it.polimi.webeep.core.common.ConnectivityObserver
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** [ConnectivityObserver] backed by the system `ConnectivityManager`; online means internet capability and a validated network. */
@Singleton
class NetworkMonitor @Inject constructor(application: Application) : ConnectivityObserver {

    private val connectivityManager =
        application.getSystemService(ConnectivityManager::class.java)

    /**
     * Emits the current connectivity immediately upon subscription, then updates.
     *
     * Follows only the default network: a secondary network (e.g. mobile data losing validation while Wi-Fi is fine) must not flip
     * the state. `onAvailable` is ignored because it fires before validation; `onCapabilitiesChanged` always follows it.
     */
    override val isOnline: Flow<Boolean> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(networkCapabilities.isUsable())
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        trySend(isCurrentlyOnline())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()

    private fun NetworkCapabilities.isUsable(): Boolean = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    private fun isCurrentlyOnline(): Boolean {
        val currentNetwork = connectivityManager.activeNetwork
        return connectivityManager.getNetworkCapabilities(currentNetwork)?.isUsable() == true
    }
}
