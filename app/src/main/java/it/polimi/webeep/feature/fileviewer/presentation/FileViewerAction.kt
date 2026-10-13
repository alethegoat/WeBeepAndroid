package it.polimi.webeep.feature.fileviewer.presentation

import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import java.io.File

/** User intents and page events the viewer sends to its ViewModel. */
sealed interface FileViewerAction {
    /** A page was composed: its content is requested only now, so unseen files never load. */
    data class PageVisible(val file: ViewerFile) : FileViewerAction

    /** The pager came to rest on [index]. */
    data class PageSettled(val index: Int) : FileViewerAction

    /** Reload a file that failed to load. */
    data class Retry(val file: ViewerFile) : FileViewerAction

    /** Save the file into the app's downloads. */
    data class Download(val file: ViewerFile) : FileViewerAction

    /** Share the file through the system chooser, downloading it first if needed. */
    data class Share(val file: ViewerFile) : FileViewerAction

    /** Open the file in another app, downloading it first if needed. */
    data class OpenWith(val file: ViewerFile) : FileViewerAction

    /** Hands an already local [file] to another app. */
    data class OpenExternally(val file: File, val mimeType: String) : FileViewerAction
}
