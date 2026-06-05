package it.polimi.webeep.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import it.polimi.webeep.core.database.entity.CourseModuleEntity
import it.polimi.webeep.core.database.entity.CourseSectionEntity
import it.polimi.webeep.core.database.entity.ModuleContentEntity
import kotlinx.coroutines.flow.Flow

data class ModuleWithContents(
    @Embedded val module: CourseModuleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "moduleId"
    )
    val contents: List<ModuleContentEntity>
)

data class SectionWithModules(
    @Embedded val section: CourseSectionEntity,
    @Relation(
        entity = CourseModuleEntity::class,
        parentColumn = "id",
        entityColumn = "sectionId"
    )
    val modules: List<ModuleWithContents>
)

/**
 * dao for accessing course section contents in local cache.
 */
@Dao
abstract class CourseContentDao {

    /**
     * retrieves all sections of a specific course, ordered by section number.
     * returns a flow that automatically emits updates when data changes.
     */
    @Transaction
    @Query("SELECT * FROM course_sections WHERE courseId = :courseId ORDER BY section ASC")
    abstract fun getSectionsByCourse(courseId: Int): Flow<List<SectionWithModules>>

    @Transaction
    @Query("SELECT * FROM course_sections WHERE courseId = :courseId ORDER BY section ASC")
    abstract suspend fun getSectionsByCourseSync(courseId: Int): List<SectionWithModules>

    /**
     * inserts a list of sections into the database.
     * in case of conflict (same id), the existing section is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSections(sections: List<CourseSectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertModules(modules: List<CourseModuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertContents(contents: List<ModuleContentEntity>)

    /**
     * deletes all sections of a specific course. (cascade will also delete modules and files)
     */
    @Query("DELETE FROM course_sections WHERE courseId = :courseId")
    abstract suspend fun deleteSectionsByCourse(courseId: Int)

    /**
     * performs a complete replacement of course contents atomically.
     * this prevents flickering in the user interface.
     */
    @Transaction
    open suspend fun replaceCourseContent(
        courseId: Int,
        sections: List<CourseSectionEntity>,
        modules: List<CourseModuleEntity>,
        contents: List<ModuleContentEntity>
    ) {
        deleteSectionsByCourse(courseId)
        insertSections(sections)
        insertModules(modules)
        insertContents(contents)
    }
}
