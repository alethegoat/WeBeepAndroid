package it.polimi.webeep.feature.course.domain

import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.course.domain.model.Section
import kotlinx.coroutines.flow.Flow

/** Course sections, modules and files, from Moodle and from the local cache. */
interface CourseRepository {
    /** Fetches [courseId] from Moodle and replaces the cached copy; fails with [Resource.Error] when logged out, offline or rejected. */
    suspend fun getCourseContents(courseId: Int): Resource<List<Section>>

    /** Cached sections of [courseId], re-emitted whenever the cache changes; empty when nothing was fetched yet. */
    fun getCachedCourseContents(courseId: Int): Flow<List<Section>>

    /** Cached full name of [courseId], or `null` if the course is not cached. */
    suspend fun getCourseName(courseId: Int): String?
}
