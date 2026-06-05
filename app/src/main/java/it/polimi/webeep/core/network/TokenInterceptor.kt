package it.polimi.webeep.core.network

import it.polimi.webeep.core.datastore.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * okhttp interceptor for token injection and moodle authentication error handling.
 */
class TokenInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    companion object {
        private const val TOKEN_PEEK_BYTES = 8_192L
        private val INVALID_TOKEN_MARKERS = listOf(
            "\"errorcode\":\"invalidtoken\"",
            "\"errorcode\":\"accessexception\""
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val explicitToken = request.url.queryParameter("wstoken")
        val token = explicitToken ?: tokenProvider.getTokenSync()
        if (token != null && explicitToken == null) {
            val url = request.url.newBuilder()
                .addQueryParameter("wstoken", token)
                .build()
            request = request.newBuilder().url(url).build()
        }

        val response = chain.proceed(request)

        if (response.code == 401 || response.code == 403) {
            tokenProvider.clearSessionSync()
            return response
        }

        if (response.isSuccessful) {
            val bodyPreview = response.peekBody(TOKEN_PEEK_BYTES).string()
            if (INVALID_TOKEN_MARKERS.any { bodyPreview.contains(it) }) {
                tokenProvider.clearSessionSync()
            }
        }

        return response
    }
}
