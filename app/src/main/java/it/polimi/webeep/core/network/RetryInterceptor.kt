package it.polimi.webeep.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * okhttp interceptor for automatic retry with exponential backoff.
 * retries failed requests for network errors or 5xx server errors.
 */
class RetryInterceptor @Inject constructor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: IOException? = null
        var lastResponse: Response? = null

        repeat(maxRetries) { attempt ->
            try {
                lastResponse?.close()
                val response = chain.proceed(chain.request())
                if (response.isSuccessful || response.code !in 500..599) {
                    return response
                }
                lastResponse = response
            } catch (e: IOException) {
                lastException = e
            }
            if (attempt < maxRetries - 1) {
                try {
                    Thread.sleep(initialDelayMs * (1L shl attempt))
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Retry interrotto", e)
                }
            }
        }

        // if we have a response (server error), return it
        lastResponse?.let { return it }

        // otherwise rethrow the last network exception
        throw lastException ?: IOException("Numero massimo di retry ($maxRetries) superato")
    }
}
