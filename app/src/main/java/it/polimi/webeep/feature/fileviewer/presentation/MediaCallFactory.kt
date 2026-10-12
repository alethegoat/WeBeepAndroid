package it.polimi.webeep.feature.fileviewer.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.Call
import okhttp3.OkHttpClient

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface MediaCallFactoryEntryPoint {
    fun okHttpClient(): OkHttpClient
}

/**
 * The shared authenticated client, for streaming media (ExoPlayer). It is a UI-side detail of
 * the player, so it is fetched through a Hilt entry point instead of being held by the
 * ViewModel: the pluginfile token is added by the client's interceptor, never by the UI state.
 */
@Composable
internal fun rememberMediaCallFactory(): Call.Factory {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, MediaCallFactoryEntryPoint::class.java).okHttpClient()
    }
}
