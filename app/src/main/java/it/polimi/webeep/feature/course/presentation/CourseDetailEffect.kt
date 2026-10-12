package it.polimi.webeep.feature.course.presentation

import it.polimi.webeep.navigation.FileViewer
import java.io.File

/** One-shot requests that need an Activity or the navigator, so the route performs them. */
sealed interface CourseDetailEffect {
    /** A downloaded copy to hand to another app. */
    data class OpenFile(val file: File, val mimeType: String) : CourseDetailEffect

    /** A web link outside WeBeep, already stripped of any token. */
    data class OpenLink(val url: String) : CourseDetailEffect

    /** Navigate to the in-app viewer. */
    data class OpenViewer(val route: FileViewer) : CourseDetailEffect
}
