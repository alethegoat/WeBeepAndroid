package it.polimi.webeep.core.common

import kotlin.coroutines.cancellation.CancellationException

/** Outcome of a data-layer call: the value, or a typed [DataError] the UI can localize. */
sealed class Resource<out T> {

    /** The call succeeded with [data]. */
    data class Success<T>(val data: T) : Resource<T>()

    /** The call failed; [throwable] is kept only for logging and for [getOrThrow], the UI shows [error]. */
    data class Error(val error: DataError, val throwable: Throwable? = null) : Resource<Nothing>()
}

/**
 * Runs [block] and wraps its outcome. Unlike `runCatching`, a [CancellationException] is
 * rethrown so structured concurrency keeps working; other exceptions become
 * [Resource.Error], keeping the throwable for logging.
 */
inline fun <T> resourceOf(block: () -> T): Resource<T> = try {
    Resource.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Resource.Error(e.toDataError(), e)
}

/** Unwraps the value or rethrows the original failure (a generic one if none was kept). */
fun <T> Resource<T>.getOrThrow(): T = when (this) {
    is Resource.Success -> data
    is Resource.Error -> {
        throwable?.let { throw it }
        error("Unhandled data error: $error")
    }
}

/** Runs [action] on a [Resource.Success] value and returns the receiver unchanged, for chaining. */
inline fun <T> Resource<T>.onSuccess(action: (T) -> Unit): Resource<T> {
    if (this is Resource.Success) action(data)
    return this
}
