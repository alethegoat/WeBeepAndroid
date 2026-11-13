package it.polimi.webeep.util.fakes

import it.polimi.webeep.core.datastore.TokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTokenProvider : TokenProvider {
    /** Token returned by [getToken]; null simulates a logged-out session. */
    var token: String? = null

    /** Pending SSO passports; tests may seed or clear it directly. */
    val passports = mutableListOf<String>()
    override val isLoggedIn: Flow<Boolean> = MutableStateFlow(false)
    override val wsToken: Flow<String?> = MutableStateFlow(null)
    override fun getTokenSync(): String? = null
    override suspend fun saveAuthData(token: String, userId: Int) = Unit
    override suspend fun updateUserId(userId: Int) = Unit
    override suspend fun getToken(): String? = token
    override suspend fun getUserId(): Int? = null
    override suspend fun clearAll() = Unit
    override fun clearSessionSync() = Unit
    override suspend fun savePendingPassport(passport: String) {
        passports.add(passport)
    }
    override suspend fun getPendingPassports(): List<String> = passports.toList()
    override suspend fun clearPendingPassport() {
        passports.clear()
    }
}
