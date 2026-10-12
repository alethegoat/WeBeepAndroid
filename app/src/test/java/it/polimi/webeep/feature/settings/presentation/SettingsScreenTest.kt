package it.polimi.webeep.feature.settings.presentation

import android.text.format.DateUtils
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.polimi.webeep.R
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import it.polimi.webeep.util.setAppContent
import it.polimi.webeep.util.string
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A tall window, so every row of the settings list is on screen. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w400dp-h1600dp")
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<SettingsAction>()
    private var backClicks = 0

    private fun show(state: SettingsUiState = SettingsUiState()) {
        composeRule.setAppContent {
            SettingsScreen(state = state, onAction = { actions += it }, onBackClick = { backClicks++ })
        }
    }

    private fun confirmButton() = composeRule.onNode(
        hasText(string(R.string.settings_logout_dialog_confirm)) and hasAnyAncestor(isDialog())
    )

    @Test
    fun `defaults are reflected by every row`() {
        show()

        composeRule.onNodeWithText(string(R.string.settings_theme_system)).assertIsSelected()
        composeRule.onNodeWithText(string(R.string.settings_theme_light)).assertIsNotSelected()
        composeRule.onNodeWithText(string(R.string.settings_theme_dark)).assertIsNotSelected()
        composeRule.onNodeWithText(string(R.string.settings_dynamic_colors_title)).assertIsOff()
        composeRule.onNodeWithText(string(R.string.settings_wifi_only_title)).assertIsOff()
        composeRule.onNodeWithText(string(R.string.settings_sync_freq_1h)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_last_check_never)).assertIsDisplayed()
    }

    @Test
    fun `rows reflect a customised state`() {
        val twoHoursAgo = System.currentTimeMillis() - 2 * DateUtils.HOUR_IN_MILLIS
        show(
            SettingsUiState(
                themePreference = ThemePreference.DARK,
                dynamicColorPreference = true,
                wifiOnlySync = true,
                syncFrequency = SyncFrequency.DAILY,
                lastCheckAt = twoHoursAgo
            )
        )

        val relative = DateUtils.getRelativeTimeSpanString(twoHoursAgo, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)

        composeRule.onNodeWithText(string(R.string.settings_theme_dark)).assertIsSelected()
        composeRule.onNodeWithText(string(R.string.settings_theme_system)).assertIsNotSelected()
        composeRule.onNodeWithText(string(R.string.settings_dynamic_colors_title)).assertIsOn()
        composeRule.onNodeWithText(string(R.string.settings_wifi_only_title)).assertIsOn()
        composeRule.onNodeWithText(string(R.string.settings_sync_freq_daily)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_last_check_at, relative)).assertIsDisplayed()
    }

    @Test
    fun `choosing a theme sends it`() {
        show()

        composeRule.onNodeWithText(string(R.string.settings_theme_dark)).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.ThemeChange(ThemePreference.DARK)), actions)
    }

    @Test
    fun `choosing the theme already selected sends nothing`() {
        show()

        composeRule.onNodeWithText(string(R.string.settings_theme_system)).performClick()

        assertEquals(emptyList<SettingsAction>(), actions)
    }

    @Test
    fun `toggling dynamic colors sends the new value`() {
        show()

        composeRule.onNodeWithText(string(R.string.settings_dynamic_colors_title)).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.DynamicColorChange(true)), actions)
    }

    @Test
    fun `toggling wifi only sends the new value in both directions`() {
        show(SettingsUiState(wifiOnlySync = true))

        composeRule.onNodeWithText(string(R.string.settings_wifi_only_title)).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.WifiOnlyChange(false)), actions)
    }

    @Test
    fun `choosing a frequency from the menu sends it`() {
        show()

        composeRule.onNodeWithText(string(R.string.update_frequency)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_sync_freq_15m)).performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.FrequencyChange(SyncFrequency.FIFTEEN_MINUTES)), actions)
    }

    @Test
    fun `check now sends its action`() {
        show()

        composeRule.onNodeWithText(string(R.string.settings_check_now)).assertIsEnabled().performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.CheckNow), actions)
    }

    @Test
    fun `a check in progress says so and disables check now`() {
        show(SettingsUiState(checkInProgress = true))

        composeRule.onNodeWithText(string(R.string.settings_check_started)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_check_now)).assertIsNotEnabled()
    }

    @Test
    fun `logout asks for confirmation before doing anything`() {
        show()

        composeRule.onNodeWithText(string(R.string.logout)).performClick()

        composeRule.onNodeWithText(string(R.string.settings_logout_dialog_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_logout_dialog_message)).assertIsDisplayed()
        assertEquals(emptyList<SettingsAction>(), actions)
    }

    @Test
    fun `confirming the dialog logs out and closes it`() {
        show()
        composeRule.onNodeWithText(string(R.string.logout)).performClick()

        confirmButton().performClick()

        assertEquals(listOf<SettingsAction>(SettingsAction.Logout), actions)
        composeRule.onNodeWithText(string(R.string.settings_logout_dialog_title)).assertDoesNotExist()
    }

    @Test
    fun `cancelling the dialog dismisses it without logging out`() {
        show()
        composeRule.onNodeWithText(string(R.string.logout)).performClick()

        composeRule.onNodeWithText(string(R.string.settings_logout_dialog_cancel)).performClick()

        assertEquals(emptyList<SettingsAction>(), actions)
        composeRule.onNodeWithText(string(R.string.settings_logout_dialog_title)).assertDoesNotExist()
    }

    @Test
    fun `back button calls onBackClick`() {
        show()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertEquals(1, backClicks)
    }
}
