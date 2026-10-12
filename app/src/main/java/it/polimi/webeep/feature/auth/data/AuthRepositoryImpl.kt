package it.polimi.webeep.feature.auth.data

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import it.polimi.webeep.core.database.WeBeepDatabase
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.di.MainDispatcher
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.core.util.AppDirectories
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.core.util.resourceOf
import it.polimi.webeep.core.util.runCatchingCancellable
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.UserSession
import it.polimi.webeep.feature.download.domain.DownloadRepository
import it.polimi.webeep.feature.sync.SyncNotifier
import it.polimi.webeep.feature.sync.SyncScheduler
import it.polimi.webeep.feature.sync.SyncStateStore
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AuthRepositoryImpl @Inject constructor(
    private val apiService: MoodleApiService,
    private val tokenProvider: TokenProvider,
    private val application: Application,
    private val syncScheduler: SyncScheduler,
    private val syncStateStore: SyncStateStore,
    private val weBeepDatabase: WeBeepDatabase,
    private val downloadRepository: DownloadRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) : AuthRepository {

    // Manual logout and SessionExpiryHandler may overlap: run wipes one at a time.
    private val logoutMutex = Mutex()

    override suspend fun validateToken(token: String): Resource<UserSession> = resourceOf {
        apiService.getSiteInfo(wsToken = token).toUserSession()
    }

    override suspend fun saveAuthData(token: String, session: UserSession) {
        tokenProvider.saveAuthData(
            token = token,
            userId = session.userId,
            fullName = session.fullName
        )
    }

    override suspend fun isLoggedIn(): Boolean {
        tokenProvider.ensureLoaded()
        return tokenProvider.isLoggedIn.first()
    }

    override suspend fun logout() {
        logoutMutex.withLock { wipeLocalData(clearCredentials = true) }
    }

    override suspend fun clearLocalData() {
        logoutMutex.withLock { wipeLocalData(clearCredentials = false) }
    }

    private suspend fun wipeLocalData(clearCredentials: Boolean) {
        syncScheduler.cancelAll()
        if (clearCredentials) {
            tokenProvider.clearAll()
        }
        withContext(ioDispatcher) {
            runCatchingCancellable { weBeepDatabase.clearAllTables() }
            runCatchingCancellable { downloadRepository.clearAllDownloads() }
            runCatchingCancellable { syncStateStore.clear() }
        }
        SyncNotifier.cancelAll(application)
        withContext(mainDispatcher) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }
        withContext(ioDispatcher) {
            deleteQuietly(File(application.cacheDir, AppDirectories.HTTP_CACHE))
            deleteQuietly(File(application.cacheDir, AppDirectories.VIEWER_CACHE))
            deleteQuietly(File(application.cacheDir, AppDirectories.IMAGE_CACHE))
        }
    }

    private fun SiteInfoDto.toUserSession(): UserSession = UserSession(
        userId = userid,
        fullName = fullname,
        userName = username,
        siteUrl = siteurl,
        avatarUrl = MoodleFileUrlBuilder.stripAuthFromUrl(userpictureurl)
    )

    private fun deleteQuietly(file: File) {
        runCatchingCancellable { file.deleteRecursively() }
    }
}
