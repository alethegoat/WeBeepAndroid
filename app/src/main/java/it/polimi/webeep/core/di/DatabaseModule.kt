package it.polimi.webeep.core.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.polimi.webeep.BuildConfig
import it.polimi.webeep.core.database.DatabaseMigrations
import it.polimi.webeep.core.database.WeBeepDatabase
import it.polimi.webeep.core.database.dao.CourseContentDao
import it.polimi.webeep.core.database.dao.CourseDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "webeep_database"

    /**
     * Full destructive fallback is DEBUG-only; production relies on explicit migrations
     * and only drops the (rebuildable) cache on a version downgrade.
     */
    @Provides
    @Singleton
    fun provideDatabase(application: Application): WeBeepDatabase {
        val builder = Room.databaseBuilder(
            application,
            WeBeepDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(*DatabaseMigrations.ALL)
        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration(dropAllTables = true)
        } else {
            builder.fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideCourseDao(database: WeBeepDatabase): CourseDao {
        return database.courseDao()
    }

    @Provides
    @Singleton
    fun provideCourseContentDao(database: WeBeepDatabase): CourseContentDao {
        return database.courseContentDao()
    }
}
