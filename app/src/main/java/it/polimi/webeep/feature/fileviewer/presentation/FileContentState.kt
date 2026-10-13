package it.polimi.webeep.feature.fileviewer.presentation

import androidx.annotation.StringRes
import it.polimi.webeep.R
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.feature.fileviewer.domain.LoadedContent
import java.io.File

sealed interface FileContentState {
    data class Downloading(val bytesRead: Long, val totalBytes: Long?) : FileContentState {
        /** 0..1, or null when the server did not send a length. */
        val progress: Float? = totalBytes?.takeIf { it > 0 }?.let { (bytesRead.toFloat() / it).coerceIn(0f, 1f) }
    }
    data class Image(val url: String) : FileContentState
    data class Pdf(val file: File, val pageCount: Int) : FileContentState
    data class Media(val url: String) : FileContentState
    data class Code(val file: File, val content: String) : FileContentState
    data class External(val file: File, val mimeType: String) : FileContentState
    data class Error(val reason: ViewerError) : FileContentState {
        @get:StringRes
        val messageRes: Int get() = reason.messageRes
    }
}

/**
 * Why a file could not be shown. The viewer words some failures more specifically than the
 * generic [it.polimi.webeep.core.common.DataError] messages, so its texts live here in one place.
 */
enum class ViewerError(@StringRes val messageRes: Int) {
    SESSION_EXPIRED(R.string.error_session_expired),
    NETWORK(R.string.file_viewer_error_network),
    CORRUPT_PDF(R.string.file_viewer_error_pdf),
    CANNOT_OPEN(R.string.error_cannot_open_file)
}

/** Local copy behind a ready state, if the state was built from one. */
val FileContentState.localFile: File?
    get() = when (this) {
        is FileContentState.Pdf -> file
        is FileContentState.Code -> file
        is FileContentState.External -> file
        else -> null
    }

internal fun DataError.toViewerError(): ViewerError = when (this) {
    DataError.Unauthorized -> ViewerError.SESSION_EXPIRED
    DataError.Network, is DataError.Server -> ViewerError.NETWORK
    DataError.Storage, DataError.Unknown -> ViewerError.CANNOT_OPEN
}

internal fun LoadedContent.toContentState(): FileContentState = when (this) {
    is LoadedContent.Downloading -> FileContentState.Downloading(bytesRead, totalBytes)
    is LoadedContent.Image -> FileContentState.Image(url)
    is LoadedContent.Pdf -> FileContentState.Pdf(file, pageCount)
    is LoadedContent.Media -> FileContentState.Media(url)
    is LoadedContent.Code -> FileContentState.Code(file, content)
    is LoadedContent.External -> FileContentState.External(file, mimeType)
    LoadedContent.CorruptPdf -> FileContentState.Error(ViewerError.CORRUPT_PDF)
    is LoadedContent.Failed -> FileContentState.Error(error.toViewerError())
}
