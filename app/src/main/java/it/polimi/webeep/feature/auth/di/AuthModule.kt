package it.polimi.webeep.feature.auth.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.feature.auth.data.AuthRepositoryImpl
import it.polimi.webeep.feature.auth.domain.AuthRepository
import javax.inject.Singleton

/** Binds the auth feature's repository. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
