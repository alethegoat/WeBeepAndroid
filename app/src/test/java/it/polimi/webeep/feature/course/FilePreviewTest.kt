package it.polimi.webeep.feature.course

import it.polimi.webeep.feature.course.presentation.FilePreviewTarget
import it.polimi.webeep.feature.course.presentation.FileUiModel
import it.polimi.webeep.feature.course.presentation.previewTargetFor
import it.polimi.webeep.navigation.FileViewer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilePreviewTest {

    @Test
    fun `external web links open in a custom tab`() {
        val target = previewTargetFor(file("https://example.com/slides"), courseId = 5)

        assertEquals(FilePreviewTarget.ExternalLink("https://example.com/slides"), target)
    }

    @Test
    fun `pluginfiles open in the viewer with the course for sibling swiping`() {
        val url = "https://webeep.polimi.it/webservice/pluginfile.php/1/mod/a.pdf"

        val target = previewTargetFor(file(url, mimeType = "application/pdf"), courseId = 5)

        assertEquals(
            FilePreviewTarget.Viewer(FileViewer(url, "a.pdf", "application/pdf", courseId = 5)),
            target
        )
    }

    @Test
    fun `tokens are stripped from the url before it reaches a route`() {
        val url = "https://webeep.polimi.it/webservice/pluginfile.php/1/mod/a.pdf?token=secret"

        val target = previewTargetFor(file(url), courseId = 5)

        assertTrue(target is FilePreviewTarget.Viewer)
        val route = (target as FilePreviewTarget.Viewer).route
        assertFalse("secret" in route.url)
    }

    @Test
    fun `tokens are stripped from external links too`() {
        val target = previewTargetFor(file("https://example.com/x?token=secret"), courseId = 5)

        assertTrue(target is FilePreviewTarget.ExternalLink)
        assertFalse("secret" in (target as FilePreviewTarget.ExternalLink).url)
    }

    private fun file(url: String, mimeType: String? = null) = FileUiModel(
        filename = "a.pdf",
        filepath = "/",
        localFileName = "a.pdf",
        fileUrl = url,
        fileSize = 1L,
        mimeType = mimeType,
        formattedSize = "1 B"
    )
}
