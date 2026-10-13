package it.polimi.webeep.feature.sync.domain

/** Surfaces the changes found by the background sync to the user. */
interface SyncNotifications {

    /** Safe to call repeatedly; registering early lets users tune the channel before the first alert. */
    fun createChannel()

    /**
     * Posts every change the sync detected but has not notified yet. When notifications cannot
     * be shown the changes stay stored and are delivered by a later call.
     */
    suspend fun deliverPending()

    /** Removes every sync notification, e.g. on logout so no course or file name is left behind. */
    fun cancelAll()
}
