package it.polimi.webeep.feature.fileviewer.domain

import it.polimi.webeep.core.common.DataError
import java.io.File

/** What the loader found out about a file, free of any UI wording; the viewer decides how to show it. */
sealed interface LoadedContent {
    /** Download in progress; [totalBytes] is `null` when the size is unknown. */
    data class Downloading(val bytesRead: Long, val totalBytes: Long?) : LoadedContent

    /** Image streamed from [url]. */
    data class Image(val url: String) : LoadedContent

    /** PDF saved in [file] and verified readable, with [pageCount] pages. */
    data class Pdf(val file: File, val pageCount: Int) : LoadedContent

    /** Audio or video streamed from [url]. */
    data class Media(val url: String) : LoadedContent

    /** Text file small enough to show inline as [content]. */
    data class Code(val file: File, val content: String) : LoadedContent

    /** File with no in-app viewer (or too big for one), to be handed to another app as [mimeType]. */
    data class External(val file: File, val mimeType: String) : LoadedContent

    /** The downloaded PDF cannot be opened; the broken cached copy is removed so a retry downloads it again. */
    data object CorruptPdf : LoadedContent

    /** The file could not be loaded; [error] is localized by the UI. */
    data class Failed(val error: DataError) : LoadedContent
}
