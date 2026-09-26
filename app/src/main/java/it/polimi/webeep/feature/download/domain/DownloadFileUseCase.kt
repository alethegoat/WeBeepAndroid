package it.polimi.webeep.feature.download.domain

import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import javax.inject.Inject

class DownloadFileUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val tokenProvider: TokenProvider
) {
    suspend fun execute(url: String, localFileName: String, displayName: String, mimeType: String?): Long {
        val token = tokenProvider.getToken() ?: throw IllegalStateException("Missing session token")
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
