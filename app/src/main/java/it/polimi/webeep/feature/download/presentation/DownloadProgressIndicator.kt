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
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.polimi.webeep.feature.course.presentation.DownloadState

/**
 * animated progress indicator for the various states of a download.
 */
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
                IconButton(
                    onClick = onDownloadClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Scarica risorsa"
                    )
                }
            }
            DownloadState.DOWNLOADING -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }
            DownloadState.COMPLETED -> {
                IconButton(
                    onClick = onOpenClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color(0xFF2E7D32) // green
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Apri file"
                    )
                }
            }
            DownloadState.FAILED -> {
                IconButton(
                    onClick = onDownloadClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Errore. Riprova"
                    )
                }
            }
        }
    }
}
