package it.polimi.webeep.core.storage

import java.io.File

/**
 * A directory of app-private files with a size budget. Shared by downloads and the viewer
 * cache so both write through `.part` files, replace atomically and evict the same way.
 *
 * Does no locking of its own: callers serialize operations on the same file.
 *
 * @property directory Root of the store; every file handed out stays inside it (see [containedFile]).
 */
class LocalFileStore(val directory: File, private val maxBytes: Long) {

    /** Creates [directory] if needed. */
    fun ensureDirectory() {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    /**
     * Defense in depth against path traversal: callers sanitize names already, but the
     * resolved file must still stay inside [directory]. Null otherwise.
     */
    fun containedFile(safeName: String): File? = try {
        val dirCanonical = directory.canonicalPath
        val file = File(directory, safeName)
        val fileCanonical = file.canonicalPath
        if (fileCanonical == dirCanonical || fileCanonical.startsWith("$dirCanonical${File.separator}")) {
            file
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    /** Scratch file next to [target]; data is written here so a failure never damages the old copy. */
    fun partFileFor(target: File): File = File(target.parentFile, "${target.name}$PART_SUFFIX")

    /** Replaces [target] with the finished [part]. */
    fun commit(part: File, target: File) {
        if (target.exists()) {
            target.delete()
        }
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
    }

    /** Deletes the oldest files until the directory fits the budget; never touches [keep] or unfinished parts. */
    fun evict(keep: File? = null) {
        val files = directory.listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        files
            .filter { it != keep && !it.name.endsWith(PART_SUFFIX) }
            .sortedBy { it.lastModified() }
            .forEach { file ->
                if (total <= maxBytes) return
                val size = file.length()
                if (file.delete()) {
                    total -= size
                }
            }
    }

    private companion object {
        const val PART_SUFFIX = ".part"
    }
}
