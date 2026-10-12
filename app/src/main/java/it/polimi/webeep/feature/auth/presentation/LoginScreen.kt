package it.polimi.webeep.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import it.polimi.webeep.core.ui.components.AuroraBackground
import it.polimi.webeep.core.ui.components.WeBeepSnackbarHost
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.currentWindowSize
import it.polimi.webeep.feature.auth.presentation.components.BrandHeader
import it.polimi.webeep.feature.auth.presentation.components.LoginPanel
import kotlinx.coroutines.delay

private const val CARD_ENTRANCE_DELAY_MS = 120L

/** Whether the logo and the card have entered yet. */
private data class LoginEntrance(val logoVisible: Boolean, val cardVisible: Boolean)

@Composable
private fun rememberLoginEntrance(): LoginEntrance {
    val reduceMotion = LocalReduceMotion.current
    // Saved: the entrance plays once, not again on rotation or language change.
    var logoVisible by rememberSaveable { mutableStateOf(reduceMotion) }
    var cardVisible by rememberSaveable { mutableStateOf(reduceMotion) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            logoVisible = true
            cardVisible = true
        } else {
            logoVisible = true
            delay(CARD_ENTRANCE_DELAY_MS)
            cardVisible = true
        }
    }
    return LoginEntrance(logoVisible = logoVisible, cardVisible = cardVisible)
}

/**
 * Brand on top and the SSO card below; on wide landscape windows (phones turned
 * sideways, tablets) they sit side by side. The content scrolls when the window
 * is too short for it and stays centered otherwise.
 */
@Composable
fun LoginScreen(state: LoginUiState, snackbarHostState: SnackbarHostState, onLoginClick: () -> Unit, modifier: Modifier = Modifier) {
    val entrance = rememberLoginEntrance()
    val isLoading = state is LoginUiState.Loading

    AuroraBackground(modifier = modifier, animated = true) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { WeBeepSnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // At least as tall as the window, so short content can still center.
                val contentModifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .padding(horizontal = WeBeepSpacing.lg, vertical = WeBeepSpacing.lg)
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    LoginContent(
                        entrance = entrance,
                        isLoading = isLoading,
                        onLoginClick = onLoginClick,
                        modifier = contentModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginContent(entrance: LoginEntrance, isLoading: Boolean, onLoginClick: () -> Unit, modifier: Modifier = Modifier) {
    if (currentWindowSize().isWideLandscape) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WeBeepSpacing.xl, Alignment.CenterHorizontally)
        ) {
            BrandHeader(visible = entrance.logoVisible, modifier = Modifier.weight(1f, fill = false))
            LoginPanel(
                visible = entrance.cardVisible,
                isLoading = isLoading,
                onLoginClick = onLoginClick,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandHeader(visible = entrance.logoVisible)
            Spacer(modifier = Modifier.height(WeBeepSpacing.xl))
            LoginPanel(visible = entrance.cardVisible, isLoading = isLoading, onLoginClick = onLoginClick)
        }
    }
}

@Composable
private fun PreviewScreen(state: LoginUiState) {
    WeBeepTheme {
        LoginScreen(
            state = state,
            snackbarHostState = remember { SnackbarHostState() },
            onLoginClick = {}
        )
    }
}

@Preview(name = "Idle")
@Composable
private fun LoginScreenPreviewIdle() {
    PreviewScreen(LoginUiState.Idle)
}

@Preview(name = "Loading")
@Composable
private fun LoginScreenPreviewLoading() {
    PreviewScreen(LoginUiState.Loading)
}
