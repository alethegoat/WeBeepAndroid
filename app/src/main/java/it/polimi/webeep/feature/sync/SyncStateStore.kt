package it.polimi.webeep.feature.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.feature.sync.FileChangeDetector.FileChange
import it.polimi.webeep.feature.sync.FileChangeDetector.FileEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CourseChanges(val courseId: Int, val courseName: String, val changes: List<FileChange>)

/**
 * @property snapshots files known to the background sync, per course. Kept apart from the
 * content cache, which the course screen also refreshes: files first seen in the app must
 * still be notified.
 * @property pending changes shown in notifications the user has not dismissed yet.
 */
@Serializable
data class SyncState(
    val snapshots: Map<Int, List<FileEntry>> = emptyMap(),
    val pending: List<CourseChanges> = emptyList()
)

/**
 * Persists [SyncState] in noBackupFilesDir. Updates are serialized so the periodic and the
 * immediate sync never interleave a read-modify-write.
 */
@Singleton
class SyncStateStore internal constructor(
    private val fileProvider: () -> File,
    private val ioDispatcher: CoroutineDispatcher
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) : this({ File(context.noBackupFilesDir, FILE_NAME) }, ioDispatcher)

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun <T> update(block: (SyncState) -> Pair<SyncState, T>): T = mutex.withLock {
        withContext(ioDispatcher) {
            val current = read()
            val (updated, result) = block(current)
            if (updated != current) write(updated)
            result
        }
    }

    suspend fun clear() {
        mutex.withLock {
            withContext(ioDispatcher) { fileProvider().delete() }
        }
    }

    private fun read(): SyncState {
        val file = fileProvider()
        if (!file.exists()) return SyncState()
        // A corrupt file only costs one silent baseline sync.
        return runCatching { json.decodeFromString(SyncState.serializer(), file.readText()) }
            .getOrDefault(SyncState())
    }

    private fun write(state: SyncState) {
        val file = fileProvider()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(json.encodeToString(SyncState.serializer(), state))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    companion object {
        const val FILE_NAME = "sync_state.json"

        /**
         * Keeps pending changes only for notifications still on screen, then adds [fresh] on top.
         */
        fun mergePending(pending: List<CourseChanges>, activeIds: Set<Int>, fresh: List<CourseChanges>): List<CourseChanges> {
            val byCourse = LinkedHashMap<Int, CourseChanges>()
            pending.filter { it.courseId in activeIds }.forEach { byCourse[it.courseId] = it }
            fresh.filter { it.changes.isNotEmpty() }.forEach { course ->
                val old = byCourse[course.courseId]
                byCourse[course.courseId] = if (old == null) {
                    course
                } else {
                    course.copy(changes = FileChangeDetector.mergeChanges(old.changes, course.changes))
                }
            }
            return byCourse.values.toList()
        }
    }
}
