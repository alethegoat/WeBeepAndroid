package it.polimi.webeep.feature.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.getOrThrow
import it.polimi.webeep.core.common.resourceOf
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.feature.sync.domain.CourseSyncOrder
import it.polimi.webeep.feature.sync.domain.FileChangeDetector
import it.polimi.webeep.feature.sync.domain.PendingChanges
import it.polimi.webeep.feature.sync.domain.SyncNotifications
import it.polimi.webeep.feature.sync.domain.SyncStateRepository
import it.polimi.webeep.feature.sync.domain.model.CourseChanges
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

// The FQN is persisted in WorkManager's queue: moving or renaming this class breaks jobs already enqueued on installed apps.
/**
 * Background sync: refreshes the course list and every course's content, diffs it against the stored snapshot and
 * records the changes for [SyncNotifications] to deliver.
 *
 * Stops with a failure when logged out; retries only when nothing could be synced, since a partial run is picked up by the next one.
 */
@HiltWorker
class CourseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val courseRepository: CourseRepository,
    private val dashboardRepository: DashboardRepository,
    private val tokenProvider: TokenProvider,
    private val syncState: SyncStateRepository,
    private val notifications: SyncNotifications
) : CoroutineWorker(appContext, workerParams) {

    /** Outcome of fetching every course: how many succeeded and whether any failed. */
    private class RunStats {
        val synced = AtomicInteger(0)
        val hasErrors = AtomicBoolean(false)
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker started")
        tokenProvider.getToken() ?: run {
            Log.d(TAG, "No token. Stopping worker.")
            return Result.failure()
        }

        return when (val outcome = resourceOf { syncAllCourses() }) {
            is Resource.Success -> outcome.data
            is Resource.Error -> {
                Log.e(TAG, "Fatal worker error type=${outcome.throwable?.let { it::class.simpleName }}")
                Result.retry()
            }
        }
    }

    // Unexpected failures bubble up to doWork, which turns them into a retry.
    private suspend fun syncAllCourses(): Result {
        // Changes found by a run that was stopped before notifying.
        notifications.deliverPending()

        val courses = refreshCourseList()
        if (courses.isEmpty()) {
            Log.d(TAG, "No local courses. Stopping worker.")
            return Result.success()
        }

        val stats = syncCourses(courses)
        recordRun(courses, stats)
        notifications.deliverPending()

        // Avoid retry loops: partial success is enough since the next
        // periodic run will pick up the rest. Retry only when nothing synced.
        return if (stats.hasErrors.get() && stats.synced.get() == 0) Result.retry() else Result.success()
    }

    private suspend fun refreshCourseList(): List<Course> {
        dashboardRepository.syncCourses()
        val cached = dashboardRepository.getCoursesFromCache().firstOrNull().orEmpty()
        return cached.sortedWith(CourseSyncOrder.currentFirst(System.currentTimeMillis() / MILLIS_PER_SECOND))
    }

    private suspend fun syncCourses(courses: List<Course>): RunStats {
        val stats = RunStats()
        val semaphore = Semaphore(MAX_CONCURRENT_COURSE_SYNCS)
        supervisorScope {
            courses.map { course ->
                async { semaphore.withPermit { syncCourse(course, stats) } }
            }.awaitAll()
        }
        return stats
    }

    private suspend fun syncCourse(course: Course, stats: RunStats) {
        val synced = resourceOf {
            val cached = courseRepository.getCachedCourseContents(course.id).first()
            val current = courseRepository.getCourseContents(course.id).getOrThrow()
            stats.synced.incrementAndGet()

            recordSnapshot(course.id, course.fullname, cached, current)
        }
        if (synced is Resource.Error) {
            Log.e(TAG, "Sync error course=${course.id} type=${synced.throwable?.let { it::class.simpleName }}")
            stats.hasErrors.set(true)
        }
    }

    /** Forgets snapshots of courses the user left and stamps the check time if anything was fetched. */
    private suspend fun recordRun(courses: List<Course>, stats: RunStats) {
        val enrolledIds = courses.map { it.id }.toSet()
        val checkedAt = System.currentTimeMillis()
        syncState.update { state ->
            state.copy(
                snapshots = state.snapshots.filterKeys { it in enrolledIds },
                lastCheckAt = if (stats.synced.get() > 0) checkedAt else state.lastCheckAt
            ) to Unit
        }
    }

    /**
     * Diffs against the snapshot of the previous background sync and stores the new one, together
     * with the changes to notify. The content cache is only a fallback for courses synced before
     * snapshots existed.
     */
    private suspend fun recordSnapshot(courseId: Int, courseName: String, cached: List<Section>, current: List<Section>) {
        val snapshot = FileChangeDetector.snapshot(current)
        syncState.update { state ->
            val baseline = state.snapshots[courseId]
                ?: cached.takeIf { it.isNotEmpty() }?.let(FileChangeDetector::snapshot)
            val changes = CourseChanges(courseId, courseName, FileChangeDetector.detect(baseline, snapshot))
            state.copy(
                snapshots = state.snapshots + (courseId to snapshot),
                undelivered = PendingChanges.append(state.undelivered, changes)
            ) to Unit
        }
    }

    private companion object {
        const val TAG = "CourseSyncWorker"
        const val MAX_CONCURRENT_COURSE_SYNCS = 2
        const val MILLIS_PER_SECOND = 1000L
    }
}
