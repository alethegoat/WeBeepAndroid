package it.polimi.webeep.feature.fileviewer.presentation

import android.app.Application
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.network.MoodleBinaryDownload
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

sealed interface FileViewerUiState {
    data object Loading : FileViewerUiState
    data class Image(val url: String) : FileViewerUiState
    data class Pdf(val file: File, val pageCount: Int) : FileViewerUiState
    data class Media(val url: String) : FileViewerUiState
    data class Code(val content: String) : FileViewerUiState
    data class Web(val url: String) : FileViewerUiState
    data class CustomTab(val url: String) : FileViewerUiState
    data class ExternalViewer(val file: File, val mimeType: String) : FileViewerUiState
    data class Error(val message: String) : FileViewerUiState
}

/**
 * ViewModel for file preview. Auth tokens are added by OkHttp interceptors
 * (pluginfile) or, for ExoPlayer only, via [MoodleFileUrlBuilder]. WebView never
 * receives a token in the URL.
 */
@HiltViewModel
class FileViewerViewModel @Inject constructor(
    private val tokenManager: TokenManager,
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

                val token = tokenManager.getToken()
                    ?: throw IllegalStateException("Session expired. Please login again.")

                _uiState.value = when {
                    mimeType.startsWith("image/") -> FileViewerUiState.Image(url)
                    mimeType == "application/pdf" || title.endsWith(".pdf", ignoreCase = true) -> {
                        val file = downloadToCache(url, "pdf")
                        FileViewerUiState.Pdf(file = file, pageCount = getPdfPageCount(file))
                    }
                    mimeType.startsWith("video/") || mimeType.startsWith("audio/") || title.endsWith(".mp4", ignoreCase = true) -> {
                        FileViewerUiState.Media(
                            url = MoodleFileUrlBuilder.buildAuthenticatedUrl(url, token)
                        )
                    }
                    isCodeFile(title, mimeType) -> {
                        val file = downloadToCache(url, MoodleFileUrlBuilder.extractExtension(title) ?: "txt")
                        FileViewerUiState.Code(content = file.readText())
                    }
                    mimeType == "text/html" -> FileViewerUiState.Web(url)
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
                _uiState.value = FileViewerUiState.Error(e.localizedMessage ?: "Cannot open file")
            }
        }
    }

    private fun isExternalWebLink(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) &&
            !lowerUrl.contains("pluginfile.php")
    }

    private fun downloadToCache(url: String, extension: String): File {
        val cacheDir = File(application.cacheDir, CACHE_DIR).apply { mkdirs() }
        evictCacheIfNeeded(cacheDir)
        val target = File(cacheDir, "${url.hashCode()}.$extension")
        val part = File(cacheDir, "${url.hashCode()}.$extension.part")
        if (part.exists()) {
            part.delete()
        }

        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            val body = MoodleBinaryDownload.requireBinaryBody(response)
            part.outputStream().use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
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
        const val MAX_CACHE_BYTES = 80L * 1024L * 1024L
    }
}
