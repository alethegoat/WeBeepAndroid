package it.polimi.webeep.feature.fileviewer.presentation

import androidx.compose.runtime.Immutable
import it.polimi.webeep.feature.download.domain.model.DownloadState
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile

/**
 * Everything the viewer draws. [files] stays null until the sibling lookup has resolved; the
 * pager then starts at [initialIndex]. [contents] only holds the pages near the current one
 * (see [FileViewerViewModel]), keyed by [ViewerFile.url].
 */
@Immutable
data class FileViewerUiState(
    val initialFile: ViewerFile,
    val files: List<ViewerFile>? = null,
    val initialIndex: Int = 0,
    val contents: Map<String, FileContentState> = emptyMap(),
    val downloadStates: Map<String, DownloadState> = emptyMap(),
    val preparingShare: Boolean = false
)
