package it.polimi.webeep.feature.fileviewer.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import it.polimi.webeep.feature.fileviewer.presentation.FileContentState
import it.polimi.webeep.feature.fileviewer.presentation.FileViewerAction
import it.polimi.webeep.feature.fileviewer.presentation.components.pdf.PdfViewer
import okhttp3.Call

private const val CONTENT_INITIAL_SCALE = 0.98f

/**
 * One pager page. Asks for its [content] once composed (so unseen files never load) and
 * cross-fades between the loading, ready and error states.
 */
@Composable
fun FileViewerPage(
    file: ViewerFile,
    content: FileContentState?,
    isCurrentPage: Boolean,
    contentPadding: PaddingValues,
    mediaCallFactory: Call.Factory,
    onAction: (FileViewerAction) -> Unit,
    onToggleChrome: () -> Unit,
    onZoomedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val currentOnAction by rememberUpdatedState(onAction)

    // Also re-requests the content if it was dropped while this page stayed composed.
    LaunchedEffect(file.url, content == null) { currentOnAction(FileViewerAction.PageVisible(file)) }

    AnimatedContent(
        targetState = content ?: FileContentState.Downloading(0L, null),
        contentKey = { it::class },
        transitionSpec = {
            (
                fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)) +
                    scaleIn(WeBeepMotion.enter(reduceMotion = reduceMotion), initialScale = CONTENT_INITIAL_SCALE)
                ) togetherWith
                fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
        },
        label = "file_content",
        modifier = modifier.fillMaxSize()
    ) { state ->
        FileContent(
            file = file,
            state = state,
            isCurrentPage = isCurrentPage,
            contentPadding = contentPadding,
            mediaCallFactory = mediaCallFactory,
            onAction = onAction,
            onToggleChrome = onToggleChrome,
            onZoomedChange = onZoomedChange
        )
    }
}

@Composable
private fun FileContent(
    file: ViewerFile,
    state: FileContentState,
    isCurrentPage: Boolean,
    contentPadding: PaddingValues,
    mediaCallFactory: Call.Factory,
    onAction: (FileViewerAction) -> Unit,
    onToggleChrome: () -> Unit,
    onZoomedChange: (Boolean) -> Unit
) {
    when (state) {
        is FileContentState.Downloading -> FileViewerLoading(title = file.title, progress = state)
        is FileContentState.Image -> ImagePreviewContent(
            url = state.url,
            title = file.title,
            onToggleFullScreen = onToggleChrome,
            onZoomedChange = onZoomedChange
        )
        is FileContentState.Pdf -> PdfViewer(
            file = state.file,
            pageCount = state.pageCount,
            contentPadding = contentPadding,
            onToggleFullScreen = onToggleChrome,
            onZoomedChange = onZoomedChange
        )
        is FileContentState.Media -> MediaViewer(
            url = state.url,
            callFactory = mediaCallFactory,
            isCurrentPage = isCurrentPage,
            modifier = Modifier.padding(top = contentPadding.calculateTopPadding())
        )
        is FileContentState.Code -> CodeViewer(
            content = state.content,
            contentPadding = contentPadding,
            onTap = onToggleChrome
        )
        is FileContentState.External -> ExternalFileContent(
            title = file.title,
            onOpenAgain = { onAction(FileViewerAction.OpenExternally(state.file, state.mimeType)) }
        )
        is FileContentState.Error -> EmptyStateView(
            icon = Icons.Rounded.Description,
            title = stringResource(R.string.file_viewer_error_title),
            subtitle = stringResource(state.messageRes),
            actionLabel = stringResource(R.string.retry),
            onAction = { onAction(FileViewerAction.Retry(file)) },
            modifier = Modifier.fillMaxSize()
        )
    }
}
