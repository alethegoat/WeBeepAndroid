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

@Dao
abstract class CourseContentDao {

    @Transaction
    @Query("SELECT * FROM course_sections WHERE courseId = :courseId ORDER BY section ASC")
    abstract fun getSectionsByCourse(courseId: Int): Flow<List<SectionWithModules>>

    @Transaction
    @Query("SELECT * FROM course_sections WHERE courseId = :courseId ORDER BY section ASC")
    abstract suspend fun getSectionsByCourseSync(courseId: Int): List<SectionWithModules>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSections(sections: List<CourseSectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertModules(modules: List<CourseModuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertContents(contents: List<ModuleContentEntity>)

    @Query("DELETE FROM course_sections WHERE courseId = :courseId")
    abstract suspend fun deleteSectionsByCourse(courseId: Int)

    /**
     * Atomic replacement avoids UI flickering.
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
