package it.polimi.webeep.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * Authentication token management contract.
 * Allows fake implementations in tests.
 */
interface TokenProvider {
    val isLoggedIn: Flow<Boolean>
    val wsToken: Flow<String?>
    fun getTokenSync(): String?
    suspend fun saveAuthData(token: String, userId: Int, fullName: String)
    suspend fun updateUserProfile(userId: Int, fullName: String)
    suspend fun getToken(): String?
    suspend fun getUserId(): Int?
    suspend fun clearAll()
    fun clearSessionSync()
    fun savePendingPassportSync(passport: String)
    fun getPendingPassportsSync(): List<String>
    fun clearPendingPassportSync()

    /** Loads the persisted session off the main thread. No-op for in-memory fakes. */
    suspend fun ensureLoaded() {}

    /** True once [ensureLoaded] has completed in this process. */
    val isLoaded: Boolean get() = true
}
