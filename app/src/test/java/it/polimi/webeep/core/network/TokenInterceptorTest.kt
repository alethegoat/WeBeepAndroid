package it.polimi.webeep.core.network

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import it.polimi.webeep.core.datastore.TokenProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    private fun formPost(path: String, form: String): Request = Request.Builder()
        .url(mockWebServer.url(path))
        .post(form.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType()))
        .build()

    @Test
    fun `adds wstoken to form body and not to url on moodle rest path`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"sitename":"WeBeep"}"""))

        client.newCall(formPost("/webservice/rest/server.php", "wsfunction=core_webservice_get_site_info"))
            .execute().use { response ->
                assertTrue(response.isSuccessful)
            }

        val recorded = mockWebServer.takeRequest()
        assertNull(recorded.requestUrl?.queryParameter("wstoken"))
        assertEquals(
            "wsfunction=core_webservice_get_site_info&wstoken=test-token",
            recorded.body.readUtf8()
        )
    }

    @Test
    fun `keeps explicit wstoken already present in form body`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        client.newCall(formPost("/webservice/rest/server.php", "wstoken=explicit&wsfunction=f")).execute().close()

        val form = mockWebServer.takeRequest().body.readUtf8()
        assertEquals("wstoken=explicit&wsfunction=f", form)
    }

    @Test
    fun `does not inject wstoken into query of get requests`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        client.newCall(Request.Builder().url(mockWebServer.url("/webservice/rest/server.php")).build())
            .execute().close()

        assertNull(mockWebServer.takeRequest().requestUrl?.queryParameter("wstoken"))
    }

    @Test
    fun `does not inject token for untrusted host`() {
        val untrustedClient = OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(tokenProvider, "webeep.polimi.it"))
            .build()
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        untrustedClient.newCall(formPost("/webservice/rest/server.php", "wsfunction=f")).execute().close()

        assertFalse(mockWebServer.takeRequest().body.readUtf8().contains("wstoken"))
        verify(exactly = 0) { tokenProvider.clearSessionSync() }
    }

    @Test
    fun `clears session on 401 from moodle rest`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        client.newCall(formPost("/webservice/rest/server.php", "wsfunction=f")).execute().close()

        verify { tokenProvider.clearSessionSync() }
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

        client.newCall(formPost("/webservice/rest/server.php", "wsfunction=f")).execute().use { response ->
            assertTrue(response.isSuccessful)
        }

        verify { tokenProvider.clearSessionSync() }
    }
}
