package it.polimi.webeep.core.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Offline-first loading shared by the list use cases: emits [cache] as it changes, and while the
 * network [refresh] runs (only when online) keeps an empty cache silent so the UI does not flash
 * "nothing here". A refresh failure is surfaced only when the cache has nothing to show; once the
 * refresh is over an empty cache is reported as an empty list.
 *
 * @param emitEmptyAfterError also emit the empty list right after the error that was reported for
 * an empty cache, so collectors that need a final "loaded" state still receive one.
 */
fun <T> cacheThenRefresh(
    cache: Flow<List<T>>,
    connectivity: ConnectivityObserver,
    refresh: suspend () -> Resource<*>,
    emitEmptyAfterError: Boolean
): Flow<Resource<List<T>>> = channelFlow {
    var isSyncing = true
    var lastCached: List<T>? = null
    val cacheStarted = CompletableDeferred<Unit>()

    val cacheJob = launch {
        cache.collect { items ->
            lastCached = items
            if (items.isNotEmpty() || !isSyncing) {
                send(Resource.Success(items))
            }
            cacheStarted.complete(Unit)
        }
    }

    cacheStarted.await()

    val online = runCatchingCancellable { connectivity.isOnline.first() }.getOrDefault(true)
    val refreshError = if (online) refresh() as? Resource.Error else null

    var errorSent = false
    if (refreshError != null && lastCached.isNullOrEmpty()) {
        send(refreshError)
        errorSent = true
    }

    isSyncing = false

    if (lastCached.isNullOrEmpty() && (!errorSent || emitEmptyAfterError)) {
        send(Resource.Success(emptyList()))
    }

    cacheJob.join()
}
