package it.polimi.webeep.core.util

import kotlin.coroutines.cancellation.CancellationException

/** Like `runCatching`, but rethrows [CancellationException] so best-effort code cannot swallow cancellation. */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
