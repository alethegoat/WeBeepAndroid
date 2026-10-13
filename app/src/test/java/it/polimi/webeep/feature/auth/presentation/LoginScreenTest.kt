package it.polimi.webeep.feature.auth.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.showErrorSnackbar
import it.polimi.webeep.util.setAppContent
import it.polimi.webeep.util.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var loginClicks = 0
    private val snackbarHostState = SnackbarHostState()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun show(state: LoginUiState) {
        composeRule.setAppContent {
            LoginScreen(state = state, snackbarHostState = snackbarHostState, onLoginClick = { loginClicks++ })
        }
    }

    @Test
    fun `idle shows the SSO button and the subtitle`() {
        show(LoginUiState.Idle)

        composeRule.onNodeWithText(string(R.string.login_sso_button)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.login_subtitle)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.login_disclaimer)).assertIsDisplayed()
    }

    @Test
    fun `clicking the SSO button calls onLoginClick`() {
        show(LoginUiState.Idle)

        composeRule.onNodeWithText(string(R.string.login_sso_button)).performClick()

        assertEquals(1, loginClicks)
    }

    @Test
    fun `loading swaps the SSO button for a spinner`() {
        show(LoginUiState.Loading)

        composeRule.onNodeWithText(string(R.string.login_sso_button)).assertDoesNotExist()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun `a message shown through the host appears with its retry action`() {
        show(LoginUiState.Idle)

        composeRule.runOnUiThread {
            scope.launch { showErrorSnackbar(snackbarHostState, message = "Token non valido", actionLabel = "Riprova") }
        }

        composeRule.onNodeWithText("Token non valido").assertIsDisplayed()
        composeRule.onNodeWithText("Riprova").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.cd_dismiss)).assertIsDisplayed()
    }
}
