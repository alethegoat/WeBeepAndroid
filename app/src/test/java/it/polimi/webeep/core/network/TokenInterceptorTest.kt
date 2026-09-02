package it.polimi.webeep.core.network

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import it.polimi.webeep.core.datastore.TokenProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenProvider: TokenProvider
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        tokenProvider = mockk(relaxed = true)
        every { tokenProvider.getTokenSync() } returns "test-token"

        client = OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(tokenProvider, mockWebServer.hostName))
            .build()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `adds wstoken query parameter on moodle rest path`() {
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
    fun `does not add wstoken to pluginfile requests`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("pdf"))

        val request = Request.Builder()
            .url(mockWebServer.url("/webservice/pluginfile.php/1/file.pdf"))
            .build()

        client.newCall(request).execute()

        val recorded = mockWebServer.takeRequest()
        assertNull(recorded.requestUrl?.queryParameter("wstoken"))
        verify(exactly = 0) { tokenProvider.clearSessionSync() }
    }

    @Test
    fun `does not clear session on 401 outside moodle rest`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val request = Request.Builder()
            .url(mockWebServer.url("/webservice/pluginfile.php/1/file.pdf"))
            .build()

        client.newCall(request).execute().use { response ->
            assertFalse(response.isSuccessful)
            assertEquals(401, response.code)
        }

        verify(exactly = 0) { tokenProvider.clearSessionSync() }
    }

    @Test
    fun `clears session when moodle rest returns invalidtoken in body`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"exception":"webservice_access_exception","errorcode":"invalidtoken"}""")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/webservice/rest/server.php"))
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
        }

        verify { tokenProvider.clearSessionSync() }
    }
}
