package it.polimi.webeep.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * Authentication token management contract.
 * Allows fake implementations in tests.
 *
 * Two families of members exist on purpose. The `suspend` ones switch to an IO dispatcher and are meant
 * for coroutines. The `*Sync` ones exist for OkHttp interceptors, which run on OkHttp's own threads
 * where a coroutine cannot be awaited: reads come from the in-memory copy, writes block briefly on the
 * secure store, so keep them off the main thread. Until [ensureLoaded] has completed the in-memory copy
 * is still empty and the `*Sync` reads see a logged-out session.
 */
interface TokenProvider {
    /** Whether a session token is currently stored; emits its current value on subscription. */
    val isLoggedIn: Flow<Boolean>

    /** The current session token, or `null` when logged out. Treat it as a secret: never log it. */
    val wsToken: Flow<String?>

    /** In-memory token for interceptors on OkHttp threads, `null` before [ensureLoaded] or when logged out. */
    fun getTokenSync(): String?

    /** Persists the token and user id together or throws `SecurityException`; credentials are never stored in clear. */
    suspend fun saveAuthData(token: String, userId: Int)

    /** Updates only the user id of the current session, e.g. once the profile has been fetched; throws if it cannot be persisted. */
    suspend fun updateUserId(userId: Int)

    /** Suspending counterpart of [getTokenSync] that loads the store first, so it is correct even before [ensureLoaded]. */
    suspend fun getToken(): String?

    /** The stored user id, or `null` when logged out or not yet known. */
    suspend fun getUserId(): Int?

    /** Erases the whole stored session (token, user id and pending passports). Idempotent. */
    suspend fun clearAll()

    /** Same as [clearAll] for threads that cannot suspend, such as an interceptor that just saw a 401. Idempotent. */
    fun clearSessionSync()

    /**
     * Remembers an SSO passport sent to the login page, so the callback can later be checked against it.
     * Persisted (not just in memory) because the process may die while the browser is in front. Entries
     * expire after a few minutes and the list is bounded.
     */
    fun savePendingPassportSync(passport: String)

    /** Passports saved by [savePendingPassportSync] that have not expired yet, oldest first. */
    fun getPendingPassportsSync(): List<String>

    /** Forgets all pending passports once a login has completed. */
    fun clearPendingPassportSync()

    /** Loads the persisted session off the main thread. No-op for in-memory fakes. */
    suspend fun ensureLoaded() {}

    /** True once [ensureLoaded] has completed in this process. */
    val isLoaded: Boolean get() = true
}
