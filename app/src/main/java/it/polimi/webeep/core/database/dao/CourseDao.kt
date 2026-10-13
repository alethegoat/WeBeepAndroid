package it.polimi.webeep.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import it.polimi.webeep.core.database.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY fullname ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id IN (:courseIds)")
    suspend fun deleteCoursesByIds(courseIds: List<Int>)

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

    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: Int): CourseEntity?
}
