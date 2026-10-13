package it.polimi.webeep.feature.sync.domain

import it.polimi.webeep.feature.sync.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

/** Persistent bookkeeping of the background sync: what it has seen and what it still has to tell the user. */
interface SyncStateRepository {

    /** Emits again whenever the background sync completes a check. */
    val lastCheckAt: Flow<Long?>

    /** Atomic read-modify-write: [block] returns the new state and a result handed back to the caller. */
    suspend fun <T> update(block: (SyncState) -> Pair<SyncState, T>): T

    suspend fun clear()
}
