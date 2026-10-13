package it.polimi.webeep.feature.settings.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.core.ui.components.GroupedListGap
import it.polimi.webeep.core.ui.components.GroupedListItem

/** The destructive account actions; for now only logout. */
@Composable
fun AccountSection(onLogoutClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GroupedListGap)) {
        SettingsSectionTitle(stringResource(R.string.settings_section_account))
        GroupedListItem(position = GroupPosition.SINGLE) {
            SettingsRow(
                icon = Icons.AutoMirrored.Rounded.Logout,
                title = stringResource(R.string.logout),
                accent = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable(role = Role.Button, onClick = onLogoutClick)
            )
        }
    }
}
