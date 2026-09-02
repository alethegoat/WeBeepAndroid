package it.polimi.webeep.core.network

import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Guards Moodle file HTTP responses so login HTML is never saved as a binary.
 */
object MoodleBinaryDownload {

    fun requireBinaryBody(response: Response): ResponseBody {
        if (!response.isSuccessful) {
            throw IllegalStateException("Download failed (${response.code})")
        }
        val contentType = response.header("Content-Type").orEmpty().lowercase()
        if (contentType.contains("text/html") || contentType.contains("text/xml")) {
            throw IllegalStateException("Server returned a login page instead of the file")
        }
        return response.body ?: throw IllegalStateException("Empty response from server")
    }
}
