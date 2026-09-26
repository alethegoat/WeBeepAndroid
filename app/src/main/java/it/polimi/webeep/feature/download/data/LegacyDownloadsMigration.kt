package it.polimi.webeep.feature.download.data

import java.io.File

/**
 * Moves downloads from the legacy external app directory (readable by other
 * apps on API 26–28) into internal storage. Performs disk I/O: call off the main thread.
 */
internal object LegacyDownloadsMigration {

    /**
     * Copies completed files from [legacyDir] into [targetDir] (existing targets
     * win), then deletes every legacy entry and the directory itself.
     */
    fun migrate(legacyDir: File?, targetDir: File) {
        if (legacyDir == null || !legacyDir.isDirectory) return
        targetDir.mkdirs()
        val targetCanonical = targetDir.canonicalPath
        legacyDir.listFiles()?.forEach { legacy ->
            if (legacy.isFile && !legacy.name.endsWith(".part")) {
                val target = File(targetDir, legacy.name)
                val isContained = runCatching {
                    target.canonicalPath.startsWith("$targetCanonical${File.separator}")
                }.getOrDefault(false)
                if (isContained && !target.exists()) {
                    runCatching {
                        legacy.copyTo(target)
                        target.setLastModified(legacy.lastModified())
                    }.onFailure { target.delete() }
                }
            }
            legacy.deleteRecursively()
        }
        legacyDir.delete()
    }
}
