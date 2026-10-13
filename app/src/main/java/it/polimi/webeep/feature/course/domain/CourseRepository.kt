package it.polimi.webeep.feature.course.domain

import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.course.domain.model.Section
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    suspend fun getCourseContents(courseId: Int): Resource<List<Section>>

    fun getCachedCourseContents(courseId: Int): Flow<List<Section>>

    suspend fun getCourseName(courseId: Int): String?
}
