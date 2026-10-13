package it.polimi.webeep.feature.fileviewer.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.util.ObserveEffects
import it.polimi.webeep.core.ui.util.openWithExternalApp
import it.polimi.webeep.core.ui.util.shareFile

/** Stateful entry of the viewer: performs share and open-with effects. */
@Composable
fun FileViewerRoute(onBackClick: () -> Unit, modifier: Modifier = Modifier, viewModel: FileViewerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.file_viewer_share_chooser)
    val openWithChooserTitle = stringResource(R.string.file_viewer_open_with)

    ObserveEffects(viewModel.effects) { effect ->
        when (effect) {
            is FileViewerEffect.Share ->
                shareFile(context, effect.file, effect.mimeType, effect.title, shareChooserTitle)
            is FileViewerEffect.OpenExternally ->
                openWithExternalApp(context, effect.file, effect.mimeType, openWithChooserTitle)
        }
    }

    FileViewerScreen(
        state = state,
        mediaCallFactory = rememberMediaCallFactory(),
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier
    )
}
