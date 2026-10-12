package it.polimi.webeep.feature.download.domain

import android.net.Uri
import it.polimi.webeep.core.util.Resource
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    /**
     * In-app download (not the system DownloadManager). Returns a local id to follow with
     * [getDownloadProgress], or an error when the download could not even start.
     */
    fun downloadFile(url: String, token: String?, localFileName: String, displayName: String, mimeType: String?): Resource<Long>

    fun cancelDownload(downloadId: Long)

    /**
     * Emits 0–99 while running, then ends with [PROGRESS_COMPLETE] or [PROGRESS_FAILED].
     */
    fun getDownloadProgress(downloadId: Long): Flow<Int>

    fun openFile(fileName: String): Boolean

    fun isFileDownloaded(fileName: String): Boolean

    /**
     * FileProvider URI for the cached file.
     */
    fun getDownloadedFileUri(fileName: String): Uri?

    fun clearAllDownloads()

    companion object {
        const val PROGRESS_FAILED = -1
        const val PROGRESS_COMPLETE = 100
    }
}
