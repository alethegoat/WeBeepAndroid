package it.polimi.webeep

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import javax.inject.Inject
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient

/**
 * main application class of the webeep app.
 * annotated with @hiltandroidapp to enable dependency injection via hilt.
 */
@HiltAndroidApp
class WeBeepApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var okHttpClientLazy: dagger.Lazy<OkHttpClient>

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .callFactory { okHttpClientLazy.get() }
            .crossfade(true) // crossfade for a premium image loading experience
            .memoryCache {
                MemoryCache.Builder(this)
                    // Leave headroom for PdfRenderer and WebView, which share the
                    // same application heap with Coil's memory cache.
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false) // optimize for aggressive local caching
            .build()
    }
}
