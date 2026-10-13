package it.polimi.webeep.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import it.polimi.webeep.core.database.dao.CourseContentDao
import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.database.entity.CourseEntity
import it.polimi.webeep.core.database.entity.CourseModuleEntity
import it.polimi.webeep.core.database.entity.CourseSectionEntity
import it.polimi.webeep.core.database.entity.ModuleContentEntity

/**
 * Local cache of the user's courses and their contents, so screens work offline.
 *
 * It only holds data that Moodle can provide again, never credentials. Bumping the database version requires
 * a migration in [DatabaseMigrations] and an exported schema.
 */
@Database(
    entities = [
        CourseEntity::class,
        CourseSectionEntity::class,
        CourseModuleEntity::class,
        ModuleContentEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class WeBeepDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao

    abstract fun courseContentDao(): CourseContentDao
}
