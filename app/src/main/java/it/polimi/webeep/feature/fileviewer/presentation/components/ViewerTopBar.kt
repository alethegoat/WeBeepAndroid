package it.polimi.webeep.feature.fileviewer.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.BackTopBar
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.feature.download.domain.model.DownloadState
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import it.polimi.webeep.feature.fileviewer.presentation.FileViewerAction

private val ActionProgressSize = 20.dp
private val ActionProgressStroke = 2.dp

/**
 * Floating top bar. Its actions (share, download, open with) are hidden by [showActions]
 * until there is a local file worth acting on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerTopBar(
    visible: Boolean,
    file: ViewerFile,
    subtitle: String?,
    showActions: Boolean,
    downloadState: DownloadState,
    preparingShare: Boolean,
    onAction: (FileViewerAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(WeBeepMotion.enter(reduceMotion = reduceMotion)) { -it } +
            fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)),
        exit = slideOutVertically(WeBeepMotion.exit(reduceMotion = reduceMotion)) { -it } +
            fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion)),
        modifier = modifier
    ) {
        BackTopBar(
            title = { ViewerTitle(title = file.title, subtitle = subtitle) },
            onBack = onBackClick,
            actions = {
                if (showActions) {
                    ShareAction(preparingShare = preparingShare, onClick = { onAction(FileViewerAction.Share(file)) })
                    DownloadAction(
                        state = downloadState,
                        onDownload = { onAction(FileViewerAction.Download(file)) }
                    )
                    OverflowMenu(onOpenWith = { onAction(FileViewerAction.OpenWith(file)) })
                }
            },
            // Opaque: pages must never show through the title.
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun ViewerTitle(title: String, subtitle: String?) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ShareAction(preparingShare: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = !preparingShare) {
        if (preparingShare) {
            CircularProgressIndicator(modifier = Modifier.size(ActionProgressSize), strokeWidth = ActionProgressStroke)
        } else {
            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.file_viewer_share))
        }
    }
}

@Composable
private fun DownloadAction(state: DownloadState, onDownload: () -> Unit) {
    when (state) {
        DownloadState.DOWNLOADING -> Box(Modifier.size(WeBeepSpacing.touchTarget), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(ActionProgressSize), strokeWidth = ActionProgressStroke)
        }
        DownloadState.COMPLETED -> Box(Modifier.size(WeBeepSpacing.touchTarget), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.DownloadDone,
                contentDescription = stringResource(R.string.file_viewer_downloaded),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DownloadState.NOT_STARTED, DownloadState.FAILED -> IconButton(onClick = onDownload) {
            Icon(
                imageVector = if (state == DownloadState.FAILED) Icons.Rounded.ErrorOutline else Icons.Rounded.Download,
                contentDescription = stringResource(R.string.file_viewer_download)
            )
        }
    }
}

@Composable
private fun OverflowMenu(onOpenWith: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.file_viewer_more_options))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.file_viewer_open_with)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onOpenWith()
                }
            )
        }
    }
}
