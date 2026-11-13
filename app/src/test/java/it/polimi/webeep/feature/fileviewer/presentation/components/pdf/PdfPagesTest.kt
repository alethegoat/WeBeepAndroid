package it.polimi.webeep.feature.fileviewer.presentation.components.pdf

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import it.polimi.webeep.R
import it.polimi.webeep.util.setAppContent
import it.polimi.webeep.util.string
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric cannot rasterise PDFs: the session never opens, so pages stay blank and only list, indicator and dialog behaviour is checked.
@RunWith(RobolectricTestRunner::class)
class PdfPagesTest {

    @get:Rule
    val rule = createComposeRule()

    private val zoomed = mutableListOf<Boolean>()

    private fun show(pageCount: Int = 20) {
        rule.setAppContent {
            PdfPages(
                session = PdfRendererSession(Dispatchers.Unconfined),
                pageCount = pageCount,
                defaultAspectRatio = 0.7f,
                highResWidth = 1000,
                onToggleFullScreen = {},
                onZoomedChange = { zoomed.add(it) },
                contentPadding = PaddingValues()
            )
        }
        rule.waitForIdle()
    }

    @Test
    fun `starts on the first page and reports not zoomed`() {
        show(20)

        rule.onNodeWithText("1 / 20").assertExists()
        assertEquals(listOf(false), zoomed)
    }

    @Test
    fun `go to page from the indicator jumps there and closes the dialog`() {
        show(20)

        rule.onNodeWithText("1 / 20").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("20")
        rule.onNodeWithText(string(R.string.file_viewer_go)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.file_viewer_go_to_page)).assertDoesNotExist()
        rule.onNodeWithText("20 / 20").assertExists()
    }

    @Test
    fun `cancelling the go to page dialog keeps the current page`() {
        show(20)

        rule.onNodeWithText("1 / 20").performClick()
        rule.onNodeWithText(string(R.string.cancel)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.file_viewer_go_to_page)).assertDoesNotExist()
        rule.onNodeWithText("1 / 20").assertExists()
    }
}
