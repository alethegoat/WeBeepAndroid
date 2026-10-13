package it.polimi.webeep.feature.fileviewer.data

import android.app.Application
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import it.polimi.webeep.core.common.AppDirectories
import it.polimi.webeep.core.common.FileUtils
import it.polimi.webeep.core.common.MissingTokenException
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.resourceOf
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.network.MoodleBinaryDownload
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.storage.LocalFileStore
import it.polimi.webeep.core.storage.mimeTypeForFileName
import it.polimi.webeep.feature.download.data.executeCancellable
import it.polimi.webeep.feature.fileviewer.domain.FileContentRepository
import it.polimi.webeep.feature.fileviewer.domain.LoadedContent
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
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

/**
 * Downloads and classifies files for the viewer. Auth tokens are added only by
 * the OkHttp interceptors (pluginfile); files already saved in `downloads/` and
 * the bounded `viewer/` cache are reused without hitting the network.
 * Downloads are written to `.part` files and committed atomically, and cancelling the collector cancels the HTTP call.
 */
class FileContentRepositoryImpl @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val okHttpClient: OkHttpClient,
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FileContentRepository {

    private val viewerCache by lazy {
        LocalFileStore(File(application.cacheDir, AppDirectories.VIEWER_CACHE), MAX_CACHE_BYTES)
    }

    override fun load(file: ViewerFile): Flow<LoadedContent> = channelFlow {
        send(LoadedContent.Downloading(0L, file.size.takeIf { it > 0 }))
        val loaded = resourceOf {
            tokenProvider.getToken() ?: throw MissingTokenException()
            classify(file)
        }
        send(
            when (loaded) {
                is Resource.Success -> loaded.data
                is Resource.Error -> LoadedContent.Failed(loaded.error)
            }
        )
    }.flowOn(ioDispatcher)

    override suspend fun localCopy(file: ViewerFile): File = withContext(ioDispatcher) {
        download(file, MoodleFileUrlBuilder.extractExtension(file.title) ?: "tmp", onProgress = null)
    }

    override fun resolveMimeType(file: ViewerFile): String {
        val mimeType = file.mimeType
        val isVague = mimeType.isBlank() ||
            mimeType == "application/octet-stream" ||
            mimeType == "application/forcedownload"
        if (!isVague) return mimeType
        return mimeTypeForFileName(file.title)
    }

    private suspend fun ProducerScope<LoadedContent>.classify(file: ViewerFile): LoadedContent {
        val title = file.title
        val mimeType = file.mimeType
        // Progress is best effort: dropped rather than slowing the download down.
        val onProgress: (LoadedContent.Downloading) -> Unit = { trySend(it) }
        return when {
            mimeType.startsWith("image/") -> LoadedContent.Image(file.url)
            mimeType == "application/pdf" || title.endsWith(".pdf", ignoreCase = true) -> {
                val local = download(file, "pdf", onProgress)
                readPdfPageCount(local)
                    ?.let { LoadedContent.Pdf(file = local, pageCount = it) }
                    ?: LoadedContent.CorruptPdf
            }
            mimeType.startsWith("video/") ||
                mimeType.startsWith("audio/") ||
                title.endsWith(".mp4", ignoreCase = true) -> LoadedContent.Media(file.url)
            isCodeFile(title, mimeType) -> {
                val local = download(file, MoodleFileUrlBuilder.extractExtension(title) ?: "txt", onProgress)
                if (local.length() > MAX_CODE_VIEW_BYTES) {
                    LoadedContent.External(local, mimeType.ifBlank { "text/plain" })
                } else {
                    LoadedContent.Code(file = local, content = local.readText())
                }
            }
            else -> {
                val ext = MoodleFileUrlBuilder.extractExtension(title) ?: "tmp"
                val local = download(file, ext, onProgress)
                LoadedContent.External(local, resolveMimeType(file))
            }
        }
    }

    private suspend fun download(file: ViewerFile, extension: String, onProgress: ((LoadedContent.Downloading) -> Unit)?): File {
        val localName = FileUtils.buildLocalFileName(file.title, file.url)
        val alreadyDownloaded = File(File(application.filesDir, AppDirectories.DOWNLOADS), localName)
        if (alreadyDownloaded.exists() && alreadyDownloaded.length() > 0) {
            return alreadyDownloaded
        }

        val cacheStore = viewerCache
        cacheStore.ensureDirectory()
        val cacheKey = FileUtils.stableUrlHash(file.url)
        val target = checkNotNull(cacheStore.containedFile("$cacheKey.$extension")) { "Viewer cache name escaped its directory" }
        if (target.exists() && target.length() > 0) {
            return target
        }

        cacheStore.evict()
        val part = cacheStore.partFileFor(target)
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
        cacheStore.commit(part, target)
        return target
    }

    private fun copyWithProgress(
        input: InputStream,
        destination: File,
        totalBytes: Long?,
        job: Job?,
        onProgress: ((LoadedContent.Downloading) -> Unit)?
    ) {
        val step = totalBytes?.let { (it / 100).coerceAtLeast(MIN_PROGRESS_STEP_BYTES) } ?: UNKNOWN_PROGRESS_STEP_BYTES
        var lastReported = 0L
        val read = input.use { source ->
            destination.outputStream().use { output ->
                source.copyChunks(output, job) { copied ->
                    if (onProgress != null && copied - lastReported >= step) {
                        lastReported = copied
                        onProgress(LoadedContent.Downloading(copied, totalBytes))
                    }
                }
            }
        }
        onProgress?.invoke(LoadedContent.Downloading(read, totalBytes ?: read))
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

    /** Page count, or null when the file is not a readable PDF. */
    private fun readPdfPageCount(file: File): Int? = try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
        }
    } catch (_: Exception) {
        // A corrupted cached copy would otherwise fail on every retry.
        if (file.parentFile?.name == AppDirectories.VIEWER_CACHE) {
            file.delete()
        }
        null
    }

    private fun isCodeFile(title: String, mimeType: String): Boolean {
        if (mimeType.startsWith("text/")) return true
        val ext = MoodleFileUrlBuilder.extractExtension(title) ?: return false
        return ext in CODE_EXTENSIONS
    }

    private companion object {
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
