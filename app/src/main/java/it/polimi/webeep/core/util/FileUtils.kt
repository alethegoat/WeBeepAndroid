package it.polimi.webeep.core.util

object FileUtils {
    private const val MAX_LOCAL_FILE_BASENAME_LENGTH = 80
    private const val URL_HASH_BYTES = 6
    private const val BYTES_PER_UNIT = 1024.0

    /**
     * Sanitizes a filename to make it safe for the local file system.
     * Trims control characters and appends a SHA-256 prefix of the URL
     * to guarantee uniqueness without 32-bit hashCode collisions.
     */
    fun buildLocalFileName(filename: String, fileUrl: String): String {
        val sanitized = filename
            .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_")
            .trim()
            .trim('.')
            .ifBlank { "file" }

        val hash = urlHashPrefix(fileUrl)
        val dotIndex = sanitized.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < sanitized.length - 1) {
            val name = sanitized.substring(0, dotIndex).take(MAX_LOCAL_FILE_BASENAME_LENGTH)
            val extension = sanitized.substring(dotIndex)
            "$name-$hash$extension"
        } else {
            "${sanitized.take(MAX_LOCAL_FILE_BASENAME_LENGTH)}-$hash"
        }
    }

    fun stableUrlHash(fileUrl: String): String = try {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(fileUrl.toByteArray(Charsets.UTF_8))
        bytes.take(URL_HASH_BYTES).joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        Integer.toHexString(fileUrl.hashCode())
    }

    private fun urlHashPrefix(fileUrl: String): String = stableUrlHash(fileUrl)

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(BYTES_PER_UNIT)).toInt()
        val scaled = size / Math.pow(BYTES_PER_UNIT, digitGroups.toDouble())
        return String.format(java.util.Locale.ROOT, "%.1f %s", scaled, units[digitGroups])
    }
}
