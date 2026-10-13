package it.polimi.webeep

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import it.polimi.webeep.core.common.AppDirectories
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.feature.sync.domain.SyncNotifications
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * Process entry point: Hilt root, WorkManager configuration (so workers get injected) and the Coil image loader.
 *
 * Images use the app's authenticated OkHttp client, so pluginfile images need no token in their URL. The client is obtained lazily to keep
 * it off the startup path.
 */
@HiltAndroidApp
class WeBeepApplication :
    Application(),
    Configuration.Provider,
    ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var okHttpClientLazy: dagger.Lazy<OkHttpClient>

    @Inject
    lateinit var tokenProvider: TokenProvider

    @Inject
    lateinit var syncNotifications: SyncNotifications

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        syncNotifications.createChannel()
        // Warm up the Keystore-backed session off the main thread, before the first frame needs it.
        appScope.launch { runCatching { tokenProvider.ensureLoaded() } }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .callFactory { okHttpClientLazy.get() }
        .crossfade(true)
        .memoryCache {
            MemoryCache.Builder(this)
                // Leave headroom for PdfRenderer and WebView sharing the app heap.
                .maxSizePercent(MEMORY_CACHE_PERCENT)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve(AppDirectories.IMAGE_CACHE))
                .maxSizePercent(DISK_CACHE_PERCENT)
                .build()
        }
        .respectCacheHeaders(false)
        .build()

    private companion object {
        const val MEMORY_CACHE_PERCENT = 0.15
        const val DISK_CACHE_PERCENT = 0.05
    }
}
