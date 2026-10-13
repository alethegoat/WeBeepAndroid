package it.polimi.webeep.feature.fileviewer.presentation

import java.io.File

/** One-shot requests that need an Activity, so the route performs them. */
sealed interface FileViewerEffect {
    /** Share [file] through the system chooser. */
    data class Share(val file: File, val mimeType: String, val title: String) : FileViewerEffect

    /** Open [file] in another app. */
    data class OpenExternally(val file: File, val mimeType: String) : FileViewerEffect
}
