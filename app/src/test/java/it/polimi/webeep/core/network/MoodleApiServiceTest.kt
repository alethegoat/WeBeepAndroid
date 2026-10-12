package it.polimi.webeep.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockk
import it.polimi.webeep.core.datastore.TokenProvider
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class MoodleApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: MoodleApiService
    private lateinit var tokenProvider: TokenProvider

    private val siteInfoJson = """
        {
            "sitename": "WeBeep - Politecnico di Milano",
            "username": "mariorossi",
            "firstname": "Mario",
            "lastname": "Rossi",
            "fullname": "Mario Rossi",
            "lang": "it",
            "userid": 98765,
            "siteurl": "https://webeep.polimi.it",
            "userpictureurl": "https://webeep.polimi.it/pluginfile.php/1/user/icon/f1",
            "downloadfiles": 1,
            "release": "4.2.1",
            "version": "2023042401",
            "functions": [
                {
                    "name": "core_webservice_get_site_info",
                    "version": "2023042400"
                }
            ]
        }
    """.trimIndent()

    private val enrolledCoursesJson = """
        [
            {
                "id": 5,
                "shortname": "CS101",
                "fullname": "Informatica 1",
                "displayname": "Informatica 1",
                "summary": "Primo corso di programmazione",
                "summaryformat": 1,
                "startdate": 1672531200,
                "enddate": 1704067200,
                "visible": 1,
                "progress": 75.0,
                "completed": false,
                "isfavourite": true,
                "hidden": false,
                "enrolledusercount": 150,
                "overviewfiles": [
                    {
                        "filename": "cover.jpg",
                        "filepath": "/",
                        "filesize": 10240,
                        "fileurl": "https://webeep.polimi.it/pluginfile.php/1/course/cover.jpg",
                        "mimetype": "image/jpeg"
                    }
                ],
                "category": 3
            }
        ]
    """.trimIndent()

    private val courseContentsJson = """
        [
            {
                "id": 10,
                "name": "General",
                "visible": 1,
                "summary": "Sezione introduttiva",
                "summaryformat": 1,
                "section": 0,
                "modules": [
                    {
                        "id": 101,
                        "url": "https://webeep.polimi.it/mod/resource/view.php?id=101",
                        "name": "Syllabus",
                        "instance": 1,
                        "contextid": 12,
                        "visible": 1,
                        "uservisible": true,
                        "modicon": "icon.png",
                        "modname": "resource",
                        "modplural": "Risorse",
                        "contents": [
                            {
                                "type": "file",
                                "filename": "syllabus.pdf",
                                "filepath": "/",
                                "filesize": 204800,
                                "fileurl": "https://webeep.polimi.it/pluginfile.php/1/mod_resource/content/1/syllabus.pdf",
                                "mimetype": "application/pdf"
                            }
                        ]
                    }
                ]
            }
        ]
    """.trimIndent()

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        tokenProvider = mockk(relaxed = true)
        every { tokenProvider.getTokenSync() } returns "saved_token"

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetries = 3, initialDelayMs = 1L))
            .addInterceptor(TokenInterceptor(tokenProvider, mockWebServer.hostName))
            .build()

        val httpClient = HttpClient(OkHttp) {
            engine {
                preconfigured = okHttpClient
            }
            defaultRequest {
                url(mockWebServer.url("/").toString())
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                    }
                )
            }
        }

        apiService = MoodleApiServiceImpl(httpClient)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getSiteInfo parses response correctly and hits correct path`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(siteInfoJson)
        )

        val response = apiService.getSiteInfo("mock_token")

        assertNotNull(response)
        assertEquals("WeBeep - Politecnico di Milano", response.sitename)
        assertEquals(98765, response.userid)
        assertEquals("Mario Rossi", response.fullname)
        assertEquals(1, response.downloadfiles)
        assertEquals(1, response.functions?.size)

        val recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recordedRequest)
        assertEquals("POST", recordedRequest!!.method)
        assertEquals("/webservice/rest/server.php", recordedRequest.path)
        assertNull(recordedRequest.requestUrl?.queryParameter("wstoken"))
        assertNull(recordedRequest.getHeader(MoodleApiServiceImpl.READ_ONLY_WS_HEADER))
        val form = recordedRequest.body.readUtf8()
        assertTrue(form.contains("wstoken=mock_token"))
        assertFalse(form.contains("saved_token"))
        assertTrue(form.contains("wsfunction=core_webservice_get_site_info"))
        assertTrue(form.contains("moodlewsrestformat=json"))
    }

    @Test
    fun `getEnrolledCourses parses array response correctly`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(enrolledCoursesJson)
        )

        val courses = apiService.getEnrolledCourses(wsToken = "mock_token", userId = 98765)

        assertEquals(1, courses.size)
        val course = courses.first()
        assertEquals(5, course.id)
        assertEquals("CS101", course.shortname)
        assertEquals("Informatica 1", course.fullname)
        assertEquals(75.0, course.progress!!, 0.01)
        assertTrue(course.isfavourite!!)
        assertEquals("cover.jpg", course.overviewfiles?.firstOrNull()?.filename)

        val recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        val form = recordedRequest?.body?.readUtf8() ?: ""
        assertTrue(form.contains("userid=98765"))
        assertTrue(form.contains("wsfunction=core_enrol_get_users_courses"))
    }

    @Test
    fun `getCourseContents parses nested sections and modules correctly`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(courseContentsJson)
        )

        val sections = apiService.getCourseContents(wsToken = "mock_token", courseId = 5)

        assertEquals(1, sections.size)
        val section = sections.first()
        assertEquals(10, section.id)
        assertEquals("General", section.name)
        assertEquals(1, section.modules.size)

        val module = section.modules.first()
        assertEquals(101, module.id)
        assertEquals("resource", module.modname)
        assertEquals("syllabus.pdf", module.contents?.firstOrNull()?.filename)
        assertEquals(204800L, module.contents?.firstOrNull()?.filesize)
    }

    @Test
    fun `saved token is sent in form body and never in url`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(courseContentsJson))

        apiService.getCourseContents(courseId = 5)

        val recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("POST", recordedRequest.method)
        assertFalse(recordedRequest.path!!.contains("token"))
        val form = recordedRequest.body.readUtf8()
        assertTrue(form.contains("wstoken=saved_token"))
        assertTrue(form.contains("courseid=5"))
    }

    @Test
    fun `read-only ws post is retried on 5xx`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(503))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(siteInfoJson))

        val response = apiService.getSiteInfo()

        assertEquals(98765, response.userid)
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `error envelope on http 200 throws MoodleException`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"exception":"moodle_exception","errorcode":"servicerequireslogin","message":"Login required"}""")
        )

        try {
            apiService.getSiteInfo("mock_token")
            fail("Expected MoodleException")
        } catch (e: MoodleException) {
            assertEquals("servicerequireslogin", e.errorCode)
            assertEquals("Login required", e.message)
        }
    }

    @Test
    fun `error envelope on list endpoint throws MoodleException`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"exception":"required_capability_exception","errorcode":"nopermissions","message":"No"}""")
        )

        try {
            apiService.getCourseContents(courseId = 5)
            fail("Expected MoodleException")
        } catch (e: MoodleException) {
            assertEquals("nopermissions", e.errorCode)
        }
    }

    @Test
    fun `site info with userid zero is rejected`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"sitename":"WeBeep"}"""))

        try {
            apiService.getSiteInfo("mock_token")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Invalid site info"))
        }
    }

    @Test
    fun `non json error response throws IOException`() = kotlinx.coroutines.test.runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("<html>Not found</html>"))

        try {
            apiService.getCourseContents(courseId = 5)
            fail("Expected IOException")
        } catch (e: IOException) {
            assertFalse(e is MoodleException)
        }
    }
}
