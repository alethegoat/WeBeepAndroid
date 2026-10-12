package it.polimi.webeep.feature.course.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import it.polimi.webeep.core.ui.util.LocalNavAnimatedVisibilityScope
import it.polimi.webeep.navigation.CourseDetail
import it.polimi.webeep.navigation.FileViewer

/** Back is dropped unless the entry is resumed; the route guards its own open-file targets the same way. */
fun NavGraphBuilder.courseDestination(onOpenViewer: (FileViewer) -> Unit, onBack: () -> Unit) {
    composable<CourseDetail> {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            CourseDetailRoute(
                onOpenViewer = onOpenViewer,
                onBackClick = dropUnlessResumed(block = onBack)
            )
        }
    }
}
