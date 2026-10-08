package it.polimi.webeep.feature.settings.presentation

import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.polimi.webeep.R
import it.polimi.webeep.core.datastore.SyncFrequency
import it.polimi.webeep.core.datastore.ThemePreference
import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.core.ui.components.GroupedListGap
import it.polimi.webeep.core.ui.components.GroupedListItem
import it.polimi.webeep.core.ui.theme.WeBeepSpacing

/**
 * Settings as grouped lists (same language as the course content) under a large
 * title that collapses into the app bar while scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastCheckAt by viewModel.lastCheckAt.collectAsStateWithLifecycle()
    val checkInProgress by viewModel.checkInProgress.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.settings_logout_dialog_title)) },
            text = { Text(stringResource(R.string.settings_logout_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onComplete = onLogoutSuccess)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_logout_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.settings_logout_dialog_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        // The scroll area spans the window; the groups stay readable on wide screens.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                SettingsSectionTitle(stringResource(R.string.settings_section_appearance))
                GroupedListItem(position = GroupPosition.Top) {
                    ThemeSelectionRow(
                        selectedTheme = uiState.themePreference,
                        onThemeSelected = { viewModel.setThemePreference(it) }
                    )
                }
                GroupedListItem(position = GroupPosition.Middle) {
                    SettingsSwitchItem(
                        icon = Icons.Rounded.ColorLens,
                        title = stringResource(R.string.settings_dynamic_colors_title),
                        subtitle = stringResource(R.string.settings_dynamic_colors_subtitle),
                        checked = uiState.dynamicColorPreference,
                        onCheckedChange = { viewModel.setDynamicColorPreference(it) }
                    )
                }
                GroupedListItem(position = GroupPosition.Bottom) {
                    LanguageSelectionRow()
                }

                SettingsSectionTitle(stringResource(R.string.settings_section_sync))
                GroupedListItem(position = GroupPosition.Top) {
                    SettingsSwitchItem(
                        icon = Icons.Rounded.Wifi,
                        title = stringResource(R.string.settings_wifi_only_title),
                        subtitle = stringResource(R.string.settings_wifi_only_subtitle),
                        checked = uiState.wifiOnlySync,
                        onCheckedChange = { viewModel.setWifiOnlySync(it) }
                    )
                }
                GroupedListItem(position = GroupPosition.Middle) {
                    SyncFrequencyItem(
                        selectedFrequency = uiState.syncFrequency,
                        onFrequencySelected = { viewModel.setSyncFrequency(it) }
                    )
                }
                GroupedListItem(position = GroupPosition.Bottom) {
                    LastCheckItem(
                        lastCheckAt = lastCheckAt,
                        checkInProgress = checkInProgress,
                        onCheckNow = viewModel::checkNow
                    )
                }

                SettingsSectionTitle(stringResource(R.string.settings_section_account))
                GroupedListItem(position = GroupPosition.Single) {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        title = stringResource(R.string.logout),
                        accent = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable(role = Role.Button) { showLogoutDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(WeBeepSpacing.xl))
            }
        }
    }
}

private val SettingsMaxWidth = 640.dp

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = WeBeepSpacing.md,
            end = WeBeepSpacing.md,
            top = WeBeepSpacing.lg,
            bottom = WeBeepSpacing.xs
        )
    )
}

/**
 * Icon, title and optional subtitle, with [trailing] content at the end. [accent]
 * colors both icon and title, e.g. error for destructive actions.
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SettingsRowMinHeight)
            .padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(WeBeepSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = accent ?: MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}

private val SettingsRowMinHeight = WeBeepSpacing.touchTarget + WeBeepSpacing.xs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionRow(
    selectedTheme: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(modifier = Modifier.padding(bottom = WeBeepSpacing.md)) {
        SettingsRow(icon = Icons.Rounded.DarkMode, title = stringResource(R.string.app_theme))
        val options = listOf(
            ThemePreference.SYSTEM_DEFAULT to stringResource(R.string.settings_theme_system),
            ThemePreference.LIGHT to stringResource(R.string.settings_theme_light),
            ThemePreference.DARK to stringResource(R.string.settings_theme_dark)
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WeBeepSpacing.md)
        ) {
            options.forEachIndexed { index, (theme, label) ->
                SegmentedButton(
                    selected = selectedTheme == theme,
                    onClick = {
                        if (selectedTheme != theme) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onThemeSelected(theme)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(text = label, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange
        )
    ) {
        Spacer(modifier = Modifier.width(WeBeepSpacing.sm))
        // Row owns the toggle semantics; unchecked thumb/border use onSurfaceVariant
        // so the "off" state stays readable on dark containers.
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

/** A row that opens a dropdown of [options]; [selected] is shown as its subtitle. */
@Composable
private fun <T> SettingsDropdownRow(
    icon: ImageVector,
    title: String,
    options: List<Pair<T, String>>,
    selected: String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SettingsRow(
            icon = icon,
            title = title,
            subtitle = selected,
            modifier = Modifier.clickable(role = Role.DropdownList) { expanded = true }
        ) {
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = stringResource(R.string.settings_cd_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun SyncFrequencyItem(
    selectedFrequency: SyncFrequency,
    onFrequencySelected: (SyncFrequency) -> Unit
) {
    val options = listOf(
        SyncFrequency.FIFTEEN_MINUTES to stringResource(R.string.settings_sync_freq_15m),
        SyncFrequency.ONE_HOUR to stringResource(R.string.settings_sync_freq_1h),
        SyncFrequency.TWELVE_HOURS to stringResource(R.string.settings_sync_freq_12h),
        SyncFrequency.DAILY to stringResource(R.string.settings_sync_freq_daily),
        SyncFrequency.NEVER to stringResource(R.string.settings_sync_freq_never)
    )
    SettingsDropdownRow(
        icon = Icons.Rounded.Sync,
        title = stringResource(R.string.update_frequency),
        options = options,
        selected = options.firstOrNull { it.first == selectedFrequency }?.second.orEmpty(),
        onSelect = onFrequencySelected
    )
}

@Composable
private fun LastCheckItem(
    lastCheckAt: Long?,
    checkInProgress: Boolean,
    onCheckNow: () -> Unit
) {
    val status = when {
        checkInProgress -> stringResource(R.string.settings_check_started)
        lastCheckAt != null -> stringResource(
            R.string.settings_last_check_at,
            DateUtils.getRelativeTimeSpanString(lastCheckAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        )
        else -> stringResource(R.string.settings_last_check_never)
    }
    SettingsRow(
        icon = Icons.Rounded.Notifications,
        title = stringResource(R.string.settings_last_check_title),
        subtitle = status
    ) {
        TextButton(
            onClick = onCheckNow,
            enabled = !checkInProgress,
            modifier = Modifier.heightIn(min = WeBeepSpacing.touchTarget)
        ) {
            Text(stringResource(R.string.settings_check_now))
        }
    }
}

@Composable
private fun LanguageSelectionRow() {
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (currentLocales.isEmpty) "system" else currentLocales.get(0)?.language ?: "system"
    val options = listOf(
        "system" to "Sistema / System",
        "it" to "Italiano",
        "en" to "English"
    )
    SettingsDropdownRow(
        icon = Icons.Rounded.Language,
        title = stringResource(R.string.settings_app_language),
        options = options,
        selected = options.firstOrNull { it.first == currentTag }?.second ?: options.first().second,
        onSelect = { tag ->
            if (tag == "system") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
    )
}
