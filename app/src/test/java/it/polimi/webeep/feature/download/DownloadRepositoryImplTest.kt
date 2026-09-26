package it.polimi.webeep.feature.download

import android.app.Application
import it.polimi.webeep.feature.download.data.DownloadRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Application
    private val client = OkHttpClient()

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        context = RuntimeEnvironment.getApplication()
        File(context.filesDir, "downloads").deleteRecursively()
        File(context.filesDir, "downloads_internal_migrated").delete()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    private fun pdfResponse(body: String) = MockResponse()
        .setHeader("Content-Type", "application/pdf")
        .setBody(body)

    @Test
    fun `download is stored in internal filesDir downloads`() = runBlocking {
        server.enqueue(pdfResponse("pdf-bytes"))
        val repository = DownloadRepositoryImpl(context, client, Dispatchers.IO)

        val id = repository.downloadFile(server.url("/file.pdf").toString(), null, "lesson.pdf", "lesson.pdf", "application/pdf")
        val result = withTimeout(TIMEOUT_MS) { repository.getDownloadProgress(id).last() }

        assertEquals(100, result)
        val stored = File(File(context.filesDir, "downloads"), "lesson.pdf")
        assertTrue(stored.exists())
        assertEquals("pdf-bytes", stored.readText())
        assertTrue(repository.isFileDownloaded("lesson.pdf"))
    }

    @Test
    fun `path traversal names stay inside the downloads directory`() = runBlocking {
        server.enqueue(pdfResponse("evil"))
        val repository = DownloadRepositoryImpl(context, client, Dispatchers.IO)
        val downloadsDir = File(context.filesDir, "downloads")

        val id = repository.downloadFile(server.url("/file.pdf").toString(), null, "../../evil.pdf", "evil.pdf", null)
        val result = withTimeout(TIMEOUT_MS) { repository.getDownloadProgress(id).last() }

        assertEquals(100, result)
        assertFalse(File(context.filesDir, "evil.pdf").exists())
        assertFalse(File(context.filesDir.parentFile, "evil.pdf").exists())
        val stored = downloadsDir.listFiles().orEmpty().filter { it.isFile }
        assertEquals(1, stored.size)
        assertEquals(downloadsDir.canonicalPath, stored.single().parentFile!!.canonicalPath)
    }

    @Test
    fun `terminal progress is replayed to a second collector`() = runBlocking {
        server.enqueue(pdfResponse("data"))
        val repository = DownloadRepositoryImpl(context, client, Dispatchers.IO)

        val id = repository.downloadFile(server.url("/file.pdf").toString(), null, "twice.pdf", "twice.pdf", null)
        withTimeout(TIMEOUT_MS) { repository.getDownloadProgress(id).last() }

        assertEquals(listOf(100), repository.getDownloadProgress(id).toList())
    }

    @Test
    fun `failed download reports -1`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = DownloadRepositoryImpl(context, client, Dispatchers.IO)

        val id = repository.downloadFile(server.url("/file.pdf").toString(), null, "broken.pdf", "broken.pdf", null)
        val result = withTimeout(TIMEOUT_MS) { repository.getDownloadProgress(id).last() }

        assertEquals(-1, result)
        assertFalse(repository.isFileDownloaded("broken.pdf"))
    }

    @Test
    fun `cancel before the job starts never reaches the network`() {
        server.enqueue(pdfResponse("never"))
        val dispatcher = StandardTestDispatcher()
        val repository = DownloadRepositoryImpl(context, client, dispatcher)

        val id = repository.downloadFile(server.url("/file.pdf").toString(), null, "cancelled.pdf", "cancelled.pdf", null)
        repository.cancelDownload(id)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, server.requestCount)
        assertFalse(repository.isFileDownloaded("cancelled.pdf"))
    }

    @Test
    fun `legacy external downloads are migrated to internal storage`() {
        val legacyDir = File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }
        File(legacyDir, "old.pdf").writeText("legacy")
        val dispatcher = StandardTestDispatcher()
        val repository = DownloadRepositoryImpl(context, client, dispatcher)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("legacy", File(File(context.filesDir, "downloads"), "old.pdf").readText())
        assertFalse(legacyDir.exists())
        assertTrue(repository.isFileDownloaded("old.pdf"))
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
