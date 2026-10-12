package it.polimi.webeep.feature.course.data

import android.database.Cursor
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.MoodleException
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.database.dao.DaoTestBase
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.dto.CourseDto
import it.polimi.webeep.core.network.dto.CourseModuleDto
import it.polimi.webeep.core.network.dto.CourseSectionDto
import it.polimi.webeep.core.network.dto.ModuleContentDto
import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.util.fakes.FakeTokenProvider
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CourseRepositoryImplTest : DaoTestBase() {

    private class FakeApi : MoodleApiService {
        var sections: List<CourseSectionDto> = emptyList()
        var failure: Throwable? = null
        var calls = 0

        override suspend fun getSiteInfo(wsToken: String?): SiteInfoDto = error("unused")

        override suspend fun getEnrolledCourses(wsToken: String?, userId: Int): List<CourseDto> = error("unused")

        override suspend fun getCourseContents(wsToken: String?, courseId: Int): List<CourseSectionDto> {
            calls++
            failure?.let { throw it }
            return sections
        }
    }

    private val api = FakeApi()
    private val tokens = FakeTokenProvider().apply { token = "secret-token" }
    private lateinit var repository: CourseRepositoryImpl

    @Before
    fun setUp() = runTest {
        db.courseDao().insertCourse(course(1, fullname = "Algebra"))
        repository = CourseRepositoryImpl(api, db.courseContentDao(), db.courseDao(), tokens, Dispatchers.Unconfined)
    }

    private fun fileSection(fileUrl: String, iconUrl: String? = null, moduleUrl: String? = null) = CourseSectionDto(
        id = 10,
        name = "Week 1",
        section = 1,
        modules = listOf(
            CourseModuleDto(
                id = 100,
                name = "Slides",
                modname = "resource",
                modicon = iconUrl,
                url = moduleUrl,
                contents = listOf(
                    ModuleContentDto(type = "file", filename = "slides.pdf", fileurl = fileUrl, filesize = 10L, timemodified = 5L)
                )
            )
        )
    )

    @Test
    fun `network success writes the contents through to the database`() = runTest {
        api.sections = listOf(fileSection("https://webeep.polimi.it/pluginfile.php/1/slides.pdf"))

        val result = repository.getCourseContents(1)

        assertTrue(result is Resource.Success)
        val stored = db.courseContentDao().getSectionsByCourseSync(1)
        assertEquals(listOf(10), stored.map { it.section.id })
        assertEquals(listOf("slides.pdf"), stored.single().modules.single().contents.map { it.filename })
        assertEquals(listOf(10), (result as Resource.Success).data.map { it.id })
    }

    @Test
    fun `a refresh replaces the previously cached sections`() = runTest {
        api.sections = listOf(fileSection("https://webeep.polimi.it/pluginfile.php/1/slides.pdf"))
        repository.getCourseContents(1)

        api.sections = listOf(fileSection("https://webeep.polimi.it/pluginfile.php/1/slides.pdf").copy(id = 11))
        repository.getCourseContents(1)

        assertEquals(listOf(11), db.courseContentDao().getSectionsByCourseSync(1).map { it.section.id })
    }

    @Test
    fun `auth query parameters never reach the stored entities`() = runTest {
        api.sections = listOf(
            fileSection(
                fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/1/slides.pdf?forcedownload=1&token=abc123",
                iconUrl = "https://webeep.polimi.it/theme/image.php/icon?wstoken=xyz789",
                moduleUrl = "https://webeep.polimi.it/mod/resource/view.php?id=100&token=abc123"
            )
        )

        repository.getCourseContents(1)

        val stored = db.courseContentDao().getSectionsByCourseSync(1).single().modules.single()
        assertEquals(
            "https://webeep.polimi.it/webservice/pluginfile.php/1/slides.pdf?forcedownload=1",
            stored.contents.single().fileUrl
        )
        assertEquals("https://webeep.polimi.it/theme/image.php/icon", stored.module.iconUrl)
        assertEquals("https://webeep.polimi.it/mod/resource/view.php?id=100", stored.module.url)
        assertNoSecretInDatabase()
    }

    @Test
    fun `link modules and fileless modules are stripped before persistence`() = runTest {
        api.sections = listOf(
            CourseSectionDto(
                id = 10,
                modules = listOf(
                    CourseModuleDto(
                        id = 101,
                        modname = "url",
                        url = "https://webeep.polimi.it/mod/url/view.php?id=101&token=abc123",
                        contents = listOf(
                            ModuleContentDto(type = "url", fileurl = "https://example.org/page?wstoken=xyz789&a=1")
                        )
                    ),
                    CourseModuleDto(
                        id = 102,
                        modname = "folder",
                        url = "https://webeep.polimi.it/mod/folder/view.php?id=102&token=abc123",
                        contents = emptyList()
                    )
                )
            )
        )

        repository.getCourseContents(1)

        assertNoSecretInDatabase()
        val modules = db.courseContentDao().getSectionsByCourseSync(1).single().modules.associateBy { it.module.id }
        assertEquals("https://example.org/page?a=1", modules.getValue(101).module.url)
    }

    @Test
    fun `auth parameters are stripped from the returned domain models too`() = runTest {
        api.sections = listOf(fileSection("https://webeep.polimi.it/webservice/pluginfile.php/1/a.pdf?token=abc123"))

        val result = repository.getCourseContents(1) as Resource.Success

        val url = result.data.single().modules.single().files.single().fileUrl
        assertFalse(url.contains("token="))
    }

    @Test
    fun `a missing token maps to Unauthorized without calling the network`() = runTest {
        tokens.token = null

        val result = repository.getCourseContents(1)

        assertEquals(DataError.Unauthorized, (result as Resource.Error).error)
        assertEquals(0, api.calls)
    }

    @Test
    fun `an IOException maps to a Network error and leaves the cache untouched`() = runTest {
        api.sections = listOf(fileSection("https://webeep.polimi.it/pluginfile.php/1/slides.pdf"))
        repository.getCourseContents(1)
        api.failure = IOException("offline")

        val result = repository.getCourseContents(1)

        assertEquals(DataError.Network, (result as Resource.Error).error)
        assertEquals(listOf(10), db.courseContentDao().getSectionsByCourseSync(1).map { it.section.id })
    }

    @Test
    fun `an invalid token reported by Moodle maps to Unauthorized`() = runTest {
        api.failure = MoodleException("invalidtoken", "Invalid token")

        val result = repository.getCourseContents(1)

        assertEquals(DataError.Unauthorized, (result as Resource.Error).error)
    }

    @Test
    fun `another Moodle error maps to a Server error with its code`() = runTest {
        api.failure = MoodleException("servicenotavailable", "down")

        val result = repository.getCourseContents(1)

        assertEquals(DataError.Server("servicenotavailable"), (result as Resource.Error).error)
    }

    @Test
    fun `an unexpected exception maps to Unknown`() = runTest {
        api.failure = IllegalStateException("boom")

        val result = repository.getCourseContents(1)

        assertEquals(DataError.Unknown, (result as Resource.Error).error)
    }

    @Test
    fun `cached contents are read from the database and mapped to the domain`() = runTest {
        api.sections = listOf(fileSection("https://webeep.polimi.it/pluginfile.php/1/slides.pdf"))
        repository.getCourseContents(1)

        val cached = repository.getCachedCourseContents(1)
        val sections = cached.first()

        assertEquals("slides.pdf", sections.single().modules.single().files.single().filename)
    }

    @Test
    fun `getCourseName returns the stored fullname or null`() = runTest {
        assertEquals("Algebra", repository.getCourseName(1))
        assertNull(repository.getCourseName(99))
    }

    private fun assertNoSecretInDatabase() {
        val sqlite = db.openHelper.readableDatabase
        val values = listOf("course_sections", "course_modules", "module_contents").flatMap { table ->
            sqlite.query("SELECT * FROM $table").use { cursor -> cursor.stringValues(table) }
        }
        values.forEach { (where, value) ->
            assertFalse("$where leaks an auth parameter: $value", value.contains("token="))
            assertFalse("$where leaks a token value: $value", value.contains("abc123") || value.contains("xyz789"))
        }
    }

    private fun Cursor.stringValues(table: String): List<Pair<String, String>> {
        val found = mutableListOf<Pair<String, String>>()
        while (moveToNext()) {
            for (column in 0 until columnCount) {
                if (getType(column) == Cursor.FIELD_TYPE_STRING) found += "$table.${getColumnName(column)}" to getString(column)
            }
        }
        return found
    }
}
