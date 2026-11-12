package it.polimi.webeep.core.ui.util

import android.provider.OpenableColumns
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FileIntentsTest {

    @Test
    fun `the content uri exposes the title as display name instead of the hashed file name`() {
        val context = RuntimeEnvironment.getApplication()
        val file = File(context.cacheDir, "viewer/0f1e2d3c4b5a.pdf").apply {
            parentFile?.mkdirs()
            writeText("pdf")
        }

        val uri = context.fileProviderUri(file, "Lecture notes")

        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use {
            it.moveToFirst()
            it.getString(0)
        }
        assertEquals("Lecture notes.pdf", name)
    }
}
