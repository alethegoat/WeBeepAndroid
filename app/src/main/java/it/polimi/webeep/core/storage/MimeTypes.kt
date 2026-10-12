package it.polimi.webeep.core.storage

import android.webkit.MimeTypeMap
import java.util.Locale

private const val UNKNOWN_MIME_TYPE = "*/*"

/**
 * MIME type guessed from the text after the last dot of [fileName], or a wildcard when unknown.
 * Done by hand because [MimeTypeMap.getFileExtensionFromUrl] returns nothing for names with spaces.
 */
fun mimeTypeForFileName(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    if (extension.isEmpty()) return UNKNOWN_MIME_TYPE
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: UNKNOWN_MIME_TYPE
}
