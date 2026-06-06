package it.polimi.webeep.feature.auth.data

import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * implementation of [AuthRepository] using moodle apis and [TokenManager].
 */
class AuthRepositoryImpl @Inject constructor(
    private val apiService: MoodleApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun validateToken(token: String): Result<SiteInfoDto> {
        // attempt to retrieve the user's site info; if it fails, the token is considered invalid
        return runCatching {
            apiService.getSiteInfo(wsToken = token)
        }
    }

    override suspend fun saveAuthData(token: String, privateToken: String?, siteInfo: SiteInfoDto) {
        tokenManager.saveAuthData(
            token = token,
            privateToken = privateToken,
            userId = siteInfo.userid,
            fullName = siteInfo.fullname
        )
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn.first()
    }

    override suspend fun logout() {
        tokenManager.clearAll()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
    }
}
