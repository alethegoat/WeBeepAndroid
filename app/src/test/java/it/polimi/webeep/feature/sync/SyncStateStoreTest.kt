package it.polimi.webeep.feature.sync

import it.polimi.webeep.feature.sync.FileChangeDetector.ChangeType
import it.polimi.webeep.feature.sync.FileChangeDetector.FileChange
import it.polimi.webeep.feature.sync.FileChangeDetector.FileEntry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
    fun `undelivered changes and last check survive a new store instance`() = runTest {
        val file = File(tempFolder.root, SyncStateStore.FILE_NAME)
        val undelivered = listOf(CourseChanges(7, "Analisi", listOf(FileChange(10, "/", "a.pdf", ChangeType.ADDED))))

        store(file).update { it.copy(undelivered = undelivered, lastCheckAt = 1_000L) to Unit }

        val reloaded = store(file).update { it to it }
        assertEquals(undelivered, reloaded.undelivered)
        assertEquals(1_000L, reloaded.lastCheckAt)
    }

    @Test
    fun `lastCheckAt emits the new value after an update`() = runTest {
        val store = store(File(tempFolder.root, SyncStateStore.FILE_NAME))
        assertEquals(null, store.lastCheckAt.first())

        store.update { it.copy(lastCheckAt = 5_000L) to Unit }

        assertEquals(5_000L, store.lastCheckAt.first())
    }

    @Test
    fun `appendChanges merges into the same course and adds new courses`() {
        val a = FileChange(10, "/", "a.pdf", ChangeType.ADDED)
        val b = FileChange(10, "/", "b.pdf", ChangeType.UPDATED)
        val c = FileChange(20, "/", "c.pdf", ChangeType.ADDED)
        val existing = listOf(CourseChanges(1, "Uno", listOf(a)))

        val sameCourse = SyncStateStore.appendChanges(existing, CourseChanges(1, "Uno", listOf(b)))
        val otherCourse = SyncStateStore.appendChanges(existing, CourseChanges(2, "Due", listOf(c)))
        val nothing = SyncStateStore.appendChanges(existing, CourseChanges(3, "Tre", emptyList()))

        assertEquals(listOf(CourseChanges(1, "Uno", listOf(b, a))), sameCourse)
        assertEquals(listOf(CourseChanges(1, "Uno", listOf(a)), CourseChanges(2, "Due", listOf(c))), otherCourse)
        assertEquals(existing, nothing)
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
