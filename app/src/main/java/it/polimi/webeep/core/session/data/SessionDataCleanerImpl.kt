package it.polimi.webeep.core.session.data

import android.app.Application
import it.polimi.webeep.core.common.AppDirectories
import it.polimi.webeep.core.common.runCatchingCancellable
import it.polimi.webeep.core.database.WeBeepDatabase
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.session.SessionDataCleaner
import it.polimi.webeep.feature.download.domain.DownloadRepository
import it.polimi.webeep.feature.sync.domain.SyncNotifications
import it.polimi.webeep.feature.sync.domain.SyncStateRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * [SessionDataCleaner] that wipes the database, downloads, sync state, notifications and the HTTP,
 * viewer and image caches. The app has no WebView, so there is no web session of its own to clear:
 * the SSO runs in the browser, whose cookies the app cannot reach.
 *
 * Each storage step is isolated: one failing must not leave the remaining user data behind.
 * Callers serialize concurrent wipes.
 */
@Singleton
class SessionDataCleanerImpl @Inject constructor(
    private val application: Application,
    private val weBeepDatabase: WeBeepDatabase,
    private val downloadRepository: DownloadRepository,
    private val syncStateRepository: SyncStateRepository,
    private val syncNotifications: SyncNotifications,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SessionDataCleaner {

    override suspend fun wipe() {
        withContext(ioDispatcher) {
            runCatchingCancellable { weBeepDatabase.clearAllTables() }
            runCatchingCancellable { downloadRepository.clearAllDownloads() }
            runCatchingCancellable { syncStateRepository.clear() }
        }
        syncNotifications.cancelAll()
        withContext(ioDispatcher) {
            deleteQuietly(File(application.cacheDir, AppDirectories.HTTP_CACHE))
            deleteQuietly(File(application.cacheDir, AppDirectories.VIEWER_CACHE))
            deleteQuietly(File(application.cacheDir, AppDirectories.IMAGE_CACHE))
        }
    }

    private fun deleteQuietly(file: File) {
        runCatchingCancellable { file.deleteRecursively() }
    }
}
