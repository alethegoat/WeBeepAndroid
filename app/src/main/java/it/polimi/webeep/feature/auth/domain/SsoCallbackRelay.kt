package it.polimi.webeep.feature.auth.domain

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * In-memory hand-off of the SSO callback blob from the Activity to the login screen.
 * The blob never enters nav routes, saved state or the Activity intent, and each
 * token is delivered to exactly one collector.
 */
@Singleton
class SsoCallbackRelay @Inject constructor() {

    private val channel = Channel<String>(Channel.CONFLATED)
    private val pending = AtomicBoolean(false)

    /** Callback blobs, each delivered to a single collector. */
    val tokens: Flow<String> = channel.receiveAsFlow().onEach { pending.set(false) }

    /** Best-effort hint used to route to the login screen while a token waits for a collector. */
    val hasPending: Boolean get() = pending.get()

    /** Hands a blob to the login screen; blank values are ignored and a newer one replaces an undelivered older one. */
    fun offer(token: String) {
        if (token.isBlank()) return
        pending.set(true)
        channel.trySend(token)
    }
}
