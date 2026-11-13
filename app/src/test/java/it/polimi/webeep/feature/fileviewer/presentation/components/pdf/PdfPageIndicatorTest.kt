package it.polimi.webeep.feature.fileviewer.presentation.components.pdf

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.polimi.webeep.util.setAppContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfPageIndicatorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `shows the one based current page over the total`() {
        rule.setAppContent { PdfPageIndicator(currentPage = 2, pageCount = 12, isScrolling = false, onClick = {}) }

        rule.onNodeWithText("3 / 12").assertExists()
    }

    @Test
    fun `the first page of a single page document reads 1 over 1`() {
        rule.setAppContent { PdfPageIndicator(currentPage = 0, pageCount = 1, isScrolling = true, onClick = {}) }

        rule.onNodeWithText("1 / 1").assertExists()
    }

    @Test
    fun `tapping the pill invokes onClick`() {
        var clicks = 0
        rule.setAppContent { PdfPageIndicator(currentPage = 0, pageCount = 5, isScrolling = false, onClick = { clicks++ }) }

        rule.onNodeWithText("1 / 5").performClick()

        assertEquals(1, clicks)
    }
}
