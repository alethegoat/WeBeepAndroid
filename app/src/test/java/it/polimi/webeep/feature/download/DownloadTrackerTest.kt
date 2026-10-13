package it.polimi.webeep.feature.download

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.download.domain.DownloadFileUseCase
import it.polimi.webeep.feature.download.domain.DownloadRepository
import it.polimi.webeep.feature.download.domain.DownloadRepository.Companion.PROGRESS_COMPLETE
import it.polimi.webeep.feature.download.domain.DownloadRepository.Companion.PROGRESS_FAILED
import it.polimi.webeep.feature.download.domain.DownloadTracker
import it.polimi.webeep.feature.download.domain.model.DownloadState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadTrackerTest {

    private val useCase = mockk<DownloadFileUseCase>()
    private val repository = mockk<DownloadRepository>()
    private val tracker = DownloadTracker(useCase, repository)

    private fun track(onStarted: (Long) -> Unit = {}) = tracker.track("url", "local.pdf", "name.pdf", null, onStarted)

    @Test
    fun `progress ends with COMPLETED and reports the download id`() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } returns Resource.Success(7L)
        every { repository.getDownloadProgress(7L) } returns flowOf(30, PROGRESS_COMPLETE)
        var startedId: Long? = null

        track { startedId = it }.test {
            assertEquals(DownloadState.DOWNLOADING, awaitItem())
            assertEquals(DownloadState.COMPLETED, awaitItem())
            awaitComplete()
        }
        assertEquals(7L, startedId)
    }

    @Test
    fun `failed progress ends with FAILED`() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } returns Resource.Success(7L)
        every { repository.getDownloadProgress(7L) } returns flowOf(PROGRESS_FAILED)

        track().test {
            assertEquals(DownloadState.DOWNLOADING, awaitItem())
            assertEquals(DownloadState.FAILED, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `a download that cannot start is FAILED`() = runTest {
        coEvery { useCase(any(), any(), any(), any()) } returns Resource.Error(DataError.Unauthorized)

        track().test {
            assertEquals(DownloadState.DOWNLOADING, awaitItem())
            assertEquals(DownloadState.FAILED, awaitItem())
            awaitComplete()
        }
    }
}
