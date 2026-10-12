package it.polimi.webeep.feature.course.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.polimi.webeep.R
import it.polimi.webeep.util.setAppContent
import it.polimi.webeep.util.string
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A tall window, so the lazy list composes every row of the sample course. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w400dp-h1600dp")
class CourseDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<CourseDetailAction>()
    private var backClicks = 0

    private val allFolders = setOf(
        folderRootKey(moduleId = 11),
        folderKey(moduleId = 11, fullPath = "/Week 1"),
        folderKey(moduleId = 11, fullPath = "/Week 2")
    )

    private fun show(state: CourseDetailUiState) {
        composeRule.setAppContent {
            CourseDetailScreen(state = state, onAction = { actions += it }, onBackClick = { backClicks++ })
        }
    }

    private fun showContent(folders: Set<String> = emptySet(), query: String = "") = show(
        previewCourseState(CourseDetailContent.Content(previewList(folders = folders, query = query)), query = query, folders = folders)
    )

    @Test
    fun `sections render with their summaries and materials`() {
        showContent()

        composeRule.onNodeWithText("General").assertIsDisplayed()
        composeRule.onNodeWithText("Welcome to the course").assertIsDisplayed()
        composeRule.onNodeWithText("Syllabus").assertIsDisplayed()
        composeRule.onNodeWithText("Lecture notes").assertIsDisplayed()
        composeRule.onNodeWithText("Exercises").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.no_materials_section)).assertIsDisplayed()
    }

    @Test
    fun `clicking a section header toggles that section`() {
        showContent()

        composeRule.onNodeWithText("General").performClick()

        assertEquals(listOf<CourseDetailAction>(CourseDetailAction.ToggleSection(1)), actions)
    }

    @Test
    fun `clicking a folder module toggles its root`() {
        showContent()

        composeRule.onNodeWithText("Lecture notes").performClick()

        assertEquals(listOf<CourseDetailAction>(CourseDetailAction.ToggleFolder(folderRootKey(moduleId = 11))), actions)
    }

    @Test
    fun `clicking a sub-folder toggles it`() {
        showContent(folders = allFolders)

        composeRule.onNodeWithText("Week 1").performClick()

        assertEquals(
            listOf<CourseDetailAction>(CourseDetailAction.ToggleFolder(folderKey(moduleId = 11, fullPath = "/Week 1"))),
            actions
        )
    }

    @Test
    fun `expanded folders show their files`() {
        showContent(folders = allFolders)

        composeRule.onNodeWithText("week1.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("week2.pptx").assertIsDisplayed()
    }

    @Test
    fun `clicking a file row previews it`() {
        showContent(folders = allFolders)

        composeRule.onNodeWithText("week1.pdf").performClick()

        val action = actions.single() as CourseDetailAction.PreviewFile
        assertEquals("week1.pdf", action.file.filename)
    }

    @Test
    fun `clicking a single-file module previews its file`() {
        showContent()

        composeRule.onNodeWithText("Syllabus").performClick()

        val action = actions.single() as CourseDetailAction.PreviewFile
        assertEquals("syllabus.pdf", action.file.filename)
    }

    @Test
    fun `a file not downloaded yet offers to download it`() {
        showContent(folders = allFolders)

        composeRule.onNodeWithContentDescription(string(R.string.cd_download_resource)).performClick()

        val action = actions.single() as CourseDetailAction.DownloadFile
        assertEquals("week1.pdf", action.file.filename)
    }

    @Test
    fun `a downloaded file offers to open it`() {
        showContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_open_file)).performClick()

        val action = actions.single() as CourseDetailAction.OpenFile
        assertEquals("syllabus.pdf", action.file.filename)
    }

    @Test
    fun `a file being downloaded shows progress and offers neither download nor open`() {
        showContent(folders = allFolders)

        composeRule.onNodeWithContentDescription(string(R.string.cd_downloading)).assertIsDisplayed().performClick()

        assertTrue(actions.none { it is CourseDetailAction.DownloadFile || it is CourseDetailAction.OpenFile })
    }

    @Test
    fun `typing in the search field sends the query`() {
        showContent()

        composeRule.onNode(hasSetTextAction()).performTextInput("notes")

        assertEquals(listOf<CourseDetailAction>(CourseDetailAction.SearchQueryChange("notes")), actions)
    }

    @Test
    fun `a search without results says so and can be reset`() {
        showContent(query = "thermodynamics")

        composeRule.onNodeWithText(string(R.string.no_materials_found)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.reset_filters)).performClick()

        assertEquals(listOf<CourseDetailAction>(CourseDetailAction.SearchQueryChange("")), actions)
    }

    @Test
    fun `error shows its message and retry works`() {
        show(previewCourseState(CourseDetailContent.Error(R.string.error_data_network)))

        composeRule.onNodeWithText(string(R.string.sync_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.error_data_network)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).performClick()

        assertEquals(listOf<CourseDetailAction>(CourseDetailAction.Retry), actions)
    }

    @Test
    fun `a course without sections offers a refresh`() {
        show(previewCourseState(CourseDetailContent.Empty))

        composeRule.onNodeWithText(string(R.string.course_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.refresh)).performClick()

        assertEquals(listOf<CourseDetailAction>(CourseDetailAction.Retry), actions)
    }

    @Test
    fun `back button calls onBackClick`() {
        showContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertEquals(1, backClicks)
    }
}
