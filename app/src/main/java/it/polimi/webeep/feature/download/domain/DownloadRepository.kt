package it.polimi.webeep.feature.download.domain

import it.polimi.webeep.core.common.Resource
import java.io.File
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

    fun isFileDownloaded(fileName: String): Boolean

    /** The finished local copy, or null when it is not on disk. */
    fun downloadedFile(fileName: String): File?

    fun clearAllDownloads()

    companion object {
        const val PROGRESS_FAILED = -1
        const val PROGRESS_COMPLETE = 100
    }
}
