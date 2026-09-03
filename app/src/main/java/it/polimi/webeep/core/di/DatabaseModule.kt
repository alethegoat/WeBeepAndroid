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
     * Destructive fallback is DEBUG-only; production relies on explicit migrations.
     */
    @Provides
    @Singleton
    fun provideDatabase(application: Application): WeBeepDatabase {
        val builder = Room.databaseBuilder(
            application,
            WeBeepDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4)
        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration(dropAllTables = true)
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
