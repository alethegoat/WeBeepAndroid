package it.polimi.webeep.feature.settings.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepSpacing

private val SettingsRowMinHeight = WeBeepSpacing.touchTarget + WeBeepSpacing.xs

@Composable
fun SettingsSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
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
fun SettingsRow(
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

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier.toggleable(
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
fun <T> SettingsDropdownRow(
    icon: ImageVector,
    title: String,
    options: List<Pair<T, String>>,
    selected: String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
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
