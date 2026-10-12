package it.polimi.webeep.core.network

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RetryInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    private fun client(vararg extra: Interceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .addInterceptor(RetryInterceptor(maxRetries = 3, initialDelayMs = 1L))
        extra.forEach { builder.addInterceptor(it) }
        return builder.build()
    }

    private fun get(): Request = Request.Builder().url(mockWebServer.url("/test")).build()

    private fun post(readOnly: Boolean): Request {
        val builder = Request.Builder()
            .url(mockWebServer.url("/webservice/rest/server.php"))
            .post("wsfunction=f".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
        if (readOnly) builder.header(MoodleApiServiceImpl.READ_ONLY_WS_HEADER, "1")
        return builder.build()
    }

    @Test
    fun `throws IOException instead of returning closed response after 503 and disconnects`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(503).setHeader("Connection", "close"))
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertThrows(IOException::class.java) {
            client().newCall(get()).execute().close()
        }
    }

    @Test
    fun `returns last open 5xx response after max retries`() {
        repeat(3) { mockWebServer.enqueue(MockResponse().setResponseCode(503).setBody("busy")) }

        client().newCall(get()).execute().use { response ->
            assertEquals(503, response.code)
            assertEquals("busy", response.body!!.string())
        }
        assertEquals(3, mockWebServer.requestCount)
    }

    @Test
    fun `succeeds after transient 5xx`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        client().newCall(get()).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("ok", response.body!!.string())
        }
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `does not retry unmarked post`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(503))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        client().newCall(post(readOnly = false)).execute().use { response ->
            assertEquals(503, response.code)
        }
        assertEquals(1, mockWebServer.requestCount)
    }

    @Test
    fun `retries marked read-only post and strips internal header`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(503))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        client().newCall(post(readOnly = true)).execute().use { response ->
            assertEquals(200, response.code)
        }
        assertEquals(2, mockWebServer.requestCount)
        repeat(2) {
            val recorded = mockWebServer.takeRequest()
            assertNull(recorded.getHeader(MoodleApiServiceImpl.READ_ONLY_WS_HEADER))
            assertEquals("wsfunction=f", recorded.body.readUtf8())
        }
    }

    @Test
    fun `stops retrying when call is canceled`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(503))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val cancelAfterFirstAttempt = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            chain.call().cancel()
            response
        }

        assertThrows(IOException::class.java) {
            client(cancelAfterFirstAttempt).newCall(get()).execute().close()
        }
        assertEquals(1, mockWebServer.requestCount)
    }
}
