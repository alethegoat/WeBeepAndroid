package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.core.util.ConnectivityObserver
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.core.util.runCatchingCancellable
import it.polimi.webeep.feature.dashboard.domain.model.Course
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Loads the user's courses: cache first, then network when online.
 */
class GetCoursesUseCase @Inject constructor(
    private val repository: DashboardRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    operator fun invoke(): Flow<Resource<List<Course>>> = channelFlow {
        var isSyncing = true
        var lastCachedCourses: List<Course>? = null
        val cacheStarted = CompletableDeferred<Unit>()

        val cacheJob = launch {
            repository.getCoursesFromCache().collect { courses ->
                lastCachedCourses = courses
                if (courses.isNotEmpty() || !isSyncing) {
                    send(Resource.Success(courses))
                }
                cacheStarted.complete(Unit)
            }
        }

        cacheStarted.await()

        val online = runCatchingCancellable { connectivityObserver.isOnline.first() }.getOrDefault(true)
        if (online) {
            val syncResult = repository.syncCourses()
            val isCacheEmpty = lastCachedCourses?.isEmpty() ?: true
            if (isCacheEmpty) {
                // An empty cache has nothing to show: report the failure, or an empty list on success.
                send(if (syncResult is Resource.Error) syncResult else Resource.Success(emptyList()))
            }
        } else {
            val isCacheEmpty = lastCachedCourses?.isEmpty() ?: true
            if (isCacheEmpty) {
                send(Resource.Success(emptyList()))
            }
        }

        isSyncing = false

        cacheJob.join()
    }
}
