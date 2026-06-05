package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.dashboard.domain.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * use case to load the user's courses.
 * immediately emits the cache for instant loading and then tries to update it via network sync.
 */
class GetCoursesUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    operator fun invoke(): Flow<Resource<List<Course>>> = channelFlow {
        send(Resource.Loading)

        var isSyncing = true
        var lastCachedCourses: List<Course>? = null
        val cacheStarted = kotlinx.coroutines.CompletableDeferred<Unit>()

        // observes the local cache continuously
        val cacheJob = launch {
            repository.getCoursesFromCache().collect { courses ->
                lastCachedCourses = courses
                // if the cache is not empty or if the synchronization is finished,
                // we emit the success state to avoid the "no courses" flash.
                if (courses.isNotEmpty() || !isSyncing) {
                    send(Resource.Success(courses))
                }
                cacheStarted.complete(Unit)
            }
        }

        // waits for the cache to emit the initial value before starting the sync
        cacheStarted.await()

        // attempts remote synchronization
        val syncResult = repository.syncCourses()
        isSyncing = false

        syncResult.onSuccess {
            val isCacheEmpty = lastCachedCourses?.isEmpty() ?: true
            if (isCacheEmpty) {
                send(Resource.Success(emptyList()))
            }
        }.onFailure { throwable ->
            val isCacheEmpty = lastCachedCourses?.isEmpty() ?: true
            if (isCacheEmpty) {
                send(Resource.Error(throwable.localizedMessage ?: "Nessun dato locale disponibile ed errore di rete", throwable))
            }
        }

        // keeps the flow active
        cacheJob.join()
    }
}
