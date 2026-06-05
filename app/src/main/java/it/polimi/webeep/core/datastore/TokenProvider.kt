package it.polimi.webeep.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * interface for the authentication token management contract.
 * allows injection of fake implementations in tests.
 */
interface TokenProvider {
    /** flow emitting user authentication state. */
    val isLoggedIn: Flow<Boolean>
    /** current ws token as flow (null if not authenticated). */
    val wsToken: Flow<String?>
    /** synchronous token read for okhttp interceptor (thread-safe). */
    fun getTokenSync(): String?
    /** securely saves authentication data. */
    suspend fun saveAuthData(token: String, privateToken: String?, userId: Int, fullName: String)
    /** retrieves current token. */
    suspend fun getToken(): String?
    /** retrieves user id. */
    suspend fun getUserId(): Int?
    /** retrieves user full name. */
    suspend fun getUserFullName(): String?
    /** clears all authentication data. */
    suspend fun clearAll()
    /** synchronous session invalidation (for http interceptor). */
    fun clearSessionSync()
}
