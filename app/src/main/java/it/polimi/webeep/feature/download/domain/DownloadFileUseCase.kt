package it.polimi.webeep.feature.download.domain

import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import javax.inject.Inject

/**
 * use case to enqueue a file download automatically adding the wstoken.
 */
class DownloadFileUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val tokenManager: TokenManager
) {
    /**
     * executes the file download by appending the token to the url request.
     *
     * @param url the original file url.
     * @param localFileName the local file name.
     * @param displayName the display name.
     * @param mimeType the mime type of the file.
     * @return the download id returned by downloadmanager.
     */
    suspend fun execute(url: String, localFileName: String, displayName: String, mimeType: String?): Long {
        val token = tokenManager.getToken() ?: throw IllegalStateException("Token non disponibile. Effettua il login.")
        val cleanUrl = MoodleFileUrlBuilder.stripAuthFromUrl(url)
        return downloadRepository.downloadFile(
            url = cleanUrl,
            token = token,
            localFileName = localFileName,
            displayName = displayName,
            mimeType = mimeType
        )
    }
}
