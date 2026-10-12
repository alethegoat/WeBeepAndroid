package it.polimi.webeep.feature.auth.domain

import it.polimi.webeep.core.util.Resource

interface AuthRepository {
    suspend fun validateToken(token: String): Resource<UserSession>

    suspend fun saveAuthData(token: String, session: UserSession)

    suspend fun isLoggedIn(): Boolean

    suspend fun logout()

    /** Wipes cached course data, downloads and caches but keeps credentials and pending SSO state. */
    suspend fun clearLocalData()
}
