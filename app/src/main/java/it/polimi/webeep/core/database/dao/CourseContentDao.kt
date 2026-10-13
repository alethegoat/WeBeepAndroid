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

/** A module with its files, as loaded by one relation query. */
data class ModuleWithContents(
    @Embedded val module: CourseModuleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "moduleId"
    )
    val contents: List<ModuleContentEntity>
)

/** A section with its modules and their files, as loaded by one relation query. */
data class SectionWithModules(
    @Embedded val section: CourseSectionEntity,
    @Relation(
        entity = CourseModuleEntity::class,
        parentColumn = "id",
        entityColumn = "sectionId"
    )
    val modules: List<ModuleWithContents>
)

/** Sections, modules and files of a course. Deleting a section cascades to its modules and their files. */
@Dao
abstract class CourseContentDao {

    /** Sections of [courseId] in course order; re-emits when any table involved changes. */
    @Transaction
    @Query("SELECT * FROM course_sections WHERE courseId = :courseId ORDER BY section ASC")
    abstract fun getSectionsByCourse(courseId: Int): Flow<List<SectionWithModules>>

    /** One-shot version of [getSectionsByCourse]. */
    @Transaction
    @Query("SELECT * FROM course_sections WHERE courseId = :courseId ORDER BY section ASC")
    abstract suspend fun getSectionsByCourseSync(courseId: Int): List<SectionWithModules>

    /** Inserts rows, replacing any with the same id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSections(sections: List<CourseSectionEntity>)

    /** Inserts rows, replacing any with the same id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertModules(modules: List<CourseModuleEntity>)

    /** Inserts rows, replacing any with the same id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertContents(contents: List<ModuleContentEntity>)

    /** Deletes the sections of [courseId] and, by cascade, their modules and files. */
    @Query("DELETE FROM course_sections WHERE courseId = :courseId")
    abstract suspend fun deleteSectionsByCourse(courseId: Int)

    /**
     * Replaces the whole content of [courseId] in one transaction, so collectors never see a half-written
     * course (which would flicker the UI).
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
