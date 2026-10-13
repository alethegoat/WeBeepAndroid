package it.polimi.webeep.core.database.dao

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CourseContentDaoTest : DaoTestBase() {

    private val dao get() = db.courseContentDao()

    @Before
    fun seedCourses() = runTest {
        db.courseDao().insertCourse(course(1))
        db.courseDao().insertCourse(course(2))
    }

    @Test
    fun `sections are returned ordered by section number`() = runTest {
        dao.insertSections(
            listOf(section(10, 1, order = 2), section(11, 1, order = 0), section(12, 1, order = 1))
        )

        assertEquals(listOf(11, 12, 10), dao.getSectionsByCourseSync(1).map { it.section.id })
    }

    @Test
    fun `sections of other courses are not returned`() = runTest {
        dao.insertSections(listOf(section(10, 1, 0), section(20, 2, 0)))

        assertEquals(listOf(10), dao.getSectionsByCourseSync(1).map { it.section.id })
    }

    @Test
    fun `relation loads modules and contents nested under their section`() = runTest {
        dao.insertSections(listOf(section(10, 1, 0)))
        dao.insertModules(listOf(module(100, 10), module(101, 10)))
        dao.insertContents(listOf(content(100, "a.pdf"), content(100, "b.pdf"), content(101, "c.pdf")))

        val sections = dao.getSectionsByCourseSync(1)

        assertEquals(1, sections.size)
        val modules = sections.single().modules.associateBy { it.module.id }
        assertEquals(setOf("a.pdf", "b.pdf"), modules.getValue(100).contents.map { it.filename }.toSet())
        assertEquals(listOf("c.pdf"), modules.getValue(101).contents.map { it.filename })
    }

    @Test
    fun `insertSections replaces a row with the same id`() = runTest {
        dao.insertSections(listOf(section(10, 1, 0)))

        dao.insertSections(listOf(section(10, 1, 0).copy(name = "Renamed")))

        val sections = dao.getSectionsByCourseSync(1)
        assertEquals(1, sections.size)
        assertEquals("Renamed", sections.single().section.name)
    }

    @Test
    fun `insertModules replaces a row with the same id`() = runTest {
        dao.insertSections(listOf(section(10, 1, 0)))
        dao.insertModules(listOf(module(100, 10)))

        dao.insertModules(listOf(module(100, 10).copy(name = "Renamed")))

        assertEquals("Renamed", dao.getSectionsByCourseSync(1).single().modules.single().module.name)
    }

    @Test
    fun `insertContents replaces a row with the same generated id`() = runTest {
        dao.insertSections(listOf(section(10, 1, 0)))
        dao.insertModules(listOf(module(100, 10)))
        dao.insertContents(listOf(content(100, "a.pdf")))
        val stored = dao.getSectionsByCourseSync(1).single().modules.single().contents.single()

        dao.insertContents(listOf(stored.copy(filename = "renamed.pdf")))

        val contents = dao.getSectionsByCourseSync(1).single().modules.single().contents
        assertEquals(listOf("renamed.pdf"), contents.map { it.filename })
    }

    @Test
    fun `deleteSectionsByCourse cascades to modules and contents`() = runTest {
        dao.replaceCourseContent(1, listOf(section(10, 1, 0)), listOf(module(100, 10)), listOf(content(100)))
        dao.replaceCourseContent(2, listOf(section(20, 2, 0)), listOf(module(200, 20)), listOf(content(200)))

        dao.deleteSectionsByCourse(1)

        assertTrue(dao.getSectionsByCourseSync(1).isEmpty())
        assertEquals(1, dao.getSectionsByCourseSync(2).single().modules.single().contents.size)
        assertEquals(1, count("course_modules"))
        assertEquals(1, count("module_contents"))
    }

    @Test
    fun `replaceCourseContent swaps old content for the new one`() = runTest {
        dao.replaceCourseContent(1, listOf(section(10, 1, 0)), listOf(module(100, 10)), listOf(content(100, "old.pdf")))

        dao.replaceCourseContent(1, listOf(section(11, 1, 0)), listOf(module(101, 11)), listOf(content(101, "new.pdf")))

        val sections = dao.getSectionsByCourseSync(1)
        assertEquals(listOf(11), sections.map { it.section.id })
        assertEquals(listOf("new.pdf"), sections.single().modules.single().contents.map { it.filename })
        assertEquals(1, count("module_contents"))
    }

    @Test
    fun `replaceCourseContent leaves other courses untouched`() = runTest {
        dao.replaceCourseContent(2, listOf(section(20, 2, 0)), listOf(module(200, 20)), listOf(content(200)))

        dao.replaceCourseContent(1, listOf(section(10, 1, 0)), emptyList(), emptyList())

        assertEquals(listOf(20), dao.getSectionsByCourseSync(2).map { it.section.id })
    }

    @Test
    fun `replaceCourseContent rolls back when a later insert fails`() = runTest {
        dao.replaceCourseContent(1, listOf(section(10, 1, 0)), listOf(module(100, 10)), listOf(content(100)))

        // The module points at a section that does not exist, so the foreign key fails mid-transaction.
        val failure = runCatching {
            dao.replaceCourseContent(1, listOf(section(11, 1, 0)), listOf(module(101, 999)), emptyList())
        }

        assertTrue(failure.isFailure)
        val sections = dao.getSectionsByCourseSync(1)
        assertEquals(listOf(10), sections.map { it.section.id })
        assertEquals(1, sections.single().modules.single().contents.size)
    }

    @Test
    fun `getSectionsByCourse emits again after the content is replaced`() = runTest {
        dao.getSectionsByCourse(1).test {
            assertTrue(awaitItem().isEmpty())

            dao.replaceCourseContent(1, listOf(section(10, 1, 0)), listOf(module(100, 10)), emptyList())
            assertEquals(listOf(10), awaitItem().map { it.section.id })

            dao.insertContents(listOf(content(100, "late.pdf")))
            assertEquals(
                listOf("late.pdf"),
                awaitItem().single().modules.single().contents.map { it.filename }
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun count(table: String): Int = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
        it.moveToFirst()
        it.getInt(0)
    }
}
