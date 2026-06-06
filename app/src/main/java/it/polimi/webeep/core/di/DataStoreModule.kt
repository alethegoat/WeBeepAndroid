package it.polimi.webeep.core.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.datastore.TokenProvider
import javax.inject.Singleton

/**
 * hilt module for datastore and token management configuration.
 * provides the tokenmanager as a singleton for the entire application.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * provides a singleton instance of tokenmanager for secure
     * management of authentication tokens via encryptedsharedpreferences.
     */
    @Provides
    @Singleton
    fun provideTokenManager(
        application: Application,
        @IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ): TokenManager {
        return TokenManager(application, ioDispatcher)
    }

    /**
     * provides the tokenprovider interface for dependency injection.
     * allows replacing the implementation with a fake in tests.
     */
    @Provides
    @Singleton
    fun provideTokenProvider(tokenManager: TokenManager): TokenProvider {
        return tokenManager
    }
}
