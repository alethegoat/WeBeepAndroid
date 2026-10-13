package it.polimi.webeep.core.common

private val AUTH_QUERY_PARAMS = setOf("token", "wstoken")

/** Rewrites `/pluginfile.php/` to its token-authenticated `/webservice/pluginfile.php/` twin. */
fun normalizePluginFileUrl(url: String): String = if (url.contains("/webservice/pluginfile.php/")) {
    url
} else {
    url.replace("/pluginfile.php/", "/webservice/pluginfile.php/")
}

/**
 * Removes tokens from query strings before persisting URLs to disk (Room, nav args).
 * Pure string logic, so every layer can use it without depending on the network package.
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
