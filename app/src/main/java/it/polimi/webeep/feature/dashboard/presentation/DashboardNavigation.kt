package it.polimi.webeep.feature.dashboard.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import it.polimi.webeep.core.ui.util.LocalNavAnimatedVisibilityScope
import it.polimi.webeep.navigation.Dashboard
import it.polimi.webeep.navigation.dropUnlessResumedWith

/** Callbacks are dropped unless the entry is resumed, so taps during a transition do not stack screens. */
fun NavGraphBuilder.dashboardDestination(onOpenCourse: (courseId: Int) -> Unit, onOpenSettings: () -> Unit) {
    composable<Dashboard> {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            DashboardRoute(
                onCourseClick = dropUnlessResumedWith(onOpenCourse),
                onSettingsClick = dropUnlessResumed(block = onOpenSettings)
            )
        }
    }
}
