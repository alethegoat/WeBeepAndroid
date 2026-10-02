package it.polimi.webeep.feature.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@HiltWorker
class CourseSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val courseDao: CourseDao,
    private val courseRepository: CourseRepository,
    private val dashboardRepository: DashboardRepository,
    private val tokenProvider: TokenProvider,
    private val syncStateStore: SyncStateStore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("CourseSyncWorker", "Worker started")
        tokenProvider.getToken() ?: run {
            Log.d("CourseSyncWorker", "No token. Stopping worker.")
            return Result.failure()
        }

        try {
            dashboardRepository.syncCourses()

            val courses = courseDao.getAllCourses().firstOrNull()
            if (courses.isNullOrEmpty()) {
                Log.d("CourseSyncWorker", "No local courses. Stopping worker.")
                return Result.success()
            }

            val courseChanges = mutableListOf<CourseChanges>()
            val hasErrors = AtomicBoolean(false)
            val syncedCourses = AtomicInteger(0)
            val courseSyncSemaphore = Semaphore(MAX_CONCURRENT_COURSE_SYNCS)

            supervisorScope {
                val jobs = courses.map { course ->
                    async {
                        courseSyncSemaphore.withPermit {
                            try {
                                val cached = courseRepository.getCachedCourseContents(course.id).first()
                                val current = courseRepository.getCourseContents(course.id).getOrThrow()
                                syncedCourses.incrementAndGet()

                                val changes = recordSnapshot(course.id, cached, current)
                                if (changes.isNotEmpty()) {
                                    synchronized(courseChanges) {
                                        courseChanges.add(CourseChanges(course.id, course.fullname, changes))
                                    }
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                Log.e("CourseSyncWorker", "Sync error course=${course.id} type=${e::class.simpleName}")
                                hasErrors.set(true)
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }

            val enrolledIds = courses.map { it.id }.toSet()
            syncStateStore.update { state ->
                state.copy(snapshots = state.snapshots.filterKeys { it in enrolledIds }) to Unit
            }
            SyncNotifier.notify(appContext, syncStateStore, courseChanges.sortedBy { it.courseName })

            // Avoid retry loops: partial success is enough since the next
            // periodic run will pick up the rest. Retry only when nothing synced.
            return if (hasErrors.get() && syncedCourses.get() == 0) Result.retry() else Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("CourseSyncWorker", "Fatal worker error type=${e::class.simpleName}")
            return Result.retry()
        }
    }

    /**
     * Diffs against the snapshot of the previous background sync and stores the new one.
     * The content cache is only a fallback for courses synced before snapshots existed.
     */
    private suspend fun recordSnapshot(
        courseId: Int,
        cached: List<Section>,
        current: List<Section>
    ): List<FileChangeDetector.FileChange> {
        val snapshot = FileChangeDetector.snapshot(current)
        return syncStateStore.update { state ->
            val baseline = state.snapshots[courseId]
                ?: cached.takeIf { it.isNotEmpty() }?.let(FileChangeDetector::snapshot)
            val changes = FileChangeDetector.detect(baseline, snapshot)
            state.copy(snapshots = state.snapshots + (courseId to snapshot)) to changes
        }
    }

    companion object {
        const val EXTRA_COURSE_ID = "EXTRA_COURSE_ID"
        private const val MAX_CONCURRENT_COURSE_SYNCS = 2
    }
}
