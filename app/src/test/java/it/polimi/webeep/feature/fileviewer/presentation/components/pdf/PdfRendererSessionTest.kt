package it.polimi.webeep.feature.fileviewer.presentation.components.pdf

import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric cannot rasterise PDFs, so this covers the session's failure paths and lifecycle.
@RunWith(RobolectricTestRunner::class)
class PdfRendererSessionTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `a missing file does not open`() = runTest {
        assertNull(PdfRendererSession(StandardTestDispatcher(testScheduler)).open(File(tmp.root, "missing.pdf")))
    }

    @Test
    fun `pages of a session that never opened render to nothing`() = runTest {
        val session = PdfRendererSession(StandardTestDispatcher(testScheduler))

        assertNull(session.fastPage(0))
        assertNull(session.highResPage(0, 1000))
        assertNull(session.cachedFast(0))
        assertNull(session.cachedHighRes(0))
    }

    @Test
    fun `close can be called repeatedly`() = runTest {
        val session = PdfRendererSession(StandardTestDispatcher(testScheduler))
        session.open(File(tmp.root, "missing.pdf"))

        session.close()
        session.close()
    }

    @Test
    fun `default cache size stays within its bounds`() {
        val kb = PdfRendererSession.defaultCacheSizeKb()

        assertTrue(kb in 8 * 1024..24 * 1024)
    }
}
