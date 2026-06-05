package it.polimi.webeep.feature.auth.domain

import it.polimi.webeep.core.network.dto.SiteInfoDto

/**
 * repository for authentication management.
 */
interface AuthRepository {
    /**
     * validates the token by retrieving site info.
     */
    suspend fun validateToken(token: String): Result<SiteInfoDto>

    /**
     * saves authentication data securely.
     */
    suspend fun saveAuthData(token: String, privateToken: String?, siteInfo: SiteInfoDto)

    /**
     * checks if the user is currently authenticated.
     */
    suspend fun isLoggedIn(): Boolean

    /**
     * logs out the user.
     */
    suspend fun logout()
}
