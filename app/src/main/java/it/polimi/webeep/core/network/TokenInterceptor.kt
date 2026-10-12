package it.polimi.webeep.core.network

import it.polimi.webeep.core.datastore.TokenProvider
import java.net.HttpURLConnection
import java.net.URLEncoder
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer

/**
 * Injects wstoken into the POST form body (never the URL) and handles Moodle
 * auth errors only for REST API calls on the trusted host.
 * File/image requests must not log the user out on 403.
 */
class TokenInterceptor @Inject constructor(private val tokenProvider: TokenProvider) : Interceptor {

    constructor(
        tokenProvider: TokenProvider,
        trustedHost: String
    ) : this(tokenProvider) {
        this.trustedHost = trustedHost
    }

    private var trustedHost: String = MoodleFileUrlBuilder.TRUSTED_HOST

    companion object {
        private const val TOKEN_PEEK_BYTES = 8_192L
        private const val FORM_SUBTYPE = "x-www-form-urlencoded"
        private val INVALID_TOKEN_MARKERS = listOf(
            "\"errorcode\":\"invalidtoken\"",
            "\"errorcode\":\"accessexception\""
        )
    }

    @Suppress("ReturnCount")
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val isMoodleRest = request.url.host.equals(trustedHost, ignoreCase = true) &&
            MoodleFileUrlBuilder.isMoodleRestPath(request.url.encodedPath)

        if (isMoodleRest) {
            request = withFormToken(request)
        }

        val response = chain.proceed(request)

        if (!isMoodleRest) {
            return response
        }

        // Fail-safe logout: 401 always means bad token. 403 alone is too
        // broad (e.g. single-file permission), so require an explicit
        // invalidtoken/accessexception marker before clearing the session.
        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            tokenProvider.clearSessionSync()
            return response
        }

        if (response.code == HttpURLConnection.HTTP_FORBIDDEN) {
            val bodyPreview = try {
                response.peekBody(TOKEN_PEEK_BYTES).string()
            } catch (_: Exception) {
                return response
            }
            if (INVALID_TOKEN_MARKERS.any { bodyPreview.contains(it) }) {
                tokenProvider.clearSessionSync()
            }
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

    private fun withFormToken(request: Request): Request {
        val body = request.body ?: return request
        // Ktor's OkHttp engine may leave the body without a media type and send
        // Content-Type only as a request header, so fall back to the header.
        val contentType = body.contentType() ?: request.header("Content-Type")?.toMediaTypeOrNull()
        if (request.method != "POST" ||
            contentType?.subtype?.equals(FORM_SUBTYPE, ignoreCase = true) != true ||
            body.isOneShot()
        ) {
            return request
        }

        val form = Buffer().also { body.writeTo(it) }.readUtf8()
        if (form.split('&').any { it.startsWith("wstoken=") }) {
            return request
        }
        val token = tokenProvider.getTokenSync() ?: return request

        val encodedToken = URLEncoder.encode(token, Charsets.UTF_8.name())
        val newForm = if (form.isEmpty()) "wstoken=$encodedToken" else "$form&wstoken=$encodedToken"
        return request.newBuilder()
            .method(request.method, newForm.toRequestBody(contentType))
            .build()
    }
}
