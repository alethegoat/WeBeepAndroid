package it.polimi.webeep.feature.course.domain

import it.polimi.webeep.feature.course.domain.model.Section
import kotlinx.coroutines.flow.Flow

/**
 * repository for managing detailed information of a course.
 */
interface CourseRepository {
    /**
     * synchronizes and extracts the tree of sections, modules, and resources for a course.
     */
    suspend fun getCourseContents(courseId: Int): Result<List<Section>>

    /**
     * retrieves the course sections saved locally in the cache.
     */
    fun getCachedCourseContents(courseId: Int): Flow<List<Section>>

    /**
     * retrieves the course name from the local db.
     */
    suspend fun getCourseName(courseId: Int): String?
}
