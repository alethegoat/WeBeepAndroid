package it.polimi.webeep.feature.download.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.feature.download.data.DownloadRepositoryImpl
import it.polimi.webeep.feature.download.domain.DownloadRepository
import javax.inject.Singleton

/** Binds the download feature's repository. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadModule {

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository
}
