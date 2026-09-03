package it.polimi.webeep.feature.download.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import it.polimi.webeep.core.ui.util.bouncyClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.polimi.webeep.feature.course.presentation.DownloadState

@Composable
fun DownloadProgressIndicator(
    state: DownloadState,
    onDownloadClick: () -> Unit,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = state,
        label = "download_progress_crossfade",
        modifier = modifier
    ) { downloadState ->
        when (downloadState) {
            DownloadState.NOT_STARTED -> {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        .bouncyClickable(onClick = onDownloadClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = androidx.compose.ui.res.stringResource(it.polimi.webeep.R.string.cd_download_resource),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            DownloadState.DOWNLOADING -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
            DownloadState.COMPLETED -> {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32).copy(alpha = 0.1f))
                        .bouncyClickable(onClick = onOpenClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = androidx.compose.ui.res.stringResource(it.polimi.webeep.R.string.cd_open_file),
                        tint = Color(0xFF2E7D32)
                    )
                }
            }
            DownloadState.FAILED -> {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .bouncyClickable(onClick = onDownloadClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = androidx.compose.ui.res.stringResource(it.polimi.webeep.R.string.cd_error_retry),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
