package it.polimi.webeep.core.common

import java.security.MessageDigest
import java.util.Locale

/** Pure helpers for naming and describing downloaded files. */
object FileUtils {
    private const val MAX_LOCAL_FILE_BASENAME_LENGTH = 80
    private val UNSAFE_FILE_NAME_CHARS = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
    private const val URL_HASH_BYTES = 6
    private const val MAX_EXTENSION_LENGTH = 8
    private const val BYTES_PER_UNIT = 1024.0

    /** Replaces path separators and control characters, trims dots and never returns a blank name. */
    fun sanitizeFileName(filename: String): String = filename
        .replace(UNSAFE_FILE_NAME_CHARS, "_")
        .trim()
        .trim('.')
        .ifBlank { "file" }

    /**
     * Name shown to apps that receive a file through a content URI. On disk files carry a URL hash, so the receiver
     * would otherwise see that; this gives it the human title instead, with the local file's extension when the title
     * has none. Only used for sharing: it never changes how files are named on disk.
     */
    fun buildDisplayName(title: String, localFileName: String): String {
        val name = sanitizeFileName(title)
        if (extensionOf(name) != null) return name
        return extensionOf(localFileName)?.let { "$name.$it" } ?: name
    }

    /** The part after the last dot when it looks like a real extension (short, alphanumeric), else null. */
    private fun extensionOf(fileName: String): String? {
        val dot = fileName.lastIndexOf('.')
        if (dot <= 0) return null
        return fileName.substring(dot + 1).takeIf { it.length in 1..MAX_EXTENSION_LENGTH && it.all(Char::isLetterOrDigit) }
    }

    /**
     * Builds a local filename safe for the local file system.
     * Trims control characters and appends a SHA-256 prefix of the URL
     * to guarantee uniqueness without 32-bit hashCode collisions.
     */
    fun buildLocalFileName(filename: String, fileUrl: String): String {
        val sanitized = sanitizeFileName(filename)

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

    /**
     * Short hex digest of [fileUrl], stable across runs and devices.
     *
     * Because it names files on disk, the algorithm and length must not change: existing installs depend on it.
     */
    fun stableUrlHash(fileUrl: String): String = try {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(fileUrl.toByteArray(Charsets.UTF_8))
        bytes.take(URL_HASH_BYTES).joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        Integer.toHexString(fileUrl.hashCode())
    }

    private fun urlHashPrefix(fileUrl: String): String = stableUrlHash(fileUrl)

    /** Human-readable size with binary (1024) steps, e.g. `1.5 MB`; non-positive sizes render as `0 B`. */
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(BYTES_PER_UNIT)).toInt()
        val scaled = size / Math.pow(BYTES_PER_UNIT, digitGroups.toDouble())
        return String.format(Locale.ROOT, "%.1f %s", scaled, units[digitGroups])
    }
}
