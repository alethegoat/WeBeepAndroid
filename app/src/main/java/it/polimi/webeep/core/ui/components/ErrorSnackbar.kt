package it.polimi.webeep.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.theme.WeBeepTheme

/**
 * data to display an error snackbar.
 *
 * @param message error message to show.
 * @param actionLabel label for the action button (e.g. "retry"). null if no action.
 * @param duration display duration.
 */
data class WeBeepSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals

/**
 * custom snackbar host for webeep with material3 error style.
 *
 * each snackbar appears from the bottom with slide + fade animation,
 * and uses the `error`/`onerror` colors from the current theme.
 *
 * @param hostState snackbar state shared with the caller.
 * @param modifier modifier for positioning and layout.
 */
@Composable
fun WeBeepSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            WeBeepSnackbar(snackbarData = snackbarData)
        },
    )
}

/**
 * single snackbar styled with error colors and rounded shape.
 *
 * @param snackbarData current snackbar data.
 */
@Composable
private fun WeBeepSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val isError = snackbarData.visuals is WeBeepSnackbarVisuals

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        Snackbar(
            modifier = modifier.padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.inverseSurface
            },
            contentColor = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.inverseOnSurface
            },
            actionContentColor = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.inversePrimary
            },
            dismissActionContentColor = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.inverseOnSurface
            },
            action = snackbarData.visuals.actionLabel?.let { label ->
                {
                    TextButton(onClick = { snackbarData.performAction() }) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            },
            dismissAction = if (snackbarData.visuals.withDismissAction) {
                {
                    TextButton(onClick = { snackbarData.dismiss() }) {
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            } else null,
        ) {
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * shows an error snackbar with retry option.
 *
 * utility function to simplify snackbar usage:
 * ```kotlin
 * showErrorSnackbar(
 *     snackbarHostState = snackbarHostState,
 *     message = "connection failed",
 *     actionLabel = "retry",
 *     onAction = { viewModel.retry() },
 * )
 * ```
 *
 * @param snackbarHostState state of the snackbar host.
 * @param message error message.
 * @param actionLabel button label (default: "retry").
 * @param onAction callback executed on action click.
 * @param onDismiss callback executed on dismissal without action.
 */
suspend fun showErrorSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
    actionLabel: String = "Riprova",
    onAction: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val result = snackbarHostState.showSnackbar(
        WeBeepSnackbarVisuals(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
        ),
    )
    when (result) {
        SnackbarResult.ActionPerformed -> onAction()
        SnackbarResult.Dismissed -> onDismiss()
    }
}

// ── previews ─────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Error Snackbar – Light")
@Composable
private fun ErrorSnackbarLightPreview() {
    WeBeepTheme(darkTheme = false) {
        val hostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            hostState.showSnackbar(
                WeBeepSnackbarVisuals(
                    message = "Errore di connessione al server",
                    actionLabel = "Riprova",
                ),
            )
        }
        WeBeepSnackbarHost(hostState = hostState)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117, name = "Error Snackbar – Dark")
@Composable
private fun ErrorSnackbarDarkPreview() {
    WeBeepTheme(darkTheme = true) {
        val hostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            hostState.showSnackbar(
                WeBeepSnackbarVisuals(
                    message = "Impossibile caricare i corsi",
                    actionLabel = "Riprova",
                ),
            )
        }
        WeBeepSnackbarHost(hostState = hostState)
    }
}
