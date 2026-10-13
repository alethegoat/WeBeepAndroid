package it.polimi.webeep.core.common

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic connectivity source for the domain layer.
 * Implemented by `NetworkMonitor` in the network package.
 */
interface ConnectivityObserver {
    /** Emits the current connectivity immediately upon subscription, then updates. */
    val isOnline: Flow<Boolean>
}
