package it.polimi.webeep.feature.course.presentation

import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.navigation.FileViewer

/** Where tapping a file in a course leads. */
sealed interface FilePreviewTarget {
    /** A web link outside WeBeep: a Custom Tab, with no viewer flash on the way. */
    data class ExternalLink(val url: String) : FilePreviewTarget

    data class Viewer(val route: FileViewer) : FilePreviewTarget
}

/**
 * Decides how to preview [file]. The URL is stripped of any token here, before it can
 * end up in a route or a Custom Tab.
 */
fun previewTargetFor(file: FileUiModel, courseId: Int): FilePreviewTarget {
    val url = MoodleFileUrlBuilder.stripAuthFromUrl(file.fileUrl)
    return if (MoodleFileUrlBuilder.isExternalWebLink(url)) {
        FilePreviewTarget.ExternalLink(url)
    } else {
        FilePreviewTarget.Viewer(
            FileViewer(url = url, title = file.filename, mimeType = file.mimeType, courseId = courseId)
        )
    }
}
