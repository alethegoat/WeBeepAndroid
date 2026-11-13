package it.polimi.webeep.feature.sync.domain

import kotlinx.coroutines.flow.Flow

/** Decides when the background sync runs. */
interface SyncScheduling {

    /**
     * Reschedules the periodic sync from the saved frequency and network settings: cancels it when logged out or set to
     * never. With [replaceExisting] `false` an already scheduled run is kept, so app starts do not reset its timing.
     */
    suspend fun applyCurrentSettings(replaceExisting: Boolean = true)

    /**
     * Enqueues a one-off sync unless one is already queued or running; does nothing when logged out.
     *
     * @param manual the user asked for a check, so it runs even with background sync disabled.
     */
    suspend fun enqueueImmediate(manual: Boolean = false)

    /**
     * Live status of the one-off sync. It follows the real work state, so a check that waits for a connection or
     * fails ends up [SyncRunStatus.QUEUED] or [SyncRunStatus.IDLE] instead of looking "in progress" forever.
     */
    fun oneShotStatus(): Flow<SyncRunStatus>

    /** Cancels both the periodic and the one-off sync. */
    fun cancelAll()
}
