package it.polimi.webeep.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion

/** Size of the tonal tile that hosts the empty-state icon. */
private val EmptyStateIconContainerSize = 88.dp

/** Size of the icon inside [EmptyStateIconContainerSize]. */
private val EmptyStateIconSize = 40.dp

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
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
            animationSpec = WeBeepMotion.enter(reduceMotion = reduceMotion)
        ) + slideInVertically(
            initialOffsetY = { it / 12 },
            animationSpec = WeBeepMotion.enter(reduceMotion = reduceMotion)
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = WeBeepSpacing.xl, vertical = WeBeepSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EmptyStateIcon(icon = icon)

            Spacer(modifier = Modifier.height(WeBeepSpacing.lg))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(WeBeepSpacing.xs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(WeBeepSpacing.lg))
                FilledTonalButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(EmptyStateIconContainerSize)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(EmptyStateIconSize),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
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
            onAction = {}
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
            subtitle = "Prova a modificare i criteri di ricerca."
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
            onAction = {}
        )
    }
}
