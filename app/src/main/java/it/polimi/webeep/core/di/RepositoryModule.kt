package it.polimi.webeep.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.core.common.ConnectivityObserver
import it.polimi.webeep.core.network.NetworkMonitor
import it.polimi.webeep.core.session.SessionDataCleaner
import it.polimi.webeep.core.session.data.SessionDataCleanerImpl
import it.polimi.webeep.feature.auth.data.AuthRepositoryImpl
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.course.data.CourseRepositoryImpl
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.dashboard.data.DashboardRepositoryImpl
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.download.data.DownloadRepositoryImpl
import it.polimi.webeep.feature.download.domain.DownloadRepository
import it.polimi.webeep.feature.fileviewer.data.FileContentRepositoryImpl
import it.polimi.webeep.feature.fileviewer.domain.FileContentRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionDataCleaner(impl: SessionDataCleanerImpl): SessionDataCleaner

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    abstract fun bindFileContentRepository(impl: FileContentRepositoryImpl): FileContentRepository

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(impl: NetworkMonitor): ConnectivityObserver
}
