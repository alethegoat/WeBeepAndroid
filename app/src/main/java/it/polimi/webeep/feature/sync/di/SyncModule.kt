package it.polimi.webeep.feature.sync.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.feature.sync.data.SyncNotifier
import it.polimi.webeep.feature.sync.data.SyncScheduler
import it.polimi.webeep.feature.sync.data.SyncStateStore
import it.polimi.webeep.feature.sync.domain.SyncNotifications
import it.polimi.webeep.feature.sync.domain.SyncScheduling
import it.polimi.webeep.feature.sync.domain.SyncStateRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncStateRepository(impl: SyncStateStore): SyncStateRepository

    @Binds
    @Singleton
    abstract fun bindSyncNotifications(impl: SyncNotifier): SyncNotifications

    @Binds
    @Singleton
    abstract fun bindSyncScheduling(impl: SyncScheduler): SyncScheduling
}
