package it.polimi.webeep.feature.fileviewer.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import it.polimi.webeep.R
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import okhttp3.Call
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.theme.WeBeepTheme

@OptIn(UnstableApi::class)
@Composable
fun MediaViewer(
    url: String,
    callFactory: Call.Factory,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    val userPaused = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    val exoPlayer = remember {
        // Goes through the shared OkHttp client: the pluginfile token is added
        // by its interceptor, never embedded in the media URL.
        val dataSourceFactory = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(callFactory))
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(MoodleFileUrlBuilder.stripAuthFromUrl(url))))
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        hasError = true
                        isBuffering = false
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            userPaused.set(false)
                        } else if (playbackState == Player.STATE_READY) {
                            userPaused.set(true)
                        }
                    }
                })
                prepare()
                playWhenReady = true
            }
    }

    val isCurrent by rememberUpdatedState(isCurrentPage)

    // Swiping to another file pauses playback right away.
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) exoPlayer.pause()
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (!userPaused.get() && isCurrent) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WeBeepTheme.colors.mediaScrim)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && !hasError) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = WeBeepTheme.colors.onMedia
            )
        }

        if (hasError) {
            MediaErrorState(url = url, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Opaque themed surface over the player so the empty state keeps its contrast. */
@Composable
private fun MediaErrorState(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val externalUrl = remember(url) { MoodleFileUrlBuilder.stripAuthFromUrl(url) }
    val canOpenExternally = remember(externalUrl) { !MoodleFileUrlBuilder.isPluginFilePath(externalUrl) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        EmptyStateView(
            icon = Icons.Outlined.VideocamOff,
            title = stringResource(R.string.media_playback_error),
            actionLabel = if (canOpenExternally) stringResource(R.string.open_in_browser) else null,
            onAction = if (canOpenExternally) {
                {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl)))
                    } catch (_: Exception) {}
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
