package it.polimi.webeep.feature.auth.data

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.di.MainDispatcher
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.UserSession
import it.polimi.webeep.feature.sync.SyncNotifier
import it.polimi.webeep.feature.sync.SyncScheduler
import it.polimi.webeep.feature.sync.SyncStateStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: MoodleApiService,
    private val tokenProvider: TokenProvider,
    private val application: Application,
    private val syncScheduler: SyncScheduler,
    private val syncStateStore: SyncStateStore,
    private val weBeepDatabase: it.polimi.webeep.core.database.WeBeepDatabase,
    private val downloadRepository: it.polimi.webeep.feature.download.domain.DownloadRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) : AuthRepository {

    // Manual logout and SessionExpiryHandler may overlap: run wipes one at a time.
    private val logoutMutex = Mutex()

    override suspend fun validateToken(token: String): Result<UserSession> {
        return runCatching {
            apiService.getSiteInfo(wsToken = token).toUserSession()
        }
    }

    override suspend fun saveAuthData(token: String, privateToken: String?, session: UserSession) {
        tokenProvider.saveAuthData(
            token = token,
            privateToken = privateToken,
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
            runCatching { weBeepDatabase.clearAllTables() }
            runCatching { downloadRepository.clearAllDownloads() }
            runCatching { syncStateStore.clear() }
        }
        SyncNotifier.cancelAll(application)
        withContext(mainDispatcher) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }
        withContext(ioDispatcher) {
            deleteQuietly(File(application.cacheDir, "http_cache"))
            deleteQuietly(File(application.cacheDir, "viewer"))
            deleteQuietly(File(application.cacheDir, "image_cache"))
        }
    }

    private fun SiteInfoDto.toUserSession(): UserSession {
        return UserSession(
            userId = userid,
            fullName = fullname,
            userName = username,
            siteUrl = siteurl,
            avatarUrl = MoodleFileUrlBuilder.stripAuthFromUrl(userpictureurl)
        )
    }

    private fun deleteQuietly(file: File) {
        runCatching { file.deleteRecursively() }
    }
}
