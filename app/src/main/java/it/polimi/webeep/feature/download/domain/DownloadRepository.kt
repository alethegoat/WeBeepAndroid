package it.polimi.webeep.feature.download.domain

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    /**
     * In-app download (not the system DownloadManager); returns a local id.
     */
    fun downloadFile(url: String, token: String?, localFileName: String, displayName: String, mimeType: String?): Long

    fun cancelDownload(downloadId: Long)

    /**
     * Emits 0–100, -1 on error.
     */
    fun getDownloadProgress(downloadId: Long): Flow<Int>

    fun openFile(fileName: String): Boolean

    fun isFileDownloaded(fileName: String): Boolean

    /**
     * FileProvider URI for the cached file.
     */
    fun getDownloadedFileUri(fileName: String): Uri?

    fun clearAllDownloads()
}
