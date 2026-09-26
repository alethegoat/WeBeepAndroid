package it.polimi.webeep.core.util

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic connectivity source for the domain layer.
 * Implemented by [NetworkMonitor].
 */
interface ConnectivityObserver {
    /** Emits the current connectivity immediately upon subscription, then updates. */
    val isOnline: Flow<Boolean>
}
