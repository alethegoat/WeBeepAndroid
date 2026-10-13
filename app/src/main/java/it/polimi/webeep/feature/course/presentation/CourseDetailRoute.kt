package it.polimi.webeep.feature.course.presentation

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.polimi.webeep.core.ui.util.ObserveEffects
import it.polimi.webeep.core.ui.util.openWithExternalApp
import it.polimi.webeep.navigation.FileViewer
import it.polimi.webeep.navigation.dropUnlessResumedWith

@Composable
fun CourseDetailRoute(
    onOpenViewer: (FileViewer) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Effects arrive after the tap: drop them unless still resumed, so a double tap opens one target.
    val openLink = dropUnlessResumedWith<String> { url ->
        CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
    }
    val openViewer = dropUnlessResumedWith(onOpenViewer)

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is CourseDetailEffect.OpenFile ->
                openWithExternalApp(context, effect.file, effect.mimeType, chooserTitle = null)
            is CourseDetailEffect.OpenLink -> openLink(effect.url)
            is CourseDetailEffect.OpenViewer -> openViewer(effect.route)
        }
    }

    CourseDetailScreen(
        // The text field reads the synchronous query; everything else comes from the flow.
        state = state.copy(searchQuery = viewModel.searchQuery),
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier
    )
}
