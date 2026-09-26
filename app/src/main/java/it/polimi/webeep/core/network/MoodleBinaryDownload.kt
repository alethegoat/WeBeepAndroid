package it.polimi.webeep.core.network

import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Guards Moodle file HTTP responses so login HTML is never saved as a binary.
 */
object MoodleBinaryDownload {

    private const val PEEK_BYTES = 4096L

    fun requireBinaryBody(response: Response): ResponseBody {
        check(response.isSuccessful) { "Download failed (${response.code})" }
        val contentType = response.header("Content-Type").orEmpty().lowercase()
        val requestUrl = response.request.url.encodedPath.lowercase()
        val isLoginRedirect = response.request.url.encodedPath.contains("/login/")

        if (contentType.contains("text/html")) {
            val bodyPreview = response.peekBody(PEEK_BYTES).string().lowercase()
            // Specific markers only: a bare "login" substring false-positives
            // on legitimate HTML containing the word "login".
            val hasLoginMarkers = isLoginRedirect ||
                bodyPreview.contains("id=\"login\"") ||
                bodyPreview.contains("loginform") ||
                bodyPreview.contains("name=\"login\"") ||
                bodyPreview.contains("name=\"username\"") ||
                bodyPreview.contains("autenticazione") ||
                bodyPreview.contains("moodle-login")

            val isExplicitHtmlRequest = requestUrl.endsWith(".html") || requestUrl.endsWith(".htm")
            check(!hasLoginMarkers && isExplicitHtmlRequest) {
                "Server returned a login page instead of the file"
            }
        }
        return response.body ?: error("Empty response from server")
    }
}
