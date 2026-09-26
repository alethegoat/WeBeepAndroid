package it.polimi.webeep.feature.fileviewer.presentation

import android.app.Application
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.R
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.network.MoodleBinaryDownload
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.feature.download.data.executeCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject

sealed interface FileViewerUiState {
    data object Loading : FileViewerUiState
    data class Image(val url: String) : FileViewerUiState
    data class Pdf(val file: File, val pageCount: Int) : FileViewerUiState
    data class Media(val url: String) : FileViewerUiState
    data class Code(val content: String) : FileViewerUiState
    data class CustomTab(val url: String) : FileViewerUiState
    data class ExternalViewer(val file: File, val mimeType: String) : FileViewerUiState
    data class Error(@StringRes val messageRes: Int) : FileViewerUiState
}

/**
 * ViewModel for file preview. Auth tokens are added only by OkHttp interceptors
 * (pluginfile): no URL in the UI state carries a token.
 */
@HiltViewModel
class FileViewerViewModel @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val okHttpClient: OkHttpClient,
    private val application: Application,
    savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val title: String = savedStateHandle["title"] ?: "Preview"
    private val url: String = MoodleFileUrlBuilder.stripAuthFromUrl(savedStateHandle["url"] ?: "")
    private val mimeType: String = savedStateHandle["mimeType"] ?: ""

    private val _uiState = MutableStateFlow<FileViewerUiState>(FileViewerUiState.Loading)
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()

    /**
     * Shared authenticated client for streaming media (ExoPlayer).
     */
    val mediaCallFactory: Call.Factory
        get() = okHttpClient

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.value = FileViewerUiState.Loading
            try {
                if (isExternalWebLink(url)) {
                    _uiState.value = FileViewerUiState.CustomTab(url)
                    return@launch
                }

                tokenProvider.getToken() ?: throw ViewerException(R.string.error_session_expired)

                _uiState.value = when {
                    mimeType.startsWith("image/") -> FileViewerUiState.Image(url)
                    mimeType == "application/pdf" || title.endsWith(".pdf", ignoreCase = true) -> {
                        val file = downloadToCache(url, "pdf")
                        FileViewerUiState.Pdf(file = file, pageCount = readPdfPageCount(file))
                    }
                    mimeType.startsWith("video/") || mimeType.startsWith("audio/") || title.endsWith(".mp4", ignoreCase = true) -> {
                        FileViewerUiState.Media(url = url)
                    }
                    isCodeFile(title, mimeType) -> {
                        val file = downloadToCache(url, MoodleFileUrlBuilder.extractExtension(title) ?: "txt")
                        if (file.length() > MAX_CODE_VIEW_BYTES) {
                            val resolvedMimeType = if (mimeType.isBlank()) "text/plain" else mimeType
                            FileViewerUiState.ExternalViewer(file = file, mimeType = resolvedMimeType)
                        } else {
                            FileViewerUiState.Code(content = file.readText())
                        }
                    }
                    else -> {
                        val ext = MoodleFileUrlBuilder.extractExtension(title) ?: "tmp"
                        val file = downloadToCache(url, ext)
                        val resolvedMimeType = if (
                            mimeType.isBlank() ||
                            mimeType == "application/octet-stream" ||
                            mimeType == "application/forcedownload"
                        ) {
                            android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase()) ?: "*/*"
                        } else {
                            mimeType
                        }
                        FileViewerUiState.ExternalViewer(file = file, mimeType = resolvedMimeType)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                ensureActive()
                _uiState.value = FileViewerUiState.Error(errorMessageRes(e))
            }
        }
    }

    private fun isExternalWebLink(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) &&
            !lowerUrl.contains("pluginfile.php")
    }

    private suspend fun downloadToCache(url: String, extension: String): File {
        val localName = it.polimi.webeep.core.util.FileUtils.buildLocalFileName(title, url)
        val downloadsDir = File(application.filesDir, DOWNLOADS_DIR)
        val alreadyDownloaded = File(downloadsDir, localName)
        if (alreadyDownloaded.exists() && alreadyDownloaded.length() > 0) {
            return alreadyDownloaded
        }

        val cacheDir = File(application.cacheDir, CACHE_DIR).apply { mkdirs() }
        val cacheKey = it.polimi.webeep.core.util.FileUtils.stableUrlHash(url)
        val target = File(cacheDir, "$cacheKey.$extension")
        if (target.exists() && target.length() > 0) {
            return target
        }

        evictCacheIfNeeded(cacheDir)
        val part = File(cacheDir, "$cacheKey.$extension.part")
        if (part.exists()) {
            part.delete()
        }

        val request = Request.Builder().url(url).build()
        try {
            okHttpClient.newCall(request).executeCancellable { response ->
                val body = MoodleBinaryDownload.requireBinaryBody(response)
                part.outputStream().use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: Exception) {
            part.delete()
            throw e
        }
        if (target.exists()) {
            target.delete()
        }
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }

        return target
    }

    private fun readPdfPageCount(file: File): Int {
        return try {
            getPdfPageCount(file)
        } catch (_: Exception) {
            // A corrupted cached copy would otherwise fail on every retry.
            if (file.parentFile?.name == CACHE_DIR) {
                file.delete()
            }
            throw ViewerException(R.string.file_viewer_error_pdf)
        }
    }

    @StringRes
    private fun errorMessageRes(e: Exception): Int = when (e) {
        is ViewerException -> e.messageRes
        is IOException -> R.string.file_viewer_error_network
        else -> R.string.error_cannot_open_file
    }

    private class ViewerException(@StringRes val messageRes: Int) : Exception()

    private fun evictCacheIfNeeded(cacheDir: File) {
        val files = cacheDir.listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= MAX_CACHE_BYTES) return
            val size = file.length()
            if (file.delete()) {
                total -= size
            }
        }
    }

    private fun getPdfPageCount(file: File): Int {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor.use { parcelFileDescriptor ->
            PdfRenderer(parcelFileDescriptor).use { renderer ->
                return renderer.pageCount
            }
        }
    }

    private fun isCodeFile(title: String, mimeType: String): Boolean {
        if (mimeType.startsWith("text/")) return true
        val ext = MoodleFileUrlBuilder.extractExtension(title) ?: return false
        val codeExtensions = setOf("java", "kt", "c", "cpp", "py", "vhd", "js", "html", "css", "json", "xml", "md", "txt", "csv")
        return ext in codeExtensions
    }

    private companion object {
        const val CACHE_DIR = "viewer"
        const val DOWNLOADS_DIR = "downloads"
        const val MAX_CACHE_BYTES = 80L * 1024L * 1024L
        const val MAX_CODE_VIEW_BYTES = 1L * 1024L * 1024L
    }
}
