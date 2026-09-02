package it.polimi.webeep.core.network

import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Guards Moodle file HTTP responses so login HTML is never saved as a binary.
 */
object MoodleBinaryDownload {

    private const val PEEK_BYTES = 4096L

    fun requireBinaryBody(response: Response): ResponseBody {
        if (!response.isSuccessful) {
            throw IllegalStateException("Download failed (${response.code})")
        }
        val contentType = response.header("Content-Type").orEmpty().lowercase()
        val requestUrl = response.request.url.encodedPath.lowercase()
        val isLoginRedirect = response.request.url.encodedPath.contains("/login/")

        if (contentType.contains("text/html")) {
            val bodyPreview = response.peekBody(PEEK_BYTES).string().lowercase()
            val hasLoginMarkers = isLoginRedirect ||
                bodyPreview.contains("id=\"login\"") ||
                bodyPreview.contains("loginform") ||
                bodyPreview.contains("name=\"login\"") ||
                bodyPreview.contains("login") ||
                bodyPreview.contains("autenticazione")

            val isExplicitHtmlRequest = requestUrl.endsWith(".html") || requestUrl.endsWith(".htm")
            if (hasLoginMarkers || !isExplicitHtmlRequest) {
                throw IllegalStateException("Server returned a login page instead of the file")
            }
        }
        return response.body ?: throw IllegalStateException("Empty response from server")
    }
}
