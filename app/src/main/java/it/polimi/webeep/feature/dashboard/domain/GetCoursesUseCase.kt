package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.core.util.NetworkMonitor
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.dashboard.domain.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loads the user's courses: cache first, then network when online.
 */
class GetCoursesUseCase @Inject constructor(
    private val repository: DashboardRepository,
    private val networkMonitor: NetworkMonitor
) {
    operator fun invoke(): Flow<Resource<List<Course>>> = channelFlow {
        var isSyncing = true
        var lastCachedCourses: List<Course>? = null
        val cacheStarted = kotlinx.coroutines.CompletableDeferred<Unit>()

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

        val online = runCatching { networkMonitor.isOnline.first() }.getOrDefault(true)
        if (online) {
            val syncResult = repository.syncCourses()
            syncResult.onSuccess {
                val isCacheEmpty = lastCachedCourses?.isEmpty() ?: true
                if (isCacheEmpty) {
                    send(Resource.Success(emptyList()))
                }
            }.onFailure { throwable ->
                val isCacheEmpty = lastCachedCourses?.isEmpty() ?: true
                if (isCacheEmpty) {
                    send(
                        Resource.Error(
                            throwable.localizedMessage ?: "Nessun dato locale disponibile ed errore di rete",
                            throwable
                        )
                    )
                }
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
