package it.polimi.webeep.core.database

import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(schemaInstrumentation(), WeBeepDatabase::class.java)

    @Test
    fun migrate1To2() {
        helper.createDatabase(testDb, 1).close()
        helper.runMigrationsAndValidate(testDb, 2, true, DatabaseMigrations.MIGRATION_1_2).close()
    }

    @Test
    fun migrate2To3() {
        helper.createDatabase(testDb, 2).close()
        helper.runMigrationsAndValidate(testDb, 3, true, DatabaseMigrations.MIGRATION_2_3).close()
    }

    @Test
    fun migrate3To4() {
        helper.createDatabase(testDb, 3).close()
        helper.runMigrationsAndValidate(testDb, 4, true, DatabaseMigrations.MIGRATION_3_4).close()
    }

    @Test
    fun migrateAll_preservesCachedRows() {
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                "INSERT INTO courses (id, shortname, fullname, startdate, enddate, isFavourite, lastSyncTimestamp) " +
                    "VALUES (10, 'ACA', 'Advanced Computer Architectures', 0, 0, 1, 0)"
            )
            execSQL("INSERT INTO course_sections (id, courseId, name, section, visible) VALUES (20, 10, 'Intro', 0, 1)")
            execSQL("INSERT INTO course_modules (id, sectionId, name, modName) VALUES (30, 20, 'Slides', 'resource')")
            execSQL(
                "INSERT INTO module_contents (moduleId, filename, fileUrl) " +
                    "VALUES (30, 'lesson1.pdf', 'https://webeep.polimi.it/webservice/pluginfile.php/1/lesson1.pdf')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 4, true, *DatabaseMigrations.ALL)

        db.query("SELECT fullname, isFavourite FROM courses WHERE id = 10").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Advanced Computer Architectures", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
        }
        db.query("SELECT url FROM course_modules WHERE id = 30").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
        }
        db.query("SELECT filename, filepath FROM module_contents WHERE moduleId = 30").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("lesson1.pdf", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
        db.close()
    }

    @Test
    fun migrateAll_opensWithCurrentEntities() {
        helper.createDatabase(testDb, 1).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WeBeepDatabase::class.java,
            testDb
        )
            .addMigrations(*DatabaseMigrations.ALL)
            .build()
        // Opening validates the migrated schema against the Room entities.
        assertEquals(4, database.openHelper.writableDatabase.version)
        database.close()
    }
}

/**
 * AGP does not package a unit-test source set's assets, so the exported schemas under
 * `app/schemas` are not reachable through the real asset manager. This instrumentation serves
 * them from the module directory (the Gradle test working directory) through a mocked
 * [AssetManager], which is the only seam [MigrationTestHelper] reads them from.
 */
private fun schemaInstrumentation(): Instrumentation {
    val app = ApplicationProvider.getApplicationContext<Context>()
    val assets = mockk<AssetManager>()
    every { assets.open(any()) } answers {
        val path = firstArg<String>()
        val file = File("schemas", path)
        if (!file.isFile) throw FileNotFoundException(path)
        file.inputStream()
    }
    val schemaContext = object : ContextWrapper(app) {
        override fun getAssets(): AssetManager = assets
    }
    return object : Instrumentation() {
        override fun getContext(): Context = schemaContext

        override fun getTargetContext(): Context = app
    }
}
