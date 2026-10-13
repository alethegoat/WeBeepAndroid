package it.polimi.webeep.feature.sync.domain

/** Decides when the background sync runs. */
interface SyncScheduling {

    suspend fun applyCurrentSettings(replaceExisting: Boolean = true)

    /**
     * @param manual the user asked for a check, so it runs even with background sync disabled.
     */
    suspend fun enqueueImmediate(manual: Boolean = false)

    fun cancelAll()
}
