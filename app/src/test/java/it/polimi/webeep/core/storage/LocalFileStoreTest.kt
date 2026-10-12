package it.polimi.webeep.core.storage

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalFileStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var directory: File

    @Before
    fun setup() {
        directory = File(tmp.root, "store")
    }

    private fun store(maxBytes: Long = 100L) = LocalFileStore(directory, maxBytes).also { it.ensureDirectory() }

    private fun File.write(size: Int, modified: Long) = apply {
        writeBytes(ByteArray(size))
        setLastModified(modified)
    }

    @Test
    fun `containedFile resolves plain names inside the directory`() {
        val file = store().containedFile("a.pdf")

        assertNotNull(file)
        assertEquals(directory.canonicalPath, file!!.parentFile!!.canonicalPath)
    }

    @Test
    fun `containedFile rejects names that escape the directory`() {
        val store = store()

        assertNull(store.containedFile("../evil.pdf"))
        assertNull(store.containedFile("../../evil.pdf"))
        assertNull(store.containedFile("../store-sibling/x"))
    }

    @Test
    fun `partFileFor sits next to the target`() {
        val store = store()
        val target = File(directory, "a.pdf")

        assertEquals(File(directory, "a.pdf.part"), store.partFileFor(target))
    }

    @Test
    fun `commit replaces an existing target and removes the part`() {
        val store = store()
        val target = File(directory, "a.txt").apply { writeText("old") }
        val part = store.partFileFor(target).apply { writeText("new") }

        store.commit(part, target)

        assertEquals("new", target.readText())
        assertFalse(part.exists())
    }

    @Test
    fun `commit creates the target when there was none`() {
        val store = store()
        val target = File(directory, "fresh.txt")
        val part = store.partFileFor(target).apply { writeText("data") }

        store.commit(part, target)

        assertEquals("data", target.readText())
    }

    @Test
    fun `evict drops the oldest files until the budget fits`() {
        val store = store(maxBytes = 100L)
        val oldest = File(directory, "oldest").write(60, modified = 1_000L)
        val middle = File(directory, "middle").write(60, modified = 2_000L)
        val newest = File(directory, "newest").write(60, modified = 3_000L)

        store.evict()

        assertFalse(oldest.exists())
        assertFalse(middle.exists())
        assertTrue(newest.exists())
    }

    @Test
    fun `evict never removes the kept file even when it is the oldest`() {
        val store = store(maxBytes = 100L)
        val kept = File(directory, "kept").write(60, modified = 1_000L)
        val other = File(directory, "other").write(60, modified = 2_000L)

        store.evict(keep = kept)

        assertTrue(kept.exists())
        assertFalse(other.exists())
    }

    @Test
    fun `evict leaves unfinished parts alone`() {
        val store = store(maxBytes = 10L)
        val part = File(directory, "x.pdf.part").write(60, modified = 1_000L)
        val done = File(directory, "x.pdf").write(60, modified = 2_000L)

        store.evict()

        assertTrue(part.exists())
        assertFalse(done.exists())
    }

    @Test
    fun `evict does nothing within budget`() {
        val store = store(maxBytes = 200L)
        val a = File(directory, "a").write(60, modified = 1_000L)
        val b = File(directory, "b").write(60, modified = 2_000L)

        store.evict()

        assertTrue(a.exists() && b.exists())
    }
}
