package it.polimi.webeep.feature.sync

import it.polimi.webeep.feature.course.domain.model.Section

/**
 * Diffs two snapshots of a course and reports only files that were added or updated.
 *
 * Files are identified by module + path + name, never by URL: Moodle embeds a revision
 * number in pluginfile URLs (`.../mod_folder/content/<rev>/...`) that changes for every
 * file of a folder as soon as one of them is touched, which would flag untouched files.
 */
object FileChangeDetector {

    enum class ChangeType { ADDED, UPDATED }

    data class FileChange(val filename: String, val type: ChangeType)

    private val fileModules = setOf("resource", "folder")

    private data class FileIdentity(val moduleId: Int, val filepath: String, val filename: String)

    private data class FileVersion(val timeModified: Long, val fileSize: Long)

    /**
     * @param previous the cached snapshot; empty means the course was never synced, so the
     * current contents are taken as the baseline and nothing is reported.
     */
    fun detect(previous: List<Section>, current: List<Section>): List<FileChange> {
        if (previous.isEmpty()) return emptyList()

        val oldFiles = indexFiles(previous)
        return indexFiles(current).mapNotNull { (identity, version) ->
            val old = oldFiles[identity]
            when {
                old == null -> FileChange(identity.filename, ChangeType.ADDED)
                isUpdated(old, version) -> FileChange(identity.filename, ChangeType.UPDATED)
                else -> null
            }
        }
    }

    private fun isUpdated(old: FileVersion, new: FileVersion): Boolean {
        if (new.timeModified > 0L && old.timeModified > 0L) {
            return new.timeModified > old.timeModified
        }
        return new.fileSize > 0L && old.fileSize > 0L && new.fileSize != old.fileSize
    }

    private fun indexFiles(sections: List<Section>): Map<FileIdentity, FileVersion> {
        val result = LinkedHashMap<FileIdentity, FileVersion>()
        sections.forEach { section ->
            section.modules
                .filter { it.modName in fileModules }
                .forEach { module ->
                    module.files.forEach { file ->
                        val identity = FileIdentity(module.id, file.filepath, file.filename)
                        result[identity] = FileVersion(file.timeModified ?: 0L, file.fileSize)
                    }
                }
        }
        return result
    }
}
