package it.polimi.webeep.util.fakes

import it.polimi.webeep.core.datastore.TokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTokenProvider : TokenProvider {
    private val passports = mutableListOf<String>()
    override val isLoggedIn: Flow<Boolean> = MutableStateFlow(false)
    override val wsToken: Flow<String?> = MutableStateFlow(null)
    override fun getTokenSync(): String? = null
    override suspend fun saveAuthData(token: String, userId: Int) = Unit
    override suspend fun updateUserId(userId: Int) = Unit
    override suspend fun getToken(): String? = null
    override suspend fun getUserId(): Int? = null
    override suspend fun clearAll() = Unit
    override fun clearSessionSync() = Unit
    override fun savePendingPassportSync(passport: String) {
        passports.add(passport)
    }
    override fun getPendingPassportsSync(): List<String> = passports.toList()
    override fun clearPendingPassportSync() {
        passports.clear()
    }
}
