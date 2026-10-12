package it.polimi.webeep.feature.auth.domain

import java.net.URLDecoder

/**
 * Moodle returns `urlscheme://token=<base64>`. Base64 contains `/`, so Android's
 * hierarchical Uri parser splits the value. Always read the raw string after `token=`.
 */
object SsoCallbackParser {

    fun extractToken(rawUri: String): String? {
        val marker = "token="
        val start = rawUri.indexOf(marker, ignoreCase = true)
        if (start < 0) return null
        var value = rawUri.substring(start + marker.length)
        value = value.substringBefore('#')
        value = value.substringBefore('&')
        val decoded = try {
            URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8.name())
        } catch (_: Exception) {
            value
        }
        return decoded.trim().takeIf { it.isNotBlank() }
    }
}
