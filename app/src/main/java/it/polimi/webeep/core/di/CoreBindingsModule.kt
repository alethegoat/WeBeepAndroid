package it.polimi.webeep.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.core.common.ConnectivityObserver
import it.polimi.webeep.core.network.NetworkMonitor
import it.polimi.webeep.core.session.SessionDataCleaner
import it.polimi.webeep.core.session.data.SessionDataCleanerImpl
import javax.inject.Singleton

/** Bindings owned by core; each feature binds its own repositories in `feature/<x>/di`. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindingsModule {

    @Binds
    @Singleton
    abstract fun bindSessionDataCleaner(impl: SessionDataCleanerImpl): SessionDataCleaner

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(impl: NetworkMonitor): ConnectivityObserver
}
