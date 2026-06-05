package it.polimi.webeep.core.network

import it.polimi.webeep.core.network.dto.SiteInfoDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class MoodleApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: MoodleApiService

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

        val httpClient = HttpClient(OkHttp) {
            defaultRequest {
                url(mockWebServer.url("/").toString())
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
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
        val path = recordedRequest?.path
        assertNotNull(path)
        assertTrue(path!!.contains("/webservice/rest/server.php"))
        assertTrue(path.contains("wstoken=mock_token"))
        assertTrue(path.contains("wsfunction=core_webservice_get_site_info"))
        assertTrue(path.contains("moodlewsrestformat=json"))
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
        val path = recordedRequest?.path ?: ""
        assertTrue(path.contains("userid=98765"))
        assertTrue(path.contains("wsfunction=core_enrol_get_users_courses"))
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
}
