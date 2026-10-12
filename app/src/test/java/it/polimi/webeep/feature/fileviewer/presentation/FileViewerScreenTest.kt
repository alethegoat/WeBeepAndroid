package it.polimi.webeep.feature.fileviewer.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.polimi.webeep.R
import it.polimi.webeep.feature.download.domain.model.DownloadState
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import it.polimi.webeep.util.setAppContent
import it.polimi.webeep.util.string
import java.io.File
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PDF, image and media pages need real rendering, which Robolectric cannot do; the pages that
 * are plain Compose (loading, error, "open in another app") and the top bar are covered here.
 */
@RunWith(AndroidJUnit4::class)
class FileViewerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val file = ViewerFile(url = "https://example.com/notes.zip", title = "Lecture notes.zip", mimeType = "application/zip")
    private val actions = mutableListOf<FileViewerAction>()
    private var backClicks = 0

    /** The pager reports page lifecycle on its own; only what the user triggered matters here. */
    private fun userActions() = actions.filterNot { it is FileViewerAction.PageVisible || it is FileViewerAction.PageSettled }

    private fun show(state: FileViewerUiState) {
        composeRule.setAppContent {
            FileViewerScreen(
                state = state,
                mediaCallFactory = OkHttpClient(),
                onAction = { actions += it },
                onBackClick = { backClicks++ }
            )
        }
    }

    private fun stateWith(
        content: FileContentState,
        downloadState: DownloadState = DownloadState.NOT_STARTED,
        preparingShare: Boolean = false
    ) = FileViewerUiState(
        initialFile = file,
        files = listOf(file),
        contents = mapOf(file.url to content),
        downloadStates = mapOf(file.url to downloadState),
        preparingShare = preparingShare
    )

    private fun external() = FileContentState.External(File("notes.zip"), "application/zip")

    @Test
    fun `while files resolve the title is shown and back works`() {
        show(FileViewerUiState(initialFile = file))

        composeRule.onAllNodesWithText(file.title).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.file_viewer_loading)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun `while files resolve there are no file actions`() {
        show(FileViewerUiState(initialFile = file))

        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_share)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_download)).assertDoesNotExist()
    }

    @Test
    fun `a download in progress shows how much has arrived and no actions`() {
        show(stateWith(FileContentState.Downloading(bytesRead = 1024L, totalBytes = 2048L)))

        composeRule.onNodeWithText(string(R.string.file_viewer_download_progress, "1.0 KB", "2.0 KB")).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_share)).assertDoesNotExist()
    }

    @Test
    fun `error shows its message and retry emits Retry for the file`() {
        show(stateWith(FileContentState.Error(ViewerError.NETWORK)))

        composeRule.onNodeWithText(string(R.string.file_viewer_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.file_viewer_error_network)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).performClick()

        assertEquals(listOf<FileViewerAction>(FileViewerAction.Retry(file)), userActions())
    }

    @Test
    fun `error hides the file actions`() {
        show(stateWith(FileContentState.Error(ViewerError.CANNOT_OPEN)))

        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_share)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_download)).assertDoesNotExist()
    }

    @Test
    fun `the top bar shows the title and back calls onBackClick`() {
        show(stateWith(FileContentState.Error(ViewerError.CANNOT_OPEN)))

        composeRule.onNodeWithText(file.title).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun `share emits Share for the current file`() {
        show(stateWith(external()))

        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_share)).performClick()

        assertEquals(listOf<FileViewerAction>(FileViewerAction.Share(file)), userActions())
    }

    @Test
    fun `download emits Download for a file not saved yet`() {
        show(stateWith(external()))

        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_download)).performClick()

        assertEquals(listOf<FileViewerAction>(FileViewerAction.Download(file)), userActions())
    }

    @Test
    fun `a failed download can be tried again`() {
        show(stateWith(external(), downloadState = DownloadState.FAILED))

        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_download)).performClick()

        assertEquals(listOf<FileViewerAction>(FileViewerAction.Download(file)), userActions())
    }

    @Test
    fun `a saved file shows it is downloaded and offers no download`() {
        show(stateWith(external(), downloadState = DownloadState.COMPLETED))

        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_downloaded)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_download)).assertDoesNotExist()
    }

    @Test
    fun `preparing a share replaces the share button with progress`() {
        show(stateWith(external(), preparingShare = true))

        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_share)).assertDoesNotExist()
    }

    @Test
    fun `open with in the overflow menu emits OpenWith`() {
        show(stateWith(external()))

        composeRule.onNodeWithContentDescription(string(R.string.file_viewer_more_options)).performClick()
        composeRule.onNodeWithText(string(R.string.file_viewer_open_with)).performClick()

        assertEquals(listOf<FileViewerAction>(FileViewerAction.OpenWith(file)), userActions())
    }

    @Test
    fun `a file for another app can be opened again`() {
        val content = external()
        show(stateWith(content))

        composeRule.onNodeWithText(string(R.string.file_viewer_external_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.file_viewer_open_again)).performClick()

        assertEquals(listOf<FileViewerAction>(FileViewerAction.OpenExternally(content.file, content.mimeType)), userActions())
    }
}
