package it.polimi.webeep.core.di

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import it.polimi.webeep.BuildConfig
import it.polimi.webeep.core.common.AppDirectories
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.network.LogRedactor
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.MoodleApiServiceImpl
import it.polimi.webeep.core.network.NoStoreAuthCacheInterceptor
import it.polimi.webeep.core.network.PluginFileAuthInterceptor
import it.polimi.webeep.core.network.RetryInterceptor
import it.polimi.webeep.core.network.TokenInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Hilt module for network dependencies (Ktor over OkHttp).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Keep host in sync with LoginUseCase.SITE_WWWROOT (a unit test pins both to MoodleFileUrlBuilder.TRUSTED_HOST).
    private const val BASE_URL = "https://webeep.polimi.it/"
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L
    private const val HTTP_CACHE_SIZE_BYTES = 10L * 1024L * 1024L

    /** HTTP logging at body level in debug builds only; every line goes through `LogRedactor` first. */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor.Logger { message ->
            Log.d("OkHttp", LogRedactor.redact(message))
        }
        return HttpLoggingInterceptor(logger).apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideTokenInterceptor(tokenProvider: TokenProvider): TokenInterceptor = TokenInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun providePluginFileAuthInterceptor(tokenProvider: TokenProvider): PluginFileAuthInterceptor = PluginFileAuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideNoStoreAuthCacheInterceptor(): NoStoreAuthCacheInterceptor = NoStoreAuthCacheInterceptor()

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor = RetryInterceptor()

    /**
     * The single HTTP client. Interceptor order matters: retry wraps everything, the plugin-file and wstoken interceptors add
     * auth before logging (which redacts tokens), and the network-level interceptor stops auth responses reaching the disk cache.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        loggingInterceptor: HttpLoggingInterceptor,
        tokenInterceptor: TokenInterceptor,
        pluginFileAuthInterceptor: PluginFileAuthInterceptor,
        noStoreAuthCacheInterceptor: NoStoreAuthCacheInterceptor,
        retryInterceptor: RetryInterceptor
    ): OkHttpClient {
        val cacheSize = HTTP_CACHE_SIZE_BYTES
        val cacheDir = File(context.cacheDir, AppDirectories.HTTP_CACHE)
        return OkHttpClient.Builder()
            .cache(Cache(cacheDir, cacheSize))
            .addInterceptor(retryInterceptor)
            .addInterceptor(pluginFileAuthInterceptor)
            .addInterceptor(tokenInterceptor)
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor(noStoreAuthCacheInterceptor)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /** Ktor client that delegates to the shared [OkHttpClient], so it gets the same auth, retry and cache behavior. */
    @Provides
    @Singleton
    fun provideKtorHttpClient(okHttpClient: OkHttpClient): HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        defaultRequest {
            url(BASE_URL)
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }
            )
        }
    }

    @Provides
    @Singleton
    fun provideMoodleApiService(httpClient: HttpClient): MoodleApiService = MoodleApiServiceImpl(httpClient)
}
