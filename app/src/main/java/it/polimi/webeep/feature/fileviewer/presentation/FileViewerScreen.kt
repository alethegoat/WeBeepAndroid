package it.polimi.webeep.feature.fileviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.feature.download.domain.model.DownloadState
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import it.polimi.webeep.feature.fileviewer.presentation.components.FileViewerLoading
import it.polimi.webeep.feature.fileviewer.presentation.components.FileViewerPage
import it.polimi.webeep.feature.fileviewer.presentation.components.SystemBarsVisibility
import it.polimi.webeep.feature.fileviewer.presentation.components.ViewerTopBar
import java.io.File
import okhttp3.Call
import okhttp3.OkHttpClient

private val TopBarHeight = 64.dp
private val PageSpacing = 16.dp

/**
 * Full-screen file viewer. The top bar floats over the content (tap toggles it
 * and the system bars), so hiding it never moves the page. When opened from a
 * course, sibling files of the same folder can be swiped through.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileViewerScreen(
    state: FileViewerUiState,
    mediaCallFactory: Call.Factory,
    onAction: (FileViewerAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var chromeVisible by rememberSaveable { mutableStateOf(true) }

    SystemBarsVisibility(visible = chromeVisible)

    // Ignoring visibility keeps the padding stable while the bars hide/show.
    val topPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding() + TopBarHeight
    val bottomPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val files = state.files
        if (files == null) {
            FileViewerLoading(title = state.initialFile.title, progress = null)
            ViewerTopBar(
                visible = chromeVisible,
                file = state.initialFile,
                subtitle = null,
                showActions = false,
                downloadState = DownloadState.NOT_STARTED,
                preparingShare = false,
                onAction = onAction,
                onBackClick = onBackClick
            )
        } else {
            FileViewerPager(
                state = state,
                files = files,
                chromeVisible = chromeVisible,
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                mediaCallFactory = mediaCallFactory,
                onToggleChrome = { chromeVisible = !chromeVisible },
                onAction = onAction,
                onBackClick = onBackClick
            )
        }
    }
}

@Composable
private fun FileViewerPager(
    state: FileViewerUiState,
    files: List<ViewerFile>,
    chromeVisible: Boolean,
    contentPadding: PaddingValues,
    mediaCallFactory: Call.Factory,
    onToggleChrome: () -> Unit,
    onAction: (FileViewerAction) -> Unit,
    onBackClick: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = state.initialIndex) { files.size }
    var currentPageZoomed by remember { mutableStateOf(false) }
    val currentOnAction by rememberUpdatedState(onAction)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            currentPageZoomed = false
            currentOnAction(FileViewerAction.PageSettled(page))
        }
    }

    HorizontalPager(
        state = pagerState,
        key = { files[it].url },
        beyondViewportPageCount = 0,
        pageSpacing = PageSpacing,
        userScrollEnabled = files.size > 1 && !currentPageZoomed,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val file = files[page]
        val isCurrent = pagerState.settledPage == page
        FileViewerPage(
            file = file,
            content = state.contents[file.url],
            isCurrentPage = isCurrent,
            contentPadding = contentPadding,
            mediaCallFactory = mediaCallFactory,
            onAction = onAction,
            onToggleChrome = onToggleChrome,
            onZoomedChange = { zoomed -> if (isCurrent) currentPageZoomed = zoomed }
        )
    }

    val currentFile = files[pagerState.currentPage.coerceIn(0, files.lastIndex)]
    val currentContent = state.contents[currentFile.url]
    ViewerTopBar(
        visible = chromeVisible,
        file = currentFile,
        subtitle = viewerSubtitle(pagerState, files.size, currentContent),
        showActions = currentContent != null &&
            currentContent !is FileContentState.Downloading &&
            currentContent !is FileContentState.Error,
        downloadState = state.downloadStates[currentFile.url] ?: DownloadState.NOT_STARTED,
        preparingShare = state.preparingShare,
        onAction = onAction,
        onBackClick = onBackClick
    )
}

/** "2 of 5 · 12 pages": position among the siblings and what the current file is. */
@Composable
private fun viewerSubtitle(pagerState: PagerState, fileCount: Int, content: FileContentState?): String? {
    val position = if (fileCount > 1) {
        stringResource(R.string.file_viewer_position, pagerState.currentPage + 1, fileCount)
    } else {
        null
    }
    val kind = when (content) {
        is FileContentState.Pdf -> pluralStringResource(R.plurals.file_viewer_pdf_pages, content.pageCount, content.pageCount)
        is FileContentState.Image -> stringResource(R.string.file_viewer_image)
        else -> null
    }
    return listOfNotNull(position, kind).joinToString(" · ").ifEmpty { null }
}

private val previewFile = ViewerFile(url = "https://example.com/notes.zip", title = "Lecture notes.zip", mimeType = "application/zip")

@Composable
private fun PreviewScreen(state: FileViewerUiState) {
    WeBeepTheme {
        FileViewerScreen(
            state = state,
            mediaCallFactory = OkHttpClient(),
            onAction = {},
            onBackClick = {}
        )
    }
}

@Preview(name = "Resolving files")
@Composable
private fun FileViewerScreenPreviewResolving() {
    PreviewScreen(FileViewerUiState(initialFile = previewFile))
}

@Preview(name = "Downloading")
@Composable
private fun FileViewerScreenPreviewDownloading() {
    PreviewScreen(
        FileViewerUiState(
            initialFile = previewFile,
            files = listOf(previewFile),
            contents = mapOf(previewFile.url to FileContentState.Downloading(bytesRead = 2_400_000L, totalBytes = 6_000_000L))
        )
    )
}

@Preview(name = "Error")
@Composable
private fun FileViewerScreenPreviewError() {
    PreviewScreen(
        FileViewerUiState(
            initialFile = previewFile,
            files = listOf(previewFile),
            contents = mapOf(previewFile.url to FileContentState.Error(ViewerError.NETWORK))
        )
    )
}

@Preview(name = "Other app")
@Composable
private fun FileViewerScreenPreviewExternal() {
    PreviewScreen(
        FileViewerUiState(
            initialFile = previewFile,
            files = listOf(previewFile),
            contents = mapOf(previewFile.url to FileContentState.External(File("notes.zip"), "application/zip")),
            downloadStates = mapOf(previewFile.url to DownloadState.COMPLETED)
        )
    )
}
