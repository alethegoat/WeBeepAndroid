package it.polimi.webeep.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * okhttp interceptor for automatic retry with exponential backoff.
 * retries failed requests for network errors or 5xx server errors.
 * NOTE: OkHttp interceptors are blocking by design; Thread.sleep here holds
 * one dispatcher thread (max ~3s total: 1s+2s) with 30s timeouts configured
 * in NetworkModule. Only idempotent GET/HEAD/OPTIONS and POSTs marked with
 * [MoodleApiServiceImpl.READ_ONLY_WS_HEADER] are retried; canceled calls stop early.
 */
class RetryInterceptor @Inject constructor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L
) : Interceptor {

    companion object {
        private val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "OPTIONS")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val isReadOnlyWs = original.header(MoodleApiServiceImpl.READ_ONLY_WS_HEADER) != null
        val request = if (isReadOnlyWs) {
            original.newBuilder().removeHeader(MoodleApiServiceImpl.READ_ONLY_WS_HEADER).build()
        } else {
            original
        }

        val retryable = (request.method in IDEMPOTENT_METHODS || isReadOnlyWs) &&
            request.body?.isOneShot() != true
        if (!retryable) {
            return chain.proceed(request)
        }

        var lastException: IOException? = null
        var lastResponse: Response? = null

        repeat(maxRetries) { attempt ->
            lastResponse?.close()
            lastResponse = null
            if (chain.call().isCanceled()) {
                throw IOException("Canceled", lastException)
            }
            try {
                val response = chain.proceed(request)
                if (response.code !in 500..599) {
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
                    lastResponse?.close()
                    throw IOException("Retry interrupted", e)
                }
            }
        }

        lastResponse?.let { return it }

        throw lastException ?: IOException("Max retries ($maxRetries) exceeded")
    }
}
