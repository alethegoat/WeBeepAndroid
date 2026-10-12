package it.polimi.webeep.feature.download.domain

import it.polimi.webeep.core.common.Resource
import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * Downloads Moodle files into app-private storage and tracks them by a process-local id.
 *
 * The file name arguments are sanitized by the implementation, which also decides what stays on disk.
 */
interface DownloadRepository {
    /**
     * In-app download (not the system DownloadManager). Returns a local id to follow with
     * [getDownloadProgress], or an error when the download could not even start.
     */
    fun downloadFile(url: String, token: String?, localFileName: String, displayName: String, mimeType: String?): Resource<Long>

    /** Stops [downloadId] and discards its partial file. Unknown ids are ignored. */
    fun cancelDownload(downloadId: Long)

    /**
     * Emits 0–99 while running, then ends with [PROGRESS_COMPLETE] or [PROGRESS_FAILED].
     */
    fun getDownloadProgress(downloadId: Long): Flow<Int>

    /** Whether a non-empty finished copy of [fileName] exists. */
    fun isFileDownloaded(fileName: String): Boolean

    /** The finished local copy, or null when it is not on disk. */
    fun downloadedFile(fileName: String): File?

    /** Cancels every running download and deletes all downloaded files. Used when a session ends. */
    fun clearAllDownloads()

    /** Terminal values of the progress flow. */
    companion object {
        /** Terminal progress value: the download failed or the id is unknown. */
        const val PROGRESS_FAILED = -1

        /** Terminal progress value: the file is fully saved. */
        const val PROGRESS_COMPLETE = 100
    }
}
