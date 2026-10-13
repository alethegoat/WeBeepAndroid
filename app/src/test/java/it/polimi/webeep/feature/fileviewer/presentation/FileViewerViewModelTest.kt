package it.polimi.webeep.feature.fileviewer.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.download.domain.DownloadFileUseCase
import it.polimi.webeep.feature.download.domain.DownloadRepository
import it.polimi.webeep.feature.download.domain.DownloadTracker
import it.polimi.webeep.feature.download.domain.model.DownloadState
import it.polimi.webeep.feature.fileviewer.domain.FileContentRepository
import it.polimi.webeep.feature.fileviewer.domain.LoadedContent
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import it.polimi.webeep.util.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// SavedStateHandle.toRoute builds a Bundle, which needs Robolectric.
@RunWith(RobolectricTestRunner::class)
class FileViewerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loader = mockk<FileContentRepository>()
    private val courseRepository = mockk<CourseRepository>()
    private val downloadFileUseCase = mockk<DownloadFileUseCase>()
    private val downloadRepository = mockk<DownloadRepository>()

    private val url = MoodleFileUrlBuilder.stripAuthFromUrl("https://webeep.polimi.it/pluginfile.php/1/notes.pdf")
    private val file = ViewerFile(url = url, title = "notes.pdf", mimeType = "application/pdf")
    private val local = File("notes.pdf")

    private fun viewModel(content: LoadedContent = LoadedContent.Pdf(local, 3)): FileViewerViewModel {
        every { loader.load(any()) } returns flowOf(content)
        every { loader.resolveMimeType(any()) } returns "application/pdf"
        every { downloadRepository.isFileDownloaded(any()) } returns false
        return FileViewerViewModel(
            loader = loader,
            courseRepository = courseRepository,
            downloadTracker = DownloadTracker(downloadFileUseCase, downloadRepository),
            downloadRepository = downloadRepository,
            savedStateHandle = SavedStateHandle(mapOf("url" to url, "title" to file.title, "mimeType" to file.mimeType))
        )
    }

    @Test
    fun `opened file is the only page without a course and its content loads`() = runTest {
        val state = viewModel().uiState.value

        assertEquals(listOf(file), state.files)
        assertEquals(0, state.initialIndex)
        assertEquals(FileContentState.Pdf(local, 3), state.contents[url])
        assertEquals(DownloadState.NOT_STARTED, state.downloadStates[url])
    }

    @Test
    fun `visible page does not reload content that is already there`() = runTest {
        val vm = viewModel()

        vm.onAction(FileViewerAction.PageVisible(file))

        verify(exactly = 1) { loader.load(any()) }
    }

    @Test
    fun `retry reloads the file`() = runTest {
        val vm = viewModel(LoadedContent.CorruptPdf)
        assertEquals(FileContentState.Error(ViewerError.CORRUPT_PDF), vm.uiState.value.contents[url])

        vm.onAction(FileViewerAction.Retry(file))

        verify(exactly = 2) { loader.load(any()) }
    }

    @Test
    fun `external file opened first launches the other app once`() = runTest {
        val vm = viewModel(LoadedContent.External(local, "application/zip"))

        vm.effects.test {
            assertEquals(FileViewerEffect.OpenExternally(local, "application/zip"), awaitItem())
            vm.onAction(FileViewerAction.PageVisible(file))
            expectNoEvents()
        }
    }

    @Test
    fun `share reuses the local file and emits a share effect`() = runTest {
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(FileViewerAction.Share(file))
            assertEquals(FileViewerEffect.Share(local, "application/pdf", "notes.pdf"), awaitItem())
        }
        assertFalse(vm.uiState.value.preparingShare)
        coVerify(exactly = 0) { loader.localCopy(any()) }
    }

    @Test
    fun `open with downloads a copy when the page has none`() = runTest {
        val copy = File("copy.png")
        coEvery { loader.localCopy(any()) } returns copy
        val vm = viewModel(LoadedContent.Image("https://img"))

        vm.effects.test {
            vm.onAction(FileViewerAction.OpenWith(file))
            assertEquals(FileViewerEffect.OpenExternally(copy, "application/pdf"), awaitItem())
        }
        assertFalse(vm.uiState.value.preparingShare)
    }

    @Test
    fun `open externally forwards the local file`() = runTest {
        val vm = viewModel()

        vm.effects.test {
            vm.onAction(FileViewerAction.OpenExternally(local, "text/plain"))
            assertEquals(FileViewerEffect.OpenExternally(local, "text/plain"), awaitItem())
        }
    }

    @Test
    fun `download follows the progress to completed`() = runTest {
        coEvery { downloadFileUseCase(any(), any(), any(), any()) } returns Resource.Success(7L)
        every { downloadRepository.getDownloadProgress(7L) } returns flowOf(40, DownloadRepository.PROGRESS_COMPLETE)
        val vm = viewModel()

        vm.onAction(FileViewerAction.Download(file))

        assertEquals(DownloadState.COMPLETED, vm.uiState.value.downloadStates[url])
    }

    @Test
    fun `a failed download start is reported`() = runTest {
        coEvery { downloadFileUseCase(any(), any(), any(), any()) } returns Resource.Error(DataError.Network)
        val vm = viewModel()

        vm.onAction(FileViewerAction.Download(file))

        assertEquals(DownloadState.FAILED, vm.uiState.value.downloadStates[url])
    }
}
