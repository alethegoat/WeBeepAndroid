package it.polimi.webeep.feature.fileviewer.domain

import it.polimi.webeep.core.common.DataError
import java.io.File

/** What the loader found out about a file, free of any UI wording; the viewer decides how to show it. */
sealed interface LoadedContent {
    data class Downloading(val bytesRead: Long, val totalBytes: Long?) : LoadedContent
    data class Image(val url: String) : LoadedContent
    data class Pdf(val file: File, val pageCount: Int) : LoadedContent
    data class Media(val url: String) : LoadedContent
    data class Code(val file: File, val content: String) : LoadedContent
    data class External(val file: File, val mimeType: String) : LoadedContent
    data object CorruptPdf : LoadedContent
    data class Failed(val error: DataError) : LoadedContent
}
