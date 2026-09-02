package it.polimi.webeep.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Prevents OkHttp from writing responses that carry auth tokens to the disk cache.
 */
class NoStoreAuthCacheInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val url = request.url
        val hasAuthQuery = url.queryParameter("token") != null || url.queryParameter("wstoken") != null
        val isMoodleRest = MoodleFileUrlBuilder.isTrustedHost(url.host) &&
            MoodleFileUrlBuilder.isMoodleRestPath(url.encodedPath)

        if (!hasAuthQuery && !isMoodleRest) {
            return response
        }

        return response.newBuilder()
            .header("Cache-Control", "no-store")
            .removeHeader("Pragma")
            .build()
    }
}
