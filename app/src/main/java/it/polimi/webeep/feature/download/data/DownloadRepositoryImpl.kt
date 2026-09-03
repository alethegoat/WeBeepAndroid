package it.polimi.webeep.feature.download.data

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.network.MoodleBinaryDownload
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.feature.download.domain.DownloadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads files with OkHttp into app-private storage. Writes to a temp file
 * and only replaces the previous copy on success.
 */
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val context: Application,
    private val okHttpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DownloadRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val nextId = AtomicLong(1)
    private val progress = ConcurrentHashMap<Long, MutableStateFlow<Int>>()
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val calls = ConcurrentHashMap<Long, Call>()

    override fun downloadFile(
        url: String,
        token: String?,
        localFileName: String,
        displayName: String,
        mimeType: String?
    ): Long {
        val directory = getDownloadsDirectory()
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val safeLocalFileName = sanitizeFileName(localFileName)
        val destination = containedFile(directory, safeLocalFileName) ?: return -1L
        val part = containedFile(directory, "$safeLocalFileName.part") ?: return -1L
        if (part.exists()) {
            part.delete()
        }

        val requestUrl = if (token != null) {
            MoodleFileUrlBuilder.buildAuthenticatedUrl(
                MoodleFileUrlBuilder.stripAuthFromUrl(url),
                token
            )
        } else {
            MoodleFileUrlBuilder.stripAuthFromUrl(url)
        }

        val id = nextId.getAndIncrement()
        val state = MutableStateFlow(0)
        progress[id] = state
        jobs[id] = scope.launch {
            try {
                val request = Request.Builder().url(requestUrl).build()
                val call = okHttpClient.newCall(request)
                calls[id] = call
                call.execute().use { response ->
                    val body = MoodleBinaryDownload.requireBinaryBody(response)
                    val total = body.contentLength()
                    part.outputStream().use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                            var read = 0L
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                read += n
                                if (total > 0) {
                                    state.value = ((read * 100) / total).toInt().coerceIn(0, 99)
                                }
                            }
                        }
                    }
                }
                if (destination.exists()) {
                    destination.delete()
                }
                if (!part.renameTo(destination)) {
                    part.copyTo(destination, overwrite = true)
                    part.delete()
                }
                evictDownloadsIfNeeded(keep = destination)
                state.value = 100
            } catch (e: CancellationException) {
                part.delete()
                throw e
            } catch (_: Exception) {
                part.delete()
                state.value = -1
            } finally {
                jobs.remove(id)
                calls.remove(id)
            }
        }
        return id
    }

    override fun cancelDownload(downloadId: Long) {
        calls.remove(downloadId)?.cancel()
        jobs.remove(downloadId)?.cancel()
        progress.remove(downloadId)
    }

    override fun getDownloadProgress(downloadId: Long): Flow<Int> {
        val state = progress[downloadId] ?: return flow { emit(-1) }
        return flow {
            state.collect { value ->
                emit(value)
                if (value == 100 || value == -1) {
                    progress.remove(downloadId)
                }
            }
        }
    }

    override fun clearAllDownloads() {
        calls.values.forEach { it.cancel() }
        calls.clear()
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        progress.clear()
        val directory = getDownloadsDirectory()
        if (directory.exists()) {
            directory.listFiles()?.forEach { it.delete() }
        }
    }

    override fun openFile(fileName: String): Boolean {
        val safeFileName = sanitizeFileName(fileName)
        val fileUri = getDownloadedFileUri(safeFileName) ?: return false
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, getMimeType(safeFileName))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(viewIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    override fun isFileDownloaded(fileName: String): Boolean {
        val directory = getDownloadsDirectory()
        val file = containedFile(directory, sanitizeFileName(fileName)) ?: return false
        return file.exists() && file.length() > 0
    }

    override fun getDownloadedFileUri(fileName: String): Uri? {
        val directory = getDownloadsDirectory()
        val file = containedFile(directory, sanitizeFileName(fileName)) ?: return null
        return if (file.exists()) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            null
        }
    }

    private fun evictDownloadsIfNeeded(keep: File) {
        val directory = getDownloadsDirectory()
        val files = directory.listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_DOWNLOADS_BYTES) return
        files
            .filter { it != keep && !it.name.endsWith(".part") }
            .sortedBy { it.lastModified() }
            .forEach { file ->
                if (total <= MAX_DOWNLOADS_BYTES) return
                val size = file.length()
                if (file.delete()) {
                    total -= size
                }
            }
    }

    private fun getMimeType(fileName: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }

    private fun getDownloadsDirectory(): File {
        return context.getExternalFilesDir(DOWNLOADS_DIR) ?: File(context.filesDir, DOWNLOADS_DIR)
    }

    private fun sanitizeFileName(fileName: String): String {
        val sanitized = fileName
            .replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_")
            .trim()
            .trim('.')

        return sanitized.ifBlank { "file" }.take(MAX_FILE_NAME_LENGTH)
    }

    /**
     * Defense in depth against path traversal: sanitize() already strips
     * separators, but verify the resolved file stays inside [directory].
     */
    private fun containedFile(directory: File, safeName: String): File? {
        return try {
            val dirCanonical = directory.canonicalPath
            val file = File(directory, safeName)
            val fileCanonical = file.canonicalPath
            if (fileCanonical == dirCanonical ||
                fileCanonical.startsWith("$dirCanonical${File.separator}")
            ) {
                file
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val DOWNLOADS_DIR = "downloads"
        const val MAX_FILE_NAME_LENGTH = 120
        const val DOWNLOAD_BUFFER_SIZE = 32 * 1024
        const val MAX_DOWNLOADS_BYTES = 500L * 1024L * 1024L
    }
}
