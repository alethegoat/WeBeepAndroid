package it.polimi.webeep.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import it.polimi.webeep.core.database.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

/** Enrolled courses. Deleting a course cascades to its sections, modules and files. */
@Dao
interface CourseDao {

    /** All courses ordered by name; re-emits on every change. */
    @Query("SELECT * FROM courses ORDER BY fullname ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    /** Inserts a new course; returns -1 and leaves the row untouched if the id already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCourse(course: CourseEntity): Long

    /** Updates the row with the same id. */
    @Update
    suspend fun updateCourse(course: CourseEntity)

    /** Deletes the given courses and, by cascade, their contents. */
    @Query("DELETE FROM courses WHERE id IN (:courseIds)")
    suspend fun deleteCoursesByIds(courseIds: List<Int>)

    /** Ids of every cached course. */
    @Query("SELECT id FROM courses")
    suspend fun getAllCourseIds(): List<Int>

    /**
     * Updates in place to avoid triggering cascade delete on sections;
     * deletes only courses that disappeared remotely.
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
                updateCourse(course)
            }
        }
    }

    /** The cached course, or `null` if absent. */
    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: Int): CourseEntity?
}
