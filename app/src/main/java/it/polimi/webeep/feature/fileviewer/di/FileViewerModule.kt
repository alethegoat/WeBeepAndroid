package it.polimi.webeep.feature.fileviewer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.feature.fileviewer.data.FileContentRepositoryImpl
import it.polimi.webeep.feature.fileviewer.domain.FileContentRepository

/** Binds the file viewer's content repository. */
@Module
@InstallIn(SingletonComponent::class)
abstract class FileViewerModule {

    @Binds
    abstract fun bindFileContentRepository(impl: FileContentRepositoryImpl): FileContentRepository
}
