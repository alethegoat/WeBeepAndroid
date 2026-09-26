package it.polimi.webeep.feature.settings.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.datastore.SyncFrequency
import it.polimi.webeep.core.datastore.ThemePreference
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.bouncyClickable
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import it.polimi.webeep.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
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
        containerColor = Color.Transparent,
            topBar = {
                val backgroundColor = MaterialTheme.colorScheme.background
                TopAppBar(
                    modifier = Modifier.drawBehind {
                        val fraction = scrollBehavior.state.overlappedFraction.coerceIn(0f, 1f)
                        if (fraction > 0f) {
                            drawRect(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        backgroundColor.copy(alpha = 0.95f * fraction),
                                        backgroundColor.copy(alpha = 0.5f * fraction),
                                        androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                            )
                        }
                    },
                    title = {
                        AnimatedVisibility(
                            visible = scrollBehavior.state.overlappedFraction > 0.5f,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = stringResource(R.string.settings),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.go_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
            Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.xs)
            )

            Spacer(modifier = Modifier.height(WeBeepSpacing.md))

            SettingsSectionTitle(stringResource(R.string.settings_section_appearance))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WeBeepSpacing.md)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(vertical = WeBeepSpacing.xs)
            ) {
                ThemeSelectionRow(
                    selectedTheme = uiState.themePreference,
                    onThemeSelected = { viewModel.setThemePreference(it) }
                )

                SettingsSwitchItem(
                    icon = Icons.Default.ColorLens,
                    title = stringResource(R.string.settings_dynamic_colors_title),
                    subtitle = stringResource(R.string.settings_dynamic_colors_subtitle),
                    checked = uiState.dynamicColorPreference,
                    onCheckedChange = { viewModel.setDynamicColorPreference(it) }
                )
                
                LanguageSelectionRow()
            }

            Spacer(modifier = Modifier.height(WeBeepSpacing.lg))

            SettingsSectionTitle(stringResource(R.string.settings_section_sync))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WeBeepSpacing.md)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(vertical = WeBeepSpacing.xs)
            ) {
                SettingsSwitchItem(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.settings_wifi_only_title),
                    subtitle = stringResource(R.string.settings_wifi_only_subtitle),
                    checked = uiState.wifiOnlySync,
                    onCheckedChange = { viewModel.setWifiOnlySync(it) }
                )

                SyncFrequencyItem(
                    selectedFrequency = uiState.syncFrequency,
                    onFrequencySelected = { viewModel.setSyncFrequency(it) }
                )
            }

            Spacer(modifier = Modifier.height(WeBeepSpacing.lg))

            SettingsSectionTitle(stringResource(R.string.settings_section_account))

            Spacer(modifier = Modifier.height(WeBeepSpacing.xs))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WeBeepSpacing.md)
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .bouncyClickable { showLogoutDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(WeBeepSpacing.xs))
                    Text(
                        text = stringResource(R.string.logout),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(WeBeepSpacing.xl))
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.xs)
    )
}

@Composable
private fun ThemeSelectionRow(
    selectedTheme: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.DarkMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(WeBeepSpacing.md))
            Text(
                text = stringResource(R.string.app_theme),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(WeBeepSpacing.sm))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(WeBeepSpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(WeBeepSpacing.xxs)
        ) {
            val options = listOf(
                ThemePreference.SYSTEM_DEFAULT to stringResource(R.string.settings_theme_system),
                ThemePreference.LIGHT to stringResource(R.string.settings_theme_light),
                ThemePreference.DARK to stringResource(R.string.settings_theme_dark)
            )
            options.forEach { (theme, label) ->
                val isSelected = selectedTheme == theme
                val background = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(background)
                        .bouncyClickable { onThemeSelected(theme) }
                        .padding(vertical = WeBeepSpacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = contentColor
                    )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { onCheckedChange(!checked) }
            .padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(WeBeepSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SyncFrequencyItem(
    selectedFrequency: SyncFrequency,
    onFrequencySelected: (SyncFrequency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val labels = mapOf(
        SyncFrequency.FIFTEEN_MINUTES to stringResource(R.string.settings_sync_freq_15m),
        SyncFrequency.ONE_HOUR to stringResource(R.string.settings_sync_freq_1h),
        SyncFrequency.TWELVE_HOURS to stringResource(R.string.settings_sync_freq_12h),
        SyncFrequency.DAILY to stringResource(R.string.settings_sync_freq_daily),
        SyncFrequency.NEVER to stringResource(R.string.settings_sync_freq_never)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { expanded = true }
            .padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(WeBeepSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.update_frequency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = labels[selectedFrequency] ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.settings_cd_expand),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            SyncFrequency.entries.forEach { frequency ->
                DropdownMenuItem(
                    text = { Text(labels[frequency] ?: "") },
                    onClick = {
                        onFrequencySelected(frequency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageSelectionRow() {
    var expanded by remember { mutableStateOf(false) }
    
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (currentLocales.isEmpty) "system" else currentLocales.get(0)?.language ?: "system"

    val labels = mapOf(
        "system" to "Sistema / System",
        "it" to "Italiano",
        "en" to "English"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { expanded = true }
            .padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(WeBeepSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_app_language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = labels[currentTag] ?: "Sistema",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.settings_cd_expand),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            listOf("system", "it", "en").forEach { tag ->
                DropdownMenuItem(
                    text = { Text(labels[tag] ?: "") },
                    onClick = {
                        expanded = false
                        if (tag == "system") {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        } else {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        }
                    }
                )
            }
        }
    }
}
