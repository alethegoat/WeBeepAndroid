package it.polimi.webeep.feature.course.domain

import it.polimi.webeep.core.common.ConnectivityObserver
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.cacheThenRefresh
import it.polimi.webeep.feature.course.domain.model.Section
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Loads the course sections tree: cache first, then network when online.
 */
class GetCourseContentsUseCase @Inject constructor(
    private val repository: CourseRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    operator fun invoke(courseId: Int): Flow<Resource<List<Section>>> = cacheThenRefresh(
        cache = repository.getCachedCourseContents(courseId),
        connectivity = connectivityObserver,
        refresh = { repository.getCourseContents(courseId) },
        emitEmptyAfterError = true
    )
}
