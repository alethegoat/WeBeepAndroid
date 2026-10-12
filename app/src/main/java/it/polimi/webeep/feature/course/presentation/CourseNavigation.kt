package it.polimi.webeep.feature.course.presentation

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import it.polimi.webeep.core.ui.util.LocalNavAnimatedVisibilityScope
import it.polimi.webeep.navigation.CourseDetail
import it.polimi.webeep.navigation.FileViewer
import it.polimi.webeep.navigation.dropUnlessResumedWith

fun NavGraphBuilder.courseDestination(onOpenViewer: (FileViewer) -> Unit, onBack: () -> Unit) {
    composable<CourseDetail> {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            val context = LocalContext.current
            val viewModel: CourseDetailViewModel = hiltViewModel()
            CourseDetailScreen(
                viewModel = viewModel,
                onPreviewFile = dropUnlessResumedWith { file ->
                    when (val target = previewTargetFor(file, viewModel.courseId)) {
                        is FilePreviewTarget.ExternalLink ->
                            CustomTabsIntent.Builder().build().launchUrl(context, target.url.toUri())
                        is FilePreviewTarget.Viewer -> onOpenViewer(target.route)
                    }
                },
                onBackClick = dropUnlessResumed(block = onBack)
            )
        }
    }
}
