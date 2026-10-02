package it.polimi.webeep.feature.sync

import it.polimi.webeep.feature.course.domain.model.Section
import kotlinx.serialization.Serializable

/**
 * Diffs two snapshots of a course and reports only files that were added or updated.
 *
 * Files are identified by module + path + name, never by URL: Moodle embeds a revision
 * number in pluginfile URLs (`.../mod_folder/content/<rev>/...`) that changes for every
 * file of a folder as soon as one of them is touched, which would flag untouched files.
 */
object FileChangeDetector {

    enum class ChangeType { ADDED, UPDATED }

    @Serializable
    data class FileEntry(
        val moduleId: Int,
        val filepath: String,
        val filename: String,
        val timeModified: Long,
        val fileSize: Long
    )

    @Serializable
    data class FileChange(val moduleId: Int, val filepath: String, val filename: String, val type: ChangeType)

    private val fileModules = setOf("resource", "folder")

    private data class FileIdentity(val moduleId: Int, val filepath: String, val filename: String)

    /** Notifiable files of a course: links and other module types are left out. */
    fun snapshot(sections: List<Section>): List<FileEntry> = sections.flatMap { section ->
        section.modules
            .filter { it.modName in fileModules }
            .flatMap { module ->
                module.files.map { file ->
                    FileEntry(module.id, file.filepath, file.filename, file.timeModified ?: 0L, file.fileSize)
                }
            }
    }

    /**
     * @param previous the last known snapshot; null means the course was never synced, so the
     * current contents become the baseline and nothing is reported.
     */
    fun detect(previous: List<FileEntry>?, current: List<FileEntry>): List<FileChange> {
        if (previous == null) return emptyList()

        val oldFiles = previous.associateBy { it.identity() }
        return current.mapNotNull { entry ->
            val old = oldFiles[entry.identity()]
            when {
                old == null -> entry.toChange(ChangeType.ADDED)
                isUpdated(old, entry) -> entry.toChange(ChangeType.UPDATED)
                else -> null
            }
        }
    }

    /**
     * Combines changes not yet seen by the user with fresh ones: newest first, one line per
     * file, and a file stays "added" if it was added and then updated before being seen.
     */
    fun mergeChanges(older: List<FileChange>, newer: List<FileChange>): List<FileChange> {
        val olderById = older.associateBy { it.identity() }
        val newerIds = newer.map { it.identity() }.toSet()
        val merged = newer.map { change ->
            if (olderById[change.identity()]?.type == ChangeType.ADDED) change.copy(type = ChangeType.ADDED) else change
        }
        return merged + older.filter { it.identity() !in newerIds }
    }

    private fun isUpdated(old: FileEntry, new: FileEntry): Boolean {
        if (new.timeModified > 0L && old.timeModified > 0L) {
            return new.timeModified > old.timeModified
        }
        return new.fileSize > 0L && old.fileSize > 0L && new.fileSize != old.fileSize
    }

    private fun FileEntry.identity() = FileIdentity(moduleId, filepath, filename)

    private fun FileChange.identity() = FileIdentity(moduleId, filepath, filename)

    private fun FileEntry.toChange(type: ChangeType) = FileChange(moduleId, filepath, filename, type)
}
