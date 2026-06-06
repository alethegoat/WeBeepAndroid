package it.polimi.webeep.feature.download.data

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.feature.download.domain.DownloadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * implementation of [DownloadRepository] based on [DownloadManager] and [FileProvider].
 */
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val context: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DownloadRepository {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    override fun downloadFile(url: String, token: String?, localFileName: String, displayName: String, mimeType: String?): Long {
        val directory = getDownloadsDirectory()
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val safeLocalFileName = sanitizeFileName(localFileName)
        val file = File(directory, safeLocalFileName)
        
        // if the file already exists, removes it to avoid duplicates
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(displayName)
            .setMimeType(mimeType ?: getMimeType(displayName))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            
        // pass token as header to avoid exposing it in url query parameters in system logs
        token?.let { request.addRequestHeader("Authorization", "Bearer $it") }

        return downloadManager.enqueue(request)
    }

    override fun getDownloadProgress(downloadId: Long): Flow<Int> = flow {
        val query = DownloadManager.Query().setFilterById(downloadId)
        var downloading = true
        
        while (downloading) {
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = if (statusColumn != -1) cursor.getInt(statusColumn) else DownloadManager.STATUS_FAILED
                
                val bytesDownloadedColumn = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesDownloaded = if (bytesDownloadedColumn != -1) cursor.getLong(bytesDownloadedColumn) else 0L
                
                val bytesTotalColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val bytesTotal = if (bytesTotalColumn != -1) cursor.getLong(bytesTotalColumn) else 0L

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        emit(100)
                        downloading = false
                    }
                    DownloadManager.STATUS_FAILED -> {
                        emit(-1)
                        downloading = false
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        val progress = if (bytesTotal > 0) (bytesDownloaded * 100L / bytesTotal).toInt() else 0
                        emit(progress)
                    }
                    DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                        emit(0)
                    }
                }
                cursor.close()
            } else {
                cursor?.close()
                emit(-1)
                downloading = false
            }
            if (downloading) {
                delay(500)
            }
        }
    }.flowOn(ioDispatcher)

    override fun openFile(fileName: String): Boolean {
        val safeFileName = sanitizeFileName(fileName)
        val fileUri = getDownloadedFileUri(safeFileName) ?: return false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, getMimeType(safeFileName))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    override fun isFileDownloaded(fileName: String): Boolean {
        val directory = getDownloadsDirectory()
        val file = File(directory, sanitizeFileName(fileName))
        return file.exists() && file.length() > 0
    }

    override fun getDownloadedFileUri(fileName: String): Uri? {
        val directory = getDownloadsDirectory()
        val file = File(directory, sanitizeFileName(fileName))
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

    private companion object {
        const val DOWNLOADS_DIR = "downloads"
        const val MAX_FILE_NAME_LENGTH = 120
    }
}
