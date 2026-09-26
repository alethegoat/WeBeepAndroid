package it.polimi.webeep.core.network

import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NoStoreAuthCacheInterceptorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var cache: Cache
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        cache = Cache(tempFolder.newFolder("http_cache"), 1024 * 1024L)
        client = OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor(NoStoreAuthCacheInterceptor())
            .build()
    }

    @After
    fun teardown() {
        cache.close()
        mockWebServer.shutdown()
    }

    private fun cacheableResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Cache-Control", "public, max-age=3600")
        .setBody(body)

    private fun fetch(path: String): String {
        val request = Request.Builder().url(mockWebServer.url(path)).build()
        return client.newCall(request).execute().use { it.body!!.string() }
    }

    @Test
    fun `response with token query is never written to cache`() {
        mockWebServer.enqueue(cacheableResponse("first"))
        mockWebServer.enqueue(cacheableResponse("second"))
        val path = "/webservice/pluginfile.php/1/file.pdf?token=secret"

        assertEquals("first", fetch(path))
        assertEquals("second", fetch(path))

        assertEquals(2, mockWebServer.requestCount)
        assertFalse(cache.urls().asSequence().any { it.contains("token=") })
    }

    @Test
    fun `response without auth query stays cacheable`() {
        mockWebServer.enqueue(cacheableResponse("cached"))
        val path = "/public/image.png"

        assertEquals("cached", fetch(path))
        assertEquals("cached", fetch(path))

        assertEquals(1, mockWebServer.requestCount)
    }
}
