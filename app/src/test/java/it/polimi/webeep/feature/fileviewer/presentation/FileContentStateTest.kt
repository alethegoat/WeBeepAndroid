package it.polimi.webeep.feature.fileviewer.presentation

import it.polimi.webeep.R
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.feature.fileviewer.domain.LoadedContent
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileContentStateTest {

    private val file = File("local.pdf")

    @Test
    fun `ready content maps one to one`() {
        assertEquals(FileContentState.Image("u"), LoadedContent.Image("u").toContentState())
        assertEquals(FileContentState.Media("u"), LoadedContent.Media("u").toContentState())
        assertEquals(FileContentState.Pdf(file, 3), LoadedContent.Pdf(file, 3).toContentState())
        assertEquals(FileContentState.Code(file, "x"), LoadedContent.Code(file, "x").toContentState())
        assertEquals(FileContentState.External(file, "a/b"), LoadedContent.External(file, "a/b").toContentState())
    }

    @Test
    fun `progress is derived from the downloaded bytes`() {
        val state = LoadedContent.Downloading(50L, 200L).toContentState() as FileContentState.Downloading

        assertEquals(0.25f, state.progress)
        assertNull((LoadedContent.Downloading(50L, null).toContentState() as FileContentState.Downloading).progress)
    }

    @Test
    fun `data errors map to the viewer messages`() {
        assertEquals(ViewerError.SESSION_EXPIRED, DataError.Unauthorized.toViewerError())
        assertEquals(ViewerError.NETWORK, DataError.Network.toViewerError())
        assertEquals(ViewerError.NETWORK, DataError.Server("x").toViewerError())
        assertEquals(ViewerError.CANNOT_OPEN, DataError.Storage.toViewerError())
        assertEquals(ViewerError.CANNOT_OPEN, DataError.Unknown.toViewerError())
    }

    @Test
    fun `failures and corrupt pdfs become error states with their messages`() {
        val expired = LoadedContent.Failed(DataError.Unauthorized).toContentState() as FileContentState.Error
        val corrupt = LoadedContent.CorruptPdf.toContentState() as FileContentState.Error

        assertEquals(R.string.error_session_expired, expired.messageRes)
        assertEquals(ViewerError.CORRUPT_PDF, corrupt.reason)
        assertEquals(R.string.file_viewer_error_pdf, corrupt.messageRes)
    }

    @Test
    fun `localFile is exposed only by states backed by a file`() {
        assertEquals(file, FileContentState.Pdf(file, 1).localFile)
        assertEquals(file, FileContentState.Code(file, "").localFile)
        assertEquals(file, FileContentState.External(file, "a/b").localFile)
        assertNull(FileContentState.Image("u").localFile)
        assertNull(FileContentState.Media("u").localFile)
    }
}
