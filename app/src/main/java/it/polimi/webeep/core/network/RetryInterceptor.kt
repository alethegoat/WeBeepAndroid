package it.polimi.webeep.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * okhttp interceptor for automatic retry with exponential backoff.
 * retries failed requests for network errors or 5xx server errors.
 * NOTE: OkHttp interceptors are blocking by design; Thread.sleep here holds
 * one dispatcher thread (max ~7s total: 1s+2s+4s) with 30s timeouts configured
 * in NetworkModule. Only idempotent GET/HEAD/OPTIONS are retried.
 */
class RetryInterceptor @Inject constructor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L
) : Interceptor {

    companion object {
        private val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "OPTIONS")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method !in IDEMPOTENT_METHODS || request.body?.isOneShot() == true) {
            return chain.proceed(request)
        }

        var lastException: IOException? = null
        var lastResponse: Response? = null

        repeat(maxRetries) { attempt ->
            try {
                lastResponse?.close()
                val response = chain.proceed(request)
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

        lastResponse?.let { return it }

        throw lastException ?: IOException("Numero massimo di retry ($maxRetries) superato")
    }
}
