package it.polimi.webeep.feature.download.domain

import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.util.DataError
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.core.util.resourceOf
import javax.inject.Inject

class DownloadFileUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val tokenProvider: TokenProvider
) {
    suspend fun execute(url: String, localFileName: String, displayName: String, mimeType: String?): Resource<Long> {
        val token = tokenProvider.getToken() ?: return Resource.Error(DataError.Unauthorized)
        val cleanUrl = MoodleFileUrlBuilder.stripAuthFromUrl(url)
        // Guarded so callers can rely on the Resource contract and never need their own try/catch.
        val started = resourceOf {
            downloadRepository.downloadFile(
                url = cleanUrl,
                token = token,
                localFileName = localFileName,
                displayName = displayName,
                mimeType = mimeType
            )
        }
        return when (started) {
            is Resource.Success -> started.data
            is Resource.Error -> started
        }
    }
}
