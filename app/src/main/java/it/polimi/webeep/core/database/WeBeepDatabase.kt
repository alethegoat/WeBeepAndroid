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
 * main room database for the webeep app.
 * manages the local cache of courses and course sections.
 *
 * entities:
 * - [courseentity]: user courses
 * - [coursesectionentity]: course sections and contents
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

    /**
     * provides the dao for course data access.
     */
    abstract fun courseDao(): CourseDao

    /**
     * provides the dao for course sections content access.
     */
    abstract fun courseContentDao(): CourseContentDao
}
