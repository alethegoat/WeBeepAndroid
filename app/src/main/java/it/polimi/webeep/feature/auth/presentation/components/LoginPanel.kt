package it.polimi.webeep.feature.auth.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme

/** The login card never stretches past this, even on tablets. */
private val LoginCardMaxWidth = 480.dp

private val CardBorderWidth = 1.dp

/** Glass card with the SSO button, plus the "unofficial app" disclaimer under it. */
@Composable
fun LoginPanel(visible: Boolean, isLoading: Boolean, onLoginClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.widthIn(max = LoginCardMaxWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(WeBeepMotion.enter(WeBeepMotion.LONG_MS)) +
                slideInVertically(WeBeepMotion.enter(WeBeepMotion.LONG_MS)) { 40 }
        ) {
            LoginCard(isLoading = isLoading, onLoginClick = onLoginClick)
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(WeBeepMotion.enter(WeBeepMotion.LONG_MS))
        ) {
            Text(
                text = stringResource(R.string.login_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = WeBeepSpacing.md)
            )
        }
    }
}

@Composable
private fun LoginCard(isLoading: Boolean, onLoginClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardShape = MaterialTheme.shapes.extraLarge
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(
                    alpha = if (isDark) 0.28f else 0.72f
                ),
                shape = cardShape
            )
            .border(
                width = CardBorderWidth,
                brush = Brush.linearGradient(
                    listOf(
                        WeBeepTheme.colors.glassHighlight.copy(alpha = if (isDark) 0.28f else 0.78f),
                        WeBeepTheme.colors.glassHighlight.copy(alpha = 0.06f)
                    )
                ),
                shape = cardShape
            )
            .padding(WeBeepSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WeBeepSpacing.lg)
        )
        SsoLoginButton(isLoading = isLoading, onClick = onLoginClick)
    }
}
