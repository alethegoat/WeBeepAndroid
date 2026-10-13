package it.polimi.webeep.feature.sync.domain.model

import kotlinx.serialization.Serializable

/**
 * The serialized field names are the on-disk format of existing installs: do not rename them.
 *
 * @property snapshots files known to the background sync, per course. Kept apart from the
 * content cache, which the course screen also refreshes: files first seen in the app must
 * still be notified.
 * @property undelivered changes detected but not notified yet. Saved together with the snapshot
 * that hides them from the next diff, so an interrupted sync (network lost, worker stopped,
 * notifications off) delivers them later instead of losing them.
 * @property pending changes shown in notifications the user has not dismissed yet.
 * @property lastCheckAt when the background sync last fetched at least one course.
 */
@Serializable
data class SyncState(
    val snapshots: Map<Int, List<FileEntry>> = emptyMap(),
    val undelivered: List<CourseChanges> = emptyList(),
    val pending: List<CourseChanges> = emptyList(),
    val lastCheckAt: Long? = null
)
