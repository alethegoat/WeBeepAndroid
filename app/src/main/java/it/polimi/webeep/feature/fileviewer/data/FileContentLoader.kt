package it.polimi.webeep.feature.fileviewer.data

import android.app.Application
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import androidx.annotation.StringRes
import it.polimi.webeep.R
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.network.MoodleBinaryDownload
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.util.FileUtils
import it.polimi.webeep.feature.download.data.executeCancellable
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

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
    data class Error(@StringRes val messageRes: Int) : FileContentState
}

/** Local copy behind a ready state, if the state was built from one. */
val FileContentState.localFile: File?
    get() = when (this) {
        is FileContentState.Pdf -> file
        is FileContentState.Code -> file
        is FileContentState.External -> file
        else -> null
    }

/**
 * Downloads and classifies files for the viewer. Auth tokens are added only by
 * the OkHttp interceptors (pluginfile); files already saved in `downloads/` and
 * the bounded `viewer/` cache are reused without hitting the network.
 */
class FileContentLoader @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val okHttpClient: OkHttpClient,
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    fun load(file: ViewerFile): Flow<FileContentState> = channelFlow {
        send(FileContentState.Downloading(0L, file.size.takeIf { it > 0 }))
        val result = try {
            tokenProvider.getToken() ?: throw ViewerException(R.string.error_session_expired)
            classify(file)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            FileContentState.Error(errorMessageRes(e))
        }
        send(result)
    }.flowOn(ioDispatcher)

    /** Local copy of [file] for sharing or opening elsewhere; downloads it if needed. */
    suspend fun localCopy(file: ViewerFile): File = withContext(ioDispatcher) {
        download(file, MoodleFileUrlBuilder.extractExtension(file.title) ?: "tmp", onProgress = null)
    }

    /** MIME type to hand to other apps, inferred from the extension when Moodle is vague. */
    fun resolveMimeType(file: ViewerFile): String {
        val mimeType = file.mimeType
        val isVague = mimeType.isBlank() ||
            mimeType == "application/octet-stream" ||
            mimeType == "application/forcedownload"
        if (!isVague) return mimeType
        val ext = MoodleFileUrlBuilder.extractExtension(file.title)?.lowercase() ?: return "*/*"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    private suspend fun ProducerScope<FileContentState>.classify(file: ViewerFile): FileContentState {
        val title = file.title
        val mimeType = file.mimeType
        // Progress is best effort: dropped rather than slowing the download down.
        val onProgress: (FileContentState.Downloading) -> Unit = { trySend(it) }
        return when {
            mimeType.startsWith("image/") -> FileContentState.Image(file.url)
            mimeType == "application/pdf" || title.endsWith(".pdf", ignoreCase = true) -> {
                val local = download(file, "pdf", onProgress)
                FileContentState.Pdf(file = local, pageCount = readPdfPageCount(local))
            }
            mimeType.startsWith("video/") || mimeType.startsWith("audio/") ||
                title.endsWith(".mp4", ignoreCase = true) -> FileContentState.Media(file.url)
            isCodeFile(title, mimeType) -> {
                val local = download(file, MoodleFileUrlBuilder.extractExtension(title) ?: "txt", onProgress)
                if (local.length() > MAX_CODE_VIEW_BYTES) {
                    FileContentState.External(local, mimeType.ifBlank { "text/plain" })
                } else {
                    FileContentState.Code(file = local, content = local.readText())
                }
            }
            else -> {
                val ext = MoodleFileUrlBuilder.extractExtension(title) ?: "tmp"
                val local = download(file, ext, onProgress)
                FileContentState.External(local, resolveMimeType(file))
            }
        }
    }

    private suspend fun download(
        file: ViewerFile,
        extension: String,
        onProgress: ((FileContentState.Downloading) -> Unit)?
    ): File {
        val localName = FileUtils.buildLocalFileName(file.title, file.url)
        val alreadyDownloaded = File(File(application.filesDir, DOWNLOADS_DIR), localName)
        if (alreadyDownloaded.exists() && alreadyDownloaded.length() > 0) {
            return alreadyDownloaded
        }

        val cacheDir = File(application.cacheDir, CACHE_DIR).apply { mkdirs() }
        val cacheKey = FileUtils.stableUrlHash(file.url)
        val target = File(cacheDir, "$cacheKey.$extension")
        if (target.exists() && target.length() > 0) {
            return target
        }

        evictCacheIfNeeded(cacheDir)
        val part = File(cacheDir, "$cacheKey.$extension.part")
        if (part.exists()) {
            part.delete()
        }

        val job = currentCoroutineContext()[Job]
        val request = Request.Builder().url(file.url).build()
        try {
            okHttpClient.newCall(request).executeCancellable { response ->
                val body = MoodleBinaryDownload.requireBinaryBody(response)
                val total = body.contentLength().takeIf { it > 0 } ?: file.size.takeIf { it > 0 }
                copyWithProgress(body.byteStream(), part, total, job, onProgress)
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

    private fun copyWithProgress(
        input: InputStream,
        destination: File,
        totalBytes: Long?,
        job: Job?,
        onProgress: ((FileContentState.Downloading) -> Unit)?
    ) {
        val step = totalBytes?.let { (it / 100).coerceAtLeast(MIN_PROGRESS_STEP_BYTES) } ?: UNKNOWN_PROGRESS_STEP_BYTES
        var lastReported = 0L
        val read = input.use { source ->
            destination.outputStream().use { output ->
                source.copyChunks(output, job) { copied ->
                    if (onProgress != null && copied - lastReported >= step) {
                        lastReported = copied
                        onProgress(FileContentState.Downloading(copied, totalBytes))
                    }
                }
            }
        }
        onProgress?.invoke(FileContentState.Downloading(read, totalBytes ?: read))
    }

    /** Copies in chunks, stopping when [job] is cancelled; returns the bytes copied. */
    private fun InputStream.copyChunks(output: OutputStream, job: Job?, onChunk: (Long) -> Unit): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var copied = 0L
        var count = read(buffer)
        while (count >= 0) {
            if (job?.isActive == false) throw CancellationException("Viewer download cancelled")
            output.write(buffer, 0, count)
            copied += count
            onChunk(copied)
            count = read(buffer)
        }
        return copied
    }

    private fun readPdfPageCount(file: File): Int {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
            }
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

    private fun isCodeFile(title: String, mimeType: String): Boolean {
        if (mimeType.startsWith("text/")) return true
        val ext = MoodleFileUrlBuilder.extractExtension(title) ?: return false
        return ext in CODE_EXTENSIONS
    }

    private companion object {
        const val CACHE_DIR = "viewer"
        const val DOWNLOADS_DIR = "downloads"
        const val MAX_CACHE_BYTES = 80L * 1024L * 1024L
        const val MAX_CODE_VIEW_BYTES = 1L * 1024L * 1024L
        const val BUFFER_SIZE = 8 * 1024
        const val MIN_PROGRESS_STEP_BYTES = 32L * 1024L
        const val UNKNOWN_PROGRESS_STEP_BYTES = 256L * 1024L
        val CODE_EXTENSIONS = setOf(
            "java", "kt", "c", "cpp", "py", "vhd", "js", "html", "css", "json", "xml", "md", "txt", "csv"
        )
    }
}
