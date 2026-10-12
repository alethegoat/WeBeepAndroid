package it.polimi.webeep.feature.sync.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.feature.sync.domain.SyncStateRepository
import it.polimi.webeep.feature.sync.domain.model.SyncState
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
import kotlinx.serialization.json.Json

/**
 * Persists [SyncState] in noBackupFilesDir. Updates are serialized so the periodic and the
 * immediate sync never interleave a read-modify-write.
 */
@Singleton
class SyncStateStore internal constructor(private val fileProvider: () -> File, private val ioDispatcher: CoroutineDispatcher) :
    SyncStateRepository {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) : this({ File(context.noBackupFilesDir, FILE_NAME) }, ioDispatcher)

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val version = MutableStateFlow(0)

    override val lastCheckAt: Flow<Long?> = version
        .map { update { state -> state to state.lastCheckAt } }
        .distinctUntilChanged()

    override suspend fun <T> update(block: (SyncState) -> Pair<SyncState, T>): T {
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

    override suspend fun clear() {
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
    }
}
