package it.polimi.webeep.core.network

import android.net.Uri
import java.net.URLEncoder

/**
 * normalizes moodle file urls and manages tokens in query strings.
 */
object MoodleFileUrlBuilder {

    private val AUTH_QUERY_PARAMS = setOf("token", "wstoken")

    fun buildAuthenticatedUrl(url: String, token: String): String {
        val normalizedUrl = normalizePluginFileUrl(url)
        if (AUTH_TOKEN_QUERY_REGEX.containsMatchIn(normalizedUrl)) {
            return normalizedUrl
        }

        // security validation: token is appended only to trusted urls
        val uri = Uri.parse(normalizedUrl)
        val isTrustedDomain = uri.scheme.equals("https", ignoreCase = true) &&
            uri.host?.endsWith("polimi.it", ignoreCase = true) == true
        if (!isTrustedDomain) {
            return normalizedUrl
        }

        val separator = if (normalizedUrl.contains("?")) "&" else "?"
        val encodedToken = URLEncoder.encode(token, Charsets.UTF_8.name())
        return "$normalizedUrl${separator}token=$encodedToken"
    }

    /**
     * removes tokens from query strings before persisting urls to disk (room).
     */
    fun stripAuthFromUrl(url: String): String {
        if (url.isBlank()) return url

        val normalizedUrl = normalizePluginFileUrl(url)
        val queryStart = normalizedUrl.indexOf('?')
        if (queryStart == -1) return normalizedUrl

        val base = normalizedUrl.substring(0, queryStart)
        val keptParams = normalizedUrl
            .substring(queryStart + 1)
            .split('&')
            .filter { param ->
                val key = param.substringBefore('=')
                key.isNotEmpty() && key !in AUTH_QUERY_PARAMS
            }

        return if (keptParams.isEmpty()) base else "$base?${keptParams.joinToString("&")}"
    }

    fun extractExtension(filename: String): String? {
        val lastDot = filename.lastIndexOf('.')
        if (lastDot > 0 && lastDot < filename.length - 1) {
            return filename.substring(lastDot + 1).lowercase()
        }
        return null
    }

    private fun normalizePluginFileUrl(url: String): String {
        return if (url.contains("/webservice/pluginfile.php/")) {
            url
        } else {
            url.replace("/pluginfile.php/", "/webservice/pluginfile.php/")
        }
    }

    private val AUTH_TOKEN_QUERY_REGEX = Regex("""[?&](token|wstoken)=""")
}
