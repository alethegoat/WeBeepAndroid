package it.polimi.webeep.core.network

import it.polimi.webeep.core.datastore.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Appends Moodle pluginfile `token=` on webeep.polimi.it file requests.
 * Does not clear the session on HTTP errors.
 */
class PluginFileAuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    constructor(
        tokenProvider: TokenProvider,
        trustedHost: String
    ) : this(tokenProvider) {
        this.trustedHost = trustedHost
    }

    private var trustedHost: String = MoodleFileUrlBuilder.TRUSTED_HOST

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val url = request.url
        val isPluginFile = url.host.equals(trustedHost, ignoreCase = true) &&
            MoodleFileUrlBuilder.isPluginFilePath(url.encodedPath)

        if (isPluginFile &&
            url.queryParameter("token") == null &&
            url.queryParameter("wstoken") == null
        ) {
            val token = tokenProvider.getTokenSync()
            if (token != null) {
                request = request.newBuilder()
                    .url(url.newBuilder().addQueryParameter("token", token).build())
                    .build()
            }
        }

        return chain.proceed(request)
    }
}
