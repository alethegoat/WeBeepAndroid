package it.polimi.webeep.feature.auth.domain

import it.polimi.webeep.core.common.Resource

/** Session lifecycle: validating a token, storing it, and wiping local state on logout. */
interface AuthRepository {
    /** Checks [token] against Moodle without saving it, so a rejected token never replaces a working session. */
    suspend fun validateToken(token: String): Resource<UserSession>

    /** Persists a token that [validateToken] accepted, together with the user id of [session]. */
    suspend fun saveAuthData(token: String, session: UserSession)

    /** Whether a session token is stored; loads the secure store first, so it is safe to call at startup. */
    suspend fun isLoggedIn(): Boolean

    /** Erases credentials and all local user data and stops background sync. Idempotent; concurrent calls run one at a time. */
    suspend fun logout()

    /** Wipes cached course data, downloads and caches but keeps credentials and pending SSO state. */
    suspend fun clearLocalData()
}
