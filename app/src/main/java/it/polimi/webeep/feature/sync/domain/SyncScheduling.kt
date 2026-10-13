package it.polimi.webeep.feature.sync.domain

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

    /** Cancels both the periodic and the one-off sync. */
    fun cancelAll()
}
