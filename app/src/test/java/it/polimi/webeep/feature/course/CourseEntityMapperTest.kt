package it.polimi.webeep.feature.course

import it.polimi.webeep.core.database.dao.ModuleWithContents
import it.polimi.webeep.core.database.dao.SectionWithModules
import it.polimi.webeep.core.database.entity.CourseModuleEntity
import it.polimi.webeep.core.database.entity.CourseSectionEntity
import it.polimi.webeep.core.database.entity.ModuleContentEntity
import it.polimi.webeep.feature.course.data.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CourseEntityMapperTest {

    @Test
    fun `maps a section tree and strips auth from persisted urls`() {
        val section = SectionWithModules(
            section = CourseSectionEntity(id = 1, courseId = 7, name = "Week 1", summary = "S", section = 3, visible = true),
            modules = listOf(
                ModuleWithContents(
                    module = CourseModuleEntity(
                        id = 10,
                        sectionId = 1,
                        name = "Slides",
                        modName = "resource",
                        iconUrl = "https://webeep.polimi.it/icon.png?token=secret",
                        description = "d",
                        url = "https://webeep.polimi.it/mod/view.php?id=1&wstoken=secret"
                    ),
                    contents = listOf(
                        ModuleContentEntity(
                            moduleId = 10,
                            filename = "a.pdf",
                            filepath = null,
                            fileUrl = "https://webeep.polimi.it/webservice/pluginfile.php/1/a.pdf?token=secret",
                            fileSize = null,
                            mimeType = "application/pdf",
                            timeModified = 5L
                        )
                    )
                )
            )
        )

        val domain = section.toDomain()

        assertEquals(1, domain.id)
        assertEquals("Week 1", domain.name)
        assertEquals(3, domain.sectionNumber)
        val module = domain.modules.single()
        assertEquals("https://webeep.polimi.it/icon.png", module.iconUrl)
        assertEquals("https://webeep.polimi.it/mod/view.php?id=1", module.url)
        val file = module.files.single()
        assertEquals("/", file.filepath)
        assertEquals(0L, file.fileSize)
        assertEquals("https://webeep.polimi.it/webservice/pluginfile.php/1/a.pdf", file.fileUrl)
        assertEquals(5L, file.timeModified)
    }

    @Test
    fun `null module urls stay null`() {
        val module = ModuleWithContents(
            module = CourseModuleEntity(id = 1, sectionId = 1, name = "n", modName = "url", iconUrl = null, description = null),
            contents = emptyList()
        ).toDomain()

        assertNull(module.iconUrl)
        assertNull(module.url)
    }
}
