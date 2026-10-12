package it.polimi.webeep.feature.sync.domain.model

import kotlinx.serialization.Serializable

/** Whether a file is new in the course or an existing one that was modified. */
enum class ChangeType { ADDED, UPDATED }

/**
 * A file as last seen by the sync, identified by [moduleId] + [filepath] + [filename] (not by URL; see `FileChangeDetector`).
 *
 * [timeModified] and [fileSize] are 0 when Moodle did not report them. The serialized field names are the on-disk format of
 * existing installs: do not rename them.
 */
@Serializable
data class FileEntry(val moduleId: Int, val filepath: String, val filename: String, val timeModified: Long, val fileSize: Long)

/** A detected [type] change to the file identified like a [FileEntry]. The serialized field names must not change. */
@Serializable
data class FileChange(val moduleId: Int, val filepath: String, val filename: String, val type: ChangeType)
