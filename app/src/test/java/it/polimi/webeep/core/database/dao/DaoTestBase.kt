package it.polimi.webeep.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.polimi.webeep.core.database.WeBeepDatabase
import it.polimi.webeep.core.database.entity.CourseEntity
import it.polimi.webeep.core.database.entity.CourseModuleEntity
import it.polimi.webeep.core.database.entity.CourseSectionEntity
import it.polimi.webeep.core.database.entity.ModuleContentEntity
import org.junit.After
import org.junit.Before

/** In-memory Room database shared by the DAO tests, plus entity builders. */
abstract class DaoTestBase {

    protected lateinit var db: WeBeepDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WeBeepDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    protected fun course(id: Int, fullname: String = "Course $id", sync: Long = 0L) = CourseEntity(
        id = id,
        shortname = "C$id",
        fullname = fullname,
        displayname = null,
        summary = null,
        startdate = 0L,
        enddate = 0L,
        progress = null,
        courseImage = null,
        category = null,
        isFavourite = false,
        lastSyncTimestamp = sync
    )

    protected fun section(id: Int, courseId: Int, order: Int) = CourseSectionEntity(
        id = id,
        courseId = courseId,
        name = "Section $id",
        summary = null,
        section = order,
        visible = true
    )

    protected fun module(id: Int, sectionId: Int) = CourseModuleEntity(
        id = id,
        sectionId = sectionId,
        name = "Module $id",
        modName = "resource",
        iconUrl = null,
        description = null
    )

    protected fun content(moduleId: Int, filename: String = "f.pdf") = ModuleContentEntity(
        moduleId = moduleId,
        filename = filename,
        filepath = null,
        fileUrl = "https://example.org/$filename",
        fileSize = null,
        mimeType = null,
        timeModified = null
    )
}
