package it.polimi.webeep.util.fakes

import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.UserSession

class FakeAuthRepository : AuthRepository {
    var validateResult: Resource<UserSession> = Resource.Error(DataError.Unknown)
    var savedToken: String? = null
    var savedSession: UserSession? = null
    var isLoggedInValue: Boolean = false

    override suspend fun validateToken(token: String): Resource<UserSession> = validateResult

    override suspend fun saveAuthData(token: String, session: UserSession) {
        savedToken = token
        savedSession = session
        isLoggedInValue = true
    }

    override suspend fun isLoggedIn(): Boolean = isLoggedInValue

    override suspend fun logout() {
        isLoggedInValue = false
    }

    override suspend fun clearLocalData() = Unit
}
