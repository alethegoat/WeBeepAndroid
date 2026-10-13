package it.polimi.webeep.feature.download.domain

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.datastore.TokenProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFileUseCaseTest {

    private val repository = mockk<DownloadRepository>()
    private val tokenProvider = mockk<TokenProvider>()
    private val useCase = DownloadFileUseCase(repository, tokenProvider)

    private suspend fun run() = useCase("https://webeep.polimi.it/f.pdf", "f.pdf", "f.pdf", null)

    @Test
    fun `missing token is reported as Unauthorized without touching the repository`() = runTest {
        coEvery { tokenProvider.getToken() } returns null

        assertEquals(Resource.Error(DataError.Unauthorized), run())
    }

    @Test
    fun `repository id is returned as a flat Success`() = runTest {
        coEvery { tokenProvider.getToken() } returns "token"
        every { repository.downloadFile(any(), any(), any(), any(), any()) } returns Resource.Success(5L)

        assertEquals(Resource.Success(5L), run())
    }

    @Test
    fun `an unexpected repository exception becomes an Error instead of escaping`() = runTest {
        coEvery { tokenProvider.getToken() } returns "token"
        every { repository.downloadFile(any(), any(), any(), any(), any()) } throws RuntimeException("boom")

        val result = run()

        assertTrue(result is Resource.Error)
        assertEquals(DataError.Unknown, (result as Resource.Error).error)
    }
}
