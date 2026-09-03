package it.polimi.webeep.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import it.polimi.webeep.BuildConfig
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.MoodleApiServiceImpl
import it.polimi.webeep.core.network.NoStoreAuthCacheInterceptor
import it.polimi.webeep.core.network.PluginFileAuthInterceptor
import it.polimi.webeep.core.network.RetryInterceptor
import it.polimi.webeep.core.network.TokenInterceptor
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for network dependencies (Ktor over OkHttp).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Keep host in sync with LoginUseCase.SITE_WWWROOT via MoodleFileUrlBuilder.TRUSTED_HOST.
    private const val BASE_URL = "https://webeep.polimi.it/"
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor.Logger { message ->
            val sanitized = message
                .replace(Regex("""(?i)wstoken=[^&\s]+"""), "wstoken=<REDACTED>")
                .replace(Regex("""(?i)private_token=[^&\s]+"""), "private_token=<REDACTED>")
                .replace(Regex("""(?i)([?&])token=[^&\s]+"""), "$1token=<REDACTED>")
                .replace(Regex("""(?i)Bearer\s+\S+"""), "Bearer <REDACTED>")
            android.util.Log.d("OkHttp", sanitized)
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
    fun provideTokenInterceptor(tokenProvider: TokenProvider): TokenInterceptor {
        return TokenInterceptor(tokenProvider)
    }

    @Provides
    @Singleton
    fun providePluginFileAuthInterceptor(tokenProvider: TokenProvider): PluginFileAuthInterceptor {
        return PluginFileAuthInterceptor(tokenProvider)
    }

    @Provides
    @Singleton
    fun provideNoStoreAuthCacheInterceptor(): NoStoreAuthCacheInterceptor {
        return NoStoreAuthCacheInterceptor()
    }

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor {
        return RetryInterceptor()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        loggingInterceptor: HttpLoggingInterceptor,
        tokenInterceptor: TokenInterceptor,
        pluginFileAuthInterceptor: PluginFileAuthInterceptor,
        noStoreAuthCacheInterceptor: NoStoreAuthCacheInterceptor,
        retryInterceptor: RetryInterceptor
    ): OkHttpClient {
        val cacheSize = 10 * 1024 * 1024L
        val cacheDir = java.io.File(context.cacheDir, "http_cache")
        return OkHttpClient.Builder()
            .cache(okhttp3.Cache(cacheDir, cacheSize))
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

    @Provides
    @Singleton
    fun provideKtorHttpClient(okHttpClient: OkHttpClient): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                preconfigured = okHttpClient
            }
            defaultRequest {
                url(BASE_URL)
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
            }
        }
    }

    @Provides
    @Singleton
    fun provideMoodleApiService(httpClient: HttpClient): MoodleApiService {
        return MoodleApiServiceImpl(httpClient)
    }
}
