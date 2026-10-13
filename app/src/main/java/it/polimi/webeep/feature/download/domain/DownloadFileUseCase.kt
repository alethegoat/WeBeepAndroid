package it.polimi.webeep.feature.download.domain

import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.resourceOf
import it.polimi.webeep.core.common.stripAuthFromUrl
import it.polimi.webeep.core.datastore.TokenProvider
import javax.inject.Inject

class DownloadFileUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val tokenProvider: TokenProvider
) {
    suspend operator fun invoke(url: String, localFileName: String, displayName: String, mimeType: String?): Resource<Long> {
        val token = tokenProvider.getToken() ?: return Resource.Error(DataError.Unauthorized)
        val cleanUrl = stripAuthFromUrl(url)
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
