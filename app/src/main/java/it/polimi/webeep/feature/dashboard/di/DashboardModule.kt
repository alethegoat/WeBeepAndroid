package it.polimi.webeep.feature.dashboard.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.feature.dashboard.data.DashboardRepositoryImpl
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import javax.inject.Singleton

/** Binds the dashboard feature's repository. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository
}
