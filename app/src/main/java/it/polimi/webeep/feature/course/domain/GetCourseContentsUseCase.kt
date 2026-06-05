package it.polimi.webeep.feature.course.domain

import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.course.domain.model.Section
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * use case to load the course sections tree.
 * combines local cache and remote sync, applying read criteria.
 */
class GetCourseContentsUseCase @Inject constructor(
    private val repository: CourseRepository
) {
    operator fun invoke(courseId: Int): Flow<Resource<List<Section>>> = channelFlow {
        send(Resource.Loading)

        var isSyncing = true
        var lastCachedSections: List<Section>? = null
        val cacheStarted = kotlinx.coroutines.CompletableDeferred<Unit>()

        // collect local data for immediate feedback
        val cacheJob = launch {
            repository.getCachedCourseContents(courseId).collect { sections ->
                lastCachedSections = sections
                // if the cache is not empty or if we have finished syncing,
                // emit the success state.
                if (sections.isNotEmpty() || !isSyncing) {
                    send(Resource.Success(sections))
                }
                cacheStarted.complete(Unit)
            }
        }

        // wait for the cache to emit the initial value before starting the sync
        cacheStarted.await()

        // sync with remote apis
        val syncResult = repository.getCourseContents(courseId)
        isSyncing = false

        syncResult.onFailure { throwable ->
            val isCacheEmpty = lastCachedSections?.isEmpty() ?: true
            if (isCacheEmpty) {
                send(Resource.Error(throwable.localizedMessage ?: "content synchronization error", throwable))
            }
        }

        // if synchronization has finished and the cache is empty, show the empty state
        val isCacheEmpty = lastCachedSections?.isEmpty() ?: true
        if (isCacheEmpty) {
            send(Resource.Success(emptyList()))
        }

        cacheJob.join()
    }
}
