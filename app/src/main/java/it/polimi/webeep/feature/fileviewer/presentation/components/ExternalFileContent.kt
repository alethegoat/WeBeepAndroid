package it.polimi.webeep.feature.fileviewer.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.EmptyStateView

/**
 * Shown for files the viewer cannot render itself. The first open happens
 * automatically (the ViewModel decides when); this lets the user open it again.
 */
@Composable
fun ExternalFileContent(title: String, onOpenAgain: () -> Unit, modifier: Modifier = Modifier) {
    EmptyStateView(
        icon = Icons.Rounded.Description,
        title = stringResource(R.string.file_viewer_external_title),
        subtitle = stringResource(R.string.file_viewer_external_subtitle, title),
        actionLabel = stringResource(R.string.file_viewer_open_again),
        onAction = onOpenAgain,
        modifier = modifier.fillMaxSize()
    )
}
