package it.polimi.webeep.feature.fileviewer.presentation

import android.app.Application
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import kotlinx.coroutines.Dispatchers
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
    data class ExternalViewer(val file: File, val mimeType: String) : FileViewerUiState
    data class Error(val message: String) : FileViewerUiState
}

/**
 * viewmodel for handling file viewer state and loading logic.
 */
@HiltViewModel
class FileViewerViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient,
    private val application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val title: String = savedStateHandle["title"] ?: "Preview"
    private val url: String = savedStateHandle["url"] ?: ""
    private val mimeType: String = savedStateHandle["mimeType"] ?: ""

    private val _uiState = MutableStateFlow<FileViewerUiState>(FileViewerUiState.Loading)
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = FileViewerUiState.Loading
            try {
                if (isExternalWebLink(url)) {
                    _uiState.value = FileViewerUiState.Web(url)
                    return@launch
                }

                val token = tokenManager.getToken() ?: throw IllegalStateException("Session expired. Please login again.")
                val authenticatedUrl = MoodleFileUrlBuilder.buildAuthenticatedUrl(url, token)

                _uiState.value = when {
                    mimeType.startsWith("image/") -> FileViewerUiState.Image(authenticatedUrl)
                    mimeType == "application/pdf" || title.endsWith(".pdf", ignoreCase = true) -> {
                        // pdf files require local caching to be rendered by the native pdfrenderer
                        val file = downloadToCache(authenticatedUrl, "pdf")
                        FileViewerUiState.Pdf(file = file, pageCount = getPdfPageCount(file))
                    }
                    mimeType.startsWith("video/") || mimeType.startsWith("audio/") || title.endsWith(".mp4", ignoreCase = true) -> {
                        FileViewerUiState.Media(url = authenticatedUrl)
                    }
                    isCodeFile(title, mimeType) -> {
                        val file = downloadToCache(authenticatedUrl, MoodleFileUrlBuilder.extractExtension(title) ?: "txt")
                        FileViewerUiState.Code(content = file.readText())
                    }
                    mimeType == "text/html" -> FileViewerUiState.Web(authenticatedUrl)
                    else -> {
                        val ext = MoodleFileUrlBuilder.extractExtension(title) ?: "tmp"
                        val file = downloadToCache(authenticatedUrl, ext)
                        
                        // ensure word/excel/etc. files open correctly if moodle
                        // returns fake mimetypes like 'application/octet-stream' or 'application/forcedownload'
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
        val target = File(cacheDir, "${url.hashCode()}.$extension")

        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Preview download failed (${response.code})")
            }
            val body = response.body ?: throw IllegalStateException("Empty response from server")
            target.outputStream().use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
        }

        return target
    }

    private fun getPdfPageCount(file: File): Int {
        // read the first few bytes of the pdf to determine the total number of pages
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
    }
}
