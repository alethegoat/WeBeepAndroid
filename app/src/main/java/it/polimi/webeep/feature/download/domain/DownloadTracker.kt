package it.polimi.webeep.feature.download.domain

import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.download.domain.DownloadRepository.Companion.PROGRESS_COMPLETE
import it.polimi.webeep.feature.download.domain.DownloadRepository.Companion.PROGRESS_FAILED
import it.polimi.webeep.feature.download.domain.model.DownloadState
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Runs a download and reports it as [DownloadState]s, so every screen that offers a download
 * maps the use case and the progress codes the same way.
 */
class DownloadTracker @Inject constructor(
    private val downloadFileUseCase: DownloadFileUseCase,
    private val downloadRepository: DownloadRepository
) {
    /**
     * Emits [DownloadState.DOWNLOADING], then ends with [DownloadState.COMPLETED] or
     * [DownloadState.FAILED]. [onStarted] receives the download id once it exists, for callers
     * that may need to cancel it.
     */
    fun track(
        url: String,
        localFileName: String,
        displayName: String,
        mimeType: String?,
        onStarted: (Long) -> Unit = {}
    ): Flow<DownloadState> = flow {
        emit(DownloadState.DOWNLOADING)
        when (val started = downloadFileUseCase(url, localFileName, displayName, mimeType)) {
            is Resource.Error -> emit(DownloadState.FAILED)
            is Resource.Success -> {
                onStarted(started.data)
                downloadRepository.getDownloadProgress(started.data).collect { progress ->
                    when (progress) {
                        PROGRESS_COMPLETE -> emit(DownloadState.COMPLETED)
                        PROGRESS_FAILED -> emit(DownloadState.FAILED)
                    }
                }
            }
        }
    }
}
