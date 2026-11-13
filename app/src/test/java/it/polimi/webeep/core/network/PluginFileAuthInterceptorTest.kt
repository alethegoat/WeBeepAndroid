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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PluginFileAuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenProvider: TokenProvider
    private lateinit var client: OkHttpClient

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        tokenProvider = mockk(relaxed = true)
        every { tokenProvider.getTokenSync() } returns "file-token"
        client = OkHttpClient.Builder()
            .addInterceptor(PluginFileAuthInterceptor(tokenProvider, server.hostName))
            .build()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `adds token query on pluginfile path`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("pdf"))

        client.newCall(
            Request.Builder().url(server.url("/webservice/pluginfile.php/1/file.pdf")).build()
        ).execute()

        val recorded = server.takeRequest()
        assertEquals("file-token", recorded.requestUrl?.queryParameter("token"))
        verify(exactly = 0) { tokenProvider.clearSessionSync() }
    }

    @Test
    fun `does not add token on rest api path`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        client.newCall(
            Request.Builder().url(server.url("/webservice/rest/server.php")).build()
        ).execute()

        val recorded = server.takeRequest()
        assertNull(recorded.requestUrl?.queryParameter("token"))
    }

    @Test
    fun `does not duplicate existing token query`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("pdf"))

        client.newCall(
            Request.Builder()
                .url(server.url("/pluginfile.php/1/file.pdf").newBuilder().addQueryParameter("token", "existing").build())
                .build()
        ).execute()

        val recorded = server.takeRequest()
        assertEquals("existing", recorded.requestUrl?.queryParameter("token"))
        assertEquals(1, recorded.requestUrl?.queryParameterValues("token")?.size)
    }

    private fun proceededUrl(url: String): okhttp3.HttpUrl {
        val sent = io.mockk.slot<Request>()
        val chain = mockk<okhttp3.Interceptor.Chain>()
        every { chain.request() } returns Request.Builder().url(url).build()
        every { chain.proceed(capture(sent)) } returns mockk(relaxed = true)
        PluginFileAuthInterceptor(tokenProvider).intercept(chain)
        return sent.captured.url
    }

    @Test
    fun `never adds the token to a cleartext url on the trusted host`() {
        val url = proceededUrl("http://webeep.polimi.it/pluginfile.php/1/file.pdf")

        assertNull(url.queryParameter("token"))
        verify(exactly = 0) { tokenProvider.getTokenSync() }
    }

    @Test
    fun `adds the token to an https url on the trusted host`() {
        val url = proceededUrl("https://webeep.polimi.it/pluginfile.php/1/file.pdf")

        assertEquals("file-token", url.queryParameter("token"))
    }
}
