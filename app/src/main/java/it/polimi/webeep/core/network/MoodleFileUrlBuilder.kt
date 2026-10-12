package it.polimi.webeep.core.network

import java.net.URI
import java.net.URLEncoder

/**
 * Normalizes Moodle file URLs and manages tokens in query strings.
 * Tokens are appended only to HTTPS pluginfile URLs on webeep.polimi.it.
 */
object MoodleFileUrlBuilder {

    const val TRUSTED_HOST = "webeep.polimi.it"

    private val AUTH_QUERY_PARAMS = setOf("token", "wstoken")
    private val AUTH_TOKEN_QUERY_REGEX = Regex("""[?&](token|wstoken)=""")

    fun isTrustedHost(host: String?): Boolean = host.equals(TRUSTED_HOST, ignoreCase = true)

    fun isMoodleRestPath(encodedPath: String): Boolean = encodedPath.contains("/webservice/rest/server.php")

    fun isPluginFilePath(encodedPath: String): Boolean = encodedPath.contains("/pluginfile.php")

    fun isTrustedWeBeepUrl(url: String): Boolean {
        val uri = parseUri(url) ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && isTrustedHost(uri.host)
    }

    /** http(s) link that is not a WeBeep pluginfile: opened in a Custom Tab, never in-app. */
    fun isExternalWebLink(url: String): Boolean {
        val lowerUrl = url.lowercase()
        val isHttp = lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")
        return isHttp && !lowerUrl.contains("pluginfile.php")
    }

    fun buildAuthenticatedUrl(url: String, token: String): String {
        val normalizedUrl = normalizePluginFileUrl(url)
        if (AUTH_TOKEN_QUERY_REGEX.containsMatchIn(normalizedUrl)) {
            return normalizedUrl
        }

        if (!isTrustedWeBeepUrl(normalizedUrl) || !isPluginFilePath(normalizedUrl)) {
            return normalizedUrl
        }

        val separator = if (normalizedUrl.contains("?")) "&" else "?"
        val encodedToken = URLEncoder.encode(token, Charsets.UTF_8.name())
        return "$normalizedUrl${separator}token=$encodedToken"
    }

    /**
     * Removes tokens from query strings before persisting URLs to disk (Room, nav args).
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

    fun normalizePluginFileUrl(url: String): String = if (url.contains("/webservice/pluginfile.php/")) {
        url
    } else {
        url.replace("/pluginfile.php/", "/webservice/pluginfile.php/")
    }

    private fun parseUri(url: String): URI? = try {
        URI(url)
    } catch (_: Exception) {
        null
    }
}
