package it.polimi.webeep.feature.fileviewer.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.rememberZoomState
import it.polimi.webeep.core.ui.util.zoomable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HINT_DURATION_MS = 2_000L
private const val HINT_SCRIM_ALPHA = 0.65f

/** Pinch-to-zoom image. Tap toggles full screen, double tap zooms. */
@Composable
fun ImagePreviewContent(
    url: String,
    title: String,
    modifier: Modifier = Modifier,
    onToggleFullScreen: () -> Unit = {},
    onZoomedChange: (Boolean) -> Unit = {}
) {
    val reduceMotion = LocalReduceMotion.current
    val zoomState = rememberZoomState()
    val coroutineScope = rememberCoroutineScope()
    var showHint by remember { mutableStateOf(true) }
    val currentOnZoomedChange by rememberUpdatedState(onZoomedChange)

    LaunchedEffect(zoomState.isZoomed) { currentOnZoomedChange(zoomState.isZoomed) }
    LaunchedEffect(Unit) {
        delay(HINT_DURATION_MS)
        showHint = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(zoomState) {
                detectTapGestures(
                    onTap = { onToggleFullScreen() },
                    onDoubleTap = { tap ->
                        coroutineScope.launch {
                            zoomState.toggleZoom(tap, WeBeepMotion.smooth(reduceMotion = reduceMotion))
                        }
                    }
                )
            }
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = title,
            modifier = Modifier
                .fillMaxSize()
                .zoomable(zoomState),
            contentScale = ContentScale.Fit,
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
            error = {
                EmptyStateView(
                    icon = Icons.Outlined.BrokenImage,
                    title = stringResource(R.string.file_viewer_error_title),
                    subtitle = stringResource(R.string.file_viewer_image_error),
                    modifier = Modifier.fillMaxSize()
                )
            },
            success = { SubcomposeAsyncImageContent() }
        )

        AnimatedVisibility(
            visible = showHint && !zoomState.isZoomed,
            enter = fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)),
            exit = fadeOut(WeBeepMotion.exit(durationMs = WeBeepMotion.MEDIUM_MS, reduceMotion = reduceMotion)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(WeBeepSpacing.md)
        ) {
            Surface(
                shape = CircleShape,
                color = WeBeepTheme.colors.mediaScrim.copy(alpha = HINT_SCRIM_ALPHA),
                contentColor = WeBeepTheme.colors.onMedia
            ) {
                Text(
                    text = stringResource(R.string.file_viewer_pinch_hint),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.xs)
                )
            }
        }
    }
}
