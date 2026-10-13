package it.polimi.webeep.feature.dashboard.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.polimi.webeep.R
import it.polimi.webeep.util.setAppContent
import it.polimi.webeep.util.string
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<DashboardAction>()
    private val openedCourses = mutableListOf<Int>()
    private var settingsClicks = 0

    private fun show(state: DashboardUiState) {
        composeRule.setAppContent {
            DashboardScreen(
                state = state,
                onAction = { actions += it },
                onCourseClick = { openedCourses += it },
                onSettingsClick = { settingsClicks++ }
            )
        }
    }

    @Test
    fun `loading shows the skeleton and no courses`() {
        show(DashboardUiState())

        composeRule.onNodeWithTag(TestTags.COURSE_SKELETON).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.search_courses_placeholder)).assertDoesNotExist()
    }

    @Test
    fun `content shows the course titles and the greeting`() {
        show(previewContentState(previewCourses))

        composeRule.onNodeWithText("Technologies for Information Systems").assertIsDisplayed()
        composeRule.onNodeWithText("Mathematical Analysis 1").assertExists()
        composeRule.onNodeWithText("Alessio Rossi").assertIsDisplayed()
    }

    @Test
    fun `typing in the search field sends the query`() {
        show(previewContentState(previewCourses))

        composeRule.onNode(hasSetTextAction()).performTextInput("math")

        assertEquals(listOf<DashboardAction>(DashboardAction.SearchQueryChange("math")), actions)
    }

    @Test
    fun `choosing a filter sends it`() {
        show(previewContentState(previewCourses))

        composeRule.onNodeWithText(string(R.string.filter_past)).performClick()

        assertEquals(listOf<DashboardAction>(DashboardAction.FilterChange(CourseFilter.PAST)), actions)
    }

    @Test
    fun `choosing the filter already selected sends nothing`() {
        show(previewContentState(previewCourses))

        composeRule.onNodeWithText(string(R.string.filter_active)).performClick()

        assertEquals(emptyList<DashboardAction>(), actions)
    }

    @Test
    fun `no courses at all offers a refresh`() {
        show(previewContentState(emptyList()))

        composeRule.onNodeWithText(string(R.string.no_courses_found)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.no_courses_enrolled)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.refresh)).performClick()

        assertEquals(listOf<DashboardAction>(DashboardAction.Refresh), actions)
    }

    @Test
    fun `filters hiding every course offer to reset them`() {
        val state = DashboardUiState(
            content = DashboardContent.Success(courses = previewCourses, visibleCourses = emptyList()),
            searchQuery = "zzz"
        )
        show(state)

        composeRule.onNodeWithText(string(R.string.try_changing_filters)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.reset_filters)).performClick()

        assertEquals(listOf<DashboardAction>(DashboardAction.ResetFilters), actions)
    }

    @Test
    fun `error shows its message and retry works`() {
        show(DashboardUiState(content = DashboardContent.Error(R.string.error_data_network)))

        composeRule.onNodeWithText(string(R.string.dashboard_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.error_data_network)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.retry)).performClick()

        assertEquals(listOf<DashboardAction>(DashboardAction.Retry), actions)
    }

    @Test
    fun `offline with cached courses shows the cache banner`() {
        show(previewContentState(previewCourses, isOnline = false))

        composeRule.onNodeWithText(string(R.string.offline_cache_message)).assertIsDisplayed()
    }

    @Test
    fun `offline without cache says there is no connection`() {
        show(previewContentState(emptyList(), isOnline = false))

        composeRule.onNodeWithText(string(R.string.offline_no_connection_message)).assertIsDisplayed()
    }

    @Test
    fun `online hides the offline banner`() {
        show(previewContentState(previewCourses))

        composeRule.onNodeWithText(string(R.string.offline_cache_message)).assertDoesNotExist()
    }

    @Test
    fun `clicking a course opens it by id`() {
        show(previewContentState(previewCourses))

        composeRule.onNodeWithText("Mathematical Analysis 1").performClick()

        assertEquals(listOf(2), openedCourses)
    }

    @Test
    fun `settings button calls the settings lambda`() {
        show(previewContentState(previewCourses))

        composeRule.onNodeWithContentDescription(string(R.string.cd_settings)).performClick()

        assertEquals(1, settingsClicks)
    }
}
