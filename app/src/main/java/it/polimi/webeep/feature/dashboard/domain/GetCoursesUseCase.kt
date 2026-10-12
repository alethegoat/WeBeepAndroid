package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.core.common.ConnectivityObserver
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.cacheThenRefresh
import it.polimi.webeep.feature.dashboard.domain.model.Course
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Loads the user's courses: cache first, then network when online.
 */
class GetCoursesUseCase @Inject constructor(
    private val repository: DashboardRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    operator fun invoke(): Flow<Resource<List<Course>>> = cacheThenRefresh(
        cache = repository.getCoursesFromCache(),
        connectivity = connectivityObserver,
        refresh = repository::syncCourses,
        emitEmptyAfterError = false
    )
}
