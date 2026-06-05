package it.polimi.webeep.feature.fileviewer.presentation.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

/**
 * component to display and play media files.
 */
@OptIn(UnstableApi::class)
@Composable
fun MediaViewer(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val errorMessageState = remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val isBufferingState = remember { androidx.compose.runtime.mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            setMediaItem(mediaItem)
            
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    errorMessageState.value = error.localizedMessage ?: "Video playback error"
                    isBufferingState.value = false
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBufferingState.value = playbackState == androidx.media3.common.Player.STATE_BUFFERING
                }
            })
            
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
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
            .background(MaterialTheme.colorScheme.scrim)
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
        
        if (isBufferingState.value && errorMessageState.value == null) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        val currentError = errorMessageState.value
        if (currentError != null) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.Center)
                    .background(MaterialTheme.colorScheme.errorContainer, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = currentError,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                // button to open externally as fallback
                androidx.compose.material3.Button(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch(e: Exception){}
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    androidx.compose.material3.Text("Open in Browser")
                }
            }
        }
    }
}
