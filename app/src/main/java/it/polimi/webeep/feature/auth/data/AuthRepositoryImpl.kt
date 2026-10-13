package it.polimi.webeep.feature.auth.data

import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.resourceOf
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.core.session.SessionDataCleaner
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.UserSession
import it.polimi.webeep.feature.sync.domain.SyncScheduling
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRepositoryImpl @Inject constructor(
    private val apiService: MoodleApiService,
    private val tokenProvider: TokenProvider,
    private val syncScheduler: SyncScheduling,
    private val sessionDataCleaner: SessionDataCleaner
) : AuthRepository {

    // Manual logout and SessionExpiryHandler may overlap: run wipes one at a time.
    private val logoutMutex = Mutex()

    override suspend fun validateToken(token: String): Resource<UserSession> = resourceOf {
        apiService.getSiteInfo(wsToken = token).toUserSession()
    }

    override suspend fun saveAuthData(token: String, session: UserSession) {
        tokenProvider.saveAuthData(
            token = token,
            userId = session.userId
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
        sessionDataCleaner.wipe()
    }

    private fun SiteInfoDto.toUserSession(): UserSession = UserSession(
        userId = userid,
        fullName = fullname,
        userName = username,
        siteUrl = siteurl,
        avatarUrl = MoodleFileUrlBuilder.stripAuthFromUrl(userpictureurl)
    )
}
