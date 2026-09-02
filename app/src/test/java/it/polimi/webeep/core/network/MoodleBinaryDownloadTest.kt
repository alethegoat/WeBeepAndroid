package it.polimi.webeep.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MoodleBinaryDownloadTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `rejects html login pages`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("<html>login</html>")
        )

        client.newCall(Request.Builder().url(server.url("/file.pdf")).build()).execute().use { response ->
            val error = runCatching { MoodleBinaryDownload.requireBinaryBody(response) }.exceptionOrNull()
            assertTrue(error is IllegalStateException)
            assertTrue(error!!.message!!.contains("login page"))
        }
    }

    @Test
    fun `accepts binary pdf content`() {
        val payload = "%PDF-1.4 fake"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/pdf")
                .setBody(payload)
        )

        client.newCall(Request.Builder().url(server.url("/file.pdf")).build()).execute().use { response ->
            val body = MoodleBinaryDownload.requireBinaryBody(response)
            assertEquals(payload, body.string())
        }
    }
}
