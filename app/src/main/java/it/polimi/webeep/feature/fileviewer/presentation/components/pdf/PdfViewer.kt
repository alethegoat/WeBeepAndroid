package it.polimi.webeep.feature.fileviewer.presentation.components.pdf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.min
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.util.currentWindowSize
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation

/** HD pages never exceed this width: a full 1800 px ARGB page is ~18 MB. */
private const val MAX_HIGH_RES_WIDTH = 1800

/** Opens [file] in a [PdfRendererSession] tied to this composition and shows its pages. */
@Composable
fun PdfViewer(
    file: File,
    pageCount: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    onToggleFullScreen: () -> Unit = {},
    onZoomedChange: (Boolean) -> Unit = {}
) {
    // HD pages never exceed the screen width, and pages never grow past MaxPageWidth
    // (tablets), so neither does their HD render.
    val windowWidth = currentWindowSize().width
    val pageWidthPx = with(LocalDensity.current) { min(windowWidth, MaxPageWidth).roundToPx() }
    val highResWidth = remember(pageWidthPx) { min(MAX_HIGH_RES_WIDTH, pageWidthPx) }

    val loadState by produceState<PdfLoadState>(PdfLoadState.Loading, file) {
        value = PdfLoadState.Loading
        // Created before opening so a cancellation during open still closes it.
        val session = PdfRendererSession(ioDispatcher)
        try {
            val ratio = session.open(file)
            value = if (ratio != null) PdfLoadState.Ready(session, ratio) else PdfLoadState.Error
            awaitCancellation()
        } finally {
            session.close()
        }
    }

    when (val state = loadState) {
        PdfLoadState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        PdfLoadState.Error -> EmptyStateView(
            icon = Icons.Rounded.Description,
            title = stringResource(R.string.file_viewer_error_title),
            subtitle = stringResource(R.string.file_viewer_error_pdf),
            modifier = modifier.fillMaxSize()
        )
        is PdfLoadState.Ready -> PdfPages(
            session = state.session,
            pageCount = pageCount,
            defaultAspectRatio = state.defaultAspectRatio,
            highResWidth = highResWidth,
            onToggleFullScreen = onToggleFullScreen,
            onZoomedChange = onZoomedChange,
            contentPadding = contentPadding,
            modifier = modifier
        )
    }
}

@Immutable
private sealed interface PdfLoadState {
    data object Loading : PdfLoadState
    class Ready(val session: PdfRendererSession, val defaultAspectRatio: Float) : PdfLoadState
    data object Error : PdfLoadState
}
