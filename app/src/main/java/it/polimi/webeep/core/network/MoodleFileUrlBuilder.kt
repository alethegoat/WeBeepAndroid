package it.polimi.webeep.core.network

import java.net.URI
import java.net.URLEncoder

/**
 * Normalizes Moodle file URLs and manages tokens in query strings.
 * Tokens are appended only to HTTPS pluginfile URLs on webeep.polimi.it.
 */
object MoodleFileUrlBuilder {

    /** The only host that ever receives the session token. */
    const val TRUSTED_HOST = "webeep.polimi.it"

    private val AUTH_TOKEN_QUERY_REGEX = Regex("""[?&](token|wstoken)=""")

    /** Case-insensitive match against [TRUSTED_HOST]; `null` is never trusted. */
    fun isTrustedHost(host: String?): Boolean = host.equals(TRUSTED_HOST, ignoreCase = true)

    /** Whether [encodedPath] targets the Moodle REST web-service endpoint. */
    fun isMoodleRestPath(encodedPath: String): Boolean = encodedPath.contains("/webservice/rest/server.php")

    /** Whether [encodedPath] targets Moodle's `pluginfile.php` file endpoint. */
    fun isPluginFilePath(encodedPath: String): Boolean = encodedPath.contains("/pluginfile.php")

    /** True only for HTTPS URLs on [TRUSTED_HOST]; unparseable URLs are not trusted. */
    fun isTrustedWeBeepUrl(url: String): Boolean {
        val uri = parseUri(url) ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && isTrustedHost(uri.host)
    }

    /** Whether [url] is an http(s) link that is not a WeBeep pluginfile: those open in a Custom Tab, never in-app. */
    fun isExternalWebLink(url: String): Boolean {
        val lowerUrl = url.lowercase()
        val isHttp = lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")
        return isHttp && !lowerUrl.contains("pluginfile.php")
    }

    /**
     * Normalizes [url] and appends `token` only to HTTPS pluginfile URLs on [TRUSTED_HOST], so the token never reaches
     * another host. URLs that already carry a token are returned untouched. The result must not be persisted: see [stripAuthFromUrl].
     */
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

    /** Delegates to the pure [it.polimi.webeep.core.common.stripAuthFromUrl] shared with the domain layer. */
    fun stripAuthFromUrl(url: String): String = it.polimi.webeep.core.common.stripAuthFromUrl(url)

    /** Lower-cased extension of [filename], or `null` when there is none (a leading dot does not count). */
    fun extractExtension(filename: String): String? {
        val lastDot = filename.lastIndexOf('.')
        if (lastDot > 0 && lastDot < filename.length - 1) {
            return filename.substring(lastDot + 1).lowercase()
        }
        return null
    }

    /** Delegates to the pure [it.polimi.webeep.core.common.normalizePluginFileUrl]. */
    fun normalizePluginFileUrl(url: String): String = it.polimi.webeep.core.common.normalizePluginFileUrl(url)

    private fun parseUri(url: String): URI? = try {
        URI(url)
    } catch (_: Exception) {
        null
    }
}
