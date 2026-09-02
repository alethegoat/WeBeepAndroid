package it.polimi.webeep.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.theme.WeBeepTheme

/**
 * view for empty states (no results, no courses, etc.).
 *
 * shows a large icon, a descriptive title, an optional subtitle
 * and an optional action button. all elements appear with a spring
 * animation for a pleasant and modern effect.
 *
 * @param icon icon to show in the center.
 * @param title descriptive title of the empty state.
 * @param subtitle subtitle with additional details. null if not needed.
 * @param actionLabel action button label. null to hide the button.
 * @param onAction callback on action button click.
 * @param modifier modifier for external layout.
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val reduceMotion = LocalReduceMotion.current
    val visibleState = remember {
        MutableTransitionState(reduceMotion).apply {
            targetState = true
        }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(
            animationSpec = WeBeepMotion.enter(reduceMotion = reduceMotion),
        ),
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // icon with spring scale-in animation
            AnimatedVisibility(
                visibleState = remember {
                    MutableTransitionState(false).apply { targetState = true }
                },
                enter = scaleIn(
                    animationSpec = WeBeepMotion.softSpring(),
                ) + fadeIn(WeBeepMotion.enter()),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // title with slide-in animation
            AnimatedVisibility(
                visibleState = remember {
                    MutableTransitionState(false).apply { targetState = true }
                },
                enter = slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = WeBeepMotion.enter(),
                ) + fadeIn(WeBeepMotion.enter()),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            // subtitle
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visibleState = remember {
                        MutableTransitionState(false).apply { targetState = true }
                    },
                    enter = slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = WeBeepMotion.enter(),
                    ) + fadeIn(WeBeepMotion.enter()),
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // action button
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visibleState = remember {
                        MutableTransitionState(false).apply { targetState = true }
                    },
                    enter = scaleIn(
                        animationSpec = WeBeepMotion.softSpring(),
                    ) + fadeIn(WeBeepMotion.enter()),
                ) {
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

// ── previews ─────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Empty State – Nessun Corso")
@Composable
private fun EmptyStateNoCoursePreview() {
    WeBeepTheme(darkTheme = false) {
        EmptyStateView(
            icon = Icons.Outlined.School,
            title = "Nessun corso trovato",
            subtitle = "Non sei iscritto a nessun corso su WeBeep.\nControlla le iscrizioni sul portale.",
            actionLabel = "Aggiorna",
            onAction = {},
        )
    }
}

@Preview(showBackground = true, name = "Empty State – Nessun Risultato")
@Composable
private fun EmptyStateNoResultPreview() {
    WeBeepTheme(darkTheme = false) {
        EmptyStateView(
            icon = Icons.Outlined.SearchOff,
            title = "Nessun risultato",
            subtitle = "Prova a modificare i criteri di ricerca.",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117, name = "Empty State – Dark")
@Composable
private fun EmptyStateDarkPreview() {
    WeBeepTheme(darkTheme = true) {
        EmptyStateView(
            icon = Icons.Outlined.School,
            title = "Nessun corso trovato",
            subtitle = "Non sei iscritto a nessun corso.",
            actionLabel = "Riprova",
            onAction = {},
        )
    }
}
