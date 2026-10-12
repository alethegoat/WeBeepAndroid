package it.polimi.webeep.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.components.GroupedListGap
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.feature.settings.presentation.components.AccountSection
import it.polimi.webeep.feature.settings.presentation.components.AppearanceSection
import it.polimi.webeep.feature.settings.presentation.components.LogoutConfirmDialog
import it.polimi.webeep.feature.settings.presentation.components.SettingsTopBar
import it.polimi.webeep.feature.settings.presentation.components.SyncSection

private val SettingsMaxWidth = 640.dp

/**
 * Settings as grouped lists (same language as the course content) under a large
 * title that collapses into the app bar while scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: SettingsUiState, onAction: (SettingsAction) -> Unit, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                onAction(SettingsAction.Logout)
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SettingsTopBar(scrollBehavior = scrollBehavior, onBackClick = onBackClick) }
    ) { innerPadding ->
        // The scroll area spans the window; the groups stay readable on wide screens.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = SettingsMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = WeBeepSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GroupedListGap)
            ) {
                AppearanceSection(
                    themePreference = state.themePreference,
                    dynamicColorPreference = state.dynamicColorPreference,
                    onThemeSelect = { onAction(SettingsAction.ThemeChange(it)) },
                    onDynamicColorChange = { onAction(SettingsAction.DynamicColorChange(it)) }
                )
                SyncSection(
                    wifiOnlySync = state.wifiOnlySync,
                    syncFrequency = state.syncFrequency,
                    lastCheckAt = state.lastCheckAt,
                    checkInProgress = state.checkInProgress,
                    onWifiOnlyChange = { onAction(SettingsAction.WifiOnlyChange(it)) },
                    onFrequencySelect = { onAction(SettingsAction.FrequencyChange(it)) },
                    onCheckNowClick = { onAction(SettingsAction.CheckNow) }
                )
                AccountSection(onLogoutClick = { showLogoutDialog = true })
                Spacer(modifier = Modifier.height(WeBeepSpacing.xl))
            }
        }
    }
}

@Composable
private fun PreviewScreen(state: SettingsUiState) {
    WeBeepTheme {
        SettingsScreen(state = state, onAction = {}, onBackClick = {})
    }
}

@Preview(name = "Default")
@Composable
private fun SettingsScreenPreviewDefault() {
    PreviewScreen(SettingsUiState())
}

@Preview(name = "Check in progress")
@Composable
private fun SettingsScreenPreviewCheckInProgress() {
    PreviewScreen(SettingsUiState(lastCheckAt = PREVIEW_LAST_CHECK, checkInProgress = true))
}

@Preview(name = "Last check shown")
@Composable
private fun SettingsScreenPreviewLastCheck() {
    PreviewScreen(SettingsUiState(wifiOnlySync = true, lastCheckAt = PREVIEW_LAST_CHECK))
}

// A fixed instant keeps the preview stable; it is rendered relative to now.
private const val PREVIEW_LAST_CHECK = 1_700_000_000_000L
