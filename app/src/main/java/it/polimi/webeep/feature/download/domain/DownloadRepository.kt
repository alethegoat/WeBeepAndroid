package it.polimi.webeep.feature.download.domain

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * repository for managing educational file downloads and their opening.
 */
interface DownloadRepository {
    /**
     * Starts an in-app download and returns a local download id.
     */
    fun downloadFile(url: String, token: String?, localFileName: String, displayName: String, mimeType: String?): Long

    /**
     * Cancels an in-flight download started by [downloadFile].
     */
    fun cancelDownload(downloadId: Long)

    /**
     * monitors the download progress as a flow of percentage (0-100).
     */
    fun getDownloadProgress(downloadId: Long): Flow<Int>

    /**
     * opens the downloaded file using an appropriate system intent.
     */
    fun openFile(fileName: String): Boolean

    /**
     * checks if the file has already been downloaded locally.
     */
    fun isFileDownloaded(fileName: String): Boolean

    /**
     * retrieves the safe uri (via fileprovider) to open the file.
     */
    fun getDownloadedFileUri(fileName: String): Uri?

    /**
     * Clears all downloaded files from local storage (e.g. on logout).
     */
    fun clearAllDownloads()
}
