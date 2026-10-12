package it.polimi.webeep.feature.course.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.feature.download.domain.model.DownloadState

private val ProgressStrokeWidth = 2.dp

/**
 * Compact download / open action: 48dp touch target, 24dp icon, no filled circle.
 * A plain switch, not a crossfade: every visible file row has one, and the state
 * changes rarely.
 */
@Composable
internal fun FileActionButton(state: DownloadState, onDownloadClick: () -> Unit, onOpenClick: () -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        DownloadState.NOT_STARTED -> FileActionIcon(
            icon = Icons.Rounded.Download,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(R.string.cd_download_resource),
            onClick = onDownloadClick,
            modifier = modifier
        )
        DownloadState.DOWNLOADING -> {
            val description = stringResource(R.string.cd_downloading)
            Box(
                modifier = modifier
                    .size(WeBeepSpacing.touchTarget)
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = ProgressStrokeWidth,
                    modifier = Modifier.size(RowIconSize - WeBeepSpacing.xxs)
                )
            }
        }
        DownloadState.COMPLETED -> FileActionIcon(
            icon = Icons.Rounded.CheckCircle,
            tint = WeBeepTheme.colors.success,
            contentDescription = stringResource(R.string.cd_open_file),
            onClick = onOpenClick,
            modifier = modifier
        )
        DownloadState.FAILED -> FileActionIcon(
            icon = Icons.Rounded.ErrorOutline,
            tint = MaterialTheme.colorScheme.error,
            contentDescription = stringResource(R.string.cd_error_retry),
            onClick = onDownloadClick,
            modifier = modifier
        )
    }
}

@Composable
private fun FileActionIcon(icon: ImageVector, tint: Color, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(RowIconSize)
        )
    }
}
