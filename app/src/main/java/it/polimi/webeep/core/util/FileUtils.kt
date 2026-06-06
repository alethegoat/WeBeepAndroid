package it.polimi.webeep.core.util

object FileUtils {
    private const val MAX_LOCAL_FILE_BASENAME_LENGTH = 80

    /**
     * Sanitizes a filename to make it safe for the local file system.
     * Trims control characters and appends a hash of the original URL to guarantee uniqueness.
     */
    fun buildLocalFileName(filename: String, fileUrl: String): String {
        val sanitized = filename
            .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_")
            .trim()
            .trim('.')
            .ifBlank { "file" }

        // append a hash of the url to the local filename to ensure uniqueness
        val hash = Integer.toHexString(fileUrl.hashCode())
        val dotIndex = sanitized.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < sanitized.lastIndex - 1) {
            val name = sanitized.substring(0, dotIndex).take(MAX_LOCAL_FILE_BASENAME_LENGTH)
            val extension = sanitized.substring(dotIndex)
            "$name-$hash$extension"
        } else {
            "${sanitized.take(MAX_LOCAL_FILE_BASENAME_LENGTH)}-$hash"
        }
    }

    /**
     * Formats a file size in bytes to a human-readable string.
     */
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(java.util.Locale.ROOT, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
