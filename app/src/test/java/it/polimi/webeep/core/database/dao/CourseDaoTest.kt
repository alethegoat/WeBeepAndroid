package it.polimi.webeep.core.database.dao

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CourseDaoTest : DaoTestBase() {

    private val dao get() = db.courseDao()

    @Test
    fun `insertCourse returns the row id and stores the course`() = runTest {
        val result = dao.insertCourse(course(1))

        assertEquals(1L, result)
        assertEquals(course(1), dao.getCourseById(1))
    }

    @Test
    fun `insertCourse ignores a conflicting id and returns -1`() = runTest {
        dao.insertCourse(course(1, fullname = "Original"))

        val result = dao.insertCourse(course(1, fullname = "Changed"))

        assertEquals(-1L, result)
        assertEquals("Original", dao.getCourseById(1)?.fullname)
    }

    @Test
    fun `updateCourse overwrites the stored row`() = runTest {
        dao.insertCourse(course(1, fullname = "Old"))

        dao.updateCourse(course(1, fullname = "New"))

        assertEquals("New", dao.getCourseById(1)?.fullname)
    }

    @Test
    fun `getCourseById returns null for an unknown id`() = runTest {
        assertNull(dao.getCourseById(99))
    }

    @Test
    fun `getAllCourseIds lists every stored id`() = runTest {
        dao.insertCourse(course(3))
        dao.insertCourse(course(1))

        assertEquals(listOf(1, 3), dao.getAllCourseIds().sorted())
    }

    @Test
    fun `deleteCoursesByIds removes only the given ids`() = runTest {
        listOf(1, 2, 3).forEach { dao.insertCourse(course(it)) }

        dao.deleteCoursesByIds(listOf(1, 3))

        assertEquals(listOf(2), dao.getAllCourseIds())
    }

    @Test
    fun `getAllCourses is ordered by fullname ascending`() = runTest {
        dao.insertCourse(course(1, fullname = "Zoology"))
        dao.insertCourse(course(2, fullname = "Algebra"))
        dao.insertCourse(course(3, fullname = "Management"))

        dao.getAllCourses().test {
            assertEquals(listOf("Algebra", "Management", "Zoology"), awaitItem().map { it.fullname })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllCourses emits again when the table changes`() = runTest {
        dao.getAllCourses().test {
            assertEquals(emptyList<Int>(), awaitItem().map { it.id })

            dao.insertCourse(course(1))
            assertEquals(listOf(1), awaitItem().map { it.id })

            dao.updateCourse(course(1, fullname = "Renamed"))
            assertEquals(listOf("Renamed"), awaitItem().map { it.fullname })

            dao.deleteCoursesByIds(listOf(1))
            assertEquals(emptyList<Int>(), awaitItem().map { it.id })
        }
    }

    @Test
    fun `replaceAllCourses inserts new courses and updates existing ones`() = runTest {
        dao.insertCourse(course(1, fullname = "Old"))

        dao.replaceAllCourses(listOf(course(1, fullname = "Updated"), course(2)))

        assertEquals("Updated", dao.getCourseById(1)?.fullname)
        assertEquals(listOf(1, 2), dao.getAllCourseIds().sorted())
    }

    @Test
    fun `replaceAllCourses deletes courses that disappeared remotely`() = runTest {
        listOf(1, 2, 3).forEach { dao.insertCourse(course(it)) }

        dao.replaceAllCourses(listOf(course(2)))

        assertEquals(listOf(2), dao.getAllCourseIds())
    }

    @Test
    fun `replaceAllCourses with an empty list clears the table`() = runTest {
        dao.insertCourse(course(1))

        dao.replaceAllCourses(emptyList())

        assertEquals(emptyList<Int>(), dao.getAllCourseIds())
    }

    @Test
    fun `replaceAllCourses keeps the sections of surviving courses`() = runTest {
        dao.insertCourse(course(1))
        db.courseContentDao().insertSections(listOf(section(10, courseId = 1, order = 0)))

        dao.replaceAllCourses(listOf(course(1, fullname = "Updated")))

        assertEquals(1, db.courseContentDao().getSectionsByCourseSync(1).size)
    }

    @Test
    fun `deleting a course cascades to its sections modules and contents`() = runTest {
        dao.insertCourse(course(1))
        val contentDao = db.courseContentDao()
        contentDao.replaceCourseContent(
            1,
            listOf(section(10, 1, 0)),
            listOf(module(100, 10)),
            listOf(content(100))
        )

        dao.replaceAllCourses(emptyList())

        assertEquals(emptyList<Any>(), contentDao.getSectionsByCourseSync(1))
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM module_contents").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }
}
