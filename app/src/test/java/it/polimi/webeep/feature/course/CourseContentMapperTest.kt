package it.polimi.webeep.feature.course

import it.polimi.webeep.core.network.dto.CourseModuleDto
import it.polimi.webeep.core.network.dto.CourseSectionDto
import it.polimi.webeep.core.network.dto.ModuleContentDto
import it.polimi.webeep.feature.course.data.CourseContentMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseContentMapperTest {

    @Test
    fun `ignores dir entries and phantom index html`() {
        val mapped = CourseContentMapper.map(
            courseId = 7,
            sectionDtos = listOf(
                CourseSectionDto(
                    id = 1,
                    name = "Files",
                    modules = listOf(
                        CourseModuleDto(
                            id = 10,
                            name = "Slides",
                            modname = "folder",
                            contents = listOf(
                                ModuleContentDto(type = "dir", filename = "week1"),
                                ModuleContentDto(
                                    type = "file",
                                    filename = "index.html",
                                    fileurl = "https://webeep.polimi.it/pluginfile.php/1/index.html",
                                    filesize = 0,
                                    mimetype = "text/html",
                                    timemodified = 1L
                                ),
                                ModuleContentDto(
                                    type = "file",
                                    filename = "slides.pdf",
                                    fileurl = "https://webeep.polimi.it/pluginfile.php/1/slides.pdf",
                                    filesize = 2048,
                                    mimetype = "application/pdf",
                                    timemodified = 99L
                                )
                            )
                        )
                    )
                )
            )
        )

        val files = mapped.sections.single().modules.single().files
        assertEquals(1, files.size)
        assertEquals("slides.pdf", files.single().filename)
        assertFalse(files.any { it.filename.equals("index.html", ignoreCase = true) })
    }

    @Test
    fun `fileKey includes stripped url filename and timeModified`() {
        val url = "https://webeep.polimi.it/pluginfile.php/1/a.pdf?token=secret"
        val first = CourseContentMapper.fileKey(url, "a.pdf", 100L)
        val updated = CourseContentMapper.fileKey(url, "a.pdf", 200L)
        assertTrue(first.contains("a.pdf"))
        assertFalse(first.contains("token="))
        assertTrue(first != updated)
    }

    @Test
    fun `retains legitimate non-empty index html files`() {
        val mapped = CourseContentMapper.map(
            courseId = 7,
            sectionDtos = listOf(
                CourseSectionDto(
                    id = 1,
                    name = "Web",
                    modules = listOf(
                        CourseModuleDto(
                            id = 10,
                            name = "Project",
                            modname = "resource",
                            contents = listOf(
                                ModuleContentDto(
                                    type = "file",
                                    filename = "index.html",
                                    fileurl = "https://webeep.polimi.it/pluginfile.php/1/index.html",
                                    filesize = 1024,
                                    mimetype = "text/html",
                                    timemodified = 50L
                                )
                            )
                        )
                    )
                )
            )
        )

        val files = mapped.sections.single().modules.single().files
        assertEquals(1, files.size)
        assertEquals("index.html", files.single().filename)
    }
}
