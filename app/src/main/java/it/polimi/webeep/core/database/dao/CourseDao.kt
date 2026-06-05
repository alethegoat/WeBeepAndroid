package it.polimi.webeep.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import it.polimi.webeep.core.database.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

/**
 * dao for accessing course data in local cache.
 * provides crud operations and reactive queries via flow.
 */
@Dao
interface CourseDao {

    /**
     * retrieves all courses ordered by full name in ascending order.
     * returns a flow that automatically emits updates when data changes.
     */
    @Query("SELECT * FROM courses ORDER BY fullname ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCourse(course: CourseEntity): Long

    @androidx.room.Update
    suspend fun updateCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id IN (:courseIds)")
    suspend fun deleteCoursesByIds(courseIds: List<Int>)

    @Query("SELECT id FROM courses")
    suspend fun getAllCourseIds(): List<Int>

    /**
     * replaces all courses in the database in a single atomic transaction,
     * updating existing ones to avoid triggering cascade delete on sections,
     * and deleting only courses that are no longer present.
     */
    @Transaction
    suspend fun replaceAllCourses(courses: List<CourseEntity>) {
        val oldIds = getAllCourseIds()
        val newIds = courses.map { it.id }.toSet()
        
        val toDelete = oldIds.subtract(newIds)
        if (toDelete.isNotEmpty()) {
            deleteCoursesByIds(toDelete.toList())
        }

        for (course in courses) {
            val id = insertCourse(course)
            if (id == -1L) {
                // if it already exists, update it so we don't trigger cascade delete
                updateCourse(course)
            }
        }
    }

    /**
     * retrieves a single course by its id.
     * returns null if the course is not present in the cache.
     */
    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: Int): CourseEntity?
}
