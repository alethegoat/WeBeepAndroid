package it.polimi.webeep.feature.auth.domain

/**
 * Repository for authentication management.
 */
interface AuthRepository {
    suspend fun validateToken(token: String): Result<UserSession>

    suspend fun saveAuthData(token: String, privateToken: String?, session: UserSession)

    suspend fun isLoggedIn(): Boolean

    suspend fun logout()
}
