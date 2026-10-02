package it.polimi.webeep.feature.sync

import it.polimi.webeep.feature.sync.FileChangeDetector.ChangeType
import it.polimi.webeep.feature.sync.FileChangeDetector.FileChange
import it.polimi.webeep.feature.sync.FileChangeDetector.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SyncStateStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun store(file: File) = SyncStateStore({ file }, Dispatchers.Unconfined)

    @Test
    fun `state survives a new store instance`() = runTest {
        val file = File(tempFolder.root, SyncStateStore.FILE_NAME)
        val snapshot = listOf(FileEntry(10, "/", "a.pdf", 100L, 1_000L))
        val pending = listOf(CourseChanges(7, "Analisi", listOf(FileChange(10, "/", "a.pdf", ChangeType.ADDED))))

        store(file).update { it.copy(snapshots = mapOf(7 to snapshot), pending = pending) to Unit }

        val reloaded = store(file).update { it to it }
        assertEquals(mapOf(7 to snapshot), reloaded.snapshots)
        assertEquals(pending, reloaded.pending)
    }

    @Test
    fun `corrupt file falls back to an empty state`() = runTest {
        val file = File(tempFolder.root, SyncStateStore.FILE_NAME).apply { writeText("{not json") }

        assertEquals(SyncState(), store(file).update { it to it })
    }

    @Test
    fun `clear deletes the file`() = runTest {
        val file = File(tempFolder.root, SyncStateStore.FILE_NAME)
        val store = store(file)
        store.update { it.copy(snapshots = mapOf(1 to emptyList())) to Unit }

        store.clear()

        assertFalse(file.exists())
    }

    @Test
    fun `mergePending drops dismissed notifications and accumulates visible ones`() {
        val a = FileChange(10, "/", "a.pdf", ChangeType.ADDED)
        val b = FileChange(10, "/", "b.pdf", ChangeType.ADDED)
        val c = FileChange(20, "/", "c.pdf", ChangeType.UPDATED)
        val pending = listOf(CourseChanges(1, "Visible", listOf(a)), CourseChanges(2, "Dismissed", listOf(c)))
        val fresh = listOf(CourseChanges(1, "Visible", listOf(b)), CourseChanges(3, "Other", emptyList()))

        val merged = SyncStateStore.mergePending(pending, activeIds = setOf(1), fresh = fresh)

        assertEquals(listOf(CourseChanges(1, "Visible", listOf(b, a))), merged)
    }
}
