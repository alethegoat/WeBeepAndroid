package it.polimi.webeep.core.network

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import it.polimi.webeep.core.datastore.TokenManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenManager: TokenManager
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        tokenManager = mockk(relaxed = true)
        every { tokenManager.getTokenSync() } returns "test-token"

        client = OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(tokenManager))
            .build()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `adds wstoken query parameter when token is available`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"sitename":"WeBeep"}"""))

        val request = Request.Builder()
            .url(mockWebServer.url("/webservice/rest/server.php"))
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
        }

        val recorded = mockWebServer.takeRequest()
        assertEquals("test-token", recorded.requestUrl?.queryParameter("wstoken"))
    }

    @Test
    fun `clears session on http 401`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        client.newCall(request).execute().use { response ->
            assertFalse(response.isSuccessful)
            assertEquals(401, response.code)
        }

        verify { tokenManager.clearSessionSync() }
    }

    @Test
    fun `clears session when moodle returns invalidtoken in body`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"exception":"webservice_access_exception","errorcode":"invalidtoken"}""")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
        }

        verify { tokenManager.clearSessionSync() }
    }
}
