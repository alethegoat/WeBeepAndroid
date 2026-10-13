package it.polimi.webeep.feature.fileviewer.presentation

import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import java.io.File

sealed interface FileViewerAction {
    /** A page was composed: its content is requested only now, so unseen files never load. */
    data class PageVisible(val file: ViewerFile) : FileViewerAction

    /** The pager came to rest on [index]. */
    data class PageSettled(val index: Int) : FileViewerAction
    data class Retry(val file: ViewerFile) : FileViewerAction
    data class Download(val file: ViewerFile) : FileViewerAction
    data class Share(val file: ViewerFile) : FileViewerAction
    data class OpenWith(val file: ViewerFile) : FileViewerAction

    /** Hands an already local [file] to another app. */
    data class OpenExternally(val file: File, val mimeType: String) : FileViewerAction
}
