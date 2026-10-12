package it.polimi.webeep.feature.settings.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import it.polimi.webeep.core.ui.util.LocalNavAnimatedVisibilityScope
import it.polimi.webeep.navigation.Settings

/** Registers the settings destination; back is dropped unless the entry is resumed. */
fun NavGraphBuilder.settingsDestination(onBack: () -> Unit, onLoggedOut: () -> Unit) {
    composable<Settings> {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            SettingsRoute(
                onBackClick = dropUnlessResumed(block = onBack),
                onLoggedOut = onLoggedOut
            )
        }
    }
}
