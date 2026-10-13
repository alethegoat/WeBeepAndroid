package it.polimi.webeep.core.session

/**
 * Erases everything a previous user left on the device (database, downloads, sync state,
 * notifications, web session and caches) without touching credentials.
 */
interface SessionDataCleaner {
    /** Erases the data described on the interface. Idempotent, and a failing step must not stop the others. */
    suspend fun wipe()
}
