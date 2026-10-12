package it.polimi.webeep.feature.download.data

import android.app.Application
import it.polimi.webeep.core.common.AppDirectories
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.FileUtils
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.runCatchingCancellable
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.network.MoodleBinaryDownload
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.storage.LocalFileStore
import it.polimi.webeep.feature.download.domain.DownloadRepository
import it.polimi.webeep.feature.download.domain.DownloadRepository.Companion.PROGRESS_COMPLETE
import it.polimi.webeep.feature.download.domain.DownloadRepository.Companion.PROGRESS_FAILED
import java.io.File
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody

/**
 * Downloads files with OkHttp into internal app storage (filesDir/downloads).
 * Writes to a temp file and only replaces the previous copy on success.
 * Downloads run on a repository-owned scope, so they outlive the screen that started them and are stopped
 * only by [cancelDownload] or [clearAllDownloads]. Progress state is in memory and is lost on process death.
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
    private val store: LocalFileStore by lazy {
        LocalFileStore(File(context.filesDir, AppDirectories.DOWNLOADS), MAX_DOWNLOADS_BYTES)
    }
    private val migrationJob: Job = scope.launch { migrateLegacyDownloads() }

    override fun downloadFile(url: String, token: String?, localFileName: String, displayName: String, mimeType: String?): Resource<Long> {
        val safeLocalFileName = sanitizeFileName(localFileName)
        val destination = store.containedFile(safeLocalFileName) ?: return Resource.Error(DataError.Storage)
        val part = store.partFileFor(destination)

        val call = try {
            okHttpClient.newCall(Request.Builder().url(requestUrl(url, token)).build())
        } catch (e: IllegalArgumentException) {
            return Resource.Error(DataError.Unknown, e)
        }

        val id = nextId.getAndIncrement()
        val state = MutableStateFlow(0)
        progress[id] = state
        // Registered before launching so an immediate cancelDownload() reaches the network call.
        calls[id] = call
        jobs[id] = scope.launch { runDownload(id, call, part, destination, state) }
        return Resource.Success(id)
    }

    private suspend fun CoroutineScope.runDownload(id: Long, call: Call, part: File, destination: File, state: MutableStateFlow<Int>) {
        try {
            migrationJob.join()
            store.ensureDirectory()
            if (part.exists()) {
                part.delete()
            }
            call.executeCancellable { response ->
                val body = MoodleBinaryDownload.requireBinaryBody(response)
                part.outputStream().use { output -> body.copyTo(output, state) }
            }
            ensureActive()
            store.commit(part, destination)
            store.evict(keep = destination)
            state.value = PROGRESS_COMPLETE
        } catch (e: CancellationException) {
            part.delete()
            throw e
        } catch (_: Exception) {
            part.delete()
            state.value = PROGRESS_FAILED
        } finally {
            jobs.remove(id)
            calls.remove(id)
            if (state.value == PROGRESS_COMPLETE || state.value == PROGRESS_FAILED) {
                scheduleProgressCleanup(id, state)
            }
        }
    }

    private fun ResponseBody.copyTo(output: OutputStream, state: MutableStateFlow<Int>) {
        val total = contentLength()
        byteStream().use { input ->
            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
            var read = 0L
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                output.write(buffer, 0, n)
                read += n
                if (total > 0) {
                    state.value = ((read * 100) / total).toInt().coerceIn(0, MAX_IN_FLIGHT_PROGRESS)
                }
            }
        }
    }

    private fun requestUrl(url: String, token: String?): String {
        val cleanUrl = MoodleFileUrlBuilder.stripAuthFromUrl(url)
        return if (token != null) MoodleFileUrlBuilder.buildAuthenticatedUrl(cleanUrl, token) else cleanUrl
    }

    override fun cancelDownload(downloadId: Long) {
        calls.remove(downloadId)?.cancel()
        jobs.remove(downloadId)?.cancel()
        progress.remove(downloadId)
    }

    /**
     * Completes after the terminal value ([PROGRESS_COMPLETE] or [PROGRESS_FAILED]). Terminal states are kept
     * for [PROGRESS_RETENTION_MS] so late or repeated collectors see the result.
     */
    override fun getDownloadProgress(downloadId: Long): Flow<Int> {
        val state = progress[downloadId] ?: return flowOf(PROGRESS_FAILED)
        return state.transformWhile { value ->
            emit(value)
            value in 0 until PROGRESS_COMPLETE
        }
    }

    override fun clearAllDownloads() {
        calls.values.forEach { it.cancel() }
        calls.clear()
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        progress.clear()
        store.directory.listFiles()?.forEach { it.delete() }
    }

    override fun isFileDownloaded(fileName: String): Boolean {
        val file = store.containedFile(sanitizeFileName(fileName)) ?: return false
        return file.exists() && file.length() > 0
    }

    override fun downloadedFile(fileName: String): File? = store.containedFile(sanitizeFileName(fileName))?.takeIf { it.exists() }

    private fun scheduleProgressCleanup(id: Long, state: MutableStateFlow<Int>) {
        scope.launch {
            delay(PROGRESS_RETENTION_MS)
            progress.remove(id, state)
        }
    }

    /**
     * One-shot move of files saved by older versions in external app storage.
     */
    private fun migrateLegacyDownloads() {
        val marker = File(context.filesDir, MIGRATION_MARKER)
        if (marker.exists()) return
        // Best effort: a failed migration is retried on the next launch because the marker is not written.
        runCatchingCancellable {
            val externalRoot = context.getExternalFilesDir(null) ?: return
            LegacyDownloadsMigration.migrate(File(externalRoot, AppDirectories.DOWNLOADS), store.directory)
            marker.createNewFile()
        }
    }

    private fun sanitizeFileName(fileName: String): String = FileUtils.sanitizeFileName(fileName).take(MAX_FILE_NAME_LENGTH)

    private companion object {
        const val MAX_FILE_NAME_LENGTH = 120
        const val DOWNLOAD_BUFFER_SIZE = 32 * 1024
        const val MAX_DOWNLOADS_BYTES = 500L * 1024L * 1024L
        const val PROGRESS_RETENTION_MS = 60_000L
        const val MAX_IN_FLIGHT_PROGRESS = 99
        const val MIGRATION_MARKER = "downloads_internal_migrated"
    }
}
