package it.polimi.webeep.feature.fileviewer

import android.app.Application
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import it.polimi.webeep.R
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.network.MoodleException
import it.polimi.webeep.feature.fileviewer.data.FileContentLoader
import it.polimi.webeep.feature.fileviewer.data.FileContentState
import it.polimi.webeep.feature.fileviewer.data.ViewerError
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileContentLoaderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var loader: FileContentLoader

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val tokenProvider = mockk<TokenProvider>()
        coEvery { tokenProvider.getToken() } returns "token"
        val application = mockk<Application>()
        val files = tmp.newFolder("files")
        val cache = tmp.newFolder("cache")
        every { application.filesDir } returns files
        every { application.cacheDir } returns cache
        loader = FileContentLoader(tokenProvider, OkHttpClient(), application, UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `emits growing progress then the parsed content`() = runTest {
        val text = "x".repeat(200 * 1024)
        server.enqueue(MockResponse().setHeader("Content-Type", "text/plain").setBody(text))

        val states = loader.load(file("notes.txt", "text/plain")).toList()

        val progress = states.filterIsInstance<FileContentState.Downloading>()
        assertTrue("expected several progress updates", progress.size > 3)
        assertEquals(text.length.toLong(), progress.last().bytesRead)
        assertEquals(1f, progress.last().progress)
        assertTrue(progress.zipWithNext().all { (a, b) -> b.bytesRead >= a.bytesRead })
        val code = states.last() as FileContentState.Code
        assertEquals(text, code.content)
        assertTrue(code.file.parentFile!!.listFiles()!!.none { it.name.endsWith(".part") })
    }

    @Test
    fun `second load reuses the cached copy without network`() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/zip").setBody(Buffer().write(ByteArray(1024))))
        val file = file("archive.zip", "application/zip")

        val first = loader.load(file).toList().last() as FileContentState.External
        val second = loader.load(file).toList().last() as FileContentState.External

        assertEquals(first.file, second.file)
        assertEquals("application/zip", second.mimeType)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `http errors become an error state`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val last = loader.load(file("notes.txt", "text/plain")).toList().last()

        assertTrue(last is FileContentState.Error)
        assertEquals(R.string.error_cannot_open_file, (last as FileContentState.Error).messageRes)
    }

    @Test
    fun `missing session becomes the session expired message`() = runTest {
        val tokenProvider = mockk<TokenProvider>()
        coEvery { tokenProvider.getToken() } returns null
        val application = mockk<Application>()
        every { application.filesDir } returns tmp.newFolder("files2")
        every { application.cacheDir } returns tmp.newFolder("cache2")
        val noSession = FileContentLoader(tokenProvider, OkHttpClient(), application, UnconfinedTestDispatcher())

        val last = noSession.load(file("notes.txt", "text/plain")).toList().last()

        assertEquals(FileContentState.Error(ViewerError.SessionExpired), last)
        assertEquals(R.string.error_session_expired, (last as FileContentState.Error).messageRes)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `expired token reported by moodle becomes the session expired message`() = runTest {
        val tokenProvider = mockk<TokenProvider>()
        coEvery { tokenProvider.getToken() } throws MoodleException("invalidtoken", "Invalid token")
        val application = mockk<Application>()
        every { application.filesDir } returns tmp.newFolder("files3")
        every { application.cacheDir } returns tmp.newFolder("cache3")
        val expired = FileContentLoader(tokenProvider, OkHttpClient(), application, UnconfinedTestDispatcher())

        val last = expired.load(file("notes.txt", "text/plain")).toList().last()

        assertEquals(FileContentState.Error(ViewerError.SessionExpired), last)
    }

    @Test
    fun `network failures use the viewer download message`() = runTest {
        server.shutdown()

        val last = loader.load(file("notes.txt", "text/plain")).toList().last()

        assertEquals(R.string.file_viewer_error_network, (last as FileContentState.Error).messageRes)
    }

    private fun file(name: String, mime: String) = ViewerFile(
        url = server.url("/pluginfile.php/1/$name").toString(),
        title = name,
        mimeType = mime
    )
}
