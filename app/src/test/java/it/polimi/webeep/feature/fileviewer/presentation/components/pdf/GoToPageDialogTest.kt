package it.polimi.webeep.feature.fileviewer.presentation.components.pdf

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import it.polimi.webeep.R
import it.polimi.webeep.util.setAppContent
import it.polimi.webeep.util.string
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoToPageDialogTest {

    @get:Rule
    val rule = createComposeRule()

    private val go get() = string(R.string.file_viewer_go)
    private val field get() = hasSetTextAction()

    private var dismissed = 0
    private val went = mutableListOf<Int>()

    private fun show(pageCount: Int = 12) {
        rule.setAppContent { GoToPageDialog(pageCount = pageCount, onDismiss = { dismissed++ }, onGo = { went.add(it) }) }
    }

    @Test
    fun `shows the title and the page range hint`() {
        show(12)

        rule.onNodeWithText(string(R.string.file_viewer_go_to_page)).assertExists()
        rule.onNodeWithText(string(R.string.file_viewer_page_hint, 12)).assertExists()
    }

    @Test
    fun `go is disabled while the input is empty`() {
        show()

        rule.onNodeWithText(go).assertIsNotEnabled()
    }

    @Test
    fun `a valid page enables go and reports its zero based index`() {
        show(12)

        rule.onNode(field).performTextInput("3")
        rule.onNodeWithText(go).assertIsEnabled().performClick()

        assertEquals(listOf(2), went)
    }

    @Test
    fun `the first and last pages are accepted`() {
        show(12)
        rule.onNode(field).performTextInput("12")
        rule.onNodeWithText(go).assertIsEnabled().performClick()

        assertEquals(listOf(11), went)
    }

    @Test
    fun `zero and pages beyond the count keep go disabled`() {
        show(12)

        rule.onNode(field).performTextInput("0")
        rule.onNodeWithText(go).assertIsNotEnabled()
        rule.onNode(field).performTextInput("13")
        rule.onNodeWithText(go).assertIsNotEnabled()
    }

    @Test
    fun `non digit characters are filtered out`() {
        show(12)

        rule.onNode(field).performTextInput("a4-b")
        rule.onNodeWithText(go).assertIsEnabled().performClick()

        assertEquals(listOf(3), went)
    }

    @Test
    fun `input is capped at five digits`() {
        show(99_999)

        rule.onNode(field).performTextInput("1234567")
        rule.onNodeWithText(go).assertIsEnabled().performClick()

        assertEquals(listOf(12_344), went)
    }

    @Test
    fun `cancel dismisses without going anywhere`() {
        show()
        rule.onNode(field).performTextInput("2")

        rule.onNodeWithText(string(R.string.cancel)).performClick()

        assertEquals(1, dismissed)
        assertEquals(emptyList<Int>(), went)
    }
}
