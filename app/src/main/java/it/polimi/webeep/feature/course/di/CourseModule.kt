package it.polimi.webeep.feature.course.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.feature.course.data.CourseRepositoryImpl
import it.polimi.webeep.feature.course.domain.CourseRepository
import javax.inject.Singleton

/** Binds the course feature's repository. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CourseModule {

    @Binds
    @Singleton
    abstract fun bindCourseRepository(impl: CourseRepositoryImpl): CourseRepository
}
