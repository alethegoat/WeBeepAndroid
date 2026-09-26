package it.polimi.webeep.feature.download.data

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

/**
 * Awaits the response headers; cancelling the coroutine cancels the [Call].
 */
internal suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { this@await.cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _: Throwable -> response.close() }
        }
    })
}

/**
 * Runs [block] on the response and closes it. The [Call] stays bound to the
 * calling coroutine while the body is read, so cancellation aborts blocking reads.
 * [block] performs blocking I/O: call this from an I/O dispatcher.
 */
internal suspend fun <T> Call.executeCancellable(block: (Response) -> T): T = coroutineScope {
    val call = this@executeCancellable
    val canceller = launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            call.cancel()
        }
    }
    try {
        call.await().use { response -> block(response) }
    } finally {
        canceller.cancel()
    }
}
