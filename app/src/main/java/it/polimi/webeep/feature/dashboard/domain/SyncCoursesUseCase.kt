package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.core.util.ConnectivityObserver
import it.polimi.webeep.core.util.DataError
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.core.util.toDataError
import it.polimi.webeep.feature.dashboard.domain.model.Course
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Explicit (pull-to-refresh) course sync. Fails fast when offline and
 * reports failures as [DataError] instead of raw exceptions.
 */
class SyncCoursesUseCase @Inject constructor(
    private val repository: DashboardRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    suspend operator fun invoke(): Resource<List<Course>> {
        val online = runCatching { connectivityObserver.isOnline.first() }.getOrDefault(true)
        if (!online) return Resource.Error(DataError.Network)
        return repository.syncCourses().fold(
            onSuccess = { Resource.Success(it) },
            onFailure = { Resource.Error(it.toDataError(), it) }
        )
    }
}
