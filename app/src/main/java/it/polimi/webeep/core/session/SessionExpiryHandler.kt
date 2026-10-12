package it.polimi.webeep.core.session

import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.feature.auth.domain.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.withContext

/**
 * Turns the loss of an authenticated session (401/invalidtoken or manual logout)
 * into a full local wipe. Collectors are expected to route back to the login screen.
 */
@Singleton
class SessionExpiryHandler @Inject constructor(private val tokenProvider: TokenProvider, private val authRepository: AuthRepository) {

    /** Emits once per true -> false transition of [TokenProvider.isLoggedIn], after the wipe. */
    val sessionLost: Flow<Unit> = tokenProvider.isLoggedIn
        .distinctUntilChanged()
        .runningFold(false to false) { (_, previous), current -> previous to current }
        .filter { (previous, current) -> previous && !current }
        .map {
            // Logout is idempotent; finish the wipe even if the collector goes away.
            withContext(NonCancellable) { authRepository.logout() }
        }

    /**
     * A session revoked while the app was not running leaves the previous user's
     * data on disk: wipe it at startup when nobody is logged in.
     */
    suspend fun wipeStaleDataIfLoggedOut() {
        tokenProvider.ensureLoaded()
        if (!tokenProvider.isLoggedIn.first()) {
            withContext(NonCancellable) { authRepository.clearLocalData() }
        }
    }
}
