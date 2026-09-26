package it.polimi.webeep.core.network

/**
 * Masks auth secrets in HTTP log lines (URLs, headers and form bodies).
 */
object LogRedactor {

    private val WS_TOKEN_REGEX = Regex("""(?i)wstoken=[^&\s]+""")
    private val PRIVATE_TOKEN_REGEX = Regex("""(?i)private_token=[^&\s]+""")
    private val TOKEN_REGEX = Regex("""(?i)(^|[?&\s])token=[^&\s]+""")
    private val BEARER_REGEX = Regex("""(?i)Bearer\s+\S+""")

    fun redact(message: String): String = message
        .replace(WS_TOKEN_REGEX, "wstoken=<REDACTED>")
        .replace(PRIVATE_TOKEN_REGEX, "private_token=<REDACTED>")
        .replace(TOKEN_REGEX, "$1token=<REDACTED>")
        .replace(BEARER_REGEX, "Bearer <REDACTED>")
}
