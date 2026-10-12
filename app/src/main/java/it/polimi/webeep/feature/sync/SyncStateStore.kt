package it.polimi.webeep.feature.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.feature.sync.FileChangeDetector.FileChange
import it.polimi.webeep.feature.sync.FileChangeDetector.FileEntry
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CourseChanges(val courseId: Int, val courseName: String, val changes: List<FileChange>)

/**
 * @property snapshots files known to the background sync, per course. Kept apart from the
 * content cache, which the course screen also refreshes: files first seen in the app must
 * still be notified.
 * @property undelivered changes detected but not notified yet. Saved together with the snapshot
 * that hides them from the next diff, so an interrupted sync (network lost, worker stopped,
 * notifications off) delivers them later instead of losing them.
 * @property pending changes shown in notifications the user has not dismissed yet.
 * @property lastCheckAt when the background sync last fetched at least one course.
 */
@Serializable
data class SyncState(
    val snapshots: Map<Int, List<FileEntry>> = emptyMap(),
    val undelivered: List<CourseChanges> = emptyList(),
    val pending: List<CourseChanges> = emptyList(),
    val lastCheckAt: Long? = null
)

/**
 * Persists [SyncState] in noBackupFilesDir. Updates are serialized so the periodic and the
 * immediate sync never interleave a read-modify-write.
 */
@Singleton
class SyncStateStore internal constructor(private val fileProvider: () -> File, private val ioDispatcher: CoroutineDispatcher) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) : this({ File(context.noBackupFilesDir, FILE_NAME) }, ioDispatcher)

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val version = MutableStateFlow(0)

    /** Emits again whenever the background sync completes a check. */
    val lastCheckAt: Flow<Long?> = version
        .map { update { state -> state to state.lastCheckAt } }
        .distinctUntilChanged()

    suspend fun <T> update(block: (SyncState) -> Pair<SyncState, T>): T {
        val (changed, result) = mutex.withLock {
            withContext(ioDispatcher) {
                val current = read()
                val (updated, result) = block(current)
                if (updated != current) write(updated)
                (updated != current) to result
            }
        }
        if (changed) version.update { it + 1 }
        return result
    }

    suspend fun clear() {
        mutex.withLock {
            withContext(ioDispatcher) { fileProvider().delete() }
        }
        version.update { it + 1 }
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

        /** Adds [fresh] to a list of per-course changes, merging with what the course already has. */
        fun appendChanges(existing: List<CourseChanges>, fresh: CourseChanges): List<CourseChanges> {
            if (fresh.changes.isEmpty()) return existing
            val old = existing.firstOrNull { it.courseId == fresh.courseId } ?: return existing + fresh
            val merged = fresh.copy(changes = FileChangeDetector.mergeChanges(old.changes, fresh.changes))
            return existing.map { if (it.courseId == fresh.courseId) merged else it }
        }

        /**
         * Keeps pending changes only for notifications still on screen, then adds [fresh] on top.
         */
        fun mergePending(pending: List<CourseChanges>, activeIds: Set<Int>, fresh: List<CourseChanges>): List<CourseChanges> =
            fresh.fold(pending.filter { it.courseId in activeIds }, ::appendChanges)
    }
}
