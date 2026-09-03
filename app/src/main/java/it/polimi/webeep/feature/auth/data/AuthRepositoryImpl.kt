package it.polimi.webeep.feature.auth.data

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.UserSession
import it.polimi.webeep.feature.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: MoodleApiService,
    private val tokenManager: TokenManager,
    private val application: Application,
    private val syncScheduler: SyncScheduler,
    private val weBeepDatabase: it.polimi.webeep.core.database.WeBeepDatabase,
    private val downloadRepository: it.polimi.webeep.feature.download.domain.DownloadRepository
) : AuthRepository {

    override suspend fun validateToken(token: String): Result<UserSession> {
        return runCatching {
            apiService.getSiteInfo(wsToken = token).toUserSession()
        }
    }

    override suspend fun saveAuthData(token: String, privateToken: String?, session: UserSession) {
        tokenManager.saveAuthData(
            token = token,
            privateToken = privateToken,
            userId = session.userId,
            fullName = session.fullName
        )
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn.first()
    }

    override suspend fun logout() {
        syncScheduler.cancelAll()
        tokenManager.clearAll()
        withContext(Dispatchers.IO) {
            runCatching { weBeepDatabase.clearAllTables() }
            runCatching { downloadRepository.clearAllDownloads() }
        }
        withContext(Dispatchers.Main) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }
        deleteQuietly(File(application.cacheDir, "http_cache"))
        deleteQuietly(File(application.cacheDir, "viewer"))
        deleteQuietly(File(application.cacheDir, "image_cache"))
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
