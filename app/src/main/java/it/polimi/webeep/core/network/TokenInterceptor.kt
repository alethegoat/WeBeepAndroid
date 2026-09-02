package it.polimi.webeep.core.network

import it.polimi.webeep.core.datastore.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Injects wstoken and handles Moodle auth errors only for REST API calls.
 * File/image requests must not log the user out on 403.
 */
class TokenInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    constructor(
        tokenProvider: TokenProvider,
        trustedHost: String
    ) : this(tokenProvider) {
        this.trustedHost = trustedHost
    }

    private var trustedHost: String = MoodleFileUrlBuilder.TRUSTED_HOST

    companion object {
        private const val TOKEN_PEEK_BYTES = 8_192L
        private val INVALID_TOKEN_MARKERS = listOf(
            "\"errorcode\":\"invalidtoken\"",
            "\"errorcode\":\"accessexception\""
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val isMoodleRest = request.url.host.equals(trustedHost, ignoreCase = true) &&
            MoodleFileUrlBuilder.isMoodleRestPath(request.url.encodedPath)

        if (isMoodleRest) {
            val explicitToken = request.url.queryParameter("wstoken")
            val token = explicitToken ?: tokenProvider.getTokenSync()
            if (token != null && explicitToken == null) {
                val url = request.url.newBuilder()
                    .addQueryParameter("wstoken", token)
                    .build()
                request = request.newBuilder().url(url).build()
            }
        }

        val response = chain.proceed(request)

        if (!isMoodleRest) {
            return response
        }

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
