package it.polimi.webeep.feature.course.domain

import it.polimi.webeep.core.util.NetworkMonitor
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.course.domain.model.Section
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loads the course sections tree: cache first, then network when online.
 */
class GetCourseContentsUseCase @Inject constructor(
    private val repository: CourseRepository,
    private val networkMonitor: NetworkMonitor
) {
    operator fun invoke(courseId: Int): Flow<Resource<List<Section>>> = channelFlow {
        var isSyncing = true
        var lastCachedSections: List<Section>? = null
        val cacheStarted = kotlinx.coroutines.CompletableDeferred<Unit>()

        val cacheJob = launch {
            repository.getCachedCourseContents(courseId).collect { sections ->
                lastCachedSections = sections
                if (sections.isNotEmpty() || !isSyncing) {
                    send(Resource.Success(sections))
                }
                cacheStarted.complete(Unit)
            }
        }

        cacheStarted.await()

        val online = runCatching { networkMonitor.isOnline.first() }.getOrDefault(true)
        if (online) {
            val syncResult = repository.getCourseContents(courseId)
            syncResult.onFailure { throwable ->
                val isCacheEmpty = lastCachedSections?.isEmpty() ?: true
                if (isCacheEmpty) {
                    send(Resource.Error(throwable.localizedMessage ?: "content synchronization error", throwable))
                }
            }
        }

        isSyncing = false

        val isCacheEmpty = lastCachedSections?.isEmpty() ?: true
        if (isCacheEmpty) {
            send(Resource.Success(emptyList()))
        }

        cacheJob.join()
    }
}
