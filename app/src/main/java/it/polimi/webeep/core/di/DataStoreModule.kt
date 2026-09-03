package it.polimi.webeep.core.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.datastore.TokenProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideTokenManager(
        application: Application,
        @IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ): TokenManager {
        return TokenManager(application, ioDispatcher)
    }

    /**
     * Exposed as interface so tests can inject a fake.
     */
    @Provides
    @Singleton
    fun provideTokenProvider(tokenManager: TokenManager): TokenProvider {
        return tokenManager
    }
}
