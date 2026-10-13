package it.polimi.webeep.feature.settings.presentation.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.core.ui.components.GroupedListGap
import it.polimi.webeep.core.ui.components.GroupedListItem
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.feature.settings.domain.model.ThemePreference

/** Theme, dynamic colors and app language. */
@Composable
fun AppearanceSection(
    themePreference: ThemePreference,
    dynamicColorPreference: Boolean,
    onThemeSelect: (ThemePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GroupedListGap)) {
        SettingsSectionTitle(stringResource(R.string.settings_section_appearance))
        GroupedListItem(position = GroupPosition.TOP) {
            ThemeSelectionRow(selectedTheme = themePreference, onThemeSelect = onThemeSelect)
        }
        GroupedListItem(position = GroupPosition.MIDDLE) {
            SettingsSwitchItem(
                icon = Icons.Rounded.ColorLens,
                title = stringResource(R.string.settings_dynamic_colors_title),
                subtitle = stringResource(R.string.settings_dynamic_colors_subtitle),
                checked = dynamicColorPreference,
                onCheckedChange = onDynamicColorChange
            )
        }
        GroupedListItem(position = GroupPosition.BOTTOM) {
            LanguageSelectionRow()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionRow(selectedTheme: ThemePreference, onThemeSelect: (ThemePreference) -> Unit) {
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
                            onThemeSelect(theme)
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
