package it.polimi.webeep.feature.fileviewer.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import it.polimi.webeep.R
import it.polimi.webeep.core.common.FileUtils
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.feature.fileviewer.presentation.FileContentState

/** Title plus download progress; [progress] is null while the size is not known yet. */
@Composable
fun FileViewerLoading(title: String, progress: FileContentState.Downloading?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(WeBeepSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = WeBeepSpacing.lg)
        )
        LoadingBar(fraction = progress?.progress)
        Text(
            text = progressLabel(progress),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = WeBeepSpacing.md),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoadingBar(fraction: Float?) {
    if (fraction != null) {
        val animated by animateFloatAsState(
            targetValue = fraction,
            animationSpec = WeBeepMotion.smooth(
                durationMs = WeBeepMotion.SHORT_MS,
                reduceMotion = LocalReduceMotion.current
            ),
            label = "download_progress"
        )
        LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth())
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun progressLabel(progress: FileContentState.Downloading?): String {
    val total = progress?.totalBytes
    val read = progress?.bytesRead ?: 0L
    return when {
        total != null && read > 0 -> stringResource(
            R.string.file_viewer_download_progress,
            FileUtils.formatFileSize(read),
            FileUtils.formatFileSize(total)
        )
        read > 0 -> FileUtils.formatFileSize(read)
        else -> stringResource(R.string.file_viewer_loading)
    }
}
